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
    

-- 입주민들 세대당 차량 1대씩 지정(칼)
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date,
     member_no, approved_at)
VALUES
    ('normal', '12가3456', 'APPROVED', '2026-07-01', NULL, 1, '2026-07-01 09:00'),
    ('normal', '34나5678', 'APPROVED', '2026-07-01', NULL, 2, '2026-07-01 09:05'),
	('normal', '12다3456', 'APPROVED', '2026-07-01', NULL, 3, '2026-07-01 09:00'),
    ('normal', '34라5678', 'APPROVED', '2026-07-01', NULL, 4, '2026-07-01 09:05'),
	('normal', '12마3456', 'APPROVED', '2026-07-01', NULL, 5, '2026-07-01 09:00'),
    ('normal', '34바5678', 'APPROVED', '2026-07-01', NULL, 6, '2026-07-01 09:05'),
	('normal', '12사3456', 'APPROVED', '2026-07-01', NULL, 7, '2026-07-01 09:00'),
    ('normal', '34아5678', 'APPROVED', '2026-07-01', NULL, 8, '2026-07-01 09:05'),
	('normal', '12자3456', 'APPROVED', '2026-07-01', NULL, 9, '2026-07-01 09:00'),
	
    ('visit', '101마0001', 'APPROVED', '2026-07-06 10:00', '2026-07-15 23:59:59', 3, '2026-07-06 10:00'),
    ('visit', '202바0002', 'EXPIRED', '2026-06-25 09:00', '2026-06-28 23:59:59', 4, '2026-06-25 09:00');

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
