# yolo_detect.py

from pathlib import Path
import cv2
import numpy as np
from ultralytics import YOLO


BASE_DIR = Path(__file__).resolve().parent

YOLO_MODEL_PATH = BASE_DIR / "model" / "yolo11s_bono1199" / "best.pt"

RUNTIME_DIR = BASE_DIR / "runtime"
UPLOAD_DIR = RUNTIME_DIR / "uploads"
CROP_DIR = RUNTIME_DIR / "crops"

UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
CROP_DIR.mkdir(parents=True, exist_ok=True)


class PlateDetector:
    def __init__(self):
        self.model = YOLO(str(YOLO_MODEL_PATH))

    def read_image_from_bytes(self, image_bytes: bytes):
        arr = np.frombuffer(image_bytes, dtype=np.uint8)
        img = cv2.imdecode(arr, cv2.IMREAD_COLOR)

        if img is None:
            raise ValueError("이미지 파일을 읽을 수 없습니다.")

        return img

    def save_image(self, path: Path, img):
        ok, encoded = cv2.imencode(path.suffix, img)

        if not ok:
            raise ValueError("이미지 저장 인코딩 실패")

        encoded.tofile(str(path))

    def detect_and_crop(self, image_bytes: bytes, filename: str):
        img = self.read_image_from_bytes(image_bytes)

        results = self.model.predict(
            source=img,
            conf=0.25,
            imgsz=640,
            verbose=False
        )

        result = results[0]
        boxes = result.boxes

        if boxes is None or len(boxes) == 0:
            return {
                "success": False,
                "message": "번호판 검출 실패",
                "crop_path": None,
                "det_conf": 0.0,
                "box": None
            }

        confs = boxes.conf.cpu().numpy()
        best_idx = int(np.argmax(confs))

        xyxy = boxes.xyxy[best_idx].cpu().numpy()
        det_conf = float(confs[best_idx])

        x1, y1, x2, y2 = xyxy.astype(int)

        h, w = img.shape[:2]

        # OCR용으로 너무 딱 붙지 않게 여백 추가
        margin_x = max(2, int((x2 - x1) * 0.04))
        margin_y = max(2, int((y2 - y1) * 0.12))

        x1 = max(0, x1 - margin_x)
        y1 = max(0, y1 - margin_y)
        x2 = min(w, x2 + margin_x)
        y2 = min(h, y2 + margin_y)

        if x2 <= x1 or y2 <= y1:
            return {
                "success": False,
                "message": "crop 좌표 오류",
                "crop_path": None,
                "det_conf": det_conf,
                "box": [int(x1), int(y1), int(x2), int(y2)]
            }

        crop = img[y1:y2, x1:x2]

        safe_name = Path(filename).stem
        crop_path = CROP_DIR / f"{safe_name}_plate.jpg"

        self.save_image(crop_path, crop)

        return {
            "success": True,
            "message": "번호판 검출 성공",
            "crop_path": str(crop_path),
            "det_conf": det_conf,
            "box": [int(x1), int(y1), int(x2), int(y2)]
        }