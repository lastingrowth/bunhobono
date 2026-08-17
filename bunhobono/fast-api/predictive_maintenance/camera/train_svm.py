# pandas == 데이터 분석 라이브러리
from pathlib import Path

import pandas as pd

# 학습용 데이터와 테스트용 데이터 분리
from sklearn.model_selection import train_test_split

# 입력 변수 표준화
from sklearn.preprocessing import StandardScaler

# Support Vector Machine 분류 모델 클래스
from sklearn.svm import SVC

# 모델 성능 평가 함수
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report,
)

# ============================
# CSV 불러오기
# ============================
CSV_PATH = Path(__file__).resolve().parents[2] / "data" / "camera" / "predictive_maintenance_timeseries_synthetic.csv"
df = pd.read_csv(CSV_PATH)

# ============================
# 학습에 사용할 입력 변수(X)
# ============================
feature_columns = [
    "temperature_c",             # 카메라 장비 온도(℃)
    "voltage_v",                 # 카메라 공급 전압(V)
    "success_rate",              # OCR 번호판 인식 성공률(%)
    "error_count",               # 카메라 오류 발생 횟수
    "days_since_maintenance",    # 최근 유지보수 후 경과일
]

# ============================
# 예측할 정답(Label, y)
# ============================
target_column = "risk_level"     # 위험 등급(정상 / 주의 / 위험)

# 입력값(X), 정답값(y) 분리
X = df[feature_columns]
y = df[target_column]

# ============================
# 학습용 데이터와 테스트용 데이터 분리
# ============================
X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.2,      # 전체 데이터의 20%를 테스트 데이터로 사용
    random_state=42,    # 항상 동일한 데이터 분할 결과 유지
    stratify=y,         # 정상/주의/위험 클래스 비율 유지
)

# ============================
# 입력 변수 표준화
# ============================
scaler = StandardScaler()

# 학습 데이터의 평균과 표준편차를 계산한 뒤 변환
X_train_scaled = scaler.fit_transform(X_train)

# 학습 데이터에서 계산한 기준으로 테스트 데이터 변환
X_test_scaled = scaler.transform(X_test)

print("\n입력 데이터 표준화 완료")

# ============================
# SVM 분류 모델 객체 생성
# ============================
model = SVC(
    kernel="rbf",               # 비선형 분류를 위한 RBF 커널
    C=1.0,                      # 오분류 허용 정도를 조절하는 규제값
    gamma="scale",              # 각 데이터가 결정 경계에 미치는 범위
    class_weight="balanced",    # 클래스 불균형 보정
    decision_function_shape="ovr",
)

# ============================
# SVM 모델 학습
# ============================
model.fit(X_train_scaled, y_train)

print("SVM 모델 학습 완료")

# ============================
# 테스트 데이터 예측
# ============================
y_pred = model.predict(X_test_scaled)

print("테스트 데이터 예측 완료")

# ============================
# 모델 성능 평가
# ============================
accuracy = accuracy_score(y_test, y_pred)

precision = precision_score(
    y_test,
    y_pred,
    average="weighted",
    zero_division=0,
)

recall = recall_score(
    y_test,
    y_pred,
    average="weighted",
    zero_division=0,
)

f1 = f1_score(
    y_test,
    y_pred,
    average="weighted",
    zero_division=0,
)

print("\n===== SVM 모델 성능 평가 =====")
print("Accuracy :", accuracy)
print("Precision :", precision)
print("Recall :", recall)
print("F1 Score :", f1)

print("\n===== Classification Report =====")
print(classification_report(y_test, y_pred, zero_division=0))

print("\n===== Confusion Matrix =====")
print(confusion_matrix(y_test, y_pred))
