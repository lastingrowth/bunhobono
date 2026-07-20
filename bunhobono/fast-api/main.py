from contextlib import asynccontextmanager
import asyncio
from datetime import datetime
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
ERROR_DIR = BASE_DIR / "runtime" / "errors"

PREPROCESS_DIR.mkdir(parents=True, exist_ok=True)
ERROR_DIR.mkdir(parents=True, exist_ok=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.spring_client = httpx.AsyncClient(
        timeout=httpx.Timeout(10.0, connect=3.0)
    )
    app.state.plate_detector = PlateDetector()
    app.state.plate_ocr = PlateOCR(device="cpu")
    # app.state.plate_ocr = PlateOCR(device="gpu:0")

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


def save_error_image(
    image_bytes: bytes,
    reason: str,
    original_filename: str = "",
):
    now = datetime.now().strftime("%Y%m%d_%H%M%S_%f")

    safe_reason = "".join(
        ch for ch in str(reason) if ch.isalnum() or ch in ["_", "-"]
    )

    if not safe_reason:
        safe_reason = "ocr_error"

    ext = Path(original_filename).suffix.lower()
    if ext not in [".jpg", ".jpeg", ".png", ".bmp", ".webp"]:
        ext = ".jpg"

    save_path = ERROR_DIR / f"{safe_reason}_{now}{ext}"

    with open(save_path, "wb") as f:
        f.write(image_bytes)

    return str(save_path)


def make_empty_ocr_result(mode: str = "ocr_pipeline_error"):
    return {
        "text": "",
        "raw_text": "",
        "score": 0.0,
        "mode": mode,
        "selector_score": 0.0,
        "repeat_count": 0,
        "is_valid_plate": False,
        "image_path": "",
        "all_candidates": [],
    }


def run_ocr_pipeline(crop_path: str):
    try:
        crop_path_obj = Path(crop_path)
        prefix = crop_path_obj.stem

        output_dir = PREPROCESS_DIR / prefix

        if output_dir.exists():
            shutil.rmtree(output_dir)

        output_dir.mkdir(parents=True, exist_ok=True)

        candidates = make_candidates(
            image_path=crop_path,
            output_dir=output_dir,
            prefix=prefix,
        )

        ocr_reader = app.state.plate_ocr
        ocr_candidates = ocr_reader.read_candidates(candidates)

        ocr_result = select_best_ocr(ocr_candidates)

        return {
            "success": True,
            "result": ocr_result,
            "candidates": ocr_candidates,
            "error": None,
        }

    except Exception as e:
        return {
            "success": False,
            "result": make_empty_ocr_result(),
            "candidates": [],
            "error": str(e),
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

    return {
        "success": True,
        "cameraNo": camera_no,
        "paused": True,
    }


@app.post("/cctv/{camera_no}/resume")
def resume_stream(camera_no: int):
    get_worker(camera_no).resume()

    return {
        "success": True,
        "cameraNo": camera_no,
        "paused": False,
    }


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

    try:
        return app.state.plate_detector.detect_and_crop(
            image_bytes=image,
            filename=file.filename,
        )

    except Exception as e:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="detect_error",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 검출 처리 중 오류",
            "error": str(e),
            "error_image_path": error_image_path,
        }


@app.post("/ocr-test")
async def ocr_test(file: UploadFile = File(...)):
    image = await file.read()

    try:
        detect_result = app.state.plate_detector.detect_and_crop(
            image_bytes=image,
            filename=file.filename,
        )

    except Exception as e:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="detect_error",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 검출 처리 중 오류",
            "error": str(e),
            "error_image_path": error_image_path,
            "detect": None,
            "ocr": None,
        }

    if not detect_result["success"]:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="detect_fail",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 검출 실패",
            "error_image_path": error_image_path,
            "detect": detect_result,
            "ocr": None,
        }

    pipeline_result = run_ocr_pipeline(
        detect_result["crop_path"]
    )

    if not pipeline_result["success"]:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="ocr_error",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "전처리 또는 OCR 처리 실패",
            "error": pipeline_result["error"],
            "error_image_path": error_image_path,
            "detect": detect_result,
            "ocr": pipeline_result["result"],
            "ocr_candidates": pipeline_result["candidates"],
        }

    ocr_result = pipeline_result["result"]

    if not ocr_result.get("is_valid_plate", False):
        error_image_path = save_error_image(
            image_bytes=image,
            reason="invalid_plate",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 형식 미인식",
            "error_image_path": error_image_path,
            "carNo": ocr_result.get("text", ""),
            "ocr_score": ocr_result.get("score", 0.0),
            "selected_mode": ocr_result.get("mode"),
            "selector_score": ocr_result.get("selector_score"),
            "detect": detect_result,
            "ocr": ocr_result,
            "ocr_candidates": pipeline_result["candidates"],
        }

    return {
        "success": True,
        "message": "번호판 검출 + 전처리 OCR 성공",
        "carNo": ocr_result["text"],
        "ocr_score": ocr_result["score"],
        "selected_mode": ocr_result.get("mode"),
        "selector_score": ocr_result.get("selector_score"),
        "detect": detect_result,
        "ocr": ocr_result,
        "ocr_candidates": pipeline_result["candidates"],
    }


