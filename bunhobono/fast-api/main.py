from contextlib import asynccontextmanager
import asyncio
import os
from pathlib import Path
import shutil
import threading

import httpx
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse

from plate_ocr import PlateOCR
from plate_preprocess_standard_v2 import make_candidates
from ocr_selector import select_best_ocr
from test_stream import TEST_STREAMS, TestStreamWorker
from yolo_detect import PlateDetector


BASE_DIR = Path(__file__).resolve().parent

SPRING_URL = os.getenv(
    "SPRING_URL",
    "http://localhost:80/api/camera-data/ocr",
)

PREPROCESS_DIR = BASE_DIR / "runtime" / "preprocess"
PREPROCESS_DIR.mkdir(parents=True, exist_ok=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.spring_client = httpx.AsyncClient(
        timeout=httpx.Timeout(10.0, connect=3.0)
    )
    app.state.plate_detector = PlateDetector()
    app.state.plate_ocr = PlateOCR(device="cpu")

    model_lock = threading.Lock()
    workers = {}
    threads = []

    for stream in TEST_STREAMS:
        worker = TestStreamWorker(
            stream=stream,
            detector=app.state.plate_detector,
            ocr_reader=app.state.plate_ocr,
            model_lock=model_lock,
        )

        for camera in stream["cameras"]:
            workers[camera["cameraNo"]] = worker

        thread = worker.start()
        threads.append(thread)

    app.state.stream_workers = workers
    app.state.stream_threads = threads
    print("스트리밍 카메라 시작:", list(workers.keys()))

    yield

    for worker in set(workers.values()):
        worker.stop()
    for worker in set(workers.values()):
        if worker.thread is not None:
            worker.thread.join(timeout=3)
    await app.state.spring_client.aclose()


app = FastAPI(title="Parking API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def run_ocr_pipeline(crop_path: str):
    crop_path_obj = Path(crop_path)
    prefix = crop_path_obj.stem

    output_dir = PREPROCESS_DIR / prefix

    if output_dir.exists():
        shutil.rmtree(output_dir)

    output_dir.mkdir(parents=True, exist_ok=True)

    candidates = make_candidates(
        image_path=crop_path,
        output_dir=output_dir,
        prefix=prefix
    )

    ocr_reader = app.state.plate_ocr
    ocr_candidates = ocr_reader.read_candidates(candidates)

    ocr_result = select_best_ocr(ocr_candidates)

    return {
        "result": ocr_result,
        "candidates": ocr_candidates
    }


@app.get("/")
def home():
    return {
        "msg": "Fast API",
        "spring_url": SPRING_URL,
        "cameras": list(app.state.stream_workers.keys()),
    }


async def generate_mjpeg(worker):
    worker.add_viewer()
    try:
        while worker.running:
            jpeg = worker.get_jpeg_frame()
            if jpeg is None:
                await asyncio.sleep(0.05)
                continue

            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + jpeg
                + b"\r\n"
            )

            if worker.video_finished:
                break

            await asyncio.sleep(0.03)
    finally:
        worker.remove_viewer()


def get_worker(camera_no):
    worker = app.state.stream_workers.get(camera_no)
    if worker is None:
        raise HTTPException(
            status_code=404,
            detail=f"카메라 {camera_no}번을 찾을 수 없습니다.",
        )
    return worker


@app.get("/cctv/{camera_no}/stream")
async def cctv_stream(camera_no: int):
    return StreamingResponse(
        generate_mjpeg(get_worker(camera_no)),
        media_type="multipart/x-mixed-replace; boundary=frame",
    )


@app.get("/cctv/{camera_no}/status")
def cctv_status(camera_no: int):
    worker = get_worker(camera_no)
    return {
        "cameraNo": camera_no,
        "paused": worker.pause_event.is_set(),
        "autoPaused": worker.auto_paused,
        "pauseReason": worker.pause_reason,
        "viewerCount": worker.viewer_count,
        "videoFinished": worker.video_finished,
        "ocrEventId": worker.ocr_event_id,
        "lastOcrCarNo": worker.last_ocr_car_no,
        "pendingCameraDataNo": worker.pending_camera_data_no,
    }


@app.post("/cctv/{camera_no}/pause")
def pause_stream(camera_no: int):
    get_worker(camera_no).pause()
    return {"success": True, "cameraNo": camera_no, "paused": True}


@app.post("/cctv/{camera_no}/resume")
def resume_stream(camera_no: int):
    get_worker(camera_no).resume()
    return {"success": True, "cameraNo": camera_no, "paused": False}


@app.post("/cctv/{camera_no}/complete")
def complete_pending_stream(camera_no: int):
    worker = get_worker(camera_no)
    worker.complete_pending()
    return {
        "success": True,
        "cameraNo": camera_no,
        "pendingCameraDataNo": None,
        "paused": False,
    }


@app.post("/cctv/{camera_no}/restart")
def restart_stream(camera_no: int):
    worker = get_worker(camera_no)
    worker.restart_playback()
    return {
        "success": True,
        "cameraNo": camera_no,
        "restarted": True,
        "paused": False,
        "videoFinished": False,
    }


@app.post("/detect-test")
async def detect_test(file: UploadFile = File(...)):
    image = await file.read()
    return app.state.plate_detector.detect_and_crop(
        image_bytes=image,
        filename=file.filename,
    )


@app.post("/ocr-test")
async def ocr_test(file: UploadFile = File(...)):
    image = await file.read()
    detect_result = app.state.plate_detector.detect_and_crop(
        image_bytes=image,
        filename=file.filename,
    )

    if not detect_result["success"]:
        return {
            "success": False,
            "message": "번호판 검출 실패",
            "detect": detect_result,
            "ocr": None,
        }

    # 3. crop 전처리 후보 생성 + OCR + 최종 선택
    pipeline_result = run_ocr_pipeline(
        detect_result["crop_path"]
    )

    ocr_result = pipeline_result["result"]

    return {
        "success": True,
        "message": "번호판 검출 + 전처리 OCR 성공",
        "carNo": ocr_result["text"],
        "ocr_score": ocr_result["score"],
        "selected_mode": ocr_result.get("mode"),
        "selector_score": ocr_result.get("selector_score"),
        "detect": detect_result,
        "ocr": ocr_result,
        "ocr_candidates": pipeline_result["candidates"]
    }


@app.post("/ocr")
async def ocr(
    file: UploadFile = File(...),
    cameraNo: int = Form(...),
):
    image = await file.read()
    detect_result = app.state.plate_detector.detect_and_crop(
        image_bytes=image,
        filename=file.filename,
    )

    if not detect_result["success"]:
        return {
            "success": False,
            "message": "번호판 검출 실패",
            "cameraNo": cameraNo,
            "detect": detect_result,
        }

    # 3. crop 전처리 후보 생성 + OCR + 최종 선택
    pipeline_result = run_ocr_pipeline(
        detect_result["crop_path"]
    )
    car_no = ocr_result["text"]

    ocr_result = pipeline_result["result"]
    carNo = ocr_result["text"]

    # 4. Spring /api/camera-data/ocr 로 보낼 multipart/form-data 구성
    # Spring 쪽 CameraDataController.ocr()는 @RequestParam + MultipartFile 구조다.
    # 원본 이미지는 Spring이 저장하고, FastAPI의 crop 이미지는 OCR용 임시 파일로만 사용한다.
    data = {
        "cameraNo": str(cameraNo),
        "carNo": carNo,
        "confidenceScore": str(ocr_result["score"] * 100)
    }

    files = {
        "file": (
            file.filename,
            img,
            file.content_type or "application/octet-stream"
        )
    }

    # 5. Spring API 호출
    client = app.state.spring_client

    response = await client.post(
        SPRING_URL,
        data={
            "cameraNo": str(cameraNo),
            "carNo": car_no,
            "confidenceScore": str(ocr_result["score"] * 100),
        },
        files={
            "file": (
                file.filename,
                image,
                file.content_type or "application/octet-stream",
            )
        },
    )

    return {
        "success": True,
        "msg": "YOLO + 전처리 OCR 처리 후 Spring OCR 전송 완료",
        "cameraNo": cameraNo,
        "carNo": car_no,
        "ocr_score": ocr_result["score"],
        "selected_mode": ocr_result.get("mode"),
        "selector_score": ocr_result.get("selector_score"),
        "detect": detect_result,
        "ocr": ocr_result,
        "spring_status": response.status_code,
        "spring_result": response.text,
    }


# cd C:\kwon\mbc_a_java21\java_17\fast-api
# C:\Users\dkddp\.conda\envs\bono\python.exe -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
# python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
