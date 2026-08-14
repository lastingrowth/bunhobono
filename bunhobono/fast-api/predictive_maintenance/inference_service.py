from __future__ import annotations

import json
import threading
from datetime import datetime
from pathlib import Path

import pandas as pd
from xgboost import XGBClassifier


PROJECT_ROOT = Path(__file__).resolve().parent.parent


class PredictiveMaintenanceReplayService:
    def __init__(self, *, equipment_type: str, equipment_id_column: str,
                 timestamp_column: str, model_path: Path,
                 metadata_path: Path, test_csv_path: Path,
                 equipment_number_min: int | None = None,
                 equipment_number_max: int | None = None) -> None:
        self.equipment_type = equipment_type
        self.equipment_id_column = equipment_id_column
        self.timestamp_column = timestamp_column
        self._lock = threading.Lock()
        self._current_index = 0

        for description, path in (("모델", model_path),
                                  ("메타데이터", metadata_path),
                                  ("테스트 CSV", test_csv_path)):
            if not path.exists():
                raise FileNotFoundError(
                    f"{equipment_type} {description} 파일이 없습니다: {path}"
                )

        self.model = XGBClassifier()
        self.model.load_model(model_path)
        with metadata_path.open("r", encoding="utf-8") as file:
            self.metadata = json.load(file)
        self.test_data = pd.read_csv(test_csv_path, encoding="utf-8-sig")

        if equipment_number_min is not None or equipment_number_max is not None:
            equipment_numbers = pd.to_numeric(
                self.test_data[equipment_id_column]
                .astype(str)
                .str.extract(r"(\d+)$", expand=False),
                errors="coerce",
            )
            allowed_rows = equipment_numbers.notna()
            if equipment_number_min is not None:
                allowed_rows &= equipment_numbers >= equipment_number_min
            if equipment_number_max is not None:
                allowed_rows &= equipment_numbers <= equipment_number_max
            self.test_data = self.test_data.loc[allowed_rows].reset_index(drop=True)

        if self.test_data.empty:
            raise RuntimeError(f"{equipment_type} 테스트 데이터가 비어 있습니다.")

        required = {equipment_id_column, timestamp_column, "risk_level",
                    *self.metadata["feature_columns"]}
        missing = sorted(required - set(self.test_data.columns))
        if missing:
            raise RuntimeError(f"{equipment_type} 필수 컬럼이 없습니다: {missing}")

    def _next_row(self) -> tuple[int, pd.Series]:
        with self._lock:
            index = self._current_index
            row = self.test_data.iloc[index].copy()
            self._current_index = (index + 1) % len(self.test_data)
        return index, row

    @staticmethod
    def _number(value):
        if pd.isna(value):
            return None
        numeric = float(value)
        return int(numeric) if numeric.is_integer() else numeric

    def predict_next(self) -> dict:
        row_index, row = self._next_row()
        features = self.metadata["feature_columns"]
        input_data = pd.DataFrame(
            [{column: row[column] for column in features}], columns=features
        )
        class_id = int(self.model.predict(input_data)[0])
        probability_values = self.model.predict_proba(input_data)[0]
        labels = self.metadata["id_to_label"]
        risk_level = labels[str(class_id)]
        expected = str(row["risk_level"])

        return {
            "equipment_type": self.equipment_type,
            "equipment_no": str(row[self.equipment_id_column]),
            "row_index": row_index,
            "risk_level": risk_level,
            "risk_probability": float(probability_values[class_id]),
            "probabilities": {
                labels[str(index)]: float(value)
                for index, value in enumerate(probability_values)
            },
            "expected_risk_level": expected,
            "prediction_correct": risk_level == expected,
            "sensor_values": {
                column: self._number(row[column]) for column in features
            },
            "sensor_collected_at": str(row[self.timestamp_column]),
            "predicted_at": datetime.now().isoformat(),
        }
