from pathlib import Path
import json

import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix
)
from xgboost import XGBClassifier


# ============================================================
# 1. 기본 설정
# ============================================================

RANDOM_STATE = 42

# 비교할 위험 클래스 가중치 배수
DANGER_WEIGHT_MULTIPLIERS = [1.0, 2.0, 3.5]

# 클래스 번호를 직접 고정
# 0 = 위험, 1 = 정상, 2 = 주의
LABEL_TO_ID = {
    "위험": 0,
    "정상": 1,
    "주의": 2
}

ID_TO_LABEL = {
    0: "위험",
    1: "정상",
    2: "주의"
}

DANGER_CLASS_ID = 0


# ============================================================
# 2. 파일 경로 설정
# ============================================================

# 현재 파일:
# fast-api/predictive_maintenance/robot/train_robot_xgboost.py
CURRENT_DIR = Path(__file__).resolve().parent

# fast-api 폴더
PROJECT_ROOT = CURRENT_DIR.parent.parent

CSV_PATH = (
    PROJECT_ROOT
    / "data"
    / "parking_robot_predictive_maintenance_10000.csv"
)

MODEL_DIR = PROJECT_ROOT / "model"
MODEL_DIR.mkdir(parents=True, exist_ok=True)

MODEL_PATH = MODEL_DIR / "parking_robot_xgboost.json"
METADATA_PATH = MODEL_DIR / "parking_robot_xgboost_metadata.json"


# ============================================================
# 3. 모델 입력 칼럼
# ============================================================

FEATURE_COLS = [
    "drive_motor_temperature_c",  # 주행 모터 온도
    "drive_motor_current_a",      # 주행 모터 전류
    "drive_vibration_mm_s",       # 주행부 진동
    "battery_voltage_v",          # 배터리 전압
    "battery_temperature_c",      # 배터리 온도
    "days_since_maintenance"      # 마지막 정비 후 경과일
]

TARGET_COL = "risk_level"
ROBOT_ID_COL = "robot_id"
TIMESTAMP_COL = "timestamp"


# ============================================================
# 4. 데이터 불러오기
# ============================================================

print("=" * 70)
print("주차로봇 XGBoost 예지보전 학습")
print("=" * 70)

print(f"\nCSV 경로: {CSV_PATH}")

if not CSV_PATH.exists():
    raise FileNotFoundError(
        f"\nCSV 파일을 찾을 수 없습니다.\n"
        f"확인할 경로: {CSV_PATH}"
    )

# BOM이 포함된 CSV도 읽을 수 있도록 utf-8-sig 사용
df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")

print("\n===== 전체 데이터 확인 =====")
print(f"전체 데이터 크기: {df.shape}")
print(f"전체 데이터 개수: {len(df):,}건")
print(f"칼럼 목록: {df.columns.tolist()}")


# ============================================================
# 5. 필수 칼럼 검사
# ============================================================

required_cols = [
    TIMESTAMP_COL,
    ROBOT_ID_COL,
    *FEATURE_COLS,
    TARGET_COL
]

missing_cols = [
    col for col in required_cols
    if col not in df.columns
]

if missing_cols:
    raise ValueError(
        f"CSV에 필요한 칼럼이 없습니다: {missing_cols}"
    )


# ============================================================
# 6. 데이터 전처리
# ============================================================

# 시간 문자열을 datetime 형식으로 변환
df[TIMESTAMP_COL] = pd.to_datetime(
    df[TIMESTAMP_COL],
    errors="coerce"
)

# 숫자형 칼럼 변환
for col in FEATURE_COLS:
    df[col] = pd.to_numeric(
        df[col],
        errors="coerce"
    )

# 클래스 이름 앞뒤 공백 제거
df[TARGET_COL] = (
    df[TARGET_COL]
    .astype(str)
    .str.strip()
)

# 알 수 없는 클래스 확인
unknown_labels = sorted(
    set(df[TARGET_COL].unique())
    - set(LABEL_TO_ID.keys())
)

if unknown_labels:
    raise ValueError(
        f"알 수 없는 risk_level 값이 있습니다: {unknown_labels}"
    )

# 결측치가 포함된 행 제거
before_drop_count = len(df)

df = df.dropna(
    subset=[
        TIMESTAMP_COL,
        ROBOT_ID_COL,
        *FEATURE_COLS,
        TARGET_COL
    ]
).copy()

after_drop_count = len(df)

print(
    f"\n결측치 제거: "
    f"{before_drop_count - after_drop_count:,}건"
)

