BEGIN;

-- =====================================================
-- DROP TABLES (역순)
-- =====================================================

DROP TABLE IF EXISTS notice;

DROP TABLE IF EXISTS camera_data;
DROP TABLE IF EXISTS camera;

DROP TABLE IF EXISTS car_log;

DROP TABLE IF EXISTS vehicle_car;

DROP TABLE IF EXISTS gate;

DROP TABLE IF EXISTS parking;

DROP TABLE IF EXISTS member;



-- =====================================================
-- CREATE TABLES
-- =====================================================


-- =====================================================
-- TABLE: member
-- =====================================================

CREATE TABLE member (
    member_no SERIAL PRIMARY KEY,   -- member 고유 아이디

    login_id VARCHAR(30) NOT NULL,          -- member 로그인 아이디
    login_pwd VARCHAR(255) NOT NULL,        -- Java에서 생성한 비밀번호 해시값
   
    mem_dong INT NOT NULL,                  -- 동 (관리실: 1 , 경비실 : 0)
    mem_ho INT NOT NULL,                     -- 호수(관리실, 경비실: 0)

    mem_name VARCHAR(30) NOT NULL,            -- 이름
    mem_phone VARCHAR(30) NOT NULL,            -- 연락처

    role VARCHAR(30) NOT NULL,            -- 권한 (관리실, 경비실, 입주민)

    create_at TimeStamp,         -- 가입일
    delete_at TimeStamp,         -- 탈퇴일   

    mem_status VARCHAR(30) NOT NULL,           -- 상태 ( 휴직, 퇴사, 거주, 전출 )   

	approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);   

	--// 세가지 글만 입력이 가능하게 함 'PENDING', 'APPROVED', 'REJECTED'
	ALTER TABLE member
	ADD CONSTRAINT chk_member_approval_status
	CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- =====================================================
-- TABLE: parking
-- =====================================================

CREATE TABLE parking (
    parking_no     SERIAL          PRIMARY KEY,       -- 주차장 고유번호
    parking_name   VARCHAR(100),                      -- 이름
    parking_spaces INT,                               -- 총 주차 가능수
    parking_Location       VARCHAR(255)               -- 위치 비고란
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
-- TABLE: vehicle_car
-- =====================================================

CREATE TABLE vehicle_car (

    vehicle_car_no SERIAL       PRIMARY KEY,  -- 등록차량 일련번호
	registered_no INT,						  -- 등록한 입주민 또는 관리인 번호 
    vehicle_type   VARCHAR(20)  NOT NULL DEFAULT 'normal' CHECK (vehicle_type IN ('normal', 'visit')), -- 차량 등록 타입
    car_no         VARCHAR(20)  NOT NULL,     -- 차량 번호
    vehicle_status VARCHAR(20)  NOT NULL DEFAULT 'WAITING' CHECK (vehicle_status IN ('WAITING', 'APPROVED', 'EXPIRED', 'UNKNOWN')), -- 승인 상태
    start_date     TIMESTAMP,                 -- 주차 허용 시작 일시
    end_date       TIMESTAMP,                 -- 주차 허용 종료 일시
    approved_no    INT,                       -- 승인 처리한 멤버 번호
    approved_at    TIMESTAMP,                 -- 최종 승인 처리 일시
	
   -- [외래키] 승인자는 member 테이블의 member_no를 참조
    CONSTRAINT fk_vehicle_approved_member FOREIGN KEY (approved_no) REFERENCES member(member_no) ON DELETE SET NULL,

   -- [외래키] 등록자는 member 테이블의 member_no를 참조
   CONSTRAINT fk_vehicle_registered_member FOREIGN KEY (registered_no) REFERENCES member(member_no) ON DELETE SET NULL


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
	direction		  VARCHAR(10) 	  CHECK(direction IN ('IN','OUT','UNKNOWN')),	-- 차량 입출차 방향 기록 
	
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
-- TABLE: car_log
-- =====================================================

CREATE TABLE car_log (
    car_log_no      SERIAL PRIMARY KEY,  -- 로그 고유번호

    vehicle_car_no  INT,                 -- 등록 차량 고유번호 (FK)
    in_gate_no      INT,                 -- 입차 게이트 번호 (FK)
    in_time         TIMESTAMP,           -- 입차 시각
    out_gate_no     INT,                 -- 출차 게이트 번호 (FK)
    out_time        TIMESTAMP,           -- 출차 시각


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

	-- //[수정 쿼리문] 중복데이터 방지 (현재 주차인 상태로 확인되는 차량이 입차하는 경우 방지.)
	CREATE UNIQUE INDEX uq_active_car_log
		ON car_log(vehicle_car_no)
		WHERE out_time IS NULL;

	

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

-- 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bono_user;

COMMIT;
