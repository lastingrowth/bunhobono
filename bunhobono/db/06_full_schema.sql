BEGIN;

-- =====================================================
-- RESET DATABASE OBJECTS
-- 현재 구조와 이전 구조의 객체를 모두 제거한다.
-- =====================================================

-- 현재 및 이전 버전의 조회용 뷰
DROP VIEW IF EXISTS v_notice_detail;
DROP VIEW IF EXISTS notice_detail;
DROP VIEW IF EXISTS notice_overstay;
DROP VIEW IF EXISTS parking_payment_detail;

-- 현재 또는 이전 버전에서 사용할 수 있는 작업·위치·결제 테이블
DROP TABLE IF EXISTS robot_task;
DROP TABLE IF EXISTS robot_charger;
DROP TABLE IF EXISTS vehicle_location;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS parking_charge;

-- 삭제·보관
DROP TABLE IF EXISTS trash_bin;

-- 공지·알림
DROP TABLE IF EXISTS resident_notice;
DROP TABLE IF EXISTS vehicle_nt;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS board;

-- 요금·결제
DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS fee_rule;

-- 주차 관제
DROP TABLE IF EXISTS robot;
DROP TABLE IF EXISTS kiosk;
DROP TABLE IF EXISTS car_log;
DROP TABLE IF EXISTS camera_data;
DROP TABLE IF EXISTS camera;
DROP TABLE IF EXISTS vehicle_car;
DROP TABLE IF EXISTS gate;
DROP TABLE IF EXISTS parking_space;
DROP TABLE IF EXISTS parking;

-- 주차 관제 함수
DROP FUNCTION IF EXISTS validate_parking_space_number();
DROP FUNCTION IF EXISTS validate_parking_capacity();

-- 회원
DROP TABLE IF EXISTS member_archive;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS apartment_unit;

-- =====================================================
-- APARTMENT UNIT
-- 실제 아파트 세대만 저장하며 관리실 0동 0호는 만들지 않는다.
-- =====================================================
CREATE TABLE apartment_unit (
    -- 세대를 다른 테이블에서 참조할 때 사용하는 내부 고유번호
    apartment_unit_no SERIAL PRIMARY KEY,
    -- 아파트 동 번호. 관리실을 의미하는 0동은 허용하지 않는다.
    dong INT NOT NULL,
    -- 아파트 호수. 0호와 음수는 허용하지 않는다.
    ho INT NOT NULL,
    -- EMPTY: 입주 가능한 빈 세대, OCCUPIED: 현재 회원이 점유한 세대
    unit_status VARCHAR(20) NOT NULL DEFAULT 'EMPTY'
        CHECK (unit_status IN ('EMPTY', 'OCCUPIED')),

    -- 같은 동·호를 두 번 만들지 못하게 한다.
    CONSTRAINT uq_apartment_unit_address UNIQUE (dong, ho),
    -- 실제 세대만 저장하도록 동·호수를 양수로 제한한다.
    CONSTRAINT chk_apartment_unit_dong CHECK (dong > 0),
    CONSTRAINT chk_apartment_unit_ho CHECK (ho > 0)
);

-- =====================================================
-- MEMBER
-- 입주민만 실제 세대를 참조하며 관리자는 unit_no가 NULL이다.
-- =====================================================
CREATE TABLE member (
    -- 회원 내부 고유번호
    member_no SERIAL PRIMARY KEY,
    -- 로그인에 사용하는 아이디. 회원 간 중복을 허용하지 않는다.
    login_id VARCHAR(30) NOT NULL UNIQUE,
    -- 암호화된 로그인 비밀번호
    login_pwd VARCHAR(100) NOT NULL,
    -- 입주민이 거주하는 세대 번호. 관리자는 NULL이다.
    unit_no INT,
    -- 회원 이름
    mem_name VARCHAR(30) NOT NULL,
    -- 회원 연락처. 미입력 상태를 허용한다.
    mem_phone VARCHAR(30),

    -- ADMIN: 관리자, RESIDENT: 입주민
    role VARCHAR(30) NOT NULL
        CHECK (role IN ('ADMIN', 'RESIDENT')),

    -- PENDING: 입주민 가입 승인 대기
    -- ACTIVE: 입주민 거주 중 또는 관리자 근무 중
    -- WITHDRAW_PENDING: 입주민 전출 승인 대기
    -- WITHDRAWN: 입주민 전출 확정 또는 탈퇴 완료
    -- INACTIVE: 관리자 퇴사
    -- ON_LEAVE: 관리자 휴직
    mem_status VARCHAR(30) NOT NULL
        CHECK (
            mem_status IN (
                'PENDING',
                'ACTIVE',
                'WITHDRAW_PENDING',
                'WITHDRAWN',
                'INACTIVE',
                'ON_LEAVE'
            )
        ),

    -- 회원 데이터 생성 시각
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 전출·탈퇴·퇴사 요청 또는 처리 시각. 해당 사항이 없으면 NULL이다.
    delete_at TIMESTAMP,

    -- 입주민의 세대가 실제 apartment_unit에 존재하도록 한다.
    CONSTRAINT fk_member_apartment_unit
        FOREIGN KEY (unit_no)
        REFERENCES apartment_unit(apartment_unit_no)

);