# 문자열 클래스를 숫자로 변환
df["target_id"] = (
    df[TARGET_COL]
    .map(LABEL_TO_ID)
    .astype(int)
)

# 로봇별 시간순 정렬
df = df.sort_values(
    by=[ROBOT_ID_COL, TIMESTAMP_COL]
).reset_index(drop=True)


# ============================================================
# 7. 클래스 및 로봇별 데이터 확인
# ============================================================

print("\n===== 정답 클래스 순서 =====")
print("0 = 위험")
print("1 = 정상")
print("2 = 주의")
print(f"위험 클래스 번호: {DANGER_CLASS_ID}")

print("\n===== 전체 클래스 분포 =====")

for class_id in sorted(ID_TO_LABEL):
    class_name = ID_TO_LABEL[class_id]
    count = int((df["target_id"] == class_id).sum())
    ratio = count / len(df)

    print(
        f"{class_name:>4}({class_id}) : "
        f"{count:>5,}건 ({ratio:.2%})"
    )

print("\n===== 로봇별 데이터 개수 =====")
print(df[ROBOT_ID_COL].value_counts().sort_index())


# ============================================================
# 8. 로봇별 시간순 Train / Validation / Test 분리
# ============================================================

# 각 로봇 데이터를 시간순으로 다음과 같이 분리
# 앞 70%: Train
# 다음 10%: Validation
# 마지막 20%: Test

train_parts = []
validation_parts = []
test_parts = []

for robot_id, robot_df in df.groupby(
    ROBOT_ID_COL,
    sort=True
):
    robot_df = robot_df.sort_values(
        TIMESTAMP_COL
    ).reset_index(drop=True)

    total_count = len(robot_df)

    train_end = int(total_count * 0.70)
    validation_end = int(total_count * 0.80)

    robot_train = robot_df.iloc[:train_end]
    robot_validation = robot_df.iloc[
        train_end:validation_end
    ]
    robot_test = robot_df.iloc[
        validation_end:
    ]

    train_parts.append(robot_train)
    validation_parts.append(robot_validation)
    test_parts.append(robot_test)

    print(f"\n[{robot_id}] 데이터 분리")
    print(f"전체       : {total_count:,}건")
    print(f"Train      : {len(robot_train):,}건")
    print(f"Validation : {len(robot_validation):,}건")
    print(f"Test       : {len(robot_test):,}건")

train_df = pd.concat(
    train_parts,
    ignore_index=True
)

validation_df = pd.concat(
    validation_parts,
    ignore_index=True
)

test_df = pd.concat(
    test_parts,
    ignore_index=True
)

X_train = train_df[FEATURE_COLS]
y_train = train_df["target_id"]

X_validation = validation_df[FEATURE_COLS]
y_validation = validation_df["target_id"]

X_test = test_df[FEATURE_COLS]
y_test = test_df["target_id"]

print("\n===== 최종 데이터 분리 결과 =====")
print(f"Train 크기      : {X_train.shape}")
print(f"Validation 크기 : {X_validation.shape}")
print(f"Test 크기       : {X_test.shape}")


# ============================================================
# 9. 데이터 분리 기간 확인
# ============================================================

def print_period(name, target_df):
    start_time = target_df[TIMESTAMP_COL].min()
    end_time = target_df[TIMESTAMP_COL].max()

    print(
        f"{name:<10}: "
        f"{start_time} ~ {end_time}"
    )


print("\n===== 데이터 기간 확인 =====")
print_period("Train", train_df)
print_period("Validation", validation_df)
print_period("Test", test_df)


# ============================================================
# 10. 성능 계산 함수
# ============================================================

def calculate_metrics(y_true, y_pred):
    accuracy = accuracy_score(
        y_true,
        y_pred
    )

    precision_weighted = precision_score(
        y_true,
        y_pred,
        average="weighted",
        zero_division=0
    )

    recall_weighted = recall_score(
        y_true,
        y_pred,
        average="weighted",
        zero_division=0
    )

    f1_weighted = f1_score(
        y_true,
        y_pred,
        average="weighted",
        zero_division=0
    )

    macro_f1 = f1_score(
        y_true,
        y_pred,
        average="macro",
        zero_division=0
    )

    danger_precision = precision_score(
        y_true,
        y_pred,
        labels=[DANGER_CLASS_ID],
        average="macro",
        zero_division=0
    )

    danger_recall = recall_score(
        y_true,
        y_pred,
        labels=[DANGER_CLASS_ID],
        average="macro",
        zero_division=0
    )

    danger_f1 = f1_score(
        y_true,
        y_pred,
        labels=[DANGER_CLASS_ID],
        average="macro",
        zero_division=0
    )

    return {
        "accuracy": accuracy,
        "precision_weighted": precision_weighted,
        "recall_weighted": recall_weighted,
        "f1_weighted": f1_weighted,
        "macro_f1": macro_f1,
        "danger_precision": danger_precision,
        "danger_recall": danger_recall,
        "danger_f1": danger_f1
    }


