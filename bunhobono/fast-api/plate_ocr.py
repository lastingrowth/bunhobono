# plate_ocr.py

from pathlib import Path
import os
import re

BASE_DIR = Path(__file__).resolve().parent
PYTHON_HOME = BASE_DIR / "runtime" / "python-home"
PYTHON_HOME.mkdir(parents=True, exist_ok=True)

# Paddle가 사용자 홈 대신 프로젝트 내부에 캐시를 저장하도록 한다.
os.environ.setdefault("HOME", str(PYTHON_HOME))
os.environ["USERPROFILE"] = str(PYTHON_HOME)

from paddleocr import TextRecognition

OCR_MODEL_DIR = (
    BASE_DIR
    / "model"
    / "ppocrv5_kor1_deploy"
    / "inference"
)


class PlateOCR:
    def __init__(self, device: str = "cpu"):
        self.ocr = TextRecognition(
            model_name="korean_PP-OCRv5_mobile_rec",
            model_dir=str(OCR_MODEL_DIR),
            device=device
        )

    def clean_text(self, text: str) -> str:
        return re.sub(r"[^0-9가-힣]", "", str(text))

    def read(self, image_path: str | Path):
        result = self.ocr.predict(str(image_path))

        raw_text = ""
        score = 0.0

        if result:
            item = result[0]

            if hasattr(item, "json"):
                data = item.json
                res = data.get("res", data)
                raw_text = res.get("rec_text", "")
                score = res.get("rec_score", 0.0)

            elif isinstance(item, dict):
                res = item.get("res", item)
                raw_text = res.get("rec_text", "")
                score = res.get("rec_score", 0.0)

        text = self.clean_text(raw_text)

        return {
            "text": text,
            "raw_text": raw_text,
            "score": float(score)
        }
