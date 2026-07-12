from pathlib import Path
import os
import re

import cv2
from ultralytics import YOLO
from paddleocr import TextRecognition

BASE_DIR = Path(__file__).resolve().parent

YOLO_MODEL_PATH = (
    BASE_DIR
    / "models"
    / "yolo"
    / "best.pt"
)

OCR_MODEL_DIR = (
    BASE_DIR
    / "models"
    / "paddleocr"
    / "ppocrv5_kor1_deploy"
    / "inference"
)

# 직접 학습한 모델 불러오기
yolo_model = YOLO( str(YOLO_MODEL_PATH) )

ocr_model = TextRecognition(
    model_name="bono_ppocrv5_kor1",
    model_dir = str(OCR_MODEL_DIR),
    device="cpu",
)

def clean_car_no(text: str) -> str:
    return re.sub(r"[^0-9가-힣a-zA-Z]", "", text)


def recognize_car_no( image_path: str ) -> tuple[str, float]:
    image = cv2.imread(image_path)

    if image is None:
        raise RuntimeError("이미지를 읽을 수 없습니다.")
            
    # 1. YOLO로 번호판 위치 찾기
    yolo_results = yolo_model.predict(
        source = image,
        verbose = False
    )

    best_box = None
    best_conf_score = -1.0

    for result in yolo_results:
        for box in result.boxes:
            score = float(box.conf[0])
            
            if score > best_conf_score:
                best_conf_score = score
                best_box = box

    if best_box is None:
        return "", 0.0
    
    # 2. 번호판 영역 crop
    x1, y1, x2, y2 = ( best_box.xyxy[0].cpu().numpy().astype(int))

    hh, ww = image.shape[:2]
    
    x1 = max(0, min(x1,ww))
    y1 = max(0, min(y1,hh))
    x2 = max(0, min(x2,ww))
    y2 = max(0, min(y2,hh))

    plate_img = image[y1:y2, x1:x2]
    
    if plate_img.size == 0:
        print("번호판 crop 실패")
        return "", 0.0

    crop_path = str(
        Path(image_path).with_name(
            "crop_" + Path(image_path).name
        )
    )
    
    try:
        cv2.imwrite(crop_path, plate_img)
        
        ocr_results = ocr_model.predict(
            input=crop_path
        )
        
        recognize_text = ""
        confidence_score = 0.0
        
        for result in ocr_results:
            result_data = result.json
            
            if callable(result_data):
                result_data = result_data()

            if "res" in result_data:
                result_data = result_data["res"]

            recognize_text = result_data.get("rec_text", result_data.get("recognize_text", ""))
            
            confidence_score = float(
                result_data.get("rec_score", 0.0)
            )
            

        car_no = clean_car_no(recognize_text)
        
        print("차량번호:", car_no)
        print("신뢰도:", confidence_score)

        return car_no, confidence_score
    
    finally:
        if os.path.exists(crop_path):
            os.remove(crop_path)


