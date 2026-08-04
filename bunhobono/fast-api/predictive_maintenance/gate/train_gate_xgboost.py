from pathlib import Path

import numpy as np
import pandas as pd

from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
)
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_sample_weight
from xgboost import XGBClassifier


# ============================================================
# 실험 설정
#
# 1차 실험: 1.0
# 2차 실험: 2.0
# 3차 실험: 3.5
#
# balanced 가중치는 기본으로 적용하고,
# 위험 클래스에 아래 배수를 추가로 적용한다.
# ============================================================

DANGER_WEIGHT_MULTIPLIER = 2.0


# ============================================================
# Test 평가 여부
#
# 가중치 비교 중에는 False
# 최종 가중치를 선택한 후에만 True
# ============================================================

RUN_TEST = False


# ============================================================
# 파일 경로 설정
# ============================================================

# 현재 파일:
# FAST-API/predictive_maintenance/gate/train_gate_xgboost.py
BASE_DIR = Path(__file__).resolve().parent

# CSV 파일:
# FAST-API/data/gate_predictive_maintenance_synthetic_10000.csv
CSV_PATH = (
    BASE_DIR.parent.parent
    / "data"
    / "gate_predictive_maintenance_synthetic_10000.csv"
)

print("=" * 60)
print("게이트 예지보전 XGBoost")
print("=" * 60)
print("Python 파일 폴더:", BASE_DIR)
print("CSV 경로:", CSV_PATH)


# CSV 파일 존재 여부 확인
if not CSV_PATH.exists():
    raise FileNotFoundError(
        f"CSV 파일을 찾을 수 없습니다.\n"
        f"확인 경로: {CSV_PATH}"
    )


# ============================================================
# 데이터 불러오기
# ============================================================

df = pd.read_csv(
    CSV_PATH,
    encoding="utf-8-sig",
)


# ============================================================
# 게이트 예지보전 입력 변수
# ============================================================

feature_columns = [
    "motor_temperature_c",       # 게이트 구동 모터 온도
    "motor_current_a",           # 게이트 구동 모터 전류
    "voltage_v",                 # 게이트 제어부 공급전압
    "vibration_mm_s",            # 모터·감속기 진동
    "open_close_time_sec",       # 차단바 개폐시간
    "operation_count",           # 누적 개폐 횟수
    "error_count",               # 센서·제어장치 오류 횟수
    "days_since_maintenance",    # 마지막 정비 이후 경과일
]

# 예측할 목표 변수
target_column = "risk_level"


# ============================================================
# 필요한 칼럼 확인
# ============================================================

required_columns = [
    "gate_id",
    "collected_at",
    *feature_columns,
    target_column,
]

missing_columns = [
    column
    for column in required_columns
    if column not in df.columns
]

if missing_columns:
    raise ValueError(
        f"CSV에 필요한 칼럼이 없습니다: {missing_columns}"
    )


# ============================================================
# 날짜 형식 변환
# ============================================================

df["collected_at"] = pd.to_datetime(
    df["collected_at"]
)


# ============================================================
# 클래스 숫자 변환
# ============================================================

label_encoder = LabelEncoder()

df["target"] = label_encoder.fit_transform(
    df[target_column]
)


print("\n===== 정답 클래스 순서 =====")

for number, label in enumerate(
    label_encoder.classes_
):
    print(f"{number} = {label}")


# 위험 클래스 번호 확인
danger_class = label_encoder.transform(
    ["위험"]
)[0]

print("위험 클래스 번호:", danger_class)


# ============================================================
# 전체 데이터 확인
# ============================================================

print("\n===== 전체 데이터 확인 =====")
print("전체 데이터 크기:", df.shape)
print("게이트 개수:", df["gate_id"].nunique())

print("\n전체 클래스 분포:")

print(
    df[target_column]
    .value_counts()
    .reindex(
        ["위험", "정상", "주의"],
        fill_value=0,
    )
)


# ============================================================
# 게이트별 시간순 데이터 분할
#
# 각 게이트별:
# 앞 70%     = Train
# 다음 10%   = Validation
# 마지막 20% = Test
# ============================================================

train_parts = []
validation_parts = []
test_parts = []


