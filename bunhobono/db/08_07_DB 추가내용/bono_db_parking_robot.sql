BEGIN;



DROP VIEW IF EXISTS robot_job_detail;
DROP VIEW IF EXISTS robot_task_detail;
DROP VIEW IF EXISTS parking_session_detail;
DROP VIEW IF EXISTS parking_process_detail;
DROP VIEW IF EXISTS car_log_detail;

DROP TABLE IF EXISTS robot_pdm;
DROP TABLE IF EXISTS robot_log;
DROP TABLE IF EXISTS robot_job;
DROP TABLE IF EXISTS robot_task;
DROP TABLE IF EXISTS parking_sensor;
DROP TABLE IF EXISTS parking_session;
DROP TABLE IF EXISTS parking_process;
DROP TABLE IF EXISTS parking_space;
DROP TABLE IF EXISTS vehicle_access;
DROP TABLE IF EXISTS robot;


-- =====================================================
-- 1. 기존 PARKING 보강
-- =====================================================

ALTER TABLE parking
    ADD COLUMN IF NOT EXISTS parking_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS parking_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

UPDATE parking
SET parking_code = 'LEGACY-' || parking_no,
    parking_type = 'LEGACY',
    active = FALSE;

INSERT INTO parking (
    parking_no,
    parking_code,
    parking_name,
    parking_type,
    parking_spaces,
    parking_location,
    active
)
VALUES
    (1, 'SURFACE', '지상 주차장', 'SURFACE', 0,   '아파트 지상', TRUE),
    (2, 'B1',      'B1 주차장',   'ROBOT',   150, '지하 1층',    TRUE),
    (3, 'B2',      'B2 주차장',   'GENERAL', 100, '지하 2층',    TRUE)
ON CONFLICT (parking_no) DO UPDATE
SET parking_code = EXCLUDED.parking_code,
    parking_name = EXCLUDED.parking_name,
    parking_type = EXCLUDED.parking_type,
    parking_spaces = EXCLUDED.parking_spaces,
    parking_location = EXCLUDED.parking_location,
    active = EXCLUDED.active;

ALTER TABLE parking
    ALTER COLUMN parking_code SET NOT NULL,
    ALTER COLUMN parking_type SET NOT NULL,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL;

ALTER TABLE parking
    DROP CONSTRAINT IF EXISTS parking_parking_code_key;

ALTER TABLE parking
    ADD CONSTRAINT parking_parking_code_key
        UNIQUE (parking_code);

SELECT setval(
    pg_get_serial_sequence('parking', 'parking_no'),
    (SELECT MAX(parking_no) FROM parking),
    TRUE
);


-- =====================================================
-- 2. 기존 GATE 보강 및 12개 게이트 설정
-- =====================================================

ALTER TABLE gate
    ALTER COLUMN parking_no DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS gate_code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS gate_area VARCHAR(20),
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

UPDATE gate
SET gate_code = 'LEGACY-' || gate_no,
    gate_area = 'LEGACY',
    active = FALSE;

INSERT INTO gate (
    gate_no,
    parking_no,
    gate_code,
    gate_name,
    gate_type,
    gate_area,
    gate_status,
    active
)
VALUES
    (1,  NULL, 'MAIN-IN',  '정문 입구게이트',     'In',  'SITE', 0, TRUE),
    (2,  NULL, 'MAIN-OUT', '정문 출구게이트',     'Out', 'SITE', 0, TRUE),
    (3,  NULL, 'REAR-IN',  '후문 입구게이트',     'In',  'SITE', 0, TRUE),
    (4,  NULL, 'REAR-OUT', '후문 출구게이트',     'Out', 'SITE', 0, TRUE),
    (5,  2,    'B1-IN-1',  'B1 주차장 입구1',     'In',  'B1',   0, TRUE),
    (6,  2,    'B1-OUT-1', 'B1 주차장 출구1',     'Out', 'B1',   0, TRUE),
    (7,  2,    'B1-IN-2',  'B1 주차장 입구2',     'In',  'B1',   0, TRUE),
    (8,  2,    'B1-OUT-2', 'B1 주차장 출구2',     'Out', 'B1',   0, TRUE),
    (9,  3,    'B2-IN-1',  'B2 주차장 입구1',     'In',  'B2',   0, TRUE),
    (10, 3,    'B2-OUT-1', 'B2 주차장 출구1',     'Out', 'B2',   0, TRUE),
    (11, 3,    'B2-IN-2',  'B2 주차장 입구2',     'In',  'B2',   0, TRUE),
    (12, 3,    'B2-OUT-2', 'B2 주차장 출구2',     'Out', 'B2',   0, TRUE)
