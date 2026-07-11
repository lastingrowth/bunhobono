BEGIN;

-- =====================================================
-- DROP TABLES (역순)
-- =====================================================
DROP TABLE IF EXISTS parking_payment;
DROP TABLE IF EXISTS parking_charge;
DROP TABLE IF EXISTS parking_fee_policy;
DROP TABLE IF EXISTS wrong_car;

DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS car_log;
DROP TABLE IF EXISTS camera_data;
DROP TABLE IF EXISTS camera;
DROP TABLE IF EXISTS vehicle_car;
DROP TABLE IF EXISTS gate;
DROP TABLE IF EXISTS parking;
DROP TABLE IF EXISTS member;




-- =====================================================
-- CREATE TABLES (역순)
-- =====================================================


-- =====================================================
-- TABLE: member
-- =====================================================

CREATE TABLE member (
    member_no SERIAL PRIMARY KEY,   -- member 고유 아이디

    login_id VARCHAR(30),          -- member 로그인 아이디
    login_pwd VARCHAR(100),         -- member 로그인 비밀번호
   
    mem_dong INT,                  -- 동 (관리실: 1 , 경비실 : 0)
    mem_ho INT,                     -- 호수(관리실, 경비실: 0)

    mem_name VARCHAR(30),            -- 이름
    mem_phone VARCHAR(30),            -- 연락처

    role VARCHAR(30)	NOT NULL
		  CHECK (role IN ('ADMIN', 'RESIDENT')),            -- 권한 (관리실,입주민)

    create_at TimeStamp,         -- 가입일
    delete_at TimeStamp,         -- 탈퇴일   

    mem_status VARCHAR(30)            -- 상태 ( 휴직, 퇴사, 거주, 전출 )   
);   


-- =====================================================
-- TABLE: parking
-- =====================================================

CREATE TABLE parking (
    parking_no     SERIAL          PRIMARY KEY,       -- 주차장 고유번호
    parking_name   VARCHAR(100)  NOT NULL,                      -- 이름
    parking_spaces INT NOT NULL CHECK (parking_spaces >= 0),-- 총 주차 가능수
    parking_location      VARCHAR(255)                       -- 위치 비고란
);


-- =====================================================
-- TABLE: gate
-- =====================================================

CREATE TABLE gate (
    gate_no      SERIAL          PRIMARY KEY,                          -- 게이트 고유번호
    parking_no   INT             NOT NULL,                             -- 연결된 주차장 고유번호 (FK)
    gate_name    VARCHAR(100)    NOT NULL,                                         -- 게이트 이름
    gate_type    VARCHAR(10)     NOT NULL   
						CHECK (gate_type IN ('In', 'Out', 'Both')), -- 게이트 구분 (In, Out, Both)

    -- 외래키(FK) 설정: Parking 테이블의 Parking_No 컬럼을 참조합니다.
    CONSTRAINT fk_gate_parking 
        FOREIGN KEY (parking_no) REFERENCES parking(parking_no)
        ON DELETE CASCADE 
      -- 주차 구역이 사라지면 해당 게이트도 같이 자동 삭제
);


-- =====================================================
-- TABLE: vehicle_car
-- =====================================================

CREATE TABLE vehicle_car (
    vehicle_car_no SERIAL       PRIMARY KEY,  -- 등록차량 일련번호
    vehicle_type   VARCHAR(20)  NOT NULL DEFAULT 'normal' CHECK (vehicle_type IN ('normal', 'visit')), -- 차량 등록 타입
    car_no         VARCHAR(20)  NOT NULL,     -- 차량 번호
    vehicle_status VARCHAR(20)  NOT NULL DEFAULT 'WAITING' CHECK (vehicle_status IN ('WAITING', 'APPROVED', 'EXPIRED', 'UNKNOWN')), -- 승인 상태
    start_date     TIMESTAMP,                 -- 주차 허용 시작 일시
    end_date       TIMESTAMP,                 -- 주차 허용 종료 일시
    member_no    INT,                       -- 승인 처리한 멤버 번호
    approved_at    TIMESTAMP,                 -- 최종 승인 처리 일시
   -- [외래키] 승인자는 member 테이블의 member_no를 참조
    CONSTRAINT fk_vehicle_member FOREIGN KEY (member_no) REFERENCES member(member_no) ON DELETE SET NULL
        
);


-- =====================================================
-- TABLE: camera
-- =====================================================

CREATE TABLE camera (
    camera_no    SERIAL          PRIMARY KEY,                          -- 카메라고유번호
    gate_no      INT             NOT NULL,                             -- 연결된 게이트 고유번호 (FK)
    camera_name  VARCHAR(100)    NOT NULL,                                         -- 카메라이름
    camera_type  VARCHAR(20)     NOT NULL   CHECK (camera_type IN ('In', 'Out', 'Both')), -- 카메라유형 (입차, 출차, 양방향)
    install_date Date,                                          -- 설치일

    -- [외래키 설정 2] gate 테이블의 gate_no 컬럼 참조
    CONSTRAINT fk_camera_gate 
        FOREIGN KEY (gate_no) REFERENCES gate(gate_no) ON DELETE CASCADE 
      -- 게이트가 철거되면 해당 카메라 데이터도 자동 삭제
);


