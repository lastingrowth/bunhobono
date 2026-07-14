BEGIN;

TRUNCATE TABLE
    trash_bin,
    notice,
    car_log,
    camera_data,
    camera,
    vehicle_car,
    gate,
    parking,
    member
RESTART IDENTITY CASCADE;

-- =====================================================
-- MEMBER
-- =====================================================
INSERT INTO member
    (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone,
     role, create_at, delete_at, mem_status)
VALUES
    ('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6',
     0, 0, '관리자1', '010-1111-1111', 'ADMIN', NOW(), NULL, '근무'),
    ('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6',
     0, 0, '관리자2', '010-1111-2222', 'ADMIN', NOW(), NULL, '근무'),
    ('res1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6',
     101, 1001, '입주민1', '010-2222-1001', 'RESIDENT', NOW(), NULL, '거주'),
    ('res2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6',
     101, 1002, '입주민2', '010-2222-1002', 'RESIDENT', NOW(), NULL, '거주'),
    ('res3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6',
     102, 1101, '입주민3', '010-2222-1101', 'RESIDENT', NOW(), NULL, '거주'),
    ('res4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6',
     102, 1102, '입주민4', '010-2222-1102', 'PENDING', NOW(), NULL, '거주');

-- =====================================================
-- PARKING
-- =====================================================
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
    ('A 주차장', 100, 'A동 지상'),
    ('B 주차장', 80, 'B동 지상'),
    ('C 주차장', 100, 'C동 지상'),
    ('D 주차장', 50, '지하 주차장');

-- =====================================================
-- GATE
-- =====================================================
INSERT INTO gate (parking_no, gate_name, gate_type)
VALUES
    (1, 'A-IN', 'In'),
    (1, 'A-OUT', 'Out'),
    (2, 'B-IN', 'In'),
    (2, 'B-OUT', 'Out'),
    (3, 'C-IN', 'In'),
    (3, 'C-OUT', 'Out'),
    (4, 'D-BOTH', 'Both');

-- =====================================================
-- VEHICLE CAR
-- =====================================================
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date,
     member_no, approved_at)
VALUES
    ('normal', '12가3456', 'APPROVED', '2026-07-01 00:00:00', NULL,
     3, '2026-07-01 09:00:00'),
    ('normal', '34나5678', 'APPROVED', '2026-07-01 00:00:00', NULL,
     4, '2026-07-01 09:05:00'),
    ('visit', '101마0001', 'APPROVED', '2026-07-06 10:00:00', '2026-07-15 23:59:59',
     3, '2026-07-06 10:00:00'),
    ('visit', '202바0002', 'EXPIRED', '2026-06-25 09:00:00', '2026-06-28 23:59:59',
     4, '2026-06-25 09:00:00'),
    ('normal', '56다7890', 'WAITING', '2026-07-14 00:00:00', NULL,
     5, NULL);

-- =====================================================
-- CAMERA
-- =====================================================
INSERT INTO camera (gate_no, camera_name, camera_type, install_date)
VALUES
    (1, 'CAM-A1', 'In', '2025-01-01'),
    (2, 'CAM-A2', 'Out', '2025-01-01'),
    (3, 'CAM-B1', 'In', '2025-01-03'),
    (4, 'CAM-B2', 'Out', '2025-01-03'),
    (5, 'CAM-C1', 'In', '2025-01-05'),
    (6, 'CAM-C2', 'Out', '2025-01-05'),
    (7, 'CAM-D1', 'Both', '2025-01-07');

-- =====================================================
-- CAMERA DATA
-- =====================================================
INSERT INTO camera_data
    (camera_no, vehicle_car_no, car_no, capture_time, image_path,
     recognition_state, confidence_score)
VALUES
    (1, 1, '12가3456', '2026-07-12 08:10:00', '/carPlateImg/a1-in.jpg', TRUE, 99.00),
    (1, 2, '34나5678', '2026-07-12 08:25:00', '/carPlateImg/a2-in.jpg', TRUE, 98.20),
    (3, 3, '101마0001', '2026-07-06 10:05:00', '/carPlateImg/b1-in.jpg', TRUE, 98.70),
    (3, 4, '202바0002', '2026-06-25 09:10:00', '/carPlateImg/b2-alert.jpg', TRUE, 97.60),
    (1, NULL, '미인식', '2026-07-12 11:00:00', '/carPlateImg/a-unknown.jpg', FALSE, 0.00),
    (5, NULL, '77라7777', '2026-07-13 18:30:00', '/carPlateImg/c-unknown.jpg', TRUE, 91.30),
    (2, 1, '12가3456', '2026-07-13 20:00:00', '/carPlateImg/a1-out.jpg', TRUE, 99.10);

-- =====================================================
-- CAR LOG
-- snapshot_car_no 포함
-- =====================================================
INSERT INTO car_log
    (vehicle_car_no, camera_data_no, in_gate_no, in_time,
     out_gate_no, out_time, free_time, snapshot_car_no)
