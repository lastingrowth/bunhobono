-- =====================================================
-- BONO DB 전체 신규 설치용
-- 주의: 기존 View와 테이블을 모두 삭제하고 빈 구조로 다시 생성한다.
-- 기존 데이터가 필요한 DB에는 실행하지 말고 noticeViews.sql만 실행한다.
-- 포함 항목: 테이블 12개, 인덱스 8개, View 2개, bono_user 권한
-- =====================================================
BEGIN;

-- =====================================================
-- DROP VIEWS / TABLES
-- =====================================================
DROP VIEW IF EXISTS v_notice_detail;
DROP VIEW IF EXISTS notice_detail;
DROP VIEW IF EXISTS notice_overstay;

DROP TABLE IF EXISTS trash_bin;
DROP TABLE IF EXISTS vehicle_nt;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS board;
DROP TABLE IF EXISTS car_log;
DROP TABLE IF EXISTS camera_data;
DROP TABLE IF EXISTS camera;
DROP TABLE IF EXISTS vehicle_car;
DROP TABLE IF EXISTS gate;
DROP TABLE IF EXISTS parking;
DROP TABLE IF EXISTS member_archive;
DROP TABLE IF EXISTS member;

-- =====================================================
-- APARTMENT UNIT
-- 실제 아파트 세대만 저장하며 관리실 0동 0호는 만들지 않는다.
-- =====================================================
CREATE TABLE apartment_unit (
    unit_no BIGSERIAL PRIMARY KEY,
    dong INT NOT NULL,
    ho INT NOT NULL,
    unit_status VARCHAR(20) NOT NULL DEFAULT 'EMPTY'
        CHECK (unit_status IN ('EMPTY', 'OCCUPIED')),

    CONSTRAINT uq_apartment_unit_address UNIQUE (dong, ho),
    CONSTRAINT chk_apartment_unit_dong CHECK (dong > 0),
    CONSTRAINT chk_apartment_unit_ho CHECK (ho > 0)
);

-- =====================================================
-- MEMBER
-- 입주민만 실제 세대를 참조하며 관리자는 unit_no가 NULL이다.
-- =====================================================
CREATE TABLE member (
    member_no BIGSERIAL PRIMARY KEY,
    login_id VARCHAR(30) NOT NULL,
    login_pwd VARCHAR(100) NOT NULL,
    unit_no BIGINT,
    mem_name VARCHAR(30) NOT NULL,
    mem_phone VARCHAR(30),

    role VARCHAR(30) NOT NULL
        CHECK (role IN ('ADMIN', 'RESIDENT')),

    -- PENDING: 입주민 가입 승인 대기
    -- ACTIVE: 입주민 거주 중 또는 관리자 근무 중
    -- WITHDRAW_PENDING: 입주민 전출 승인 대기
    -- WITHDRAWN: 입주민 전출 확정 또는 탈퇴 완료
    -- REJECTED: 입주민 가입 거절
    -- INACTIVE: 관리자 퇴사
    -- ON_LEAVE: 관리자 휴직
    mem_status VARCHAR(30) NOT NULL
        CHECK (
            mem_status IN (
                'PENDING',
                'ACTIVE',
                'WITHDRAW_PENDING',
                'WITHDRAWN',
                'REJECTED',
                'INACTIVE',
                'ON_LEAVE'
            )
        ),

    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_at TIMESTAMP,

    CONSTRAINT uq_member_login_id UNIQUE (login_id),
    CONSTRAINT fk_member_apartment_unit
        FOREIGN KEY (unit_no)
        REFERENCES apartment_unit(unit_no),
    CONSTRAINT chk_member_unit_by_role
        CHECK (
            (role = 'RESIDENT' AND unit_no IS NOT NULL)
            OR
            (role = 'ADMIN' AND unit_no IS NULL)
        ),
    CONSTRAINT chk_member_status_by_role
        CHECK (
            (
                role = 'RESIDENT'
                AND mem_status IN (
                    'PENDING',
                    'ACTIVE',
                    'WITHDRAW_PENDING',
                    'WITHDRAWN',
                    'REJECTED'
                )
            )
            OR
            (
                role = 'ADMIN'
                AND mem_status IN ('ACTIVE', 'INACTIVE', 'ON_LEAVE')
            )
        )
);

-- 승인 대기, 거주 중, 전출 승인 대기 회원은 해당 세대를 점유한다.
CREATE UNIQUE INDEX uq_current_resident_unit
    ON member (unit_no)
    WHERE role = 'RESIDENT'
      AND mem_status IN ('PENDING', 'ACTIVE', 'WITHDRAW_PENDING');

