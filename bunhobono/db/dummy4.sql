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

-- =====================================================
-- PARKING
-- =====================================================
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
    ('A주차장', 100, 'A동 지하'),
    ('B주차장', 80, 'B동 지하'),
    ('C주차장', 100, 'C동 지하'),
    ('D주차장', 50, '지상 주차장');

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
    (4, 'D-IN', 'In'),
	(4, 'D-OUT', 'Out');

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
