BEGIN;

-- 기존 뷰 삭제
DROP VIEW IF EXISTS notice_overstay;
DROP VIEW IF EXISTS notice_detail;
DROP VIEW IF EXISTS robot_task_detail;
DROP VIEW IF EXISTS car_log_detail;


-- =====================================================
-- CAR LOG DETAIL VIEW
-- B1·B2 입출차 기록 조회용
-- =====================================================
CREATE VIEW car_log_detail AS
SELECT
    cl.car_log_no,
    cl.vehicle_car_no,
    vc.vehicle_type,
    vc.vehicle_status,
    cl.camera_data_no,
    cl.out_camera_data_no,
    cl.in_gate_no,
    in_gate.gate_name AS in_gate_name,
    cl.in_time,
    cl.out_gate_no,
    out_gate.gate_name AS out_gate_name,
    cl.out_time,
    cl.free_time,
    cl.snapshot_car_no,
    cl.snapshot_car_kind,
    in_gate.parking_no,
    parking.parking_code,
    parking.parking_name,
    COALESCE(
        vc.car_no,
        entry_data.ocr_car_no,
        entry_data.car_no,
        cl.snapshot_car_no
    ) AS car_no,
    CASE
        WHEN cl.out_time IS NULL THEN 'PARKING'
        ELSE 'OUT'
    END AS parking_state
FROM car_log cl
LEFT JOIN vehicle_car vc
    ON cl.vehicle_car_no = vc.vehicle_car_no
LEFT JOIN camera_data entry_data
    ON cl.camera_data_no = entry_data.camera_data_no
LEFT JOIN gate in_gate
    ON cl.in_gate_no = in_gate.gate_no
LEFT JOIN gate out_gate
    ON cl.out_gate_no = out_gate.gate_no
LEFT JOIN parking
    ON in_gate.parking_no = parking.parking_no;

-- =====================================================
-- ROBOT TASK DETAIL VIEW
-- 로봇 작업과 차량·이동 공간을 한 번에 조회한다.
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