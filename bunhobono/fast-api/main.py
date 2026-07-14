from contextlib import asynccontextmanager
import os

from fastapi import FastAPI, UploadFile, File, Form
import httpx

from yolo_detect import PlateDetector
from plate_ocr import PlateOCR


SPRING_URL = os.getenv(
    "SPRING_URL",
    "http://localhost:80/api/camera-data/ocr"
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    timeout = httpx.Timeout(10.0, connect=3.0)

    app.state.spring_client = httpx.AsyncClient(
        timeout=timeout
    )

    app.state.plate_detector = PlateDetector()

    # 제출/테스트는 CPU 기준
    app.state.plate_ocr = PlateOCR(device="cpu")

    yield

    await app.state.spring_client.aclose()


app = FastAPI(
    title="Parking API test",
    lifespan=lifespan
)


@app.get("/")
def home():
    return {
        "msg": "Fast Api",
        "spring_url": SPRING_URL
    }


@app.post("/detect-test")
async def detect_test(
    file: UploadFile = File(...)
):
    # 차량 이미지 업로드
    img = await file.read()

    # YOLO 번호판 검출 + crop 저장
    detector = app.state.plate_detector

    result = detector.detect_and_crop(
        image_bytes=img,
        filename=file.filename
    )

    return result


@app.post("/ocr-test")
async def ocr_test(
    file: UploadFile = File(...)
):
    # 1. 차량 이미지 업로드
    img = await file.read()

    # 2. YOLO 번호판 검출 + crop 저장
    detector = app.state.plate_detector

    detect_result = detector.detect_and_crop(
        image_bytes=img,
        filename=file.filename
    )

    if not detect_result["success"]:
        return {
            "success": False,
            "message": "번호판 검출 실패",
            "detect": detect_result,
            "ocr": None
        }

    # 3. crop 이미지 OCR
    ocr_reader = app.state.plate_ocr

    ocr_result = ocr_reader.read(
        detect_result["crop_path"]
    )

    return {
        "success": True,
        "message": "번호판 검출 + OCR 성공",
        "carNo": ocr_result["text"],
        "ocr_score": ocr_result["score"],
        "detect": detect_result,
        "ocr": ocr_result
    }


@app.post("/ocr")
async def ocr(
    file: UploadFile = File(...),
    cameraNo: int = Form(...)
):
    # 1. 차량 이미지 업로드
    img = await file.read()

    # 2. YOLO 번호판 검출 + crop 저장
    detector = app.state.plate_detector

    detect_result = detector.detect_and_crop(
        image_bytes=img,
        filename=file.filename
    )

    if not detect_result["success"]:
        return {
            "success": False,
            "message": "번호판 검출 실패",
            "cameraNo": cameraNo,
            "detect": detect_result
        }

    # 3. OCR 처리
    ocr_reader = app.state.plate_ocr

    ocr_result = ocr_reader.read(
        detect_result["crop_path"]
    )

    carNo = ocr_result["text"]

    # 4. Spring /api/camera-data/ocr 로 보낼 multipart/form-data 구성
    # Spring 쪽 CameraDataController.ocr()는 @RequestParam + MultipartFile 구조다.
    # 원본 이미지는 Spring이 저장하고, FastAPI의 crop 이미지는 OCR용 임시 파일로만 사용한다.
    data = {
        "cameraNo": str(cameraNo),
        "carNo": carNo,
        "confidenceScore": str(ocr_result["score"]* 100)
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
        data=data,
        files=files
    )

    return {
        "success": True,
        "msg": "YOLO + OCR 처리 후 Spring OCR 전송 완료",
        "cameraNo": cameraNo,
        "carNo": carNo,
        "ocr_score": ocr_result["score"],
        "detect": detect_result,
        "ocr": ocr_result,
        "spring_status": response.status_code,
        "spring_result": response.text
    }

    
# cd C:\kwon\mbc_a_java21\java_17\fast-api
# C:\Users\dkddp\.conda\envs\bono\python.exe -m uvicorn main:app --reload --host 0.0.0.0 --port 8000