for gate_id, gate_df in df.groupby("gate_id"):

    # 게이트별 시간순 정렬
    gate_df = (
        gate_df
        .sort_values("collected_at")
        .reset_index(drop=True)
    )

    total_count = len(gate_df)

    # Train 종료 위치
    train_end = int(total_count * 0.70)

    # Validation 종료 위치
    validation_end = int(total_count * 0.80)

    # 앞 70%
    train_parts.append(
        gate_df.iloc[:train_end]
    )

    # 다음 10%
    validation_parts.append(
        gate_df.iloc[
            train_end:validation_end
        ]
    )

    # 마지막 20%
    test_parts.append(
        gate_df.iloc[validation_end:]
    )


# 게이트별 분할 결과 결합
train_df = pd.concat(
    train_parts,
    ignore_index=True,
)

validation_df = pd.concat(
    validation_parts,
    ignore_index=True,
)

test_df = pd.concat(
    test_parts,
    ignore_index=True,
)


# ============================================================
# 입력 변수와 정답 분리
# ============================================================

X_train = train_df[feature_columns]
y_train = train_df["target"]

X_val = validation_df[feature_columns]
y_val = validation_df["target"]

X_test = test_df[feature_columns]
y_test = test_df["target"]


# ============================================================
# 데이터 분할 결과
# ============================================================

print("\n===== 데이터 분할 결과 =====")
print("전체 데이터 크기:", df.shape)
print("Train 크기:", X_train.shape)
print("Validation 크기:", X_val.shape)
print("Test 크기:", X_test.shape)


print("\n===== Train 클래스 분포 =====")

print(
    train_df[target_column]
    .value_counts()
    .reindex(
        ["위험", "정상", "주의"],
        fill_value=0,
    )
)


print("\n===== Validation 클래스 분포 =====")

print(
    validation_df[target_column]
    .value_counts()
    .reindex(
        ["위험", "정상", "주의"],
        fill_value=0,
    )
)


print("\n===== Test 클래스 분포 =====")

print(
    test_df[target_column]
    .value_counts()
    .reindex(
        ["위험", "정상", "주의"],
        fill_value=0,
    )
)


# ============================================================
# 클래스 불균형 보정
# ============================================================

# 모든 클래스에 balanced 가중치 적용
sample_weights = compute_sample_weight(
    class_weight="balanced",
    y=y_train,
)

sample_weights = np.asarray(
    sample_weights,
    dtype=float,
)


# 위험 클래스에 추가 가중치 배수 적용
danger_mask = (
    y_train.to_numpy() == danger_class
)

sample_weights[
    danger_mask
] *= DANGER_WEIGHT_MULTIPLIER


print("\n===== 가중치 설정 =====")
print("위험 클래스 번호:", danger_class)

print(
    "위험 추가 가중치 배수:",
    DANGER_WEIGHT_MULTIPLIER,
)


# 클래스별 평균 가중치 출력
for class_number, class_name in enumerate(
    label_encoder.classes_
):

    class_mask = (
        y_train.to_numpy() == class_number
    )

    class_count = class_mask.sum()

    if class_count == 0:
        print(
            f"{class_name}: 개수=0"
        )
        continue

    print(
        f"{class_name}: "
        f"개수={class_count}, "
        f"평균 가중치="
        f"{sample_weights[class_mask].mean():.4f}"
    )


# ============================================================
# XGBoost 모델 생성
# ============================================================

model = XGBClassifier(
    objective="multi:softprob",
    num_class=3,

    # 최대 반복 횟수
    n_estimators=1000,

    learning_rate=0.05,
    max_depth=5,
    min_child_weight=1,
    gamma=0.0,

    subsample=0.8,
    colsample_bytree=0.8,

    eval_metric="mlogloss",

    # Validation 손실이 30회 동안
    # 개선되지 않으면 학습 종료
    early_stopping_rounds=30,

    random_state=42,
    n_jobs=-1,
)


# ============================================================
# 모델 학습
# ============================================================

model.fit(
    X_train,
    y_train,

    sample_weight=sample_weights,

    # Validation 데이터로 조기 종료 판단
    eval_set=[
        (X_val, y_val)
    ],

    verbose=True,
)


# ============================================================
# 학습 결과
# ============================================================

print("\n===== 학습 결과 =====")

print(
    "최적 반복 번호(0부터 시작):",
    model.best_iteration,
)

print(
    "최적 부스팅 라운드 수:",
    model.best_iteration + 1,
)

print(
    "최적 Validation mlogloss:",
    model.best_score,
)