@app.post("/ocr")
async def ocr(
    file: UploadFile = File(...),
    cameraNo: int = Form(...),
):
    image = await file.read()

    try:
        detect_result = app.state.plate_detector.detect_and_crop(
            image_bytes=image,
            filename=file.filename,
        )

    except Exception as e:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="detect_error",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 검출 처리 중 오류",
            "cameraNo": cameraNo,
            "error": str(e),
            "error_image_path": error_image_path,
            "detect": None,
        }

    if not detect_result["success"]:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="detect_fail",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 검출 실패",
            "cameraNo": cameraNo,
            "error_image_path": error_image_path,
            "detect": detect_result,
        }

    pipeline_result = run_ocr_pipeline(
        detect_result["crop_path"]
    )

    if not pipeline_result["success"]:
        error_image_path = save_error_image(
            image_bytes=image,
            reason="ocr_error",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "전처리 또는 OCR 처리 실패",
            "cameraNo": cameraNo,
            "error": pipeline_result["error"],
            "error_image_path": error_image_path,
            "detect": detect_result,
            "ocr": pipeline_result["result"],
        }

    ocr_result = pipeline_result["result"]
    carNo = ocr_result.get("text", "")

    if not ocr_result.get("is_valid_plate", False):
        error_image_path = save_error_image(
            image_bytes=image,
            reason="invalid_plate",
            original_filename=file.filename,
        )

        return {
            "success": False,
            "message": "번호판 형식 미인식",
            "cameraNo": cameraNo,
            "carNo": carNo,
            "ocr_score": ocr_result.get("score", 0.0),
            "selected_mode": ocr_result.get("mode"),
            "selector_score": ocr_result.get("selector_score"),
            "error_image_path": error_image_path,
            "detect": detect_result,
            "ocr": ocr_result,
        }

    data = {
        "cameraNo": str(cameraNo),
        "carNo": carNo,
        "confidenceScore": str(ocr_result["score"] * 100),
    }

    files = {
        "file": (
            file.filename,
            image,
            file.content_type or "application/octet-stream",
        )
    }

    client = app.state.spring_client

    response = await client.post(
        SPRING_URL,
        data=data,
        files=files,
    )

    return {
        "success": True,
        "msg": "YOLO + 전처리 OCR 처리 후 Spring OCR 전송 완료",
        "cameraNo": cameraNo,
        "carNo": carNo,
        "ocr_score": ocr_result["score"],
        "selected_mode": ocr_result.get("mode"),
        "selector_score": ocr_result.get("selector_score"),
        "detect": detect_result,
        "ocr": ocr_result,
        "spring_status": response.status_code,
        "spring_result": response.text,
    }


# cd C:\Users\dkddp\Documents\bunhobono\bunhobono\fast-api
# C:\Users\dkddp\.conda\envs\bono\python.exe -m uvicorn main:app --reload --host 0.0.0.0 --port 8000





# cd C:\Users\dkddp\Documents\bunhobono\bunhobono\fast-api
# C:\ProgramData\anaconda3\envs\bono_gpu\python.exe -m uvicorn main:app --reload --host 0.0.0.0 --port 8000