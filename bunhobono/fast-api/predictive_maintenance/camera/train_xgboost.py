from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.metrics import accuracy_score, f1_score, precision_score, recall_score
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_sample_weight
from xgboost import XGBClassifier


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = PROJECT_ROOT / "data" / "camera"
CSV_PATH = DATA_DIR / "predictive_maintenance_timeseries_synthetic.csv"
TEST_DATA_PATH = DATA_DIR / "camera_predictive_maintenance_test.csv"
MODEL_DIR = PROJECT_ROOT / "model" / "camera"
MODEL_PATH = MODEL_DIR / "parking_camera_xgboost.json"
METADATA_PATH = MODEL_DIR / "parking_camera_xgboost_metadata.json"

FEATURE_COLUMNS = [
    "temperature_c",
    "voltage_v",
    "success_rate",
    "error_count",
    "days_since_maintenance",
]
TARGET_COLUMN = "risk_level"
LABEL_TO_ID = {"위험": 0, "정상": 1, "주의": 2}
ID_TO_LABEL = {value: key for key, value in LABEL_TO_ID.items()}
DANGER_CLASS_ID = LABEL_TO_ID["위험"]
DANGER_WEIGHT_MULTIPLIER = 3.5


def metrics(model: XGBClassifier, features: pd.DataFrame, target: pd.Series) -> dict:
    prediction = model.predict(features)
    return {
        "accuracy": float(accuracy_score(target, prediction)),
        "precision_weighted": float(precision_score(target, prediction, average="weighted", zero_division=0)),
        "recall_weighted": float(recall_score(target, prediction, average="weighted", zero_division=0)),
        "f1_weighted": float(f1_score(target, prediction, average="weighted", zero_division=0)),
        "macro_f1": float(f1_score(target, prediction, average="macro", zero_division=0)),
        "danger_precision": float(precision_score(target, prediction, labels=[DANGER_CLASS_ID], average=None, zero_division=0)[0]),
        "danger_recall": float(recall_score(target, prediction, labels=[DANGER_CLASS_ID], average=None, zero_division=0)[0]),
        "danger_f1": float(f1_score(target, prediction, labels=[DANGER_CLASS_ID], average=None, zero_division=0)[0]),
    }


def main() -> None:
    data = pd.read_csv(CSV_PATH, encoding="utf-8-sig")
    missing = [column for column in [*FEATURE_COLUMNS, TARGET_COLUMN] if column not in data.columns]
    if missing:
        raise ValueError(f"필수 컬럼이 없습니다: {missing}")

    unknown_labels = sorted(set(data[TARGET_COLUMN].dropna()) - set(LABEL_TO_ID))
    if unknown_labels:
        raise ValueError(f"알 수 없는 위험 등급입니다: {unknown_labels}")

    features = data[FEATURE_COLUMNS]
    target = data[TARGET_COLUMN].map(LABEL_TO_ID).astype(int)
    x_temp, x_test, y_temp, y_test = train_test_split(
        features, target, test_size=0.2, random_state=42, stratify=target
    )
    x_train, x_validation, y_train, y_validation = train_test_split(
        x_temp, y_temp, test_size=0.125, random_state=42, stratify=y_temp
    )

    test_data = data.loc[x_test.index].copy()
    test_data.to_csv(TEST_DATA_PATH, index=False, encoding="utf-8-sig")
    print(f"테스트 데이터 저장: {TEST_DATA_PATH} ({len(test_data)}건)")

    sample_weights = np.asarray(compute_sample_weight("balanced", y_train), dtype=float)
    sample_weights[y_train.to_numpy() == DANGER_CLASS_ID] *= DANGER_WEIGHT_MULTIPLIER

    model = XGBClassifier(
        objective="multi:softprob",
        num_class=len(LABEL_TO_ID),
        n_estimators=1000,
        learning_rate=0.05,
        max_depth=5,
        subsample=0.8,
        colsample_bytree=0.8,
        eval_metric="mlogloss",
        early_stopping_rounds=30,
        random_state=42,
        n_jobs=-1,
    )
    model.fit(
        x_train,
        y_train,
        sample_weight=sample_weights,
        eval_set=[(x_validation, y_validation)],
        verbose=False,
    )

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    model.save_model(MODEL_PATH)
    metadata = {
        "model_type": "XGBoost",
        "equipment_type": "camera",
        "target_column": TARGET_COLUMN,
        "feature_columns": FEATURE_COLUMNS,
        "label_to_id": LABEL_TO_ID,
        "id_to_label": {str(key): value for key, value in ID_TO_LABEL.items()},
        "danger_class_id": DANGER_CLASS_ID,
        "danger_weight_multiplier": DANGER_WEIGHT_MULTIPLIER,
        "best_tree_count": int(model.best_iteration + 1),
        "data_split": {"train": 0.7, "validation": 0.1, "test": 0.2, "method": "계층 무작위 분리"},
        "validation_metrics": metrics(model, x_validation, y_validation),
        "test_metrics": metrics(model, x_test, y_test),
    }
    METADATA_PATH.write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"모델 저장: {MODEL_PATH}")
    print(f"메타데이터 저장: {METADATA_PATH}")


if __name__ == "__main__":
    main()
