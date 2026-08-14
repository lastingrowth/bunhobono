BEGIN;
-- =====================================================
-- DROP schema 전체삭제후 재생성 
-- 21개의 테이블		2026-08-10
-- =====================================================
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- =====================================================
-- APARTMENT UNIT
-- 실제 아파트 세대만 저장하며 관리실 0동 0호는 만들지 않는다.
-- =====================================================
CREATE TABLE apartment_unit (
    
    apartment_unit_no SERIAL PRIMARY KEY,				-- 세대를 다른 테이블에서 참조할 때 사용하는 내부 고유번호
    dong INT NOT NULL,							 		-- 아파트 동 번호. 관리실을 의미하는 0동은 허용하지 않는다.
    ho INT NOT NULL,							 		-- 아파트 호수. 0호와 음수는 허용하지 않는다.
    unit_status VARCHAR(20) NOT NULL DEFAULT 'EMPTY'	-- EMPTY: 입주 가능한 빈 세대, OCCUPIED: 현재 회원이 점유한 세대
        CHECK (unit_status IN ('EMPTY', 'OCCUPIED')),
		
    CONSTRAINT uq_apartment_unit_address UNIQUE (dong, ho)		-- 같은 동·호를 두 번 만들지 못하게 한다.
);

-- =====================================================
-- MEMBER
-- 입주민만 실제 세대를 참조하며 관리자는 unit_no가 NULL이다.
-- =====================================================
CREATE TABLE member (
    member_no SERIAL PRIMARY KEY,					-- 회원 내부 고유번호
    login_id VARCHAR(30) NOT NULL UNIQUE,			-- 로그인에 사용하는 아이디. 회원 간 중복을 허용하지 않는다.
    login_pwd VARCHAR(100) NOT NULL,				-- 암호화된 로그인 비밀번호
    unit_no INT,									-- 입주민이 거주하는 세대 번호. 관리자는 NULL이다.
    mem_name VARCHAR(30) NOT NULL,					-- 회원 이름
    mem_phone VARCHAR(30),							-- 회원 연락처. 미입력 상태를 허용한다.

    role VARCHAR(30) NOT NULL						-- ADMIN: 관리자, RESIDENT: 입주민
        CHECK (role IN ('ADMIN', 'RESIDENT')),	    
    mem_status VARCHAR(30) NOT NULL									    -- PENDING: 입주민 가입 승인 대기, ACTIVE: 입주민 거주 중 또는 관리자 근무 중, WITHDRAW_PENDING: 입주민 전출 승인 대기
        CHECK (mem_status IN ('PENDING', 'ACTIVE', 'WITHDRAW_PENDING',	-- WITHDRAWN: 입주민 전출 확정 또는 탈퇴 완료, INACTIVE: 관리자 퇴사, ON_LEAVE: 관리자 휴직
		  'WITHDRAWN',	'INACTIVE', 'ON_LEAVE')),
		
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,		-- 회원 데이터 생성 시각
    delete_at TIMESTAMP,										-- 전출·탈퇴·퇴사 요청 또는 처리 시각. 해당 사항이 없으면 NULL이다.

    CONSTRAINT fk_member_apartment_unit							-- 입주민의 세대가 실제 apartment_unit에 존재하도록 한다.
        FOREIGN KEY (unit_no)
        REFERENCES apartment_unit(apartment_unit_no)
);

-- =====================================================
-- MEMBER ARCHIVE
-- =====================================================
CREATE TABLE member_archive (

    member_archive_no SERIAL PRIMARY KEY,                    -- 회원 보관 이력 고유번호
    original_member_no INT NOT NULL,                         -- 보관되기 전 member.member_no 값
    login_id VARCHAR(50),                                    -- 보관 당시 로그인 아이디
    mem_name VARCHAR(50),                                    -- 보관 당시 회원 이름
    mem_phone VARCHAR(30),                                   -- 보관 당시 회원 전화번호
    role VARCHAR(30),                                        -- 보관 당시 회원 권한
    mem_status VARCHAR(30),                                  -- 보관 당시 회원 상태
    mem_dong INT,                                            -- 전출·탈퇴 당시 아파트 동
    mem_ho INT,                                              -- 전출·탈퇴 당시 아파트 호수
    create_at TIMESTAMP,                                     -- 원본 회원의 가입 시각
    delete_at TIMESTAMP,                                     -- 원본 회원의 전출·탈퇴 처리 시각
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- 회원 정보가 보관 테이블로 이동된 시각
);


-- =====================================================
-- PARKING
-- =====================================================
CREATE TABLE parking (
    parking_no SERIAL PRIMARY KEY,       					 -- 주차장 고유번호
    parking_code VARCHAR(20) NOT NULL UNIQUE,                -- 주차장 식별 코드
    parking_name VARCHAR(100) NOT NULL,  					 -- 주차장 이름
    parking_type VARCHAR(20) NOT NULL,                       -- 주차장 종류
    parking_spaces INT NOT NULL,                             -- 주차장이 수용할 수 있는 전체 주차면 수
    parking_location VARCHAR(255),                           -- 주차장 위치 설명
    active BOOLEAN NOT NULL DEFAULT TRUE                     -- 주차장 사용 여부
);