-- =====================================================
-- TABLE: camera_data
-- =====================================================

CREATE TABLE camera_data (
    camera_data_no    SERIAL          PRIMARY KEY,                                  -- 촬영데이터고유번호
    camera_no         INT             NOT NULL,                                     -- 연결된 카메라 고유번호 (FK)
    vehicle_car_no        INT,                                                          -- 인식된 등록 차량 고유번호 (미등록 차량을 위해 NOT NULL 제거, FK)
    car_no            VARCHAR(50),                                                  -- 인식된 차량번호 (OCR 실패 대비 텍스트 저장)
    capture_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,                    -- 촬영시각 (기본값: 현재 시각)
    image_path        TEXT,                                                         -- 저장된 이미지 파일 경로
    recognition_state BOOLEAN,                                                      -- 인식 성공/실패 여부 (TRUE/FALSE)
    confidence_score  NUMERIC(5,2),                                                 -- 번호판 인식 신뢰도 (예: 99.85)

    --  [외래키 설정 1] camera 테이블의 camera_no 컬럼 참조
    CONSTRAINT fk_cameradata_camera 
        FOREIGN KEY (camera_no) REFERENCES camera(camera_no)
        ON DELETE CASCADE,
		-- 카메라 기기가 삭제되면 관련 촬영 데이터도 함께 삭제
        
    --  [외래키 설정 2] vehicle_car 테이블의 vehicle_car_no 컬럼 참조
    CONSTRAINT fk_cameradata_vehicle 
        FOREIGN KEY (vehicle_car_no) REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL 
		-- 등록된 차량 정보가 삭제되더라도 사진 촬영 기록(증거)은 남겨둠
); 

-- =====================================================
-- TABLE: car_log
-- =====================================================

CREATE TABLE car_log (
    car_log_no      SERIAL PRIMARY KEY,  -- 로그 고유번호
    vehicle_car_no  INT,                 -- 등록 차량 고유번호 (FK)
	camera_data_no	int,
    in_gate_no      INT,                 -- 입차 게이트 번호 (FK)
    in_time         TIMESTAMP,           -- 입차 시각
    out_gate_no     INT,                 -- 출차 게이트 번호 (FK)
    out_time        TIMESTAMP,           -- 출차 시각
    free_time       INTEGER,             -- 무료 주차 시간 (분 단위 등)

    -- [외래키 설정 1] 등록 차량 테이블 참조
    CONSTRAINT fk_log_vehicle_car 
        FOREIGN KEY (vehicle_car_no) REFERENCES vehicle_car(vehicle_car_no) 
        ON DELETE SET NULL,

    -- [외래키 설정 2] 입차 게이트 참조 (gate 테이블의 gate_no)
    CONSTRAINT fk_log_in_gate  
        FOREIGN KEY (in_gate_no) REFERENCES gate(gate_no) 
        ON DELETE SET NULL,

    -- [외래키 설정 3] 출차 게이트 참조 (gate 테이블의 gate_no)
    CONSTRAINT fk_log_out_gate 
        FOREIGN KEY (out_gate_no) REFERENCES gate(gate_no) 
        ON DELETE SET NULL,

	CONSTRAINT fk_log_camera_data 
        FOREIGN KEY (camera_data_no) REFERENCES camera_data(camera_data_no) 
        ON DELETE SET NULL
);




-- =====================================================
-- TABLE: notice
-- =====================================================

CREATE TABLE notice (
    notice_no SERIAL PRIMARY KEY,                          -- 알림 고유번호
    car_log_no INT NOT NULL,                               -- 연결된 입·출차 로그 고유번호 (FK)

    detect_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 장기주차 감지 일시
    stay_days INT NOT NULL CHECK (stay_days >= 0),         -- 감지 시점 기준 체류 일수
    alert_stat VARCHAR(20) NOT NULL DEFAULT 'Unresolved'   -- 알림 처리 상태
        CHECK (alert_stat IN ('Unresolved', 'Checked', 'Resolved')),
	handled_by_member_no INT,          -- 알림을 마지막으로 처리한 직원 번호
	handled_at TIMESTAMP,              -- 마지막 처리 일시

    -- car_log 테이블의 car_log_no를 참조
    -- 로그가 삭제되면 연결된 알림도 함께 삭제
    CONSTRAINT fk_notice_car_log
    FOREIGN KEY (car_log_no) REFERENCES car_log(car_log_no)
    ON DELETE CASCADE,

	CONSTRAINT fk_notice_handler
    FOREIGN KEY (handled_by_member_no) REFERENCES member(member_no)
    ON DELETE SET NULL
);




COMMIT;