ON CONFLICT (gate_no) DO UPDATE
SET parking_no = EXCLUDED.parking_no,
    gate_code = EXCLUDED.gate_code,
    gate_name = EXCLUDED.gate_name,
    gate_type = EXCLUDED.gate_type,
    gate_area = EXCLUDED.gate_area,
    gate_status = EXCLUDED.gate_status,
    active = EXCLUDED.active;

ALTER TABLE gate
    ALTER COLUMN gate_code SET NOT NULL,
    ALTER COLUMN gate_area SET NOT NULL,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL;

ALTER TABLE gate
    DROP CONSTRAINT IF EXISTS gate_gate_code_key;

ALTER TABLE gate
    ADD CONSTRAINT gate_gate_code_key
        UNIQUE (gate_code);

SELECT setval(
    pg_get_serial_sequence('gate', 'gate_no'),
    (SELECT MAX(gate_no) FROM gate),
    TRUE
);


-- =====================================================
-- 3. 기존 CAMERA 보강 및 게이트별 카메라 설정
-- =====================================================

ALTER TABLE camera
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

UPDATE camera
SET active = FALSE;

INSERT INTO camera (
    camera_no,
    gate_no,
    camera_name,
    camera_type,
    install_date,
    active
)
VALUES
    (1,  1,  '정문 입구게이트 카메라', 'In',  CURRENT_DATE, TRUE),
    (2,  2,  '정문 출구게이트 카메라', 'Out', CURRENT_DATE, TRUE),
    (3,  3,  '후문 입구게이트 카메라', 'In',  CURRENT_DATE, TRUE),
    (4,  4,  '후문 출구게이트 카메라', 'Out', CURRENT_DATE, TRUE),
    (5,  5,  'B1 주차장 입구1 카메라', 'In',  CURRENT_DATE, TRUE),
    (6,  6,  'B1 주차장 출구1 카메라', 'Out', CURRENT_DATE, TRUE),
    (7,  7,  'B1 주차장 입구2 카메라', 'In',  CURRENT_DATE, TRUE),
    (8,  8,  'B1 주차장 출구2 카메라', 'Out', CURRENT_DATE, TRUE),
    (9,  9,  'B2 주차장 입구1 카메라', 'In',  CURRENT_DATE, TRUE),
    (10, 10, 'B2 주차장 출구1 카메라', 'Out', CURRENT_DATE, TRUE),
    (11, 11, 'B2 주차장 입구2 카메라', 'In',  CURRENT_DATE, TRUE),
    (12, 12, 'B2 주차장 출구2 카메라', 'Out', CURRENT_DATE, TRUE)
ON CONFLICT (camera_no) DO UPDATE
SET gate_no = EXCLUDED.gate_no,
    camera_name = EXCLUDED.camera_name,
    camera_type = EXCLUDED.camera_type,
    install_date = EXCLUDED.install_date,
    active = EXCLUDED.active;

ALTER TABLE camera
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL;

SELECT setval(
    pg_get_serial_sequence('camera', 'camera_no'),
    (SELECT MAX(camera_no) FROM camera),
    TRUE
);


-- =====================================================
-- 4. 기존 CAMERA DATA 보강
-- =====================================================

ALTER TABLE camera_data
    ADD COLUMN IF NOT EXISTS gate_opened BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS gate_opened_at TIMESTAMP;

UPDATE camera_data
SET gate_opened = FALSE
WHERE gate_opened IS NULL;

ALTER TABLE camera_data
    ALTER COLUMN gate_opened SET DEFAULT FALSE,
    ALTER COLUMN gate_opened SET NOT NULL;


-- =====================================================
-- 5. PARKING SPACE
-- B1 주차면 150개, 입차대기면 4개, 출차대기면 6개
-- =====================================================

CREATE TABLE parking_space (
    space_no BIGSERIAL PRIMARY KEY,

    parking_no INT NOT NULL,
    gate_no INT,
    car_log_no INT UNIQUE,

    space_code VARCHAR(20) NOT NULL UNIQUE,
    space_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_space_parking
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT,

    CONSTRAINT fk_space_gate
        FOREIGN KEY (gate_no)
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_space_car_log
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE SET NULL
);

INSERT INTO parking_space (
    parking_no,
    space_code,
    space_type
)
SELECT
    parking_no,
    'B1-P' || LPAD(no::TEXT, 3, '0'),
    'PARKING'
FROM parking
CROSS JOIN generate_series(1, 150) AS no
WHERE parking_code = 'B1';

INSERT INTO parking_space (
    parking_no,
    gate_no,
    space_code,
    space_type
)
SELECT
    parking.parking_no,
    gate.gate_no,
    space.space_code,
    space.space_type
FROM (
    VALUES
        ('B1-IN1-01',  'ENTRY_WAIT', 'B1-IN-1'),
        ('B1-IN1-02',  'ENTRY_WAIT', 'B1-IN-1'),
        ('B1-IN2-01',  'ENTRY_WAIT', 'B1-IN-2'),
        ('B1-IN2-02',  'ENTRY_WAIT', 'B1-IN-2'),
        ('B1-OUT1-01', 'EXIT_WAIT',  'B1-OUT-1'),
        ('B1-OUT1-02', 'EXIT_WAIT',  'B1-OUT-1'),
        ('B1-OUT1-03', 'EXIT_WAIT',  'B1-OUT-1'),
        ('B1-OUT2-01', 'EXIT_WAIT',  'B1-OUT-2'),
        ('B1-OUT2-02', 'EXIT_WAIT',  'B1-OUT-2'),
        ('B1-OUT2-03', 'EXIT_WAIT',  'B1-OUT-2')
) AS space(space_code, space_type, gate_code)
JOIN parking
    ON parking.parking_code = 'B1'
JOIN gate
    ON gate.gate_code = space.gate_code;


-- =====================================================
-- 6. ROBOT
-- 2대 1세트, 총 4세트·8대
-- =====================================================

CREATE TABLE robot (
    robot_no BIGSERIAL PRIMARY KEY,
    robot_code VARCHAR(20) NOT NULL UNIQUE,

    set_no INT NOT NULL,
    set_position VARCHAR(1) NOT NULL
        CHECK (set_position IN ('A', 'B')),

    robot_status VARCHAR(20) NOT NULL DEFAULT 'STANDBY',
    battery_level NUMERIC(5,2),
    operating_hours NUMERIC(12,2) NOT NULL DEFAULT 0,

    last_heartbeat_at TIMESTAMPTZ,
    last_maintenance_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_robot_set_position
        UNIQUE (set_no, set_position)
);

INSERT INTO robot (
    robot_code,
    set_no,
    set_position,
    battery_level
)
VALUES
    ('ROBOT-01A', 1, 'A', 100),
    ('ROBOT-01B', 1, 'B', 100),
    ('ROBOT-02A', 2, 'A', 100),
    ('ROBOT-02B', 2, 'B', 100),
    ('ROBOT-03A', 3, 'A', 100),
    ('ROBOT-03B', 3, 'B', 100),
    ('ROBOT-04A', 4, 'A', 100),
    ('ROBOT-04B', 4, 'B', 100);


-- =====================================================
-- 7. ROBOT TASK
-- 로봇의 입차·출차 이동 작업
-- =====================================================

CREATE TABLE robot_task (
    task_no BIGSERIAL PRIMARY KEY,

    car_log_no INT NOT NULL,
    pickup_space_no BIGINT NOT NULL,
    dropoff_space_no BIGINT NOT NULL,
    set_no INT,

    task_type VARCHAR(20) NOT NULL,
    task_phase VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    task_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    priority INT NOT NULL DEFAULT 0,

    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),

    CONSTRAINT fk_task_car_log
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE RESTRICT,

    CONSTRAINT fk_task_pickup_space
        FOREIGN KEY (pickup_space_no)
        REFERENCES parking_space(space_no)
        ON DELETE RESTRICT,

    CONSTRAINT fk_task_dropoff_space
        FOREIGN KEY (dropoff_space_no)
        REFERENCES parking_space(space_no)
        ON DELETE RESTRICT
);


-- =====================================================
-- 8. ROBOT LOG
-- 가상 로봇이 전송하는 원시 상태값
-- =====================================================