-- =====================================================
-- GATE
-- 연결된 게이트가 있으면 해당 주차장의 삭제를 제한한다.
-- =====================================================
CREATE TABLE gate (

    gate_no SERIAL PRIMARY KEY,               -- 게이트 고유번호
    parking_no INT,                           -- 게이트가 설치된 주차장 고유번호
    gate_code VARCHAR(30) NOT NULL UNIQUE,    -- 게이트 식별 코드
    gate_name VARCHAR(100) NOT NULL,          -- 게이트 이름

    gate_type VARCHAR(10) NOT NULL            -- In: 입차 게이트, Out: 출차 게이트
        CHECK (gate_type IN ('In', 'Out')),
    gate_area VARCHAR(20) NOT NULL,           -- 게이트가 위치한 구역
    gate_status INT NOT NULL DEFAULT 0        -- 게이트 개폐 상태. 0: 닫힘, 1: 열림
        CHECK (gate_status IN (0, 1)),
    operating_status VARCHAR(20) NOT NULL     -- 게이트 장비의 현재 작동 상태
        DEFAULT 'NORMAL'
        CHECK (operating_status IN ('NORMAL', 'FAULT', 'MAINTENANCE')), -- 정상 작동-- 고장-- 점검 중
    active BOOLEAN NOT NULL DEFAULT TRUE,     -- 게이트 사용 여부

    CONSTRAINT fk_gate_parking
        FOREIGN KEY (parking_no)              -- gate.parking_no를 parking 테이블과 연결
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT                    -- 연결된 게이트가 있으면 주차장 삭제 제한
);

-- =====================================================
-- VEHICLE CAR
-- =====================================================
CREATE TABLE vehicle_car (

    vehicle_car_no SERIAL PRIMARY KEY,                         -- 차량 고유번호
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'normal'         -- normal: 입주민 등록차량, visit: 방문차량
        CHECK (vehicle_type IN ('normal', 'visit')),
    car_no VARCHAR(20) NOT NULL,                               -- 실제 차량번호
    alias_car_no VARCHAR(50) UNIQUE,                           -- 차량을 구분하기 위한 별칭
	
    vehicle_status VARCHAR(20) NOT NULL DEFAULT 'WAITING' 		-- WAITING: 승인 대기, APPROVED: 이용 가능, EXPIRED: 기간 만료, UNKNOWN: 상태 확인 불가
    CHECK (vehicle_status IN ('WAITING', 'APPROVED', 'EXPIRED', 'UNKNOWN')),
		
    start_date TIMESTAMP,                                     -- 등록차량: 유효 시작 시각, 방문차량: 예상 방문 시각
    end_date TIMESTAMP,                                       -- 등록차량: 유효 종료 시각, 방문차량: 예상 이용 종료 시각
    member_no INT,                                            -- 차량을 등록하거나 방문 신청한 회원 고유번호
    approved_at TIMESTAMP,                                    -- 차량 등록이 승인된 시각

    -- fee_exempt BOOLEAN NOT NULL DEFAULT FALSE,                 -- 긴급·작업 차량 등 주차요금 면제 여부

    CONSTRAINT fk_vehicle_member
        FOREIGN KEY (member_no)                               -- vehicle_car.member_no를 member 테이블과 연결
        REFERENCES member(member_no)
        ON DELETE SET NULL                                    -- 회원 삭제 후에도 차량정보는 남기고 회원번호만 NULL 처리
);

-- =====================================================
-- CAMERA
-- 연결된 카메라가 있으면 해당 게이트의 삭제를 제한한다.
-- =====================================================
CREATE TABLE camera (

    camera_no SERIAL PRIMARY KEY,          -- 카메라 고유번호
    gate_no INT NOT NULL,                  -- 카메라가 연결된 게이트 고유번호
    camera_name VARCHAR(100) NOT NULL,      -- 카메라 이름
    camera_type VARCHAR(20) NOT NULL        -- In: 입차 촬영용, Out: 출차 촬영용
        CHECK (camera_type IN ('In', 'Out')),
    install_date DATE,                     -- 카메라 설치일
    active BOOLEAN NOT NULL DEFAULT TRUE,  -- 카메라 사용 여부
    camera_status VARCHAR(20) NOT NULL      -- 카메라 현재 상태
        DEFAULT 'NORMAL'
        CHECK (camera_status IN ('NORMAL', 'FAULT', 'MAINTENANCE')), -- 정상 작동 -- 고장 -- 점검 중
		
    CONSTRAINT fk_camera_gate
        FOREIGN KEY (gate_no)              -- camera.gate_no를 gate 테이블과 연결
        REFERENCES gate(gate_no)
        ON DELETE RESTRICT                 -- 연결된 카메라가 있으면 게이트 삭제 제한
);

