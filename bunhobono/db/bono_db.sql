BEGIN;

-- =====================================================
-- DROP TABLES
-- =====================================================
DROP TABLE IF EXISTS trash_bin;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS car_log;
DROP TABLE IF EXISTS camera_data;
DROP TABLE IF EXISTS camera;
DROP TABLE IF EXISTS vehicle_car_alias;
DROP TABLE IF EXISTS vehicle_car;
DROP TABLE IF EXISTS gate;
DROP TABLE IF EXISTS parking;
DROP TABLE IF EXISTS member_archive;
DROP TABLE IF EXISTS member;

-- =====================================================
-- MEMBER
-- =====================================================
CREATE TABLE member (
    member_no SERIAL PRIMARY KEY,
    login_id VARCHAR(30),
    login_pwd VARCHAR(100),
	mem_dong INT NOT NULL,
	mem_ho INT NOT NULL,
    mem_name VARCHAR(30),
    mem_phone VARCHAR(30),
    role VARCHAR(30) NOT NULL
        CHECK (role IN ('ADMIN', 'RESIDENT', 'PENDING')),
    create_at TIMESTAMP,
    delete_at TIMESTAMP,
    mem_status VARCHAR(30)
);

-- =====================================================
-- MEMBER ARCHIVE
-- =====================================================
CREATE TABLE member_archive (
    archive_no BIGSERIAL PRIMARY KEY,
    original_member_no BIGINT NOT NULL,

    login_id VARCHAR(50),
    mem_name VARCHAR(50),
    mem_phone VARCHAR(20),
    role VARCHAR(20),
    mem_status VARCHAR(20),

    mem_dong INTEGER,
    mem_ho INTEGER,

    create_at TIMESTAMP,
    delete_at TIMESTAMP,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =====================================================
-- PARKING
-- =====================================================
CREATE TABLE parking (
    parking_no SERIAL PRIMARY KEY,
    parking_name VARCHAR(100) NOT NULL,
    parking_spaces INT NOT NULL
        CHECK (parking_spaces >= 0),
    parking_location VARCHAR(255)
);

-- =====================================================
-- GATE
-- 연결된 게이트가 있으면 주차장 삭제를 차단한다.
-- =====================================================
CREATE TABLE gate (
    gate_no SERIAL PRIMARY KEY,
    parking_no INT NOT NULL,
    gate_name VARCHAR(100) NOT NULL,
    gate_type VARCHAR(10) NOT NULL,
        CHECK (gate_type IN ('In', 'Out', 'Both')),
	gate_status INT NOT NULL DEFAULT 0
		CHECK (gate_status IN (0, 1)),

    CONSTRAINT fk_gate_parking
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT
);
-- =====================================================
-- VEHICLE CAR
-- =====================================================
CREATE TABLE vehicle_car (
    vehicle_car_no SERIAL PRIMARY KEY,
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'normal'
        CHECK (vehicle_type IN ('normal', 'visit')),
    car_no VARCHAR(20) NOT NULL,
    alias_car_no VARCHAR(50) UNIQUE,
    vehicle_status VARCHAR(20) NOT NULL DEFAULT 'WAITING'
        CHECK (vehicle_status IN ('WAITING', 'APPROVED', 'EXPIRED', 'UNKNOWN')),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    member_no INT,
    approved_at TIMESTAMP,

    CONSTRAINT fk_vehicle_member
        FOREIGN KEY (member_no)
        REFERENCES member(member_no)
        ON DELETE SET NULL
);

-- =====================================================
-- CAMERA
-- 연결된 카메라가 있으면 게이트 삭제를 차단한다.
-- =====================================================
CREATE TABLE camera (
    camera_no SERIAL PRIMARY KEY,
    gate_no INT NOT NULL,
    camera_name VARCHAR(100) NOT NULL,
    camera_type VARCHAR(20) NOT NULL
        CHECK (camera_type IN ('In', 'Out', 'Both')),
    install_date DATE,

    CONSTRAINT fk_camera_gate
        FOREIGN KEY (gate_no)
        REFERENCES gate(gate_no)
        ON DELETE RESTRICT
);

-- =====================================================
-- CAMERA DATA
-- 촬영 기록이 있으면 카메라 삭제를 차단한다.
-- =====================================================
CREATE TABLE camera_data (
    camera_data_no SERIAL PRIMARY KEY,
    camera_no INT NOT NULL,
    vehicle_car_no INT,
    car_no VARCHAR(50),
    capture_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    image_path TEXT,
    crop_image_path TEXT,
    recognition_state BOOLEAN,
    confidence_score NUMERIC(5,2),

    CONSTRAINT fk_cameradata_camera
        FOREIGN KEY (camera_no)
        REFERENCES camera(camera_no)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cameradata_vehicle
        FOREIGN KEY (vehicle_car_no)
        REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL
);

-- =====================================================
-- CAR LOG
-- camera_data가 삭제되어도 차량번호를 유지한다.
-- =====================================================
CREATE TABLE car_log (
    car_log_no SERIAL PRIMARY KEY,
    vehicle_car_no INT,
    camera_data_no INT,
    out_camera_data_no INT,
    in_gate_no INT,
    in_time TIMESTAMP,
    out_gate_no INT,
    out_time TIMESTAMP,
    free_time INTEGER,
    snapshot_car_no VARCHAR(50),

    CONSTRAINT fk_log_vehicle_car
        FOREIGN KEY (vehicle_car_no)
        REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_in_gate
        FOREIGN KEY (in_gate_no)
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_out_gate
        FOREIGN KEY (out_gate_no)
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_camera_data
        FOREIGN KEY (camera_data_no)
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_out_camera_data
        FOREIGN KEY (out_camera_data_no)
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL
);


-- =====================================================
-- NOTICE
-- car_log가 삭제되어도 알림과 당시 표시값을 유지한다.
-- =====================================================
CREATE TABLE notice (
    notice_no SERIAL PRIMARY KEY,
    car_log_no INT,
    detect_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stay_days INT NOT NULL
        CHECK (stay_days >= 0),
    alert_stat VARCHAR(20) NOT NULL DEFAULT 'Unresolved'
        CHECK (alert_stat IN ('Unresolved', 'Checked', 'Resolved')),
    handled_by_member_no INT,
    handled_at TIMESTAMP,

    snapshot_car_log_no INT,
    snapshot_registered_car_no VARCHAR(50),
    snapshot_captured_car_no VARCHAR(50),
    snapshot_car_kind VARCHAR(20),
    snapshot_parking_name VARCHAR(100),
    snapshot_in_time TIMESTAMP,

    CONSTRAINT fk_notice_car_log
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_notice_handler
        FOREIGN KEY (handled_by_member_no)
        REFERENCES member(member_no)
        ON DELETE SET NULL
);

-- =====================================================
-- TRASH BIN
-- 삭제 전 원본 행을 JSONB로 보관한다.
-- =====================================================
CREATE TABLE trash_bin (
    trash_no BIGSERIAL PRIMARY KEY,                 -- 휴지통 데이터 고유 번호
    data_type VARCHAR(30) NOT NULL                  -- 원본 데이터 종류
        CHECK (data_type IN (
            'CAMERA_DATA',                          -- 카메라 촬영 기록
            'CAR_LOG',                              -- 차량 입출차 기록
            'NOTICE'                                -- 알림 기록
        )),
    original_no INT NOT NULL,                       -- 삭제 전 원본 테이블의 PK 번호
    data_json JSONB NOT NULL,                       -- 삭제 전 원본 행 전체 데이터 및 복원용 JSON
    delete_type VARCHAR(20) NOT NULL                -- 삭제 방식
        CHECK (delete_type IN (
            'MANUAL',                               -- 사용자가 직접 삭제
            'SCHEDULED'                             -- 스케줄러 자동 삭제
        )),
    deleted_at TIMESTAMP NOT NULL                   -- 휴지통으로 이동된 시각
        DEFAULT CURRENT_TIMESTAMP,
    purge_at TIMESTAMP NOT NULL                     -- 영구 삭제 예정 시각
        DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days'), -- 이동 후 30일
    UNIQUE (data_type, original_no)                 -- 같은 원본의 휴지통 중복 저장 방지
);

CREATE INDEX idx_trash_type_deleted_at
    ON trash_bin(data_type, deleted_at DESC);

CREATE INDEX idx_trash_purge_at
    ON trash_bin(purge_at);

COMMIT;

-- 테이블을 모두 생성한 뒤 애플리케이션 계정에 권한을 부여한다.
GRANT USAGE ON SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bono_user;

-- 이후 같은 실행 계정으로 생성되는 테이블과 시퀀스에도 자동으로 권한을 부여한다.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO bono_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON SEQUENCES TO bono_user;
