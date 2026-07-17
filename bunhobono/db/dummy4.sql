BEGIN;

-- 기존 데이터를 모두 비우고 PK 번호를 1부터 다시 시작합니다.
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

INSERT INTO member
    (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone,
     role, create_at, delete_at, mem_status)
VALUES
    ('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자1', '010-1111-1111', 'ADMIN', NOW(), NULL, '근무'),
    ('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자2', '010-1111-2222', 'ADMIN', NOW(), NULL, '근무'),
    -- 101~104동, 1~15층, 1~4라인 안에서 사용하는 정상 거주 세대
    ('res1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 101, '입주민1', '010-2222-0101', 'RESIDENT', NOW(), NULL, '거주'),
    ('res2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 102, '입주민2', '010-2222-0102', 'RESIDENT', NOW(), NULL, '거주'),
    ('res3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 103, '입주민3', '010-2222-0103', 'RESIDENT', NOW(), NULL, '거주'),
    ('res4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 104, '입주민4', '010-2222-0104', 'RESIDENT', NOW(), NULL, '거주'),
    ('res5', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 201, '입주민5', '010-2222-0201', 'RESIDENT', NOW(), NULL, '거주'),
    ('res6', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 202, '입주민6', '010-2222-0202', 'RESIDENT', NOW(), NULL, '거주'),
    ('res7', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 203, '입주민7', '010-2222-0203', 'RESIDENT', NOW(), NULL, '거주'),
    ('res8', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 204, '입주민8', '010-2222-0204', 'RESIDENT', NOW(), NULL, '거주'),
    ('res9', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 301, '입주민9', '010-2222-0301', 'RESIDENT', NOW(), NULL, '거주'),
    ('res10', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 302, '입주민10', '010-2222-0302', 'RESIDENT', NOW(), NULL, '거주'),
    ('res11', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 303, '입주민11', '010-2222-0303', 'RESIDENT', NOW(), NULL, '거주'),
    ('res12', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 304, '입주민12', '010-2222-0304', 'RESIDENT', NOW(), NULL, '거주'),
    ('res13', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 103, 401, '입주민13', '010-2222-0401', 'RESIDENT', NOW(), NULL, '거주'),
    ('res14', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 103, 402, '입주민14', '010-2222-0402', 'RESIDENT', NOW(), NULL, '거주'),
    ('res15', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 103, 403, '입주민15', '010-2222-0403', 'RESIDENT', NOW(), NULL, '거주'),
    ('res16', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 103, 404, '입주민16', '010-2222-0404', 'RESIDENT', NOW(), NULL, '거주'),

    -- 전출됐지만 같은 세대에 승인대기 회원이 있어 공개 가입 목록에는 나오지 않는 사례
    ('res17', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 104, 501, '입주민17', '010-2222-0501', 'RESIDENT', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', '전출'),
    ('res18', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 104, 502, '입주민18', '010-2222-0502', 'RESIDENT', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days', '전출'),

    -- 승인대기 회원이 없어 공개 가입 화면에 표시되는 사례(104동 503호, 504호)
    ('res19', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 104, 503, '입주민19', '010-2222-0503', 'RESIDENT', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', '전출'),
    ('res20', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 104, 504, '입주민20', '010-2222-0504', 'RESIDENT', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days', '전출'),

    ('pending1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 104, 501, '승인대기1', '010-3333-0501', 'PENDING', NOW(), NULL, '거주'),
    ('pending2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 104, 502, '승인대기2', '010-3333-0502', 'PENDING', NOW(), NULL, '거주');


-- 기존과 동일한 주차장 4개
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
    ('A 주차장', 100, 'A동 지상'),
    ('B 주차장', 80, 'B동 지상'),
    ('C 주차장', 100, 'C동 지상'),
    ('D 주차장', 50, '지하 주차장');

-- 기존과 동일한 게이트 8개
INSERT INTO gate (parking_no, gate_name, gate_type)
VALUES
    (1, 'A-IN', 'In'),
    (1, 'A-OUT', 'Out'),
    (2, 'B-IN', 'In'),
    (2, 'B-OUT', 'Out'),
    (3, 'C-IN', 'In'),
    (3, 'C-OUT', 'Out'),
    (4, 'D-IN', 'In'),
    (4, 'D-OUT', 'Out');

-- 기존과 동일한 카메라 7개
INSERT INTO camera (gate_no, camera_name, camera_type, install_date)
VALUES
    (1, 'CAM-A1', 'In',   DATE '2025-01-01'),
    (2, 'CAM-A2', 'Out',  DATE '2025-01-01'),
    (3, 'CAM-B1', 'In',   DATE '2025-01-03'),
    (4, 'CAM-B2', 'Out',  DATE '2025-01-03'),
    (5, 'CAM-C1', 'In',   DATE '2025-01-05'),
    (6, 'CAM-C2', 'Out',  DATE '2025-01-05'),
    (7, 'CAM-D1', 'In',  DATE '2025-02-05'),
    (8, 'CAM-D2', 'Out',  DATE '2025-02-05');
    

-- 기존 차량 5대 유지
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date,
     member_no, approved_at)
VALUES
    ('normal', '12가3456', 'APPROVED', '2026-07-01', NULL, 3, '2026-07-01 09:00'),
    ('normal', '34나5678', 'APPROVED', '2026-07-01', NULL, 4, '2026-07-01 09:05'),
    ('visit', '101마0001', 'APPROVED', '2026-07-06 10:00', '2026-07-15 23:59:59', 3, '2026-07-06 10:00'),
    ('visit', '202바0002', 'EXPIRED', '2026-06-25 09:00', '2026-06-28 23:59:59', 4, '2026-06-25 09:00'),
    ('normal', '56다7890', 'WAITING', '2026-07-14', NULL, 5, NULL);

-- 차량 45대 추가(vehicle_car_no 6~50)
-- 기존 입주민 3명은 이미 1~2대를 보유하므로 추가 차량은 res4~res30에게 배정합니다.
-- 27명의 추가 입주민이 각각 1~2대를 보유하게 됩니다.
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
