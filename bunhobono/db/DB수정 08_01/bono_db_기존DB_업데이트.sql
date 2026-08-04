-- =====================================================
-- 기존 bono_db_통합.sql 설치 DB 업데이트용
--
-- 유지: 회원, 차량, 카메라, 입출차, 입주민 알림, 휴지통 데이터
-- 초기화: 기존 관리자 알림(notice) 데이터
-- 추가: 현재 notice 구조, board, 알림 인덱스, 관리자 알림 View 2개
-- =====================================================
BEGIN;

-- 기존 View는 notice 교체 전에 먼저 제거한다.
DROP VIEW IF EXISTS v_notice_detail;
DROP VIEW IF EXISTS notice_detail;
DROP VIEW IF EXISTS notice_overstay;

-- 예전 notice 데이터는 새 알림 유형과 출처 컬럼이 없어 정확한 변환이 불가능하다.
-- 개발 DB의 기존 관리자 알림만 초기화하고 현재 구조로 다시 만든다.
DROP TABLE IF EXISTS notice;

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

-- 원본 합본에 없던 현재 공지사항 테이블이다.
CREATE TABLE IF NOT EXISTS board (
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

CREATE INDEX IF NOT EXISTS idx_board_active_period
    ON board(active, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_board_created_at
    ON board(created_at DESC, board_no DESC);

-- =====================================================
-- 관리자 알림 목록 및 상세 조회 View
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
-- 방문차량·미등록차량 초과 알림 대상 계산 View
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

-- 생성 객체 권한 동기화
GRANT USAGE ON SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bono_user;