# ============================================================
# 클래스 평가 순서
# ============================================================

class_order_text = [
    "위험",
    "정상",
    "주의",
]

class_order_number = label_encoder.transform(
    class_order_text
)


# ============================================================
# 공통 평가 함수
# ============================================================

def evaluate_model(
    split_name,
    X_data,
    y_true,
):
    # 클래스 예측
    y_pred = model.predict(
        X_data
    )

    # 숫자 클래스를 한글로 변환
    y_true_text = label_encoder.inverse_transform(
        np.asarray(y_true)
    )

    y_pred_text = label_encoder.inverse_transform(
        np.asarray(y_pred)
    )


    # 전체 평가 지표
    accuracy = accuracy_score(
        y_true,
        y_pred,
    )

    weighted_precision = precision_score(
        y_true,
        y_pred,
        average="weighted",
        zero_division=0,
    )

    weighted_recall = recall_score(
        y_true,
        y_pred,
        average="weighted",
        zero_division=0,
    )

    weighted_f1 = f1_score(
        y_true,
        y_pred,
        average="weighted",
        zero_division=0,
    )

    macro_f1 = f1_score(
        y_true,
        y_pred,
        average="macro",
        zero_division=0,
    )


    # 위험 클래스 평가 지표
    danger_precision = precision_score(
        y_true,
        y_pred,
        labels=[danger_class],
        average=None,
        zero_division=0,
    )[0]

    danger_recall = recall_score(
        y_true,
        y_pred,
        labels=[danger_class],
        average=None,
        zero_division=0,
    )[0]

    danger_f1 = f1_score(
        y_true,
        y_pred,
        labels=[danger_class],
        average=None,
        zero_division=0,
    )[0]


    # 성능 출력
    print(
        f"\n===== {split_name} 성능 ====="
    )

    print(
        f"Accuracy           : "
        f"{accuracy:.6f}"
    )

    print(
        f"Precision weighted : "
        f"{weighted_precision:.6f}"
    )

    print(
        f"Recall weighted    : "
        f"{weighted_recall:.6f}"
    )

    print(
        f"F1 weighted        : "
        f"{weighted_f1:.6f}"
    )

    print(
        f"Macro F1           : "
        f"{macro_f1:.6f}"
    )

    print(
        f"위험 Precision     : "
        f"{danger_precision:.6f}"
    )

    print(
        f"위험 Recall        : "
        f"{danger_recall:.6f}"
    )

    print(
        f"위험 F1            : "
        f"{danger_f1:.6f}"
    )


    # 클래스별 성능표
    print(
        f"\n===== "
        f"{split_name} Classification Report "
        f"====="
    )

    print(
        classification_report(
            y_true_text,
            y_pred_text,
            labels=class_order_text,
            zero_division=0,
            digits=6,
        )
    )


    # 혼동행렬
    print(
        f"===== "
        f"{split_name} Confusion Matrix "
        f"====="
    )

    print(
        confusion_matrix(
            y_true,
            y_pred,
            labels=class_order_number,
        )
    )


    return {
        "split": split_name,

        "danger_weight_multiplier":
            DANGER_WEIGHT_MULTIPLIER,

        "best_iteration":
            model.best_iteration,

        "best_boosting_rounds":
            model.best_iteration + 1,

        "accuracy":
            accuracy,

        "weighted_precision":
            weighted_precision,

        "weighted_recall":
            weighted_recall,

        "weighted_f1":
            weighted_f1,

        "macro_f1":
            macro_f1,

        "danger_precision":
            danger_precision,

        "danger_recall":
            danger_recall,

        "danger_f1":
            danger_f1,
    }


# ============================================================
# Validation 평가
# ============================================================

# 가중치와 설정 비교는
# Validation 성능으로만 진행한다.
validation_result = evaluate_model(
    "Validation",
    X_val,
    y_val,
)


results = [
    validation_result
]


# ============================================================
# Test 최종 평가
# ============================================================

# 최종 가중치를 선택한 후에만
# RUN_TEST를 True로 변경한다.
if RUN_TEST:

    test_result = evaluate_model(
        "Test",
        X_test,
        y_test,
    )

    results.append(
        test_result
    )


# ============================================================
# 결과 요약
# ============================================================

print("\n===== 실험 결과 요약 =====")

result_df = pd.DataFrame(
    results
)

print(
    result_df.to_string(
        index=False
    )
)