-- =====================================================
-- CAMERA DATA
-- 촬영 기록이 있으면 해당 카메라의 삭제를 제한한다.
-- =====================================================
CREATE TABLE camera_data (

    camera_data_no SERIAL PRIMARY KEY,                -- 카메라 촬영 데이터 고유번호
    camera_no INT NOT NULL,                           -- 촬영한 카메라 고유번호
    vehicle_car_no INT,                               -- 인식 결과와 연결된 등록·방문 차량 고유번호
    car_no VARCHAR(50),                               -- 매칭·보정 후 화면과 입출차 처리에 사용하는 확정 차량번호
    ocr_car_no VARCHAR(50),                           -- 카메라 OCR이 최초로 인식한 원본 차량번호
    capture_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 차량번호판 촬영 시각
    image_path TEXT,                                  -- 차량 전체 원본 이미지 저장 경로
    crop_image_path TEXT,                             -- 차량번호판 영역 이미지 저장 경로
    recognition_state BOOLEAN,                        -- 차량번호 인식 성공 여부
    confidence_score NUMERIC(5,2),                    -- OCR 차량번호 인식 신뢰도
    cam_note VARCHAR(100),                            -- 카메라 데이터에 대한 관리자 비고

    gate_opened BOOLEAN NOT NULL DEFAULT FALSE,       -- 정문·후문에서 실제 게이트가 열린 촬영 기록
    gate_opened_at TIMESTAMP,                         -- 게이트가 열린 시각

    CONSTRAINT fk_cameradata_camera
        FOREIGN KEY (camera_no)                       -- camera_data.camera_no를 camera 테이블과 연결
        REFERENCES camera(camera_no)
        ON DELETE RESTRICT,                           -- 촬영 기록이 있으면 카메라 삭제 제한

    CONSTRAINT fk_cameradata_vehicle
        FOREIGN KEY (vehicle_car_no)                  -- 인식 결과를 등록·방문 차량과 연결
        REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL                            -- 차량 삭제 후에도 촬영기록은 남기고 연결값만 NULL 처리
);

-- =====================================================
-- CAR LOG
-- 연결된 차량·게이트·카메라 데이터가 삭제되어도 입출차 기록은 유지한다.
-- 차량번호와 차량 종류는 스냅숏으로 별도 보관한다.
-- =====================================================
CREATE TABLE car_log (

    car_log_no SERIAL PRIMARY KEY,                  -- 차량 입출차 기록 고유번호
    vehicle_car_no INT,                             -- 입출차 차량과 연결된 등록·방문 차량 고유번호
    camera_data_no INT,                             -- 입차 시 생성된 카메라 촬영 데이터 고유번호
    out_camera_data_no INT,                         -- 출차 시 생성된 카메라 촬영 데이터 고유번호
    in_gate_no INT,                                 -- 차량이 입차한 게이트 고유번호
    in_time TIMESTAMP,                              -- 차량 입차 시각
    out_gate_no INT,                                -- 차량이 출차한 게이트 고유번호
    out_time TIMESTAMP,                             -- 차량 출차 시각
    free_time INTEGER,                              -- 방문차량 등에 적용되는 무료 주차시간(분)
    snapshot_car_no VARCHAR(50),                    -- 입차 당시 확정된 차량번호 스냅숏
	
    -- REGISTERED: 입주민 등록차량, VISIT: 방문차량, UNKNOWN: 미등록·확인 불가 차량
    snapshot_car_kind VARCHAR(20) NOT NULL
        CHECK (snapshot_car_kind IN ('REGISTERED', 'VISIT', 'UNKNOWN')),

    CONSTRAINT fk_log_vehicle_car
        FOREIGN KEY (vehicle_car_no)                -- car_log.vehicle_car_no를 vehicle_car와 연결
        REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL,                         -- 차량정보 삭제 후에도 입출차 기록 유지
    CONSTRAINT fk_log_in_gate
        FOREIGN KEY (in_gate_no)                    -- 입차 게이트와 연결
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,                         -- 게이트 삭제 후에도 입차 기록 유지
    CONSTRAINT fk_log_out_gate
        FOREIGN KEY (out_gate_no)                   -- 출차 게이트와 연결
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,                         -- 게이트 삭제 후에도 출차 기록 유지
    CONSTRAINT fk_log_camera_data
        FOREIGN KEY (camera_data_no)                -- 입차 카메라 촬영 데이터와 연결
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL,                         -- 입차 촬영 데이터 삭제 후에도 입출차 기록 유지
    CONSTRAINT fk_log_out_camera_data
        FOREIGN KEY (out_camera_data_no)            -- 출차 카메라 촬영 데이터와 연결
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL                          -- 출차 촬영 데이터 삭제 후에도 입출차 기록 유지
);

-- =====================================================
-- PARKING SPACE
-- 주차장의 일반 주차면과 입·출차 대기면을 관리한다.
-- car_log_no가 NULL이면 빈자리이고,
-- 값이 있으면 해당 입출차 기록의 차량이 사용 중인 자리이다.
-- =====================================================
CREATE TABLE parking_space (
    space_no BIGSERIAL PRIMARY KEY,                            -- 주차면 내부 고유번호
    parking_no INT NOT NULL,                                   -- 주차면이 소속된 주차장 고유번호
    gate_no INT,                                               -- 입·출차 대기면과 연결된 게이트 고유번호
    car_log_no INT UNIQUE,                                     -- 현재 주차면을 사용하는 차량의 입출차 기록번호
    space_code VARCHAR(20) NOT NULL UNIQUE,                    -- 주차면을 구분하는 고유 코드
    space_type VARCHAR(20) NOT NULL,                           -- 일반 주차면 또는 입·출차 대기면 등의 주차면 종류
    active BOOLEAN NOT NULL DEFAULT TRUE,                      -- 주차면 사용 가능 여부
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 주차면 정보 생성 시각
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 주차면 정보 최종 수정 시각

    CONSTRAINT fk_space_parking
        FOREIGN KEY (parking_no)                              -- 소속 주차장과 연결
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT,                                   -- 주차면이 존재하면 주차장 삭제 제한

    CONSTRAINT fk_space_gate
        FOREIGN KEY (gate_no)                                 -- 입·출차 대기면의 게이트와 연결
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,                                   -- 게이트 삭제 시 연결값만 제거

    CONSTRAINT fk_space_car_log
        FOREIGN KEY (car_log_no)                              -- 현재 주차 중인 차량의 입출차 기록과 연결
        REFERENCES car_log(car_log_no)
        ON DELETE SET NULL                                    -- 입출차 기록 삭제 시 빈자리로 처리
);

-- =====================================================
-- ROBOT
-- 주차 차량을 이동하는 물리 로봇의 현재 상태와 운영 정보를 관리한다.
-- 두 대의 로봇을 하나의 세트로 구성하며 A·B 위치로 구분한다.
-- =====================================================
CREATE TABLE robot (
    robot_no SERIAL PRIMARY KEY,                               -- 로봇 내부 고유번호
    robot_code VARCHAR(20) NOT NULL UNIQUE,                    -- 로봇 장비를 식별하는 고유 코드
    set_no INT NOT NULL,                                       -- 두 대의 로봇을 하나로 묶는 세트 번호
    set_position VARCHAR(1) NOT NULL                           -- 세트 안에서 해당 로봇의 위치
        CHECK (set_position IN ('A', 'B')),                    -- 세트 위치는 A 또는 B만 허용
    robot_status VARCHAR(20) NOT NULL DEFAULT 'STANDBY',       -- 로봇 현재 상태. 기본값은 대기 상태
    battery_level NUMERIC(5,2),                                -- 현재 배터리 잔량 또는 충전율
    operating_hours NUMERIC(12,2) NOT NULL DEFAULT 0,          -- 로봇의 누적 운행시간
    last_heartbeat_at TIMESTAMPTZ,                             -- 로봇이 마지막으로 정상 신호를 보낸 시각
    last_maintenance_at TIMESTAMPTZ,                           -- 로봇을 마지막으로 정비한 시각
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 로봇 정보 생성 시각
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 로봇 정보 최종 수정 시각

    CONSTRAINT uk_robot_set_position
        UNIQUE (set_no, set_position)                         -- 하나의 세트에서 A·B 위치가 중복되지 않도록 제한
);

-- =====================================================
-- ROBOT TASK
-- 입차 대기면, 일반 주차면, 출차 대기면 사이에서
-- 로봇이 차량을 이동하는 작업과 진행 상태를 관리한다.
-- =====================================================
CREATE TABLE robot_task (
    task_no BIGSERIAL PRIMARY KEY,                               -- 로봇 작업 내부 고유번호
    car_log_no INT NOT NULL,                                     -- 이동 대상 차량의 입출차 기록번호
    pickup_space_no BIGINT NOT NULL,                             -- 차량을 들어 올릴 출발 주차면 번호
    dropoff_space_no BIGINT NOT NULL,                            -- 차량을 내려놓을 도착 주차면 번호
    set_no INT,                                                  -- 작업을 수행하는 로봇 세트 번호
    task_type VARCHAR(20) NOT NULL,                              -- 입차·출차·재배치 등의 작업 종류
    task_phase VARCHAR(30) NOT NULL DEFAULT 'WAITING',           -- 작업의 세부 진행 단계
    task_status VARCHAR(20) NOT NULL DEFAULT 'WAITING',    	     -- 작업 전체 처리 상태
    priority INT NOT NULL DEFAULT 0,                         	 -- 작업 우선순위. 값이 높을수록 우선 처리
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 로봇 작업 요청 시각
    started_at TIMESTAMPTZ,                                      -- 로봇이 실제 작업을 시작한 시각
    completed_at TIMESTAMPTZ,                                    -- 로봇 작업이 완료된 시각
    failure_reason VARCHAR(500),                                 -- 작업 실패 또는 중단 사유

    CONSTRAINT fk_task_car_log
        FOREIGN KEY (car_log_no)                              -- 이동 대상 차량의 입출차 기록과 연결
        REFERENCES car_log(car_log_no)
        ON DELETE RESTRICT,                                   -- 작업이 남아 있으면 입출차 기록 삭제 제한

    CONSTRAINT fk_task_pickup_space
        FOREIGN KEY (pickup_space_no)                         -- 차량을 가져올 출발 주차면과 연결
        REFERENCES parking_space(space_no)
        ON DELETE RESTRICT,                                   -- 작업이 남아 있으면 출발 주차면 삭제 제한

    CONSTRAINT fk_task_dropoff_space
        FOREIGN KEY (dropoff_space_no)                        -- 차량을 내려놓을 도착 주차면과 연결
        REFERENCES parking_space(space_no)
        ON DELETE RESTRICT                                    -- 작업이 남아 있으면 도착 주차면 삭제 제한
);