CREATE TABLE robot_log (
    robot_log_no BIGSERIAL PRIMARY KEY,
    source_event_id UUID NOT NULL UNIQUE,

    robot_no BIGINT NOT NULL,
    task_no BIGINT,

    robot_status VARCHAR(20) NOT NULL,
    task_phase VARCHAR(30),
    payload_state VARCHAR(20),

    drive_motor_temperature_c NUMERIC(6,2),
    drive_motor_current_a NUMERIC(8,3),
    drive_vibration_mm_s NUMERIC(8,4),
    battery_voltage_v NUMERIC(8,3),
    battery_temperature_c NUMERIC(6,2),
    days_since_maintenance INT NOT NULL,
    battery_level NUMERIC(5,2),

    obstacle_detected BOOLEAN NOT NULL DEFAULT FALSE,
    safety_stop BOOLEAN NOT NULL DEFAULT FALSE,
    alarm_code VARCHAR(50),

    sampled_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_robot_log_robot
        FOREIGN KEY (robot_no)
        REFERENCES robot(robot_no)
        ON DELETE RESTRICT,

    CONSTRAINT fk_robot_log_task
        FOREIGN KEY (task_no)
        REFERENCES robot_task(task_no)
        ON DELETE SET NULL
);


-- =====================================================
-- 9. ROBOT PDM
-- 원시 상태값의 예지보전 분석 결과
-- =====================================================

CREATE TABLE robot_pdm (
    pdm_no BIGSERIAL PRIMARY KEY,
    robot_log_no BIGINT NOT NULL UNIQUE,

    risk_score NUMERIC(5,2) NOT NULL,
    risk_level VARCHAR(10) NOT NULL,
    normal_probability NUMERIC(6,5),
    warning_probability NUMERIC(6,5),
    critical_probability NUMERIC(6,5),
    prediction_reason VARCHAR(500),
    model_version VARCHAR(50) NOT NULL,
    predicted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_robot_pdm_log
        FOREIGN KEY (robot_log_no)
        REFERENCES robot_log(robot_log_no)
        ON DELETE RESTRICT
);


-- =====================================================
-- 10. CAR LOG DETAIL VIEW
-- =====================================================

CREATE VIEW car_log_detail AS
SELECT
    log.car_log_no,
    log.vehicle_car_no,
    vehicle.vehicle_type,
    vehicle.vehicle_status,
    log.camera_data_no,
    log.out_camera_data_no,
    log.in_gate_no,
    in_gate.gate_name AS in_gate_name,
    log.in_time,
    log.out_gate_no,
    out_gate.gate_name AS out_gate_name,
    log.out_time,
    log.free_time,
    log.snapshot_car_no,
    log.snapshot_car_kind,
    in_gate.parking_no,
    parking.parking_code,
    parking.parking_name,
    COALESCE(
        vehicle.car_no,
        entry_data.ocr_car_no,
        entry_data.car_no,
        log.snapshot_car_no
    ) AS car_no,
    CASE
        WHEN log.out_time IS NULL THEN 'PARKING'
        ELSE 'OUT'
    END AS parking_state
FROM car_log log
LEFT JOIN vehicle_car vehicle
    ON log.vehicle_car_no = vehicle.vehicle_car_no
LEFT JOIN camera_data entry_data
    ON log.camera_data_no = entry_data.camera_data_no
LEFT JOIN gate in_gate
    ON log.in_gate_no = in_gate.gate_no
LEFT JOIN gate out_gate
    ON log.out_gate_no = out_gate.gate_no
LEFT JOIN parking
    ON in_gate.parking_no = parking.parking_no;


-- =====================================================
-- 11. ROBOT TASK DETAIL VIEW
-- =====================================================

CREATE VIEW robot_task_detail AS
SELECT
    task.task_no,
    task.car_log_no,
    task.pickup_space_no,
    pickup.space_code AS pickup_space_code,
    task.dropoff_space_no,
    dropoff.space_code AS dropoff_space_code,
    task.set_no,
    task.task_type,
    task.task_phase,
    task.task_status,
    task.priority,
    task.requested_at,
    task.started_at,
    task.completed_at,
    task.failure_reason,
    log.car_no,
    log.snapshot_car_kind AS car_kind,
    log.parking_code,
    log.parking_name
FROM robot_task task
JOIN car_log_detail log
    ON task.car_log_no = log.car_log_no
JOIN parking_space pickup
    ON task.pickup_space_no = pickup.space_no
JOIN parking_space dropoff
    ON task.dropoff_space_no = dropoff.space_no;


COMMIT;

GRANT SELECT, INSERT, UPDATE, DELETE
ON parking_space,
   robot,
   robot_task,
   robot_log,
   robot_pdm
TO bono_user;

GRANT SELECT
ON car_log_detail,
   robot_task_detail
TO bono_user;

GRANT USAGE, SELECT
ON SEQUENCE parking_space_space_no_seq,
            robot_robot_no_seq,
            robot_task_task_no_seq,
            robot_log_robot_log_no_seq,
            robot_pdm_pdm_no_seq
TO bono_user;