def print_metrics(title, metrics):
    print(f"\n===== {title} =====")
    print(
        f"Accuracy           : "
        f"{metrics['accuracy']:.6f}"
    )
    print(
        f"Precision weighted : "
        f"{metrics['precision_weighted']:.6f}"
    )
    print(
        f"Recall weighted    : "
        f"{metrics['recall_weighted']:.6f}"
    )
    print(
        f"F1 weighted        : "
        f"{metrics['f1_weighted']:.6f}"
    )
    print(
        f"Macro F1           : "
        f"{metrics['macro_f1']:.6f}"
    )
    print(
        f"위험 Precision     : "
        f"{metrics['danger_precision']:.6f}"
    )
    print(
        f"위험 Recall        : "
        f"{metrics['danger_recall']:.6f}"
    )
    print(
        f"위험 F1            : "
        f"{metrics['danger_f1']:.6f}"
    )


# ============================================================
# 11. 클래스 가중치 계산 함수
# ============================================================

def create_sample_weights(
    y,
    danger_weight_multiplier
):
    """
    Train 데이터의 클래스 빈도를 기준으로 기본 가중치를 계산하고,
    위험 클래스 가중치에 추가 배수를 적용한다.
    """

    class_counts = (
        y.value_counts()
        .sort_index()
    )

    total_count = len(y)
    class_count = len(ID_TO_LABEL)

    base_weights = {}

    for class_id in sorted(ID_TO_LABEL):
        count = class_counts.get(
            class_id,
            0
        )

        if count == 0:
            raise ValueError(
                f"Train 데이터에 "
                f"{ID_TO_LABEL[class_id]} 클래스가 없습니다."
            )

        base_weights[class_id] = (
            total_count
            / (class_count * count)
        )

    final_weights = base_weights.copy()

    final_weights[DANGER_CLASS_ID] *= (
        danger_weight_multiplier
    )

    sample_weights = (
        y.map(final_weights)
        .astype(float)
        .to_numpy()
    )

    return (
        sample_weights,
        base_weights,
        final_weights
    )


# ============================================================
# 12. 가중치별 Validation 실험
# ============================================================

validation_results = []
trained_models = {}

for danger_multiplier in DANGER_WEIGHT_MULTIPLIERS:
    print("\n" + "=" * 70)
    print(
        f"위험 클래스 가중치 "
        f"{danger_multiplier:.1f}배 실험"
    )
    print("=" * 70)

    (
        train_sample_weights,
        base_class_weights,
        final_class_weights
    ) = create_sample_weights(
        y_train,
        danger_multiplier
    )

    print("\n기본 클래스 가중치")

    for class_id in sorted(ID_TO_LABEL):
        print(
            f"{ID_TO_LABEL[class_id]}({class_id}) : "
            f"{base_class_weights[class_id]:.6f}"
        )

    print("\n최종 적용 클래스 가중치")

    for class_id in sorted(ID_TO_LABEL):
        print(
            f"{ID_TO_LABEL[class_id]}({class_id}) : "
            f"{final_class_weights[class_id]:.6f}"
        )

    model = XGBClassifier(
        objective="multi:softprob",
        num_class=3,

        # 충분히 크게 설정하고 Validation 기준 조기 종료
        n_estimators=2000,
        learning_rate=0.03,

        max_depth=4,
        min_child_weight=1,

        subsample=0.9,
        colsample_bytree=0.9,

        reg_alpha=0.0,
        reg_lambda=1.0,

        eval_metric="mlogloss",
        early_stopping_rounds=50,

        random_state=RANDOM_STATE,
        n_jobs=-1,
        tree_method="hist"
    )

    model.fit(
        X_train,
        y_train,
        sample_weight=train_sample_weights,
        eval_set=[
            (X_validation, y_validation)
        ],
        verbose=False
    )

    validation_pred = model.predict(
        X_validation
    )

    validation_metrics = calculate_metrics(
        y_validation,
        validation_pred
    )

    print_metrics(
        (
            f"Validation 성능 "
            f"(위험 가중치 {danger_multiplier:.1f}배)"
        ),
        validation_metrics
    )

    best_iteration = getattr(
        model,
        "best_iteration",
        None
    )

    if best_iteration is not None:
        best_tree_count = best_iteration + 1
    else:
        best_tree_count = model.n_estimators

    print(
        f"최적 트리 개수     : "
        f"{best_tree_count}"
    )

    validation_results.append({
        "danger_multiplier": danger_multiplier,
        "model": model,
        "best_tree_count": best_tree_count,
        **validation_metrics
    })

    trained_models[danger_multiplier] = model