-- =====================================================
-- ROBOT LOG
-- 로봇이 운행하거나 작업을 수행하는 동안 전송한
-- 센서값, 배터리 상태, 안전 상태 등의 원시 데이터를 기록한다.
-- =====================================================
CREATE TABLE robot_log (
    robot_log_no SERIAL PRIMARY KEY,                      	   -- 로봇 상태 로그 내부 고유번호
    source_event_id UUID NOT NULL UNIQUE,                      -- 외부 로봇 시스템에서 생성한 이벤트 고유번호
    robot_no BIGINT NOT NULL,                                  -- 상태 데이터를 전송한 로봇 번호
    task_no BIGINT,                                            -- 상태 데이터와 관련된 로봇 작업번호
    robot_status VARCHAR(20) NOT NULL,                         -- 로그 생성 당시 로봇 상태
    task_phase VARCHAR(30),                                    -- 로그 생성 당시 작업 진행 단계
    payload_state VARCHAR(20),                                 -- 차량 또는 적재물의 탑재 상태
    drive_motor_temperature_c NUMERIC(6,2),                    -- 구동 모터 온도(섭씨)
    drive_motor_current_a NUMERIC(8,3),                        -- 구동 모터 전류(A)
    drive_vibration_mm_s NUMERIC(8,4),                         -- 구동부 진동 속도(mm/s)
    battery_voltage_v NUMERIC(8,3),                            -- 배터리 전압(V)
    battery_temperature_c NUMERIC(6,2),                        -- 배터리 온도(섭씨)
    -- days_since_maintenance INT NOT NULL,                       -- 마지막 정비 이후 경과 일수
    battery_level NUMERIC(5,2),                                -- 로그 생성 당시 배터리 잔량 또는 충전율
    obstacle_detected BOOLEAN NOT NULL DEFAULT FALSE,          -- 장애물 감지 여부
    safety_stop BOOLEAN NOT NULL DEFAULT FALSE,                -- 안전장치에 의한 긴급 정지 여부
    alarm_code VARCHAR(50),                                    -- 로봇에서 발생한 경고·오류 코드
    sampled_at TIMESTAMPTZ NOT NULL,                           -- 로봇이 상태 데이터를 측정한 시각
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 상태 로그가 DB에 저장된 시각

    CONSTRAINT fk_robot_log_robot
        FOREIGN KEY (robot_no)                                -- 상태 데이터를 전송한 로봇과 연결
        REFERENCES robot(robot_no)
        ON DELETE RESTRICT,                                   -- 로그가 남아 있으면 로봇 정보 삭제 제한

    CONSTRAINT fk_robot_log_task
        FOREIGN KEY (task_no)                                 -- 로그가 발생한 로봇 작업과 연결
        REFERENCES robot_task(task_no)
        ON DELETE SET NULL                                    -- 작업 삭제 후에도 로그는 유지하고 연결값만 NULL 처리
);

-- =====================================================
-- ROBOT PDM
-- PDM(Predictive Maintenance): 예지보전
-- robot_log에 저장된 센서값을 분석한 고장 위험도와
-- 예지보전 모델의 예측 결과를 관리한다.
-- =====================================================
CREATE TABLE robot_pdm (
    pdm_no BIGSERIAL PRIMARY KEY,                             -- 예지보전 분석 결과 내부 고유번호
    robot_log_no BIGINT NOT NULL UNIQUE,                      -- 분석 대상 로봇 로그번호. 로그당 결과는 한 건만 허용
    risk_score NUMERIC(5,2) NOT NULL,                         -- 모델이 계산한 고장 위험 점수
    risk_level VARCHAR(10) NOT NULL,                          -- 정상·경고·위험 등의 최종 위험 등급
    normal_probability NUMERIC(6,5),                          -- 정상 상태일 확률
    warning_probability NUMERIC(6,5),                         -- 경고 상태일 확률
    critical_probability NUMERIC(6,5),                        -- 위험 상태일 확률
    prediction_reason VARCHAR(500),                           -- 해당 예측 결과가 나온 주요 원인 또는 설명
    model_version VARCHAR(50) NOT NULL,                       -- 예측에 사용한 모델 버전
    predicted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 예지보전 모델이 분석한 시각

    CONSTRAINT fk_robot_pdm_log
        FOREIGN KEY (robot_log_no)                            -- 분석 대상 로봇 상태 로그와 연결
        REFERENCES robot_log(robot_log_no)
        ON DELETE RESTRICT                                    -- 분석 결과가 있으면 원본 로그 삭제 제한
);

-- =====================================================
-- MEMBER NOTICE
-- 입주민에게 전달하는 통합 알림과 읽음 상태를 보관한다.
-- =====================================================
CREATE TABLE mem_notice (

    mem_notice_no SERIAL PRIMARY KEY,                         -- 입주민 알림 고유번호

    recipient_member_no INT NOT NULL                          -- 알림을 받는 입주민 회원 고유번호
        REFERENCES member(member_no)
        ON DELETE CASCADE,                                    -- 수신 회원 삭제 시 해당 회원의 알림도 삭제

    reference_table VARCHAR(50) NOT NULL,                     -- 알림을 발생시킨 원본 테이블 이름
    reference_no INT NOT NULL,                                -- 원본 테이블 데이터의 고유번호

    notice_type VARCHAR(40) NOT NULL,                         -- 알림 종류
    title VARCHAR(100) NOT NULL,                              -- 알림 제목
    message VARCHAR(500) NOT NULL,                            -- 알림 내용

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 알림 생성 시각
    read_at TIMESTAMP,                                        -- 알림 확인 시각, NULL이면 읽지 않음

    CONSTRAINT uq_mem_notice_reference UNIQUE (               -- 동일한 원본의 같은 알림 중복 방지
        recipient_member_no,
        reference_table,
        reference_no,
        notice_type
    )
);

-- 입주민별 알림을 최신순으로 조회할 때 사용한다.
CREATE INDEX idx_mem_notice_recipient_created
    ON mem_notice(recipient_member_no, created_at DESC);

