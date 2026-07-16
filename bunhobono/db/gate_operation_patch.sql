-- 기존 bono_db에 게이트 작동 및 중복 입차 방지를 추가할 때 한 번 실행한다.
ALTER TABLE gate
    ADD COLUMN IF NOT EXISTS gate_status INT NOT NULL DEFAULT 0;

ALTER TABLE gate
    DROP CONSTRAINT IF EXISTS gate_gate_status_check;

ALTER TABLE gate
    ADD CONSTRAINT gate_gate_status_check
        CHECK (gate_status IN (0, 1));

-- 인덱스 생성 전 아래 조회 결과가 있다면 중복된 미출차 기록을 먼저 정리해야 한다.
-- SELECT vehicle_car_no, COUNT(*)
-- FROM car_log
-- WHERE out_time IS NULL AND vehicle_car_no IS NOT NULL
-- GROUP BY vehicle_car_no HAVING COUNT(*) > 1;

-- SELECT snapshot_car_no, COUNT(*)
-- FROM car_log
-- WHERE out_time IS NULL AND vehicle_car_no IS NULL
-- GROUP BY snapshot_car_no HAVING COUNT(*) > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_car_log_open_vehicle
    ON car_log (vehicle_car_no)
    WHERE out_time IS NULL AND vehicle_car_no IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_car_log_open_snapshot_car
    ON car_log (snapshot_car_no)
    WHERE out_time IS NULL
      AND vehicle_car_no IS NULL
      AND snapshot_car_no IS NOT NULL;
