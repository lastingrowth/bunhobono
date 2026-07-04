BEGIN;

-- =====================================================
-- DROP TABLES (역순)
-- =====================================================

DROP TABLE IF EXISTS parking_payment;
DROP TABLE IF EXISTS parking_charge;
DROP TABLE IF EXISTS parking_fee_policy;

DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS wrong_car;

DROP TABLE IF EXISTS camera_data;
DROP TABLE IF EXISTS camera;

DROP TABLE IF EXISTS car_log;

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

    role VARCHAR(30),            -- 권한 (관리실, 경비실, 입주민)

    create_at TimeStamp,         -- 가입일
    delete_at TimeStamp,         -- 탈퇴일   

    mem_status VARCHAR(30)            -- 상태 ( 휴직, 퇴사, 거주, 전출 )   
);   


-- =====================================================
-- TABLE: parking
-- =====================================================

CREATE TABLE parking (
    parking_no     SERIAL          PRIMARY KEY,       -- 주차장 고유번호
    parking_name   VARCHAR(100),                      -- 이름
    parking_spaces VARCHAR(50),                       -- 총 주차 가능수
    parking_Location       VARCHAR(255)                       -- 위치 비고란
);


-- =====================================================
-- TABLE: gate
-- =====================================================

CREATE TABLE gate (
    gate_no      SERIAL          PRIMARY KEY,                          -- 게이트 고유번호
    parking_no   INT             NOT NULL,                             -- 연결된 주차장 고유번호 (FK)
    gate_name    VARCHAR(100),                                         -- 게이트 이름
    gate_type    VARCHAR(10)     CHECK (gate_type IN ('In', 'Out', 'Both')), -- 게이트 구분 (In, Out, Both)

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
    approved_no    INT,                       -- 승인 처리한 멤버 번호
    approved_at    TIMESTAMP,                 -- 최종 승인 처리 일시
   -- [외래키] 승인자는 member 테이블의 member_no를 참조
    CONSTRAINT fk_vehicle_member FOREIGN KEY (approved_no) REFERENCES member(member_no) ON DELETE SET NULL
        
);


-- =====================================================
-- TABLE: camera
-- =====================================================

CREATE TABLE camera (
    camera_no    SERIAL          PRIMARY KEY,                          -- 카메라고유번호
    parking_no   INT             NOT NULL,                             -- 연결된 주차장 고유번호 (FK)
    gate_no      INT             NOT NULL,                             -- 연결된 게이트 고유번호 (FK)
    camera_name  VARCHAR(100),                                         -- 카메라이름
    camera_type  VARCHAR(20)     CHECK (camera_type IN ('In', 'Out', 'Both')), -- 카메라유형 (입차, 출차, 양방향)
    install_date VARCHAR(20),                                          -- 설치일

    --  [외래키 설정 1] parking 테이블의 parking_no 컬럼 참조
    CONSTRAINT fk_camera_parking FOREIGN KEY (parking_no) REFERENCES parking(parking_no)
        ON DELETE CASCADE,
      -- 주차 구역이 사라지면 해당 카메라 데이터도 자동 삭제
        
    -- [외래키 설정 2] gate 테이블의 gate_no 컬럼 참조
    CONSTRAINT fk_camera_gate 
        FOREIGN KEY (gate_no) REFERENCES gate(gate_no) ON DELETE CASCADE 
      -- 게이트가 철거되면 해당 카메라 데이터도 자동 삭제
);


-- =====================================================
-- TABLE: car_log
-- =====================================================

CREATE TABLE car_log (
    car_log_no      SERIAL PRIMARY KEY,  -- 로그 고유번호

    vehicle_car_no  INT,                 -- 등록 차량 고유번호 (FK)
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
        ON DELETE SET NULL
);


-- =====================================================
-- TABLE: camera_data
-- =====================================================

CREATE TABLE camera_data (
    camera_data_no    SERIAL          PRIMARY KEY,                                  -- 촬영데이터고유번호
    camera_no         INT             NOT NULL,                                     -- 연결된 카메라 고유번호 (FK)
    vehicle_no        INT,                                                          -- 인식된 등록 차량 고유번호 (미등록 차량을 위해 NOT NULL 제거, FK)
    car_no            VARCHAR(50),                                                  -- 인식된 차량번호 (OCR 실패 대비 텍스트 저장)
    capture_time      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,                    -- 촬영시각 (기본값: 현재 시각)
    image_path        TEXT,                                                         -- 저장된 이미지 파일 경로
    recognition_state BOOLEAN,                                                      -- 인식 성공/실패 여부 (TRUE/FALSE)
    confidence_score  NUMERIC(5,2),                                                 -- 번호판 인식 신뢰도 (예: 99.85)

    --  [외래키 설정 1] camera 테이블의 camera_no 컬럼 참조
    CONSTRAINT fk_cameradata_camera 
        FOREIGN KEY (camera_no) REFERENCES camera(camera_no)
        ON DELETE CASCADE, -- 카메라 기기가 삭제되면 관련 촬영 데이터도 함께 삭제
        
    --  [외래키 설정 2] vehicle_car 테이블의 vehicle_car_no 컬럼 참조
    CONSTRAINT fk_cameradata_vehicle 
        FOREIGN KEY (vehicle_no) REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL -- 등록된 차량 정보가 삭제되더라도 사진 촬영 기록(증거)은 남겨둠
); 


