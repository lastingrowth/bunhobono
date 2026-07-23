# 시연용 더미 데이터 사용법

## 1. 데이터 입력

`demo_dummy_full.sql`은 스키마 생성용 `bono_db_통합.sql`을 먼저 실행한 뒤 사용한다.
이 파일은 기존 데이터를 `TRUNCATE`하므로 시연 DB에서만 실행한다.

```powershell
psql -U bono_user -d bono_db -f "C:\Users\503-30\Desktop\bunhobono\db\demo_dummy_full.sql"
```

회원부터 카메라까지의 데이터는 전달받은 두 번째 SQL을 그대로 사용하며,
시연용 차량/카메라/입출차/알림 데이터는 5절부터 추가되어 있다.
시간값은 `CURRENT_TIMESTAMP`와 `CURRENT_DATE` 기준이라 실행 시각보다 미래인
카메라 기록이나 입출차 기록을 만들지 않는다.

## 2. 포함된 시연 상황

- 필수 차량번호 9대: 입주민 7대, 방문 2대로 등록만 하고 입출차 기록에서는 제외
- 최근 22일 입차 비교: 매일 입주민 24건과 비입주민 20건을 기본으로 분산
- 현재 주차 현황: 입주민 30대, 방문 9대, 미등록 11대로 60:18:22 비율
- 시간대별 평균 주차 대수: 50, 50, 36, 10, 10, 10, 42, 50, 50
- A/B/C/D 주차장에 현재 주차 차량을 순환 분산
- 방문 승인 대기 3건
- 오전 승인·입차 후 만료됐지만 미출차한 방문차량 1건
- 방문 및 미등록 장기주차 알림의 미확인/확인/처리완료 상태
- OCR 성공/확인 필요 데이터를 혼합
- 모든 카로그에 입차 카메라 데이터 연결, 출차 완료 로그에는 출차 데이터도 연결
- 2026년 1~6월 연간 그래프용 입주민/방문/미등록 지난 기록 추가
- 활성 카로그 약 94건, 활성 카메라 데이터 약 138건으로 축소
- 통계용 CAR_LOG 지난 기록은 일반 지난 기록을 포함해 3,468건
- 방문 평균 약 4시간 30분, 미등록 평균 약 1시간 30분이 되도록 과거 기록 구성
- 지난 기록 36건으로 페이지 크기 10 기준 4페이지 제공
- 만기된 입주민 차량은 현재 차량 목록에 두지 않고 새 1년 승인 등록만 유지

## 3. OCR 이미지가 보이는 원리

백엔드의 `CameraDataController`는 DB의 `camera_data.image_path` 및
`crop_image_path`를 읽어 `/api/camera-data/{번호}/image`와
`/api/camera-data/{번호}/crop-image`로 반환한다.

`val.txt`의 번호를 실제 이미지와 대조한 이미지 100장을 다음 Git 공유 폴더에 넣었다.

```text
C:\Users\503-30\Desktop\bunhobono\backend\camera-data
```

Git에는 `backend/camera-data`를 포함한다. DB에는 `camera-data/파일명` 형식의
프로젝트 상대경로를 저장한다. 애플리케이션 코드는 변경하지 않았으므로 팀원도
`backend` 폴더를 작업 디렉터리로 백엔드를 실행해야 동일한 사진이 표시된다.

다른 이미지를 추가하려면 파일을 위 폴더로 복사한 뒤 아래처럼 넣으면 된다.

```sql
INSERT INTO camera_data
    (camera_no, vehicle_car_no, car_no, ocr_car_no, capture_time,
     image_path, crop_image_path, recognition_state, confidence_score)
VALUES
    (1, NULL, '12가3456', '12가3456', CURRENT_TIMESTAMP,
     'camera-data/example.jpeg',
     'camera-data/example_crop.jpeg',
     TRUE, 98.50);
```

Windows 경로도 SQL 안에서는 `/`를 사용하면 이스케이프 문제를 피할 수 있다.

## 4. 지난 기록 통계

통계 화면의 `statisticsStore.js`만 수정하여 `trash_bin`의 `CAR_LOG` JSON을 함께
조회한다. `statistics_scope=HOURLY`는 시간대 평균에만, `ENTRY_AVERAGE`는
주간/월간/연간 입차 비교와 평균 주차시간에만 사용하므로 중복 집계되지 않는다.
알림, 카로그, 카메라 데이터 기능 코드는 수정하지 않는다.