VALUES
    (1, 1, 1, '2026-07-12 08:10:00', 2, '2026-07-13 20:00:00', 180, '12가3456'),
    (2, 2, 1, '2026-07-12 08:25:00', NULL, NULL, 180, '34나5678'),
    (3, 3, 3, '2026-07-06 10:05:00', NULL, NULL, 180, '101마0001'),
    (4, 4, 3, '2026-06-25 09:10:00', NULL, NULL, 180, '202바0002'),
    (NULL, 5, 1, '2026-07-12 11:00:00', NULL, NULL, 0, '미인식'),
    (NULL, 6, 5, '2026-07-13 18:30:00', NULL, NULL, 0, '77라7777');

-- =====================================================
-- NOTICE
-- 모든 스냅샷 값 포함
-- =====================================================
INSERT INTO notice
    (car_log_no, detect_at, stay_days, alert_stat,
     handled_by_member_no, handled_at,
     snapshot_car_log_no, snapshot_registered_car_no,
     snapshot_captured_car_no, snapshot_car_kind,
     snapshot_parking_name, snapshot_in_time)
VALUES
    (4, '2026-07-03 09:10:00', 8, 'Unresolved', NULL, NULL,
     4, '202바0002', '202바0002', 'VISIT', 'B 주차장', '2026-06-25 09:10:00'),
    (3, '2026-07-09 10:05:00', 3, 'Checked', 2, '2026-07-09 10:20:00',
     3, '101마0001', '101마0001', 'VISIT', 'B 주차장', '2026-07-06 10:05:00'),
    (5, '2026-07-13 11:00:00', 1, 'Unresolved', NULL, NULL,
     5, NULL, '미인식', 'UNKNOWN', 'A 주차장', '2026-07-12 11:00:00'),
    (6, '2026-07-14 18:30:00', 1, 'Resolved', 1, '2026-07-14 19:00:00',
     6, NULL, '77라7777', 'UNKNOWN', 'C 주차장', '2026-07-13 18:30:00');

-- =====================================================
-- TRASH BIN
-- 실제 원본 테이블에는 존재하지 않는 번호를 사용한 독립 테스트 데이터
-- =====================================================
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
VALUES
    (
        'CAMERA_DATA',
        9001,
        jsonb_build_object(
            'camera_data_no', 9001,
            'camera_no', 1,
            'vehicle_car_no', NULL,
            'car_no', '99테9001',
            'capture_time', '2026-07-10T08:30:00',
            'image_path', '/carPlateImg/trash-camera.jpg',
            'recognition_state', TRUE,
            'confidence_score', 95.50
        ),
        'MANUAL',
        '2026-07-14 09:00:00',
        '2026-08-13 09:00:00'
    ),
    (
        'CAR_LOG',
        9001,
        jsonb_build_object(
            'car_log_no', 9001,
            'vehicle_car_no', NULL,
            'camera_data_no', NULL,
            'in_gate_no', 1,
            'in_time', '2026-07-09T10:00:00',
            'out_gate_no', 2,
            'out_time', '2026-07-09T15:00:00',
            'free_time', 0,
            'snapshot_car_no', '88테9001'
        ),
        'SCHEDULED',
        '2026-07-14 09:10:00',
        '2026-08-13 09:10:00'
    ),
    (
        'NOTICE',
        9001,
        jsonb_build_object(
            'notice_no', 9001,
            'car_log_no', NULL,
            'detect_at', '2026-07-10T11:00:00',
            'stay_days', 2,
            'alert_stat', 'Resolved',
            'handled_by_member_no', 1,
            'handled_at', '2026-07-14T08:00:00',
            'snapshot_car_log_no', 9001,
            'snapshot_registered_car_no', '77테9001',
            'snapshot_captured_car_no', '77테9001',
            'snapshot_car_kind', 'REGISTERED',
            'snapshot_parking_name', 'A 주차장',
            'snapshot_in_time', '2026-07-08T11:00:00'
        ),
        'MANUAL',
        '2026-07-14 09:20:00',
        '2026-08-13 09:20:00'
    );

COMMIT;

-- =====================================================
-- 확인용 조회
-- =====================================================
SELECT 'member' AS table_name, COUNT(*) AS row_count FROM member
UNION ALL SELECT 'parking', COUNT(*) FROM parking
UNION ALL SELECT 'gate', COUNT(*) FROM gate
UNION ALL SELECT 'vehicle_car', COUNT(*) FROM vehicle_car
UNION ALL SELECT 'camera', COUNT(*) FROM camera
UNION ALL SELECT 'camera_data', COUNT(*) FROM camera_data
UNION ALL SELECT 'car_log', COUNT(*) FROM car_log
UNION ALL SELECT 'notice', COUNT(*) FROM notice
UNION ALL SELECT 'trash_bin', COUNT(*) FROM trash_bin
ORDER BY table_name;