-- =====================================================
-- NOTICE
-- 차량 입출차와 카메라 데이터에서 발생한 관리자 관제 알림을 보관한다.
-- =====================================================
CREATE TABLE notice (

    notice_no SERIAL PRIMARY KEY,                              -- 관리자 관제 알림 고유번호
    notice_type VARCHAR(30) NOT NULL,                          -- 장기주차·미등록차량 등 관제 알림 종류
	
    car_log_no INT                                             -- 알림이 발생한 입출차 기록 고유번호
        REFERENCES car_log(car_log_no)
        ON DELETE SET NULL,                                    -- 입출차 기록 삭제 후에도 알림은 유지
    camera_data_no INT                                         -- 알림이 발생한 카메라 촬영 데이터 고유번호
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL,                                    -- 촬영 데이터 삭제 후에도 알림은 유지

    detect_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 이상 상황을 감지하고 알림을 생성한 시각
    due_at TIMESTAMP,                                          -- 알림을 처리해야 하는 기준·만료 시각

    alert_stat VARCHAR(20) NOT NULL DEFAULT 'Unresolved',      -- 알림 처리 상태: 미처리·처리 완료 등

    handled_by_member_no INT                                   -- 알림을 처리한 관리자 회원 고유번호
        REFERENCES member(member_no)
        ON DELETE SET NULL,                                    -- 관리자 삭제 후에도 알림 처리 이력은 유지

    handled_at TIMESTAMP,                                      -- 관리자가 알림을 처리한 시각

    snapshot_car_log_no INT,                                   -- 알림 발생 당시 입출차 기록번호 스냅숏
    snapshot_camera_data_no INT,                               -- 알림 발생 당시 카메라 데이터번호 스냅숏
    snapshot_registered_car_no VARCHAR(50),                    -- 차량 테이블에 등록된 차량번호 스냅숏
    snapshot_captured_car_no VARCHAR(50),                      -- 카메라가 촬영·인식한 차량번호 스냅숏
    snapshot_car_kind VARCHAR(20),                             -- 알림 발생 당시 차량 종류 스냅숏
    snapshot_parking_name VARCHAR(100),                        -- 알림 발생 당시 주차장 이름 스냅숏
    snapshot_in_time TIMESTAMP,                                -- 알림 관련 차량의 입차 시각 스냅숏
    snapshot_image_path TEXT,                                  -- 알림 발생 당시 촬영 이미지 경로 스냅숏
    snapshot_confidence_score NUMERIC(5,2),                    -- 알림 발생 당시 OCR 인식 신뢰도 스냅숏

    CONSTRAINT uk_notice_type_car_log
        UNIQUE (notice_type, snapshot_car_log_no),             -- 같은 입출차 기록에 동일 종류 알림 중복 생성 방지

    CONSTRAINT uk_notice_type_camera
        UNIQUE (notice_type, snapshot_camera_data_no)          -- 같은 촬영 기록에 동일 종류 알림 중복 생성 방지
);

-- =====================================================
-- BOARD
-- 입주민과 관리자에게 표시할 공지사항 정보를 관리한다.
-- =====================================================
CREATE TABLE board (

    board_no SERIAL PRIMARY KEY,                              -- 공지사항 고유번호
    title VARCHAR(150) NOT NULL,                              -- 공지사항 제목
    content TEXT NOT NULL,                                    -- 공지사항 본문 내용
    image_path VARCHAR(500),                                  -- 첨부 이미지 저장 경로
    image_name VARCHAR(255),                                  -- 첨부 이미지 원본 파일명
    image_type VARCHAR(100),                                  -- 첨부 이미지 MIME 타입
    start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 공지사항 게시 시작 시각
    end_at TIMESTAMP,                                         -- 공지사항 게시 종료 시각
    active BOOLEAN NOT NULL DEFAULT TRUE,                     -- 공지사항 활성 여부: TRUE 게시, FALSE 비활성
    created_by VARCHAR(30) NOT NULL,                           -- 공지사항 작성자 아이디 또는 이름
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 공지사항 최초 작성 시각
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 공지사항 마지막 수정 시각

    CONSTRAINT chk_board_period
        CHECK (end_at IS NULL OR end_at >= start_at)          -- 게시 종료 시각이 시작 시각보다 빠르지 않도록 제한
);

CREATE INDEX idx_board_active_period
    ON board(active, start_at, end_at);

CREATE INDEX idx_board_created_at
    ON board(created_at DESC, board_no DESC);

-- =====================================================
-- TRASH BIN
-- 삭제 전 원본 행을 JSONB로 보관한다.
-- =====================================================
CREATE TABLE trash_bin (
    trash_no SERIAL PRIMARY KEY,                    -- 휴지통 데이터 고유 번호
    data_type VARCHAR(30) NOT NULL                  -- 원본 데이터 종류
        CHECK (data_type IN (
            'CAMERA_DATA',                          -- 카메라 촬영 기록
            'CAR_LOG',                              -- 차량 입출차 기록
            'NOTICE',                               -- 알림 기록
			'INQUIRY',								-- 문의 사항 기록	
			'BILL'									-- 완료 정산 기록
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

-- =====================================================
-- KIOSK
-- 주차장에 설치된 키오스크 장비 정보를 관리한다.
-- =====================================================
CREATE TABLE kiosk (
    kiosk_no SERIAL PRIMARY KEY,       -- 키오스크 고유번호
    parking_no INT NOT NULL,           -- 키오스크가 설치된 주차장 번호
    model_name VARCHAR(100),           -- 키오스크 장비 모델명
    kiosk_location VARCHAR(255),       -- 주차장 내부의 키오스크 설치 위치
    install_date DATE,                 -- 키오스크 장비 설치일

    CONSTRAINT fk_kiosk_parking        -- 소속 주차장이 존재하도록 제한
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT             -- 키오스크가 남아 있으면 주차장 삭제 차단
);

-- =====================================================
-- FAQ
-- 사용자가 자주 묻는 질문과 답변을 관리한다.
-- =====================================================
CREATE TABLE faq (

    faq_no SERIAL PRIMARY KEY,                              -- 자주 묻는 질문 고유번호
    category VARCHAR(30) NOT NULL,                          -- 질문 분류
    question VARCHAR(200) NOT NULL,                         -- 질문 내용
    answer TEXT NOT NULL,                                   -- 질문에 대한 답변 내용
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 질문과 답변이 최초 등록된 시각
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 질문 또는 답변이 마지막으로 수정된 시각
);

-- =====================================================
-- INQUIRY
-- 입주민의 1:1 문의와 관리자 답변을 관리한다.
-- =====================================================
CREATE TABLE inquiry (

    inquiry_no SERIAL PRIMARY KEY,                            -- 1:1 문의 고유번호
    member_no INT NOT NULL,                                   -- 문의를 작성한 입주민 회원 고유번호
    root_inquiry_no INT,                                      -- 재문의가 연결되는 최초 문의 고유번호
    category VARCHAR(30) NOT NULL,                            -- 문의 분류
    title VARCHAR(200) NOT NULL,                              -- 문의 제목
    content TEXT NOT NULL,                                    -- 문의 내용
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',            -- 문의 처리 상태: WAITING 답변 대기 등
    answer_content TEXT,                                      -- 관리자가 작성한 답변 내용
    answered_by INT,                                          -- 답변을 작성한 관리자 회원 고유번호
    answered_at TIMESTAMP,                                    -- 관리자가 답변을 등록한 시각
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 문의 작성 시각

    CONSTRAINT fk_inquiry_member
        FOREIGN KEY (member_no)                               -- 문의 작성자를 member 테이블과 연결
        REFERENCES member(member_no),

    CONSTRAINT fk_inquiry_root
        FOREIGN KEY (root_inquiry_no)                         -- 재문의를 최초 문의와 자기참조로 연결
        REFERENCES inquiry(inquiry_no),

    CONSTRAINT fk_inquiry_answered_by
        FOREIGN KEY (answered_by)                             -- 답변 관리자를 member 테이블과 연결
        REFERENCES member(member_no)
);

CREATE TABLE board_comment (

    comment_no SERIAL PRIMARY KEY,                     -- 댓글 고유번호
    board_no INT NOT NULL,                             -- 댓글이 작성된 공지사항 번호
    member_no INT NOT NULL,                            -- 댓글 작성 회원 번호
    parent_comment_no INT,                             -- 부모 댓글 번호
                                                       -- NULL이면 일반 댓글
                                                       -- 값이 있으면 대댓글
    content TEXT NOT NULL,                             -- 댓글 내용
    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,                     -- 댓글 최초 작성 시각
    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,                     -- 댓글 마지막 수정 시각
    
    CONSTRAINT fk_board_comment_board                   -- 공지사항과 연결
        FOREIGN KEY (board_no)
        REFERENCES board(board_no),

    CONSTRAINT fk_board_comment_member                  -- 작성 회원과 연결
        FOREIGN KEY (member_no)
        REFERENCES member(member_no),

    CONSTRAINT fk_board_comment_parent                  -- 부모 댓글과 연결
        FOREIGN KEY (parent_comment_no)
        REFERENCES board_comment(comment_no)
);

-- =====================================================
-- FEE RULE
-- 방문차량의 무료시간 종료 후와 미등록차량의 입차 직후부터 적용할
-- 주차요금 계산 규칙을 관리한다.
-- 요금 계산과 값 검증은 백엔드에서 처리한다.
-- 사용 중인 규칙은 직접 수정하지 않고 비활성화한 뒤
-- 새로운 규칙을 등록하여 기존 정산 기록의 계산 기준을 보존한다.
-- =====================================================
CREATE TABLE fee_rule (

    fee_rule_no SERIAL PRIMARY KEY,                         -- 요금 규칙 고유번호
    rule_name VARCHAR(100) NOT NULL UNIQUE,                 -- 요금 규칙을 구분하기 위한 이름
    unit_minutes INT NOT NULL,                              -- 요금이 한 번 부과되는 시간 단위(분)
    unit_fee NUMERIC(12, 0) NOT NULL,                       -- 시간 단위마다 부과되는 금액
    daily_max_fee NUMERIC(12, 0),                           -- 과금 24시간당 부과할 수 있는 최대요금
                                                             -- 최대요금 제한이 없으면 NULL
    active BOOLEAN NOT NULL DEFAULT TRUE,                   -- 현재 요금 규칙 사용 여부
    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,                           -- 요금 규칙 등록 시각
	effective_from TIMESTAMP NOT NULL                     	-- 요금 규칙 적용 시작시각
);


-- =====================================================
-- BILL
-- 차량 입출차 기록별 주차요금과 결제 상태를 관리한다.
-- 별도의 결제 테이블을 만들지 않고 토스페이먼츠 테스트 결제 결과를
-- 정산서에 함께 저장한다.
-- =====================================================
CREATE TABLE bill (

    bill_no SERIAL PRIMARY KEY,                             -- 정산서 고유번호
    car_log_no INT NOT NULL UNIQUE,                         -- 정산 대상 차량 입출차 기록 고유번호
                                                             -- 입출차 기록 한 건당 정산서 한 건만 생성
    fee_rule_no INT NOT NULL,                               -- 정산에 적용된 요금 규칙 고유번호
    kiosk_no INT,                                           -- 결제가 진행된 키오스크 고유번호
                                                             -- 미결제 또는 요금 면제 정산이면 NULL 가능
    charge_minutes INT NOT NULL,                            -- 무료시간을 제외하고 요금 계산에 적용한 시간(분)
    bill_amount NUMERIC(12, 0) NOT NULL,                    -- 백엔드에서 계산한 최종 정산금액
    bill_status VARCHAR(20) NOT NULL
        DEFAULT 'UNPAID',                                   -- 정산 상태
                                                             -- UNPAID: 미결제
                                                             -- PAID: 결제완료
    payment_order_id VARCHAR(64) UNIQUE,                    -- 백엔드에서 생성한 토스페이먼츠 주문번호
                                                             -- 결제를 요청하기 전에는 NULL 가능
    payment_key VARCHAR(200) UNIQUE,                        -- 토스페이먼츠에서 발급한 결제 고유키
                                                             -- 결제 승인 전에는 NULL
    payment_method VARCHAR(30),                             -- 승인된 결제수단
                                                             -- 카드·간편결제 등
                                                             -- 결제 승인 전에는 NULL
    issued_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,                          -- 정산서 생성 시각
    paid_at TIMESTAMP,                                      -- 결제 승인 완료 시각
                                                             -- 미결제 상태이면 NULL
    CONSTRAINT fk_bill_car_log
        FOREIGN KEY (car_log_no)                            -- 정산서를 차량 입출차 기록과 연결
        REFERENCES car_log(car_log_no)
        ON DELETE RESTRICT,                                 -- 정산서가 존재하면 입출차 기록 삭제 제한
    CONSTRAINT fk_bill_fee_rule
        FOREIGN KEY (fee_rule_no)                           -- 정산서를 적용된 요금 규칙과 연결
        REFERENCES fee_rule(fee_rule_no)
        ON DELETE RESTRICT,                                 -- 사용된 요금 규칙 삭제 제한
    CONSTRAINT fk_bill_kiosk
        FOREIGN KEY (kiosk_no)                              -- 정산서를 결제가 진행된 키오스크와 연결
        REFERENCES kiosk(kiosk_no)
        ON DELETE RESTRICT                                  -- 정산 기록이 있으면 키오스크 삭제 제한
);


-- 미결제·결제완료 상태별 정산 목록을 생성 시각 역순으로 조회하는 데 사용한다.
CREATE INDEX idx_bill_status_issued_at
    ON bill(bill_status, issued_at DESC);


-- 키오스크별 결제 내역을 조회하는 데 사용한다.
CREATE INDEX idx_bill_kiosk_no
    ON bill(kiosk_no);


-- 완료된 정산 내역을 결제 시각 역순으로 조회하는 데 사용한다.
CREATE INDEX idx_bill_paid_at
    ON bill(paid_at DESC);


CREATE TABLE gate_pdm (

    pdm_no SERIAL PRIMARY KEY,  -- 예지보전 결과 고유번호
    gate_no INT NOT NULL,          -- 예측 대상 게이트 번호
    risk_score NUMERIC(6,5) NOT NULL, -- 최종 예측 등급의 확률
    risk_level VARCHAR(10) NOT NULL,  -- 정상, 주의, 위험
    normal_probability NUMERIC(6,5),   -- 정상 확률
    warning_probability NUMERIC(6,5),  -- 주의 확률
    critical_probability NUMERIC(6,5), -- 위험 확률
    expected_risk_level VARCHAR(10), -- 테스트 CSV의 실제 정답
    prediction_correct BOOLEAN,      -- 예측 정답 일치 여부
    sensor_collected_at TIMESTAMPTZ, -- 센서 데이터 수집 시각
    model_version VARCHAR(50) NOT NULL, -- 사용 모델 버전
    predicted_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,   -- 모델 예측 시각

    CONSTRAINT fk_gate_pdm_gate
        FOREIGN KEY (gate_no)
        REFERENCES gate(gate_no)
        ON DELETE RESTRICT
);
-- =====================================================
-- CAMERA PDM
-- 카메라 센서값을 XGBoost 모델로 분석한
-- 예지보전 위험도와 예측 결과를 저장한다.
-- =====================================================
CREATE TABLE camera_pdm (

    pdm_no SERIAL PRIMARY KEY,     -- 예지보전 결과 고유번호
    camera_no INT NOT NULL,           -- 예측 대상 카메라 번호
    risk_score NUMERIC(6,5) NOT NULL, -- 최종 예측 등급의 확률
    risk_level VARCHAR(10) NOT NULL,  -- 정상, 주의, 위험
    normal_probability NUMERIC(6,5),   -- 정상 확률
    warning_probability NUMERIC(6,5),  -- 주의 확률
    critical_probability NUMERIC(6,5), -- 위험 확률
    expected_risk_level VARCHAR(10), -- 테스트 CSV의 실제 정답
    prediction_correct BOOLEAN,      -- 모델 예측과 테스트 정답 일치 여부
    sensor_collected_at TIMESTAMPTZ, -- 센서 데이터 수집 시각
    model_version VARCHAR(50) NOT NULL, -- 사용한 모델 버전
    predicted_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,   -- 모델 예측 시각

    CONSTRAINT fk_camera_pdm_camera
        FOREIGN KEY (camera_no)
        REFERENCES camera(camera_no)
        ON DELETE RESTRICT
);
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