# ============================================================
# 13. Validation 결과 비교
# ============================================================

result_df = pd.DataFrame([
    {
        "위험 가중치": (
            f"{result['danger_multiplier']:.1f}배"
        ),
        "Accuracy": result["accuracy"],
        "Weighted F1": result["f1_weighted"],
        "Macro F1": result["macro_f1"],
        "위험 Precision": result["danger_precision"],
        "위험 Recall": result["danger_recall"],
        "위험 F1": result["danger_f1"],
        "최적 트리 수": result["best_tree_count"]
    }
    for result in validation_results
])

print("\n===== Validation 가중치 비교 =====")
print(result_df.to_string(index=False))


# ============================================================
# 14. 최종 설정 선택
# ============================================================

# 1순위: Macro F1
# 2순위: 위험 Recall
# 3순위: 위험 F1
best_result = max(
    validation_results,
    key=lambda result: (
        result["macro_f1"],
        result["danger_recall"],
        result["danger_f1"]
    )
)

best_multiplier = best_result[
    "danger_multiplier"
]

best_model = best_result["model"]

print("\n" + "=" * 70)
print("최종 설정 선택")
print("=" * 70)

print(
    f"선택된 위험 가중치 : "
    f"{best_multiplier:.1f}배"
)

print(
    f"Validation Macro F1 : "
    f"{best_result['macro_f1']:.6f}"
)

print(
    f"Validation 위험 Recall: "
    f"{best_result['danger_recall']:.6f}"
)

print(
    f"Validation 위험 F1 : "
    f"{best_result['danger_f1']:.6f}"
)

print(
    f"최적 트리 개수     : "
    f"{best_result['best_tree_count']}"
)


# ============================================================
# 15. 선택된 모델로 Test를 마지막 한 번 평가
# ============================================================

test_pred = best_model.predict(
    X_test
)

test_metrics = calculate_metrics(
    y_test,
    test_pred
)

print_metrics(
    "최종 Test 성능",
    test_metrics
)

print("\n===== Test 혼동행렬 =====")
print("행: 실제 클래스 / 열: 예측 클래스")
print("클래스 순서: 위험, 정상, 주의")

test_confusion_matrix = confusion_matrix(
    y_test,
    test_pred,
    labels=[0, 1, 2]
)

print(test_confusion_matrix)

print("\n===== Test 상세 분류 결과 =====")

print(
    classification_report(
        y_test,
        test_pred,
        labels=[0, 1, 2],
        target_names=[
            "위험",
            "정상",
            "주의"
        ],
        digits=6,
        zero_division=0
    )
)


# ============================================================
# 16. 모델과 메타데이터 저장
# ============================================================

best_model.save_model(
    MODEL_PATH
)

metadata = {
    "model_type": "XGBoost",
    "target_column": TARGET_COL,
    "feature_columns": FEATURE_COLS,
    "label_to_id": LABEL_TO_ID,
    "id_to_label": {
        str(key): value
        for key, value in ID_TO_LABEL.items()
    },
    "danger_class_id": DANGER_CLASS_ID,
    "danger_weight_multiplier": best_multiplier,
    "best_tree_count": best_result[
        "best_tree_count"
    ],
    "data_split": {
        "train": 0.70,
        "validation": 0.10,
        "test": 0.20,
        "method": "robot별 시간순 분리"
    },
    "validation_metrics": {
        key: float(best_result[key])
        for key in [
            "accuracy",
            "precision_weighted",
            "recall_weighted",
            "f1_weighted",
            "macro_f1",
            "danger_precision",
            "danger_recall",
            "danger_f1"
        ]
    },
    "test_metrics": {
        key: float(value)
        for key, value in test_metrics.items()
    }
}

with open(
    METADATA_PATH,
    "w",
    encoding="utf-8"
) as file:
    json.dump(
        metadata,
        file,
        ensure_ascii=False,
        indent=2
    )

print("\n===== 저장 완료 =====")
print(f"모델 저장 경로: {MODEL_PATH}")
print(f"설정 저장 경로: {METADATA_PATH}")