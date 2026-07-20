# plate_ocr.py

from pathlib import Path
import os
import re

from paddleocr import TextRecognition


BASE_DIR = Path(__file__).resolve().parent

OCR_MODEL_DIR = Path(
    os.getenv(
        "OCR_MODEL_DIR",
        str(BASE_DIR / "model" / "ppocrv5_kor1_deploy" / "inference"),
    )
)


def clean_text(text: str) -> str:
    return re.sub(r"[^0-9가-힣]", "", str(text))


class PlateOCR:
    def __init__(self, device: str = "cpu"):
        self.ocr = TextRecognition(
            model_name="korean_PP-OCRv5_mobile_rec",
            model_dir=str(OCR_MODEL_DIR),
            device=device,
        )

    def _parse_result_item(self, item):
        raw_text = ""
        score = 0.0

        if hasattr(item, "json"):
            data = item.json

            if callable(data):
                data = data()

            res = data.get("res", data)
            raw_text = res.get("rec_text", "")
            score = res.get("rec_score", 0.0)

        elif isinstance(item, dict):
            res = item.get("res", item)
            raw_text = res.get("rec_text", "")
            score = res.get("rec_score", 0.0)

        return raw_text, float(score)

    def read(self, image_path: str | Path, mode: str = "single"):
        image_path = Path(image_path)

        result = self.ocr.predict(str(image_path))

        raw_text = ""
        score = 0.0

        if result:
            raw_text, score = self._parse_result_item(result[0])

        text = clean_text(raw_text)

        return {
            "text": text,
            "raw_text": raw_text,
            "score": float(score),
            "mode": mode,
            "image_path": str(image_path),
        }

    def read_candidates(self, candidates: list[dict]):
        """
        candidates 예시:
        [
            {
                "mode": "raw",
                "path": "C:/.../crop.jpg"
            },
            {
                "mode": "gray_h96",
                "path": "C:/.../gray.jpg"
            }
        ]

        반환:
        [
            {
                "text": "...",
                "raw_text": "...",
                "score": 0.998,
                "mode": "gray_h96",
                "image_path": "..."
            },
            ...
        ]
        """

        results = []

        for cand in candidates:
            mode = str(cand.get("mode", "unknown"))
            path = cand.get("path") or cand.get("image_path")

            if not path:
                continue

            try:
                ocr_result = self.read(path, mode=mode)
                results.append(ocr_result)

            except Exception as e:
                results.append({
                    "text": "",
                    "raw_text": "",
                    "score": 0.0,
                    "mode": mode,
                    "image_path": str(path),
                    "error": str(e),
                })

        return results