-- =====================================================
-- MEMBER ARCHIVE
-- =====================================================
CREATE TABLE member_archive (
    -- 보관 이력 고유번호
    member_archive_no SERIAL PRIMARY KEY,
    -- 보관되기 전 member.member_no 값
    original_member_no INT NOT NULL,

    -- 원본 회원 삭제 후에도 조회할 수 있도록 회원 정보를 복사해 둔다.
    login_id VARCHAR(50),
    mem_name VARCHAR(50),
    mem_phone VARCHAR(30),
    role VARCHAR(30),
    mem_status VARCHAR(30),

    -- 전출 당시 주소 스냅숏
    mem_dong INT,
    mem_ho INT,

    -- 원본 회원의 가입 시각과 전출·탈퇴 시각
    create_at TIMESTAMP,
    delete_at TIMESTAMP,
    -- member_archive로 이동된 시각
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PARKING
-- 주차장 기본 정보
-- =====================================================
CREATE TABLE parking (
    -- 주차장 내부 고유번호
    parking_no SERIAL PRIMARY KEY,
    -- 화면과 통계에 표시할 주차장 이름
    parking_name VARCHAR(100) NOT NULL,
    -- 일반 차량을 보관할 수 있는 최대 주차면 수
    -- 드롭존·픽업존·로봇 충전 공간은 이 수에 포함하지 않는다.
    parking_spaces INT NOT NULL
        CHECK (parking_spaces >= 0),
    -- 층·건물 등 주차장의 실제 위치 설명
    parking_location VARCHAR(255)
);

-- =====================================================
-- PARKING SPACE
-- 일반 주차면, 드롭존, 픽업존, 로봇 충전소
-- =====================================================
CREATE TABLE parking_space (
    -- 주차장 내부의 물리적 공간 고유번호
    parking_space_no SERIAL PRIMARY KEY,
    -- 공간이 속한 주차장 번호
    parking_no INT NOT NULL,
    -- 같은 공간 종류 안에서 사용하는 양수 순번
    space_number INT NOT NULL
        CHECK (space_number > 0),
    -- PARKING: 일반 주차면, DROP_OFF: 차량 인계 구역
    -- PICK_UP: 출차 차량 인수 구역, ROBOT_CHARGING: 로봇 충전 구역
    space_type VARCHAR(20) NOT NULL
        DEFAULT 'PARKING'
        CHECK (
            space_type IN (
                'PARKING',
                'DROP_OFF',
                'PICK_UP',
                'ROBOT_CHARGING'
            )
        ),
    -- EMPTY: 비어 있음, RESERVED: 사용 예정, OCCUPIED: 사용 중
    space_status VARCHAR(10) NOT NULL DEFAULT 'EMPTY'
        CHECK (space_status IN ('EMPTY', 'RESERVED', 'OCCUPIED')),

    -- 한 주차장 안에서 같은 종류와 번호의 공간을 중복 등록하지 못하게 한다.
    CONSTRAINT uq_parking_space_number
        UNIQUE (parking_no, space_type, space_number),

    -- 소속 공간이 남아 있는 주차장은 삭제할 수 없다.
    CONSTRAINT fk_parking_space_parking
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT
);

-- 주차면 번호가 주차장의 최대 주차 가능 대수를 넘지 않도록 검사한다.
-- 일반 주차면(PARKING)에만 parking.parking_spaces 범위를 적용한다.
-- 드롭존·픽업존·충전 공간은 일반 주차 가능 대수에 포함되지 않으므로 제외한다.
CREATE OR REPLACE FUNCTION validate_parking_space_number()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    -- 입력된 공간이 속한 주차장의 일반 주차 가능 대수
    max_parking_spaces INT;
BEGIN
    -- 트리거 대상 행의 parking_no로 주차장 최대 대수를 조회한다.
    SELECT parking_spaces
      INTO max_parking_spaces
      FROM parking
     WHERE parking_no = NEW.parking_no;

    -- 연결할 주차장이 존재하지 않으면 공간 생성을 중단한다.
    IF max_parking_spaces IS NULL THEN
        RAISE EXCEPTION
            '존재하지 않는 주차장입니다. parking_no=%',
            NEW.parking_no;
    END IF;

    -- 일반 주차면의 번호만 최대 대수 범위 안에 있는지 검사한다.
    IF NEW.space_type = 'PARKING'
       AND NEW.space_number > max_parking_spaces THEN
        RAISE EXCEPTION
            '주차면 번호 P-%는 해당 주차장의 최대 범위 P-%를 초과합니다.',
            LPAD(NEW.space_number::TEXT, 2, '0'),
            LPAD(max_parking_spaces::TEXT, 2, '0');
    END IF;

    -- 모든 검사에 통과한 신규·수정 행을 저장 단계로 전달한다.
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_parking_space_number
-- 신규 공간을 만들거나 소속·종류·번호를 바꾸기 전에 위 함수를 실행한다.
BEFORE INSERT OR UPDATE OF parking_no, space_type, space_number
ON parking_space
FOR EACH ROW
EXECUTE FUNCTION validate_parking_space_number();

-- 이미 생성된 주차면보다 주차장의 최대 대수를 작게 줄이지 못하게 한다.
-- 특수 공간은 제외하고 PARKING 타입 중 가장 큰 번호만 기준으로 삼는다.
CREATE OR REPLACE FUNCTION validate_parking_capacity()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    -- 현재 생성된 일반 주차면 중 가장 큰 space_number
    max_space_number INT;
BEGIN
    SELECT MAX(space_number)
      INTO max_space_number
      FROM parking_space
     WHERE parking_no = OLD.parking_no
       AND space_type = 'PARKING';

    -- 존재하는 가장 큰 주차면 번호보다 parking_spaces를 작게 변경하면 중단한다.
    IF max_space_number IS NOT NULL
       AND NEW.parking_spaces < max_space_number THEN
        RAISE EXCEPTION
            '현재 P-% 주차면까지 존재하므로 주차 가능 대수를 %대로 줄일 수 없습니다.',
            LPAD(max_space_number::TEXT, 2, '0'),
            NEW.parking_spaces;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_parking_capacity
-- parking_spaces 값을 UPDATE하기 직전에 축소 가능 여부를 검사한다.
BEFORE UPDATE OF parking_spaces
ON parking
FOR EACH ROW
EXECUTE FUNCTION validate_parking_capacity();


-- =====================================================
-- GATE
-- 연결된 게이트가 있으면 주차장 삭제를 차단한다.
-- =====================================================
CREATE TABLE gate (
    -- 게이트 내부 고유번호
    gate_no SERIAL PRIMARY KEY,
    -- 게이트가 연결된 주차장 번호
    parking_no INT NOT NULL,
    -- 정문·후문·층별 입구 등 관리자 화면에 표시할 이름
    gate_name VARCHAR(100) NOT NULL,
    -- In: 입차 게이트, Out: 출차 게이트
    gate_type VARCHAR(10) NOT NULL,
        CHECK (gate_type IN ('In', 'Out')),
    -- 0: 비활성 또는 닫힘, 1: 활성 또는 열림
    gate_status INT NOT NULL DEFAULT 0
        CHECK (gate_status IN (0, 1)),

    -- 게이트가 남아 있는 주차장은 삭제할 수 없다.
    CONSTRAINT fk_gate_parking
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT
);
-- =====================================================
-- VEHICLE CAR
-- 등록 차량과 방문 차량의 승인 정보 및 현재 위치를 관리한다.
-- =====================================================
CREATE TABLE vehicle_car (
    -- 차량 내부 고유번호
    vehicle_car_no SERIAL PRIMARY KEY,

    -- normal: 입주민 등록 차량, visit: 방문 차량
    vehicle_type VARCHAR(20) NOT NULL DEFAULT 'normal'
        CHECK (vehicle_type IN ('normal', 'visit')),

    -- 실제 차량번호
    car_no VARCHAR(20) NOT NULL,

    -- 화면 표시나 내부 식별에 사용할 별칭 차량번호. 입력 시 중복을 허용하지 않는다.
    alias_car_no VARCHAR(50) UNIQUE,

    -- WAITING: 승인 대기, APPROVED: 승인 완료
    -- EXPIRED: 등록 기간 만료, UNKNOWN: 승인 여부를 확인할 수 없음
    vehicle_status VARCHAR(20) NOT NULL DEFAULT 'WAITING'
        CHECK (vehicle_status IN ('WAITING', 'APPROVED', 'EXPIRED', 'UNKNOWN')),

    -- normal: 등록 유효 시작시간
    -- visit: 예상 방문시간
    start_date TIMESTAMP,

    -- normal: 등록 유효 종료시간
    -- visit: 예상 방문시간 + 등록시간
    end_date TIMESTAMP,

    -- 차량을 등록한 입주민 번호. 회원 삭제 후에는 NULL로 남긴다.
    member_no INT,

    -- 관리자가 차량 등록을 승인한 시각
    approved_at TIMESTAMP,

    -- 차량이 현재 정차한 parking_space 번호
    -- 외부에 있거나 로봇이 운반 중이면 NULL이다.
    current_space_no INT,

    -- 차량의 현재 위치 상태
    location_status VARCHAR(20) NOT NULL
        DEFAULT 'OUTSIDE'
        CHECK (
            location_status IN (
                'OUTSIDE',
                'AT_SPACE',
                'IN_TRANSIT'
            )
        ),

    CONSTRAINT fk_vehicle_member
        FOREIGN KEY (member_no)
        REFERENCES member(member_no)
        ON DELETE SET NULL,

     -- 차량이 위치한 공간은 사용 중에 삭제할 수 없다.
    CONSTRAINT fk_vehicle_current_space
        FOREIGN KEY (current_space_no)
        REFERENCES parking_space(parking_space_no)
        ON DELETE RESTRICT,

    -- 위치 상태와 공간번호가 일치해야 한다.
    CONSTRAINT chk_vehicle_location
        CHECK (
            (
                location_status = 'AT_SPACE'
                AND current_space_no IS NOT NULL
            )
            OR
            (
                location_status IN (
                    'OUTSIDE',
                    'IN_TRANSIT'
                )
                AND current_space_no IS NULL
            )
        ),

    -- 입주민 승인 차량만 로봇 주차 위치를 가질 수 있다.
    CONSTRAINT chk_vehicle_robot_parking_access
        CHECK (
            location_status = 'OUTSIDE'
            OR (
                vehicle_type = 'normal'
                AND member_no IS NOT NULL
                AND vehicle_status = 'APPROVED'
            )
        )
);

-- 현재 하나의 공간에는 차량 한 대만 위치할 수 있다.
CREATE UNIQUE INDEX uq_vehicle_current_space
    ON vehicle_car(current_space_no)
    WHERE current_space_no IS NOT NULL;

-- =====================================================
-- CAMERA
-- 연결된 카메라가 있으면 게이트 삭제를 차단한다.
-- =====================================================
CREATE TABLE camera (
    -- 카메라 내부 고유번호
    camera_no SERIAL PRIMARY KEY,
    -- 카메라가 설치된 게이트 번호
    gate_no INT NOT NULL,
    -- 관리자 화면에 표시할 카메라 이름
    camera_name VARCHAR(100) NOT NULL,
    -- In: 입차 촬영용, Out: 출차 촬영용
    camera_type VARCHAR(20) NOT NULL
        CHECK (camera_type IN ('In', 'Out')),
    -- 카메라 설치일. 확인할 수 없으면 NULL이다.
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
    -- 촬영 데이터 내부 고유번호
    camera_data_no SERIAL PRIMARY KEY,
    -- 촬영한 카메라 번호
    camera_no INT NOT NULL,
    -- 인식 결과와 연결된 등록 차량 번호. 미등록 차량이면 NULL일 수 있다.
    vehicle_car_no INT,
    -- 최종적으로 확정한 차량번호
    car_no VARCHAR(50),
    -- OCR이 이미지에서 판독한 원본 차량번호
    ocr_car_no VARCHAR(50),
    -- 이미지 촬영 시각
    capture_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 원본 촬영 이미지 저장 경로
    image_path TEXT,
    -- 번호판 영역을 잘라낸 이미지 저장 경로
    crop_image_path TEXT,
    -- 차량번호 인식 성공 여부
    recognition_state BOOLEAN,
    -- OCR 인식 신뢰도. 소수점 둘째 자리까지 저장한다.
    confidence_score NUMERIC(5,2),
    -- 촬영·판독 과정에 대한 비고
    cam_note VARCHAR(100),

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
    -- 입출차 기록 내부 고유번호
    car_log_no SERIAL PRIMARY KEY,
    -- 등록 차량과 연결되는 번호. 미등록 차량 기록이면 NULL일 수 있다.
    vehicle_car_no INT,
    -- 입차 시 생성된 camera_data 번호
    camera_data_no INT,
    -- 출차 시 생성된 camera_data 번호
    out_camera_data_no INT,
    -- 차량이 들어온 게이트 번호
    in_gate_no INT,
    -- 실제 입차 시각
    in_time TIMESTAMP,
    -- 차량이 나간 게이트 번호
    out_gate_no INT,
    -- 실제 출차 시각. 아직 주차 중이면 NULL이다.
    out_time TIMESTAMP,
    -- 요금 계산에서 제외할 무료 주차시간(분)
    free_time INTEGER,
    -- 연결된 차량·촬영 데이터가 삭제되어도 보존할 당시 차량번호
    snapshot_car_no VARCHAR(50),
    -- [지난 기록 통계] 입차 당시 차량 종류를 보존한다.
    snapshot_car_kind VARCHAR(20) NOT NULL
        CHECK (snapshot_car_kind IN ('REGISTERED', 'VISIT', 'UNKNOWN')),

    CONSTRAINT fk_log_vehicle_car
        -- 등록 차량이 삭제되면 입출차 기록은 유지하고 연결만 해제한다.
        FOREIGN KEY (vehicle_car_no)
        REFERENCES vehicle_car(vehicle_car_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_in_gate
        -- 입차 게이트가 삭제되더라도 과거 입출차 기록은 유지한다.
        FOREIGN KEY (in_gate_no)
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_out_gate
        -- 출차 게이트가 삭제되더라도 과거 입출차 기록은 유지한다.
        FOREIGN KEY (out_gate_no)
        REFERENCES gate(gate_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_camera_data
        -- 입차 촬영 데이터가 삭제되면 연결만 NULL로 변경한다.
        FOREIGN KEY (camera_data_no)
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_out_camera_data
        -- 출차 촬영 데이터가 삭제되면 연결만 NULL로 변경한다.
        FOREIGN KEY (out_camera_data_no)
        REFERENCES camera_data(camera_data_no)
        ON DELETE SET NULL
);

-- =====================================================
-- 키오스크 장비 정보
-- 주차장별 정산 키오스크의 용도와 사용 여부를 관리한다.
-- =====================================================
CREATE TABLE kiosk (
    kiosk_no SERIAL PRIMARY KEY,             -- 키오스크 내부 고유번호

    kiosk_name VARCHAR(100) NOT NULL,           -- 관리자 화면에 표시할 키오스크 이름
    -- B1 키오스크1
    -- B1 키오스크2
    -- B2 키오스크1
    -- B2 키오스크2

    parking_no INT NOT NULL,                   -- 키오스크가 설치된 주차장 번호 (B1,B2)

    active BOOLEAN NOT NULL DEFAULT TRUE,      -- 현재 사용 여부

    -- 키오스크가 설치된 주차장이 먼저 삭제되지 않도록 제한한다.
    CONSTRAINT fk_kiosk_parking
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT
);


-- =====================================================
-- ROBOT
-- 로봇 주차장에서 운행하는 주차 로봇의 현재 소속·위치·운영 상태를 관리한다.
-- =====================================================
CREATE TABLE robot (
    -- 로봇 내부 고유번호
    robot_no SERIAL PRIMARY KEY,
    -- 로봇이 소속된 주차장 번호
    parking_no INT NOT NULL,
    -- 로봇이 현재 정지해 있는 공간 번호
    -- 이동 중이거나 위치를 확인할 수 없으면 NULL이다.
    current_space_no INT,
    -- 관리자 화면에 표시할 로봇 이름
    robot_name VARCHAR(100) NOT NULL,
    -- 작업 상태가 아닌 장비 운영 상태
    operating_status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE'
        CHECK (
            operating_status IN (
                'ACTIVE',
                'MAINTENANCE',
                'FAULT',
                'OFFLINE'
            )
        ),
    -- 로봇 설치일
    install_date DATE,

    -- 위치 또는 상태 정보의 마지막 갱신 시각
    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_robot_parking
        -- 소속 로봇이 남아 있는 주차장은 삭제할 수 없다.
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT,

    CONSTRAINT fk_robot_current_space
        -- 로봇이 위치한 공간은 로봇 이동 전까지 삭제할 수 없다.
        FOREIGN KEY (current_space_no)
        REFERENCES parking_space(parking_space_no)
        ON DELETE RESTRICT
);

-- 현재 한 공간에는 로봇 한 대만 위치할 수 있다.
CREATE UNIQUE INDEX uq_robot_current_space
    ON robot(current_space_no)
    WHERE current_space_no IS NOT NULL;

-- =====================================================
-- NOTIFICATIONS
-- 특정 입주민에게 전달되는 차량 관련 개인 알림을 저장한다.
-- 관리자 처리용 이상 상황을 저장하는 notice와 용도가 다르다.
-- =====================================================

CREATE TABLE vehicle_nt (
    -- 입주민 차량 알림 내부 고유번호
    vehicle_nt_no SERIAL PRIMARY KEY,

    -- 알림을 받는 입주민 번호. 회원 삭제 시 개인 알림도 함께 삭제한다.
    recipient_member_no INT NOT NULL
        REFERENCES member(member_no) ON DELETE CASCADE,

    -- 알림을 보낸 관리자 번호. 자동 알림이거나 관리자가 삭제되면 NULL이다.
    sender_member_no INT
        REFERENCES member(member_no) ON DELETE SET NULL,

    -- 알림과 관련된 차량 번호. 차량 삭제 후에도 알림 문장은 유지한다.
    vehicle_car_no INT
        REFERENCES vehicle_car(vehicle_car_no) ON DELETE SET NULL,

    -- 알림과 관련된 입출차 기록. 기록 삭제 후에는 연결만 해제한다.
    car_log_no INT
        REFERENCES car_log(car_log_no) ON DELETE SET NULL,

    -- 원본 차량이 삭제되어도 표시할 수 있도록 저장하는 당시 차량번호
    snapshot_car_no VARCHAR(20) NOT NULL,

    -- ADMIN_APPROVED: 관리자가 차량 등록을 승인함
    -- ADMIN_REJECTED: 관리자가 차량 등록을 거절함
    -- APPROVAL_TIMEOUT: 승인 대기 시간이 초과됨
    -- NO_ENTRY_EXPIRED: 승인됐지만 유효기간 안에 입차하지 않음
    -- VISIT_OVERDUE: 방문 예정 시간이 초과됨
    -- VISIT_OVERDUE_EXIT: 시간 초과 방문 차량이 출차함
    notification_type VARCHAR(30) NOT NULL CHECK (
        notification_type IN (
            'ADMIN_APPROVED',
            'ADMIN_REJECTED',
            'APPROVAL_TIMEOUT',
            'NO_ENTRY_EXPIRED',
            'VISIT_OVERDUE',
            'VISIT_OVERDUE_EXIT'
        )
    ),

    -- 입주민에게 표시할 알림 본문
    message VARCHAR(500) NOT NULL,
    -- 초과 알림인 경우 초과된 시간(분). 해당하지 않으면 NULL이다.
    overdue_minutes INT CHECK (overdue_minutes >= 0),
    -- 알림 생성 시각
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 입주민이 알림을 읽은 시각. 읽지 않았으면 NULL이다.
    read_at TIMESTAMP
);

-- =====================================================
-- NOTICE
-- car_log가 삭제되어도 알림과 당시 표시값을 유지한다.
-- 관리자가 확인하고 해결 처리해야 하는 주차 이상 상황을 저장한다.
-- =====================================================
CREATE TABLE notice (
    -- 관리자 알림 내부 고유번호
    notice_no SERIAL PRIMARY KEY,
    -- 감지된 이상 상황의 종류
    notice_type VARCHAR(30) NOT NULL,
    -- 장기 주차·방문시간 초과와 관련된 입출차 기록
    car_log_no INT,
    -- 무입차 출차·OCR 검토와 관련된 촬영 기록
    camera_data_no INT,
    -- 이상 상황 감지 시각
    detect_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 방문·미등록 차량이 출차해야 하는 기준 시각
    due_at TIMESTAMP,
    -- Unresolved: 미처리, Resolved: 관리자 처리 완료
    alert_stat VARCHAR(20) NOT NULL DEFAULT 'Unresolved'
        CHECK (alert_stat IN ('Unresolved', 'Resolved')),
    -- 알림을 처리한 관리자 번호
    handled_by_member_no INT,
    -- 처리 완료 시각. 미처리 상태에서는 NULL이다.
    handled_at TIMESTAMP,

    -- 아래 snapshot_* 컬럼은 연결된 원본이 삭제된 후에도
    -- 알림 발생 당시 화면 표시값과 증거를 보존하기 위한 복사본이다.
    snapshot_car_log_no INT,
    snapshot_camera_data_no INT,
    snapshot_registered_car_no VARCHAR(50),
    snapshot_captured_car_no VARCHAR(50),
    snapshot_car_kind VARCHAR(20),
    snapshot_parking_name VARCHAR(100),
    snapshot_in_time TIMESTAMP,
    snapshot_image_path TEXT,
    snapshot_confidence_score NUMERIC(5, 2),

    CONSTRAINT chk_notice_type CHECK (
        -- EXIT_WITHOUT_ENTRY: 대응하는 입차 기록 없이 출차 감지
        -- VISIT_OVERDUE: 방문 차량의 허용시간 초과
        -- UNKNOWN_OVERSTAY: 미등록 차량의 장기 주차
        -- OCR_REVIEW: 차량번호 판독 결과의 관리자 확인 필요
        notice_type IN (
            'EXIT_WITHOUT_ENTRY',
            'VISIT_OVERDUE',
            'UNKNOWN_OVERSTAY',
            'OCR_REVIEW'
        )
    ),

    CONSTRAINT chk_notice_car_kind CHECK (
        -- 스냅숏 차량 종류는 등록·방문·미등록 중 하나만 허용한다.
        snapshot_car_kind IS NULL
        OR snapshot_car_kind IN ('REGISTERED', 'VISIT', 'UNKNOWN')
    ),

    CONSTRAINT chk_notice_handled CHECK (
        -- 처리 상태와 처리 시각이 서로 모순되지 않도록 한다.
        (alert_stat = 'Unresolved' AND handled_at IS NULL)
        OR
        (alert_stat = 'Resolved' AND handled_at IS NOT NULL)
    ),

    CONSTRAINT chk_notice_source CHECK (
        -- 시간 초과 알림은 기준 시각과 입출차 기록이 필요하다.
        -- 무입차 출차·OCR 알림은 촬영 기록이 필요하고 기준 시각은 사용하지 않는다.
        (
            notice_type IN ('VISIT_OVERDUE', 'UNKNOWN_OVERSTAY')
            AND due_at IS NOT NULL
            AND (car_log_no IS NOT NULL OR snapshot_car_log_no IS NOT NULL)
        )
        OR
        (
            notice_type IN ('EXIT_WITHOUT_ENTRY', 'OCR_REVIEW')
            AND due_at IS NULL
            AND (
                camera_data_no IS NOT NULL
                OR snapshot_camera_data_no IS NOT NULL
            )
        )
    ),

    FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no) ON DELETE SET NULL,

    FOREIGN KEY (camera_data_no)
        REFERENCES camera_data(camera_data_no) ON DELETE SET NULL,

    FOREIGN KEY (handled_by_member_no)
        REFERENCES member(member_no) ON DELETE SET NULL
);

-- 같은 입출차 기록에 같은 종류의 관리자 알림이 중복 생성되는 것을 막는다.
CREATE UNIQUE INDEX uq_notice_type_car_log
    ON notice(notice_type, snapshot_car_log_no)
    WHERE snapshot_car_log_no IS NOT NULL;

-- 같은 촬영 기록에 같은 종류의 관리자 알림이 중복 생성되는 것을 막는다.
CREATE UNIQUE INDEX uq_notice_type_camera
    ON notice(notice_type, snapshot_camera_data_no)
    WHERE snapshot_camera_data_no IS NOT NULL;

-- 처리 상태별로 최신 알림을 빠르게 조회한다.
CREATE INDEX ix_notice_status_detect
    ON notice(alert_stat, detect_at DESC);

-- 특정 관리자가 처리한 알림을 빠르게 조회한다.
CREATE INDEX ix_notice_handler
    ON notice(handled_by_member_no);

-- =====================================================
-- BOARD
-- 전체 사용자에게 게시하는 일반 공지사항을 저장한다.
-- 개인 차량 알림(vehicle_nt)이나 관리자 이상 알림(notice)과 구분한다.
-- =====================================================
CREATE TABLE board (
    -- 공지사항 내부 고유번호
    board_no SERIAL PRIMARY KEY,
    -- 공지 제목
    title VARCHAR(150) NOT NULL,
    -- 공지 본문
    content TEXT NOT NULL,
    -- 첨부 이미지의 저장 경로
    image_path VARCHAR(500),
    -- 사용자가 업로드한 이미지 원본 파일명
    image_name VARCHAR(255),
    -- 이미지 MIME 타입
    image_type VARCHAR(100),
    -- 공지 노출 시작 시각
    start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 공지 노출 종료 시각. 종료 기한이 없으면 NULL이다.
    end_at TIMESTAMP,
    -- 공지를 즉시 숨기거나 다시 활성화할 때 사용하는 값
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- 공지를 작성한 관리자 로그인 아이디
    created_by VARCHAR(30) NOT NULL,
    -- 최초 작성 시각
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 마지막 수정 시각
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 종료 시각은 시작 시각보다 빠를 수 없다.
    CONSTRAINT chk_board_period
        CHECK (end_at IS NULL OR end_at >= start_at)
);

-- 게시기간 조회와 최신 등록순 조회에 사용하는 인덱스를 생성.
CREATE INDEX idx_board_active_period
ON board (active, start_at, end_at);

CREATE INDEX idx_board_created_at
ON board (created_at DESC, board_no DESC);


-- =====================================================
-- NOTICE DETAIL VIEW
-- 관리자 알림 목록 화면에 필요한 원본·스냅숏 정보를 한 번에 조회한다.
-- 원본 데이터가 남아 있으면 최신 원본을 우선하고, 삭제됐으면 snapshot 값을 사용한다.
-- =====================================================
CREATE VIEW notice_detail AS
SELECT
    -- 알림 기본 식별 정보
    n.notice_no,
    n.notice_type,
    n.car_log_no,
    n.camera_data_no,

    -- 등록 차량번호: 입출차 차량 → 촬영 연결 차량 → 저장된 스냅숏 순으로 선택한다.
    COALESCE(
        log_vc.car_no,
        event_vc.car_no,
        n.snapshot_registered_car_no
    ) AS registered_car_no,

    -- 촬영 차량번호: 이벤트·입차 OCR 결과와 저장된 차량번호를 순서대로 보완한다.
    COALESCE(
        event_cd.ocr_car_no,
        event_cd.car_no,
        entry_cd.ocr_car_no,
        entry_cd.car_no,
        cl.snapshot_car_no,
        n.snapshot_captured_car_no
    ) AS captured_car_no,

    -- 원본 입출차·촬영 정보를 이용해 차량 종류를 판정하고 없으면 스냅숏을 사용한다.
    COALESCE(
        cl.snapshot_car_kind,
        CASE
            WHEN cl.car_log_no IS NULL THEN NULL
            WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN'
            WHEN log_vc.vehicle_type = 'visit' THEN 'VISIT'
            ELSE 'REGISTERED'
        END,
        CASE
            WHEN event_cd.camera_data_no IS NULL THEN NULL
            WHEN event_cd.vehicle_car_no IS NULL THEN 'UNKNOWN'
            WHEN event_vc.vehicle_type = 'visit' THEN 'VISIT'
            ELSE 'REGISTERED'
        END,
        n.snapshot_car_kind
    ) AS car_kind,

    n.detect_at,
    n.due_at,

    -- due_at부터 출차 시각 또는 현재 시각까지의 초과 시간을 분 단위로 계산한다.
    CASE
        WHEN n.due_at IS NULL THEN NULL
        ELSE GREATEST(
            0,
            FLOOR(
                EXTRACT(
                    EPOCH FROM (
                        COALESCE(cl.out_time, CURRENT_TIMESTAMP)
                        - n.due_at
                    )
                ) / 60
            )::INT
        )
    END AS overdue_minutes,

    -- 감지 시점부터 출차·처리·현재 시각까지 경과한 일수를 계산한다.
    GREATEST(
        0,
        FLOOR(
            EXTRACT(
                EPOCH FROM (
                    COALESCE(
                        cl.out_time,
                        n.handled_at,
                        CURRENT_TIMESTAMP
                    ) - n.detect_at
                )
            ) / 86400
        )::INTEGER
    ) AS stay_days,

    n.alert_stat,
    n.handled_by_member_no,
    handler.mem_name AS handled_by_member_name,
    n.handled_at,
    COALESCE(cl.in_time, n.snapshot_in_time) AS in_time,
    n.due_at AS expected_out_time,
    cl.out_time,

    -- 입차 게이트 또는 이벤트 카메라를 통해 주차장을 찾고 없으면 스냅숏을 사용한다.
    COALESCE(
        log_parking.parking_name,
        event_parking.parking_name,
        n.snapshot_parking_name
    ) AS parking_name,

    -- 관리자 확인용 촬영 이미지 경로
    COALESCE(
        event_cd.image_path,
        entry_cd.image_path,
        n.snapshot_image_path
    ) AS image_path,

    -- 관리자 확인용 OCR 신뢰도
    COALESCE(
        event_cd.confidence_score,
        entry_cd.confidence_score,
        n.snapshot_confidence_score
    ) AS confidence_score,

    -- 원본 삭제 여부와 관계없이 감사·이력 확인에 사용할 당시 스냅숏 값
    n.snapshot_car_log_no,
    n.snapshot_camera_data_no,
    n.snapshot_registered_car_no,
    n.snapshot_captured_car_no,
    n.snapshot_car_kind,
    n.snapshot_parking_name,
    n.snapshot_in_time,
    n.snapshot_image_path,
    n.snapshot_confidence_score

FROM notice n

-- 각 LEFT JOIN은 관련 원본이 삭제돼도 notice 행 자체가 조회되도록 한다.
LEFT JOIN car_log cl
    ON n.car_log_no = cl.car_log_no

LEFT JOIN vehicle_car log_vc
    ON cl.vehicle_car_no = log_vc.vehicle_car_no

LEFT JOIN camera_data entry_cd
    ON cl.camera_data_no = entry_cd.camera_data_no

LEFT JOIN gate log_gate
    ON cl.in_gate_no = log_gate.gate_no

LEFT JOIN parking log_parking
    ON log_gate.parking_no = log_parking.parking_no

LEFT JOIN camera_data event_cd
    ON n.camera_data_no = event_cd.camera_data_no

LEFT JOIN vehicle_car event_vc
    ON event_cd.vehicle_car_no = event_vc.vehicle_car_no

LEFT JOIN camera event_camera
    ON event_cd.camera_no = event_camera.camera_no

LEFT JOIN gate event_gate
    ON event_camera.gate_no = event_gate.gate_no

LEFT JOIN parking event_parking
    ON event_gate.parking_no = event_parking.parking_no

LEFT JOIN member handler
    ON n.handled_by_member_no = handler.member_no;

-- =====================================================
-- NOTICE OVERSTAY VIEW
-- 방문차량 등록시간 초과 및 미등록차량 24시간 초과 대상 계산용
-- 아직 출차하지 않은 차량 중 notice를 생성할 후보와 기준 시각을 제공한다.
-- =====================================================
CREATE VIEW notice_overstay AS
SELECT
    -- 방문 차량은 VISIT_OVERDUE, 그 외 미등록 차량은 UNKNOWN_OVERSTAY로 분류한다.
    CASE
        WHEN vc.vehicle_type = 'visit'
            THEN 'VISIT_OVERDUE'
        ELSE 'UNKNOWN_OVERSTAY'
    END AS notice_type,

    -- 알림 생성 시 원본 연결과 스냅숏 저장에 사용할 값
    cl.car_log_no,
    cl.camera_data_no AS snapshot_camera_data_no,
    vc.car_no AS snapshot_registered_car_no,

    -- OCR 번호, 확정 번호, 입출차 스냅숏 중 존재하는 차량번호를 선택한다.
    COALESCE(
        cd.ocr_car_no,
        cd.car_no,
        cl.snapshot_car_no
    ) AS snapshot_captured_car_no,

    -- 방문 차량과 미등록 차량을 화면용 차량 종류로 구분한다.
    CASE
        WHEN vc.vehicle_type = 'visit'
            THEN 'VISIT'
        ELSE 'UNKNOWN'
    END AS snapshot_car_kind,

    p.parking_name AS snapshot_parking_name,
    cl.in_time AS snapshot_in_time,
    cd.image_path AS snapshot_image_path,
    cd.confidence_score AS snapshot_confidence_score,

    -- 방문 차량은 등록된 방문 허용시간에 30분을 더하고,
    -- 미등록 차량은 입차 후 24시간을 출차 기준 시각으로 정한다.
    CASE
        WHEN vc.vehicle_type = 'visit'
            THEN cl.in_time
                 + (vc.end_date - vc.start_date)
                 + INTERVAL '30 minutes'
        ELSE cl.in_time + INTERVAL '24 hours'
    END AS due_at

FROM car_log cl

LEFT JOIN vehicle_car vc
    ON cl.vehicle_car_no = vc.vehicle_car_no

LEFT JOIN camera_data cd
    ON cl.camera_data_no = cd.camera_data_no

LEFT JOIN gate g
    ON cl.in_gate_no = g.gate_no

LEFT JOIN parking p
    ON g.parking_no = p.parking_no

-- 아직 출차하지 않았고 입차 시각을 확인할 수 있는 차량만 대상으로 한다.
WHERE cl.out_time IS NULL
  AND cl.in_time IS NOT NULL
  AND (
      (
          vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'APPROVED'
          AND vc.start_date IS NOT NULL
          AND vc.end_date IS NOT NULL
      )
      OR
      (
          COALESCE(
              cl.snapshot_car_kind,
              CASE
                  WHEN cl.vehicle_car_no IS NULL
                      THEN 'UNKNOWN'
                  ELSE NULL
              END
          ) = 'UNKNOWN'
      )
  );

-- =====================================================
-- BILLING
-- 주차 요금 계산 규칙과 차량별 청구·결제 결과를 관리한다.
-- =====================================================

CREATE TABLE fee_rule (
    fee_rule_no SERIAL PRIMARY KEY,             -- 요금 규칙 번호
    rule_name VARCHAR(100) NOT NULL UNIQUE,     -- 요금 규칙명

    unit_minutes INT NOT NULL                   -- 요금 계산 단위(분)
        CHECK (unit_minutes > 0),

    unit_fee NUMERIC(12, 0) NOT NULL            -- 한 계산 단위당 요금
        CHECK (unit_fee >= 0),

    daily_max_fee NUMERIC(12, 0)                -- 하루 최대요금. 제한이 없으면 NULL
        CHECK (
            daily_max_fee IS NULL
            OR daily_max_fee > 0
        ),

    active BOOLEAN NOT NULL DEFAULT TRUE        -- 사용 여부
);


-- =====================================================
-- BILL
-- 청구 명세와 카드 결제 결과를 한 행에서 함께 관리한다.
-- 결제 전에는 결제 관련 컬럼이 NULL이고, 결제 후 해당 값을 채운다.
-- =====================================================
CREATE TABLE bill (
    bill_no SERIAL PRIMARY KEY,              -- 명세서 번호

    car_log_no INT,                             -- 요금을 계산한 입출차 기록 번호
    fee_rule_no INT NOT NULL,                   -- 적용 요금 규칙

    snapshot_car_no VARCHAR(50) NOT NULL,       -- 원본 삭제 후에도 보존할 결제 당시 차량번호

    snapshot_car_kind VARCHAR(20) NOT NULL      -- 차량 종류
        CHECK (snapshot_car_kind IN (
            'VISIT',                            -- 방문차량
            'UNKNOWN'                           -- 미등록차량
        )),

    charge_minutes INT NOT NULL                 -- 실제 과금시간
        CHECK (charge_minutes >= 0),

    bill_amount NUMERIC(12, 0) NOT NULL         -- 결제 금액
        CHECK (bill_amount >= 0),

    bill_status VARCHAR(20) NOT NULL
        DEFAULT 'UNPAID'                        -- 명세서 상태
        CHECK (bill_status IN (
            'UNPAID',                           -- 결제 전
            'PAID',                             -- 결제 완료
            'CANCELLED'                         -- 명세서 취소
        )),

    issued_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,              -- 생성 시각

    -- 결제가 이루어진 키오스크 번호. 미결제 상태이거나 장비 삭제 후에는 NULL이다.
    kiosk_no INT,

    -- 키오스크가 삭제돼도 결제 당시 장비명을 보존하기 위한 값
    snapshot_kiosk_name VARCHAR(100),

    -- 카드사·결제 대행사가 발급한 거래 식별번호
    transaction_id VARCHAR(100) UNIQUE,

    -- 실제 결제된 금액. 미결제 상태에서는 NULL이다.
    paid_amount NUMERIC(12, 0)
        CHECK (
            paid_amount IS NULL
            OR paid_amount > 0
        ),

    -- 결제 완료 시각
    paid_at TIMESTAMP,

    -- 청구 또는 결제가 취소된 시각
    cancelled_at TIMESTAMP,

    -- 하나의 입출차 기록에는 명세서를 한 건만 생성한다.
    CONSTRAINT uq_bill_car_log
        UNIQUE (car_log_no),

    -- 입출차 기록이 삭제되어도 명세서는 유지한다.
    CONSTRAINT fk_bill_car_log
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE SET NULL,

    -- 사용된 요금 규칙은 삭제할 수 없다.
    CONSTRAINT fk_bill_fee_rule
        FOREIGN KEY (fee_rule_no)
        REFERENCES fee_rule(fee_rule_no)
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_bill_kiosk
        -- 키오스크를 교체·삭제해도 결제 이력은 유지하고 연결만 해제한다.
        FOREIGN KEY (kiosk_no)
        REFERENCES kiosk(kiosk_no)
        ON DELETE SET NULL
);

-- =====================================================
-- DATA ARCHIVE
-- 사용자가 삭제한 운영 데이터를 일정 기간 보관한 뒤 영구 삭제하기 위한 영역이다.
-- =====================================================

CREATE TABLE trash_bin (
    trash_no SERIAL PRIMARY KEY,                 -- 휴지통 데이터 고유 번호
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
    -- 데이터 종류별 삭제 이력을 최신순으로 조회할 때 사용한다.
    ON trash_bin(data_type, deleted_at DESC);

CREATE INDEX idx_trash_purge_at
    -- 영구 삭제 예정 시각이 지난 데이터를 스케줄러가 찾을 때 사용한다.
    ON trash_bin(purge_at);

-- 모든 DROP·CREATE 작업이 성공한 경우에만 변경 내용을 확정한다.
COMMIT;
