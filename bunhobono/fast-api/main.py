from contextlib import asynccontextmanager
import asyncio
import os
import threading
import time

import httpx
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse

from plate_ocr import PlateOCR
from test_stream import TEST_STREAMS, TestStreamWorker
from yolo_detect import PlateDetector


SPRING_URL = os.getenv(
    "SPRING_URL",
    "http://localhost:80/api/camera-data/ocr",
)


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
    }


@app.post("/cctv/{camera_no}/pause")
def pause_stream(camera_no: int):
    get_worker(camera_no).pause()
    return {"success": True, "cameraNo": camera_no, "paused": True}


@app.post("/cctv/{camera_no}/resume")
def resume_stream(camera_no: int):
    get_worker(camera_no).resume()
    return {"success": True, "cameraNo": camera_no, "paused": False}


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

    ocr_result = app.state.plate_ocr.read(
        detect_result["crop_path"]
    )
    return {
        "success": True,
        "carNo": ocr_result["text"],
        "ocr_score": ocr_result["score"],
        "detect": detect_result,
        "ocr": ocr_result,
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

    ocr_result = app.state.plate_ocr.read(
        detect_result["crop_path"]
    )
    car_no = ocr_result["text"]

    response = await app.state.spring_client.post(
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
        "cameraNo": cameraNo,
        "carNo": car_no,
        "ocr_score": ocr_result["score"],
        "spring_status": response.status_code,
        "spring_result": response.text,
    }
