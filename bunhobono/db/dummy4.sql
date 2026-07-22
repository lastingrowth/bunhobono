

-- 새로운 회원을 등록하고 그 사람은 새로운 차를 그냥 등록하면 됨.
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
SELECT
    CASE WHEN i % 5 = 0 THEN 'visit' ELSE 'normal' END,
    (60 + i)::text || (ARRAY['가','나','다','라','마'])[((i - 1) % 5) + 1]
        || LPAD((2000 + i)::text, 4, '0'),
    CASE WHEN i % 11 = 0 THEN 'EXPIRED'
         WHEN i % 13 = 0 THEN 'WAITING'
         ELSE 'APPROVED' END,
    TIMESTAMP '2026-07-01 00:00:00' + (i * INTERVAL '3 hour'),
    CASE WHEN i % 5 = 0 THEN TIMESTAMP '2026-07-20 23:59:59' ELSE NULL END,
    6 + ((i - 1) % 27),
    CASE WHEN i % 13 = 0 THEN NULL
         ELSE TIMESTAMP '2026-07-01 09:00:00' + (i * INTERVAL '3 hour') END
FROM generate_series(1, 45) AS s(i);

-- 기존 촬영 데이터 7건 유지
INSERT INTO camera_data
    (camera_no, vehicle_car_no, car_no, capture_time, image_path,
     recognition_state, confidence_score)
VALUES
    (1, 1, '12가3456', '2026-07-12 08:10', '/carPlateImg/a1-in.jpg', TRUE, 99.00),
    (1, 2, '34나5678', '2026-07-12 08:25', '/carPlateImg/a2-in.jpg', TRUE, 98.20),
    (3, 3, '101마0001', '2026-07-06 10:05', '/carPlateImg/b1-in.jpg', TRUE, 98.70),
    (3, 4, '202바0002', '2026-06-25 09:10', '/carPlateImg/b2-alert.jpg', TRUE, 97.60),
    (1, NULL, '미인식', '2026-07-12 11:00', '/carPlateImg/a-unknown.jpg', FALSE, 0.00),
    (5, NULL, '77라7777', '2026-07-13 18:30', '/carPlateImg/c-unknown.jpg', TRUE, 91.30),
    (2, 1, '12가3456', '2026-07-13 20:00', '/carPlateImg/a1-out.jpg', TRUE, 99.10);

-- 추가 차량 45대의 입차 촬영 데이터(camera_data_no 8~52)
INSERT INTO camera_data
    (camera_no, vehicle_car_no, car_no, capture_time, image_path,
     recognition_state, confidence_score)
SELECT
    (ARRAY[1, 3, 5, 7])[((i - 1) % 4) + 1],
    5 + i,
    (60 + i)::text || (ARRAY['가','나','다','라','마'])[((i - 1) % 5) + 1]
        || LPAD((2000 + i)::text, 4, '0'),
    TIMESTAMP '2026-07-15 11:30:00' - (i * INTERVAL '2 hour'),
    '/carPlateImg/extra-' || LPAD(i::text, 2, '0') || '.jpg',
    TRUE,
    90.00 + ((i * 17) % 900) / 100.0
FROM generate_series(1, 45) AS s(i)
ORDER BY i;

-- 기존 입출차 로그 6건 유지
INSERT INTO car_log
    (vehicle_car_no, camera_data_no, in_gate_no, in_time,
     out_gate_no, out_time, free_time, snapshot_car_no)
VALUES
    (1, 1, 1, '2026-07-12 08:10', 2, '2026-07-13 20:00', 180, '12가3456'),
    (2, 2, 1, '2026-07-12 08:25', NULL, NULL, 180, '34나5678'),
    (3, 3, 3, '2026-07-06 10:05', NULL, NULL, 180, '101마0001'),
    (4, 4, 3, '2026-06-25 09:10', NULL, NULL, 180, '202바0002'),
    (NULL, 5, 1, '2026-07-12 11:00', NULL, NULL, 0, '미인식'),
    (NULL, 6, 5, '2026-07-13 18:30', NULL, NULL, 0, '77라7777');

-- 추가 차량 45대의 입차 로그: 기존 5대 + 추가 45대 = 현재 주차 50대
INSERT INTO car_log
    (vehicle_car_no, camera_data_no, in_gate_no, in_time,
     out_gate_no, out_time, free_time, snapshot_car_no)
SELECT
    5 + i,
    7 + i,
    (ARRAY[1, 3, 5, 7])[((i - 1) % 4) + 1],
    TIMESTAMP '2026-07-15 11:30:00' - (i * INTERVAL '2 hour'),
    NULL, NULL, 180,
    (60 + i)::text || (ARRAY['가','나','다','라','마'])[((i - 1) % 5) + 1]
        || LPAD((2000 + i)::text, 4, '0')
FROM generate_series(1, 45) AS s(i)
ORDER BY i;

-- 기존 알림 4건 유지
INSERT INTO notice
    (car_log_no, detect_at, stay_days, alert_stat,
     handled_by_member_no, handled_at,
     snapshot_car_log_no, snapshot_registered_car_no,
     snapshot_captured_car_no, snapshot_car_kind,
     snapshot_parking_name, snapshot_in_time)
VALUES
    (4, '2026-07-03 09:10', 8, 'Unresolved', NULL, NULL,
     4, '202바0002', '202바0002', 'VISIT', 'B 주차장', '2026-06-25 09:10'),
    (3, '2026-07-09 10:05', 3, 'Checked', 2, '2026-07-09 10:20',
     3, '101마0001', '101마0001', 'VISIT', 'B 주차장', '2026-07-06 10:05'),
    (5, '2026-07-13 11:00', 1, 'Unresolved', NULL, NULL,
     5, NULL, '미인식', 'UNKNOWN', 'A 주차장', '2026-07-12 11:00'),
    (6, '2026-07-14 18:30', 1, 'Resolved', 1, '2026-07-14 19:00',
     6, NULL, '77라7777', 'UNKNOWN', 'C 주차장', '2026-07-13 18:30');

-- 휴지통 화면 확인용 3종 데이터
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
VALUES
    ('CAMERA_DATA', 9001,
     jsonb_build_object('camera_data_no', 9001, 'camera_no', 1,
                        'car_no', '99테9001', 'capture_time', '2026-07-10T08:30:00',
                        'image_path', '/carPlateImg/trash-camera.jpg',
                        'recognition_state', TRUE, 'confidence_score', 95.50),
     'MANUAL', TIMESTAMP '2026-07-14 09:00:00', TIMESTAMP '2026-08-13 09:00:00'),
    ('CAR_LOG', 9001,
     jsonb_build_object('car_log_no', 9001, 'snapshot_car_no', '88테9001',
                        'in_gate_no', 1, 'in_time', '2026-07-09T10:00:00',
                        'out_gate_no', 2, 'out_time', '2026-07-09T15:00:00'),
     'SCHEDULED', TIMESTAMP '2026-07-14 09:10:00', TIMESTAMP '2026-08-13 09:10:00'),
    ('NOTICE', 9001,
     jsonb_build_object('notice_no', 9001, 'snapshot_car_log_no', 9001,
                        'snapshot_captured_car_no', '77테9001',
                        'snapshot_car_kind', 'REGISTERED',
                        'snapshot_parking_name', 'A 주차장',
                        'alert_stat', 'Resolved'),
     'MANUAL', TIMESTAMP '2026-07-14 09:20:00', TIMESTAMP '2026-08-13 09:20:00');

COMMIT;