-- =====================================================
-- MEMBER ARCHIVE
-- =====================================================
CREATE TABLE member_archive (
    archive_no BIGSERIAL PRIMARY KEY,
    original_member_no BIGINT NOT NULL,

    login_id VARCHAR(50),
    mem_name VARCHAR(50),
    mem_phone VARCHAR(30),
    role VARCHAR(30),
    mem_status VARCHAR(30),

    -- 전출 당시 주소 스냅숏
    mem_dong INT,
    mem_ho INT,

    create_at TIMESTAMP,
    delete_at TIMESTAMP,
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
        CHECK (gate_type IN ('In', 'Out')),
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

    -- normal: 등록 유효 시작시간
    -- visit: 예상 방문시간
    start_date TIMESTAMP,

    -- normal: 등록 유효 종료시간
    -- visit: 예상 방문시간 + 등록시간
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
        CHECK (camera_type IN ('In', 'Out')),
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
    ocr_car_no VARCHAR(50),
    capture_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    image_path TEXT,
    crop_image_path TEXT,
    recognition_state BOOLEAN,
    confidence_score NUMERIC(5,2),
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
    -- [지난 기록 통계] 입차 당시 차량 종류를 보존한다.
    snapshot_car_kind VARCHAR(20) NOT NULL
        CHECK (snapshot_car_kind IN ('REGISTERED', 'VISIT', 'UNKNOWN')),

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
-- VEHICLE NOTIFICATION
-- 입주민 차량 알림 및 읽음 상태를 보관한다.
-- =====================================================
CREATE TABLE vehicle_nt (
    vehicle_nt_no SERIAL PRIMARY KEY,

    recipient_member_no INT NOT NULL
        REFERENCES member(member_no) ON DELETE CASCADE,

    sender_member_no INT
        REFERENCES member(member_no) ON DELETE SET NULL,

    vehicle_car_no INT
        REFERENCES vehicle_car(vehicle_car_no) ON DELETE SET NULL,

    car_log_no INT
        REFERENCES car_log(car_log_no) ON DELETE SET NULL,

    snapshot_car_no VARCHAR(20) NOT NULL,

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

    message VARCHAR(500) NOT NULL,
    overdue_minutes INT CHECK (overdue_minutes >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

-- =====================================================
-- NOTICE
-- 관리자 관제 알림과 원본 삭제 이후 표시용 스냅샷을 보관한다.
-- =====================================================
CREATE TABLE notice (
    notice_no SERIAL PRIMARY KEY,
    notice_type VARCHAR(30) NOT NULL,
    car_log_no INT,
    camera_data_no INT,
    detect_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at TIMESTAMP,
    alert_stat VARCHAR(20) NOT NULL DEFAULT 'Unresolved'
        CHECK (alert_stat IN ('Unresolved', 'Resolved')),
    handled_by_member_no INT,
    handled_at TIMESTAMP,

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
        notice_type IN (
            'EXIT_WITHOUT_ENTRY',
            'VISIT_OVERDUE',
            'UNKNOWN_OVERSTAY',
            'OCR_REVIEW'
        )
    ),

    CONSTRAINT chk_notice_car_kind CHECK (
        snapshot_car_kind IS NULL
        OR snapshot_car_kind IN ('REGISTERED', 'VISIT', 'UNKNOWN')
    ),

    CONSTRAINT chk_notice_handled CHECK (
        (alert_stat = 'Unresolved' AND handled_at IS NULL)
        OR
        (alert_stat = 'Resolved' AND handled_at IS NOT NULL)
    ),

    CONSTRAINT chk_notice_source CHECK (
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

CREATE UNIQUE INDEX uq_notice_type_car_log
    ON notice(notice_type, snapshot_car_log_no)
    WHERE snapshot_car_log_no IS NOT NULL;

CREATE UNIQUE INDEX uq_notice_type_camera
    ON notice(notice_type, snapshot_camera_data_no)
    WHERE snapshot_camera_data_no IS NOT NULL;

CREATE INDEX ix_notice_status_detect
    ON notice(alert_stat, detect_at DESC);

CREATE INDEX ix_notice_handler
    ON notice(handled_by_member_no);

-- =====================================================
-- BOARD
-- =====================================================
CREATE TABLE board (
    board_no SERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    image_path VARCHAR(500),
    image_name VARCHAR(255),
    image_type VARCHAR(100),
    start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_board_period CHECK (
        end_at IS NULL OR end_at >= start_at
    )
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

-- =====================================================
-- NOTICE DETAIL VIEW
-- 관리자 알림 목록 및 상세 조회용
-- =====================================================
CREATE VIEW notice_detail AS
SELECT
    n.notice_no,
    n.notice_type,
    n.car_log_no,
    n.camera_data_no,

    COALESCE(
        log_vc.car_no,
        event_vc.car_no,
        n.snapshot_registered_car_no
    ) AS registered_car_no,

    COALESCE(
        event_cd.ocr_car_no,
        event_cd.car_no,
        entry_cd.ocr_car_no,
        entry_cd.car_no,
        cl.snapshot_car_no,
        n.snapshot_captured_car_no
    ) AS captured_car_no,

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
            )::BIGINT
        )
    END AS overdue_minutes,

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

    COALESCE(
        log_parking.parking_name,
        event_parking.parking_name,
        n.snapshot_parking_name
    ) AS parking_name,

    COALESCE(
        event_cd.image_path,
        entry_cd.image_path,
        n.snapshot_image_path
    ) AS image_path,

    COALESCE(
        event_cd.confidence_score,
        entry_cd.confidence_score,
        n.snapshot_confidence_score
    ) AS confidence_score,

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
-- =====================================================
CREATE VIEW notice_overstay AS
SELECT
    CASE
        WHEN vc.vehicle_type = 'visit'
            THEN 'VISIT_OVERDUE'
        ELSE 'UNKNOWN_OVERSTAY'
    END AS notice_type,

    cl.car_log_no,
    cl.camera_data_no AS snapshot_camera_data_no,
    vc.car_no AS snapshot_registered_car_no,

    COALESCE(
        cd.ocr_car_no,
        cd.car_no,
        cl.snapshot_car_no
    ) AS snapshot_captured_car_no,

    CASE
        WHEN vc.vehicle_type = 'visit'
            THEN 'VISIT'
        ELSE 'UNKNOWN'
    END AS snapshot_car_kind,

    p.parking_name AS snapshot_parking_name,
    cl.in_time AS snapshot_in_time,
    cd.image_path AS snapshot_image_path,
    cd.confidence_score AS snapshot_confidence_score,

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
