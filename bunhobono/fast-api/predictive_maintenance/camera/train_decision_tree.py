# pands == 데이터 분석 라이브러리 
from pathlib import Path

import pandas as pd
from sklearn.model_selection import train_test_split
# DecisionTreeClassifier 클래스 가져옴 
from sklearn.tree import DecisionTreeClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report
)


# CSV 불러오기
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

# 입력값(X), 정답(y) 분리
X = df[feature_columns]
y = df[target_column]

# ============================
# 학습용 데이터와 테스트용 데이터 분리
# ============================
# X_train = 모델 학습에 사용할 입력 데이터
# X_test  = 학습 후 시험에 사용할 입력 데이터
# y_train = X_train에 대응하는 정답
# y_test  = X_test에 대응하는 정답

X_train, X_test, y_train, y_test = train_test_split(
    X,                  # 입력 데이터
    y,                  # 정답 데이터
    test_size=0.2,      # 전체 데이터 중 20%를 테스트용으로 사용
    random_state=42,    # 실행할 때마다 동일하게 데이터를 나누기 위한 고정값
    stratify=y,         # 정상/주의/위험 비율을 비슷하게 유지
)

# ============================
# Decision Tree 분류 모델 객체 생성
# ============================

model = DecisionTreeClassifier(
    criterion="gini",        # 불순도 계산 방식(Gini Index)
    max_depth=5,             # 트리의 최대 깊이
    random_state=42,         # 결과 재현을 위한 고정값
    class_weight="balanced"  # 클래스 불균형 보정
)


# ============================
# Decision Tree 모델 학습
# ============================
# model → 방금 만든 Decision Tree 모델 객체
# fit() → 학습시키는 메서드
# X_train → 온도, 전압, 성공률, 오류 횟수, 유지보수 경과일
# y_train → 각 데이터의 실제 정답인 정상·주의·위험
model.fit(X_train, y_train)




# 데이터 크기 확인
print("입력 데이터 크기:", X.shape)
print("정답 데이터 크기:", y.shape)

# 입력 변수 확인
print("\n사용할 입력 컬럼:")
print(X.columns.tolist())

# 위험 등급(Label) 분포 확인
print("\n위험 등급별 데이터 개수:")
print(y.value_counts())

# 결측치 확인
print("\n결측치 개수:")
print(X.isnull().sum())

# 학습용/테스트용 데이터 크기 확인
print("\n학습/테스트 데이터 분리 결과:")
print("X_train 크기:", X_train.shape)
print("X_test 크기:", X_test.shape)
print("y_train 크기:", y_train.shape)
print("y_test 크기:", y_test.shape)


# 학습진행 성공인지 확인 
print("\nDecision Tree 모델 학습 완료")

# ============================
# 테스트 데이터 예측
# ============================

# predict() → 학습된 모델을 이용하여 테스트 데이터 예측
# X_test → 처음 보는 테스트 데이터
# y_pred → 모델이 예측한 결과 저장
y_pred = model.predict(X_test)

print("\n테스트 데이터 예측 완료")

# ============================
# 모델 성능 평가
# ============================

accuracy = accuracy_score(y_test, y_pred)
precision = precision_score(y_test, y_pred, average="weighted")
recall = recall_score(y_test, y_pred, average="weighted")
f1 = f1_score(y_test, y_pred, average="weighted")

print("\n===== 모델 성능 평가 =====")
print("Accuracy :", accuracy)
print("Precision :", precision)
print("Recall :", recall)
print("F1 Score :", f1)

print("\n===== Classification Report =====")
print(classification_report(y_test, y_pred))

print("\n===== Confusion Matrix =====")
print(confusion_matrix(y_test, y_pred))
