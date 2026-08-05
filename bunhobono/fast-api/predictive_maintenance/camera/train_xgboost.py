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
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_sample_weight
from xgboost import XGBClassifier


# ============================================================
# 실험 1: 현재 설정 + 위험 클래스 추가 가중치 1.0배
# - 기존 balanced 가중치는 그대로 적용
# - 위험 클래스에 추가 가중치를 주지 않는 기준 모델
# - 이후 1.5배, 2.0배, 3.0배 실험과 비교하기 위한 Baseline
# ============================================================
DANGER_WEIGHT_MULTIPLIER = 3.5


# ============================
# 데이터 불러오기
# ============================
df = pd.read_csv("data/predictive_maintenance_timeseries_synthetic.csv")

feature_columns = [
    "temperature_c",
    "voltage_v",
    "success_rate",
    "error_count",
    "days_since_maintenance",
]
target_column = "risk_level"

X = df[feature_columns]
y_text = df[target_column]


# ============================
# 클래스 숫자 변환
# ============================
label_encoder = LabelEncoder()
y = label_encoder.fit_transform(y_text)

print("정답 클래스 순서:")
for number, label in enumerate(label_encoder.classes_):
    print(f"{number} = {label}")

danger_class = label_encoder.transform(["위험"])[0]


# ============================
# Train 70% / Validation 10% / Test 20%
# ============================
X_temp, X_test, y_temp, y_test = train_test_split(
    X,
    y,
    test_size=0.2,
    random_state=42,
    stratify=y,
)

X_train, X_val, y_train, y_val = train_test_split(
    X_temp,
    y_temp,
    test_size=0.125,
    random_state=42,
    stratify=y_temp,
)

print("\n===== 데이터 분리 결과 =====")
print("전체 데이터 크기:", X.shape)
print("Train 크기:", X_train.shape)
print("Validation 크기:", X_val.shape)
print("Test 크기:", X_test.shape)


# ============================
# 클래스 불균형 보정
# ============================
# 먼저 전체 클래스에 balanced 가중치를 적용한다.
sample_weights = compute_sample_weight(
    class_weight="balanced",
    y=y_train,
)

# 위험 클래스에 추가 배수를 적용한다.
# 실험 1에서는 1.0배이므로 기존 balanced 결과와 동일하다.
sample_weights = np.asarray(sample_weights, dtype=float)
sample_weights[y_train == danger_class] *= DANGER_WEIGHT_MULTIPLIER

print("\n===== 가중치 설정 =====")
print("위험 클래스 번호:", danger_class)
print("위험 추가 가중치 배수:", DANGER_WEIGHT_MULTIPLIER)

for class_number, class_name in enumerate(label_encoder.classes_):
    class_mask = y_train == class_number
    print(
        f"{class_name}: 개수={class_mask.sum()}, "
        f"평균 가중치={sample_weights[class_mask].mean():.4f}"
    )


# ============================
# 기준 XGBoost 모델
# ============================
model = XGBClassifier(
    objective="multi:softprob",
    num_class=3,
    n_estimators=1000,
    learning_rate=0.05,
    max_depth=5,
    min_child_weight=1,
    gamma=0.0,  # 다음 실험
    subsample=0.8,
    colsample_bytree=0.8,
    eval_metric="mlogloss",
    early_stopping_rounds=30,
    random_state=42,
    n_jobs=-1,
)

model.fit(
    X_train,
    y_train,
    sample_weight=sample_weights,
    eval_set=[(X_val, y_val)],
    verbose=True,
)

print("\n===== 학습 결과 =====")
print("최적 반복 번호(0부터 시작):", model.best_iteration)
print("최적 부스팅 라운드 수:", model.best_iteration + 1)
print("최적 검증 mlogloss:", model.best_score)


# ============================
# 공통 평가 함수
# ============================
class_order_text = ["위험", "정상", "주의"]
class_order_number = label_encoder.transform(class_order_text)


def evaluate_model(split_name, X_data, y_true):
    y_pred = model.predict(X_data)
    y_true_text = label_encoder.inverse_transform(y_true)
    y_pred_text = label_encoder.inverse_transform(y_pred)

    accuracy = accuracy_score(y_true, y_pred)
    weighted_precision = precision_score(
        y_true, y_pred, average="weighted", zero_division=0
    )
    weighted_recall = recall_score(
        y_true, y_pred, average="weighted", zero_division=0
    )
    weighted_f1 = f1_score(
        y_true, y_pred, average="weighted", zero_division=0
    )
    macro_f1 = f1_score(
        y_true, y_pred, average="macro", zero_division=0
    )
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

    print(f"\n===== {split_name} 성능 =====")
    print(f"Accuracy           : {accuracy:.6f}")
    print(f"Precision weighted : {weighted_precision:.6f}")
    print(f"Recall weighted    : {weighted_recall:.6f}")
    print(f"F1 weighted        : {weighted_f1:.6f}")
    print(f"Macro F1           : {macro_f1:.6f}")
    print(f"위험 Precision     : {danger_precision:.6f}")
    print(f"위험 Recall        : {danger_recall:.6f}")
    print(f"위험 F1            : {danger_f1:.6f}")

    print(f"\n===== {split_name} Classification Report =====")
    print(
        classification_report(
            y_true_text,
            y_pred_text,
            labels=class_order_text,
            zero_division=0,
        )
    )

    print(f"===== {split_name} Confusion Matrix =====")
    print(
        confusion_matrix(
            y_true,
            y_pred,
            labels=class_order_number,
        )
    )

    return {
        "split": split_name,
        "danger_weight_multiplier": DANGER_WEIGHT_MULTIPLIER,
        "best_iteration": model.best_iteration,
        "accuracy": accuracy,
        "weighted_f1": weighted_f1,
        "macro_f1": macro_f1,
        "danger_precision": danger_precision,
        "danger_recall": danger_recall,
        "danger_f1": danger_f1,
    }


# 설정 비교에는 Validation 결과를 사용한다.
validation_result = evaluate_model("Validation", X_val, y_val)

# Test 결과는 최종 확인용이다. 여러 설정을 Test 결과로 선택하지 않는다.
test_result = evaluate_model("Test", X_test, y_test)

print("\n===== 실험 2 비교용 요약 =====")
print(pd.DataFrame([validation_result, test_result]).to_string(index=False))