-- =====================================================
-- TABLE: notice
-- =====================================================

CREATE TABLE notice (
    notice_no   SERIAL          PRIMARY KEY,                          -- 알림 고유번호
    parking_no  INT             NOT NULL,                             -- 연결된 주차장 고유번호 (FK)
    plate_no    VARCHAR(50)     NOT NULL,                             -- 차량 번호
    entry_at    TIMESTAMP       NOT NULL,                             -- 입차 일시
    detect_at   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,            -- 장기주차 감지 일시
    stay_days   INT             NOT NULL,                             -- 체류/주차 일수 (예: 5일, 7일 등)
    alert_stat  VARCHAR(20)     DEFAULT 'Unresolved'                  -- 알림 상태 (예: 미확인, 확인, 조치완료)
                                CHECK (alert_stat IN ('Unresolved', 'Checked', 'Resolved')), 
                        
    --  [외래키 설정] parking 테이블의 parking_no 컬럼 참조
    CONSTRAINT fk_notice_parking 
        FOREIGN KEY (parking_no) REFERENCES parking(parking_no)
        ON DELETE CASCADE -- 주차 구역이 시스템에서 삭제되면 관련 알림 내역도 함께 삭제
);


-- =====================================================
-- TABLE: wrong_car
-- =====================================================

CREATE TABLE wrong_car (
    wrong_car_no   SERIAL          PRIMARY KEY,                          -- 블랙리스트 고유번호
    plate_no       VARCHAR(50)     NOT NULL UNIQUE,                      -- 블랙리스트 차량 번호
	car_log_no     INT   		   NOT NULL,
    reason_type    VARCHAR(30)     NOT NULL 
                                   CHECK (reason_type IN ('Unpaid', 'Unregistered', 'Suspicious', 'Other')), -- 등록 사유 구분
    description    TEXT,                                                 -- 상세 사유 비고란
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 블랙리스트 등록 일시
    updated_at     TIMESTAMP,                                            -- 수정 일시
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,                 -- 차단 활성화 여부

	CONSTRAINT fk_wrong_car_car_log		
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE CASCADE
);


-- =====================================================
-- TABLE: parking_fee_policy
-- =====================================================

CREATE TABLE parking_fee_policy (
    fee_policy_no SERIAL PRIMARY KEY,	-- 요금 정책 고유 아이디

    parking_no INT NOT NULL,				--  주차장 고유 번호

    vehicle_type VARCHAR(20) NOT NULL,   		-- VISITOR / RESIDENT

    free_minutes INT NOT NULL,				-- 무료 주차 시간 (분)
    unit_minutes INT NOT NULL,				-- 과금 단위 시간 (분)
    unit_fee INT NOT NULL,					-- 단위 시간당 요금
    daily_max_fee INT NOT NULL DEFAULT 0	,	-- 1일 최대 요금

    is_active BOOLEAN DEFAULT TRUE,			-- 정책 사용 여부

  -- 외래키 (FK) 설정 : parking 테이블의 parking_no 컬럼을 참조합니다.
    CONSTRAINT fk_fee_policy_parking		
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE CASCADE
);


-- =====================================================
-- TABLE: parking_charge
-- =====================================================

CREATE TABLE parking_charge (
    charge_no SERIAL PRIMARY KEY,  -- 청구 고유 ID

    car_log_no INT NOT NULL,        -- 주차 이용 기록 (car_log FK)
    fee_policy_no INT NOT NULL,     -- 적용된 요금 정책

    charge_type VARCHAR(20) NOT NULL,  -- 청구 유형 (MONTHLY / VIOLATION)

    amount INT NOT NULL,  -- 최종 청구 금액

    payer_type VARCHAR(20) NOT NULL,  -- 부담 주체 (RESIDENT / VISITOR / LOT)
    payer_no INT,  -- 부담자 ID (회원/차량 등)

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- 청구 상태

    -- [외래키 설정 1] car_log 데이터 참조 (car_log 테이블의 car_log_no)
    CONSTRAINT fk_charge_car_log
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE CASCADE,

    -- [외래키 설정 2] 요금 정책 테이블 참조 (parking_fee_policy 테이블의 fee_policy_no)
    CONSTRAINT fk_charge_policy
        FOREIGN KEY (fee_policy_no)
        REFERENCES parking_fee_policy(fee_policy_no)
        ON DELETE RESTRICT
);


-- =====================================================
-- TABLE: parking_payment
-- =====================================================

CREATE TABLE parking_payment (
    payment_no SERIAL PRIMARY KEY,  -- 결제 고유 ID

    charge_no INT NOT NULL,  -- 청구 ID (parking_charge FK)

    payment_amount INT NOT NULL,  -- 결제 금액

    payment_method VARCHAR(20) NOT NULL,  -- 결제 방식 (CARD / AUTO)

    payment_status VARCHAR(20) NOT NULL,  -- 결제 상태 (SUCCESS / FAIL)

    transaction_no VARCHAR(50),  -- 거래 ID

    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 결제 시각

    -- 외래키(FK) 설정: parking_charge 테이블의 charge_no 컬럼을 참조합니다
    CONSTRAINT fk_payment_charge
        FOREIGN KEY (charge_no)
        REFERENCES parking_charge(charge_no)
        ON DELETE CASCADE
);

COMMIT;