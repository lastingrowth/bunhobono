# 3개월 지난 카로그 통계 포함 — 최소 백엔드 변경안

> 상태: **승인 전 / 백엔드 미적용**

## 먼저 확인된 현재 스케줄 기준

- `car_log`: 출차 완료 후 **15일**이 지나면 `trash_bin`으로 이동
- `camera_data`: 촬영 후 **3개월**이 지나면 `trash_bin`으로 이동

따라서 3개월 지난 입출차 통계는 `CAMERA_DATA`가 아니라, 이미 15일 시점에
이동한 `trash_bin`의 `CAR_LOG` JSON을 읽어 계산합니다.

## 변경 파일

`backend/src/main/java/api/trash_p/TrashMapper.java`

`saveCarLog()` 한 곳만 변경합니다. 테이블과 API는 변경하지 않습니다.

## 필요한 이유

스케줄러가 `car_log`를 `trash_bin`으로 옮기면 `in_time`, `out_time`,
`vehicle_car_no` 등은 JSON에 남습니다. 그러나 나중에 등록 차량이 삭제되면
`vehicle_car_no`만으로 입주민과 방문 차량을 구분할 수 없습니다.

이동 시점의 차량 종류인 `car_kind` 하나만 JSON에 스냅샷으로 보관하면,
프런트 통계 스토어가 지난 기록을 정확하게 분류할 수 있습니다.

## 원본 코드

```java
// car_log를 JSON으로 변환해 휴지통에 저장
@Insert("INSERT INTO trash_bin " +
        "(data_type, original_no, data_json, delete_type) " +
        "SELECT 'CAR_LOG', cl.car_log_no, " +
        "to_jsonb(cl) || jsonb_build_object(" +
        "'captured_car_no', COALESCE(cd.car_no, cl.snapshot_car_no)), " +
        "#{deleteType} " +
        "FROM car_log cl " +
        "LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no " +
        "WHERE cl.car_log_no = #{carLogNo}")
int saveCarLog(
        @Param("carLogNo") int carLogNo,
        @Param("deleteType") String deleteType
);
```

## 최소 수정 코드

```java
// car_log를 JSON으로 변환해 휴지통에 저장
// [지난 기록 통계] 이동 당시 차량 종류를 JSON에 보관해 통계 분류에 사용한다.
@Insert("INSERT INTO trash_bin " +
        "(data_type, original_no, data_json, delete_type) " +
        "SELECT 'CAR_LOG', cl.car_log_no, " +
        "to_jsonb(cl) || jsonb_build_object(" +
        "'captured_car_no', COALESCE(cd.car_no, cl.snapshot_car_no), " +
        "'car_kind', CASE " +
        "    WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN' " +
        "    WHEN vc.vehicle_type = 'visit' THEN 'VISIT' " +
        "    ELSE 'REGISTERED' " +
        "END), " +
        "#{deleteType} " +
        "FROM car_log cl " +
        "LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no " +
        "LEFT JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no " +
        "WHERE cl.car_log_no = #{carLogNo}")
int saveCarLog(
        @Param("carLogNo") int carLogNo,
        @Param("deleteType") String deleteType
);
```

## 실제 차이

```diff
 to_jsonb(cl) || jsonb_build_object(
-    'captured_car_no', COALESCE(cd.car_no, cl.snapshot_car_no)
+    'captured_car_no', COALESCE(cd.car_no, cl.snapshot_car_no),
+    'car_kind', CASE
+        WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN'
+        WHEN vc.vehicle_type = 'visit' THEN 'VISIT'
+        ELSE 'REGISTERED'
+    END
 ),
 FROM car_log cl
 LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no
+LEFT JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no
```

## 변경하지 않는 항목

- DB 테이블 및 컬럼
- 스케줄러
- 카로그 삭제 및 복원
- 카메라 데이터와 이미지
- 알림
- API 주소와 응답 DTO

`restoreCarLog()`는 기존 필드만 읽으므로 JSON에 추가된 `car_kind`는 복원에
영향을 주지 않습니다.

## 프런트 통계 처리

통계 스토어는 다음처럼 처리합니다.

1. 현재 `car_log`와 `trash_bin`의 `CAR_LOG` 기록을 함께 조회합니다.
2. 입차 비교와 평균 주차 시간은 두 목록을 합쳐 계산합니다.
3. 날짜는 기존 `in_time`, 체류 시간은 기존 `in_time/out_time`을 사용합니다.
4. 차량 종류는 새 `car_kind` 스냅샷을 사용합니다.
5. 승인 전의 기존 지난 기록은 현재 `vehicle_car` 목록으로 임시 분류합니다.
6. 차량 등록까지 삭제된 기존 기록은 임시 분류가 불가능하므로 `UNKNOWN` 처리됩니다.

## 시간대별 평균 주차 대수

- 시연 날짜에 `HOURLY` 스냅샷이 있으면 기존 요청 곡선을 유지합니다.
- 스냅샷이 없는 실제 날짜는 현재 기록과 지난 기록의 체류 구간을 계산합니다.

```text
in_time <= 집계 시각 < out_time
```

따라서 스케줄러가 3개월 지난 카로그를 이동해도 통계에서 제외되지 않습니다.
같은 날짜의 시연 스냅샷과 실제 카로그는 동시에 세지 않아 중복 집계하지 않습니다.
