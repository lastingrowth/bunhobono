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

	--동호수 
	('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자', '010-1111-1111', 'ADMIN', NOW(), NULL, '근무'),
	('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '경비원', '010-1111-1111', 'ADMIN', NOW(), NULL, '근무'),
	
    ('res1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 101, '마틴', '010-2222-0101', 'RESIDENT', NOW(), NULL, '거주'),
    ('res2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 201, '제임스', '010-2222-0102', 'RESIDENT', NOW(), NULL, '거주'),
    ('res3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 301, '오드리', '010-2222-0103', 'RESIDENT', NOW(), NULL, '거주'),
    ('res4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 401, '닉', '010-2222-0104', 'RESIDENT', NOW(), NULL, '거주'),
    ('res5', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 501, '칼', '010-2222-0201', 'RESIDENT', NOW(), NULL, '거주'),
    ('res6', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 601, '찰스', '010-2222-0202', 'RESIDENT', NOW(), NULL, '거주'),
    ('res7', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 701, '마이클', '010-2222-0203', 'RESIDENT', NOW(), NULL, '거주'),
    ('res8', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 801, '케빈', '010-2222-0204', 'RESIDENT', NOW(), NULL, '거주'),
    ('res9', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 901, '오스틴', '010-2222-0301', 'RESIDENT', NOW(), NULL, '거주'),
    ('unit_101_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_102_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_201_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_202_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_301_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_302_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_401_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 101, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_201', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 201, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_301', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 301, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 401, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_501', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 501, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_601', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 601, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_701', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 701, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_801', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 801, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_901', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 901, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출'),
    ('unit_402_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 1001, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), '전출');

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

-- 기존과 동일한 카메라 8개
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


    

-- 입주민 등록 차량 더미
-- vehicle_type: normal
-- vehicle_status: APPROVED
-- start_date는 차량마다 다르게 지정
-- end_date는 실제 프론트 구조처럼 start_date + 등록 개월 수로 생성
INSERT INTO vehicle_car
    (vehicle_type, car_no, alias_car_no, vehicle_status, start_date, end_date, member_no, approved_at)
VALUES
    ('normal', '222하5233', NULL, 'APPROVED',
        TIMESTAMP '2026-07-01 09:00:00',
        TIMESTAMP '2026-07-01 09:00:00' + INTERVAL '1 month',
        (SELECT member_no FROM member WHERE login_id = 'res1'),
        NOW()),

    ('normal', '26무3111', NULL, 'APPROVED',
        TIMESTAMP '2026-07-03 10:00:00',
        TIMESTAMP '2026-07-03 10:00:00' + INTERVAL '3 months',
        (SELECT member_no FROM member WHERE login_id = 'res2'),
        NOW()),

    ('normal', '41소2593', NULL, 'APPROVED',
        TIMESTAMP '2026-07-05 11:00:00',
        TIMESTAMP '2026-07-05 11:00:00' + INTERVAL '6 months',
        (SELECT member_no FROM member WHERE login_id = 'res3'),
        NOW()),

    ('normal', '47조2603', NULL, 'APPROVED',
        TIMESTAMP '2026-07-07 12:00:00',
        TIMESTAMP '2026-07-07 12:00:00' + INTERVAL '12 months',
        (SELECT member_no FROM member WHERE login_id = 'res4'),
        NOW()),

    ('normal', '817라7385', NULL, 'APPROVED',
        TIMESTAMP '2026-07-09 13:00:00',
        TIMESTAMP '2026-07-09 13:00:00' + INTERVAL '1 month',
        (SELECT member_no FROM member WHERE login_id = 'res5'),
        NOW()),

    ('normal', '95마7152', NULL, 'APPROVED',
        TIMESTAMP '2026-07-11 14:00:00',
        TIMESTAMP '2026-07-11 14:00:00' + INTERVAL '3 months',
        (SELECT member_no FROM member WHERE login_id = 'res6'),
        NOW()),

    ('normal', '67모4231', NULL, 'APPROVED',
        TIMESTAMP '2026-07-13 15:00:00',
        TIMESTAMP '2026-07-13 15:00:00' + INTERVAL '6 months',
        (SELECT member_no FROM member WHERE login_id = 'res7'),
        NOW()),

    ('normal', '222마2574', NULL, 'APPROVED',
        TIMESTAMP '2026-07-15 16:00:00',
        TIMESTAMP '2026-07-15 16:00:00' + INTERVAL '12 months',
        (SELECT member_no FROM member WHERE login_id = 'res8'),
        NOW()),

    ('normal', '55오0359', NULL, 'APPROVED',
        TIMESTAMP '2026-07-17 17:00:00',
        TIMESTAMP '2026-07-17 17:00:00' + INTERVAL '3 months',
        (SELECT member_no FROM member WHERE login_id = 'res9'),
        NOW());




COMMIT;
