BEGIN;

-- =====================================================
-- 1. MEMBER
-- =====================================================
INSERT INTO member (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone, role, create_at, mem_status)
VALUES
('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자1', '010-1111-1111', 'ADMIN', NOW(), '거주'),
('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자2', '010-1111-2222', 'ADMIN', NOW(), '거주'),
('res1',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1001, '입주민1',  '010-2222-1111', 'RESIDENT', NOW(), '거주'),
('res2',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1002, '입주민2',  '010-2222-2222', 'RESIDENT', NOW(), '거주'),
('res3',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1003, '입주민3',  '010-2222-1003', 'RESIDENT', NOW(), '거주'),
('res4',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1004, '입주민4',  '010-2222-1004', 'RESIDENT', NOW(), '거주'),
('res5',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1005, '입주민5',  '010-2222-1005', 'RESIDENT', NOW(), '거주'),
('res6',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1006, '입주민6',  '010-2222-1006', 'RESIDENT', NOW(), '거주'),
('res7',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1007, '입주민7',  '010-2222-1007', 'RESIDENT', NOW(), '거주'),
('res8',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1008, '입주민8',  '010-2222-1008', 'RESIDENT', NOW(), '거주'),
('res9',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1009, '입주민9',  '010-2222-1009', 'RESIDENT', NOW(), '거주'),
('res10', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1010, '입주민10', '010-2222-1010', 'RESIDENT', NOW(), '거주'),

('res11', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2001, '입주민11', '010-2222-2001', 'RESIDENT', NOW(), '거주'),
('res12', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2002, '입주민12', '010-2222-2002', 'RESIDENT', NOW(), '거주'),
('res13', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2003, '입주민13', '010-2222-2003', 'RESIDENT', NOW(), '거주'),
('res14', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2004, '입주민14', '010-2222-2004', 'RESIDENT', NOW(), '거주'),
('res15', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2005, '입주민15', '010-2222-2005', 'RESIDENT', NOW(), '거주'),
('res16', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2006, '입주민16', '010-2222-2006', 'RESIDENT', NOW(), '거주'),
('res17', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2007, '입주민17', '010-2222-2007', 'RESIDENT', NOW(), '거주'),
('res18', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2008, '입주민18', '010-2222-2008', 'RESIDENT', NOW(), '거주'),
('res19', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2009, '입주민19', '010-2222-2009', 'RESIDENT', NOW(), '거주'),
('res20', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 2010, '입주민20', '010-2222-2010', 'RESIDENT', NOW(), '거주');


-- =====================================================
-- 2. PARKING
-- =====================================================
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
('A주차장', 100, 'A동 지하'),  
('B주차장', 80, 'B동 지하'),
('C주차장', 100, 'C동 지하'),
('D주차장', 50, '지상 주차장'); 

-- =====================================================
-- 3. GATE
-- =====================================================
INSERT INTO gate (parking_no, gate_name, gate_type)
VALUES
(1, 'A-IN', 'In'),
(1, 'A-OUT', 'Out'),
(2, 'B-IN', 'In'),
(2, 'B-OUT', 'Out'),
(3, 'C-IN', 'In'),
(3, 'C-OUT', 'Out'),
(4, 'D-BOTH', 'Both'); -- 지상주차장 게이트 양방향

-- =====================================================
-- 4. CAMERA
-- =====================================================

INSERT INTO camera (parking_no, gate_no, camera_name, camera_type, install_date)
VALUES
-- 입차: 전면, 후면 / 출차: 전면 **** alpha test때 입차시 전면 카메라만 사용
(1, 1, 'CAM-A1', 'In', '2025-01-01'), -- 입차
(1, 1, 'CAM-A2', 'In', '2025-01-02'), -- A 동 후면 카메라
(1, 2, 'CAM-A3', 'Out', '2025-01-01'), -- 출차

(2, 3, 'CAM-B1', 'In', '2025-01-03'), -- 입차
(2, 3, 'CAM-B2', 'In', '2025-01-04'), -- B 동 후면 카메라
(2, 4, 'CAM-B3', 'Out', '2025-01-03'), -- 출차

(3, 5, 'CAM-C1', 'In', '2025-01-05'), -- 입차
(3, 5, 'CAM-C2', 'In', '2025-01-06'),  --C 동 후면 카메라
(3, 6, 'CAM-C3', 'Out', '2025-01-05'), -- 출차

(4, 7, 'CAM-D1', 'Both', '2025-01-07'); -- 지상 양방향 카메라

-- =====================================================
-- 5. VEHICLE
-- =====================================================
INSERT INTO vehicle_car ( registered_no, vehicle_type, car_no, vehicle_status, start_date, end_date, approved_no, approved_at)
VALUES

-- 일반 입주민 등록 차량

('3','normal', '345다1234', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('4','normal', '456라5678', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('5','normal', '234나7890', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('6','normal', '234나7891', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('7','normal', '123가4567', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),

('8','normal', '678바3456', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('9','normal', '789사7890', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('10','normal', '567마9012', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),

('11','normal', '912자6789', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('12','normal', '135차2468', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('13','normal', '891아2345', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),

('14','normal', '246카1357', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),
('15','normal', '246카1358', 'APPROVED', '2026-07-10 00:00:00', '2026-08-09 23:59:59', 1, '2026-07-10 09:00:00'),

-- =====================================================

-- 방문 등록 차량

-- A주차장 방문 차량: 현재 주차 중
('5','visit', '101하1001', 'APPROVED', '2026-07-06 10:00:00', '2026-07-09 10:00:00', 1, '2026-07-06 10:00:00'),
('9','visit', '101하1002', 'APPROVED', '2026-07-06 10:10:00', '2026-07-09 10:10:00', 1, '2026-07-06 10:10:00'),
('14','visit', '103호1003', 'APPROVED', '2026-07-06 10:20:00', '2026-07-09 10:20:00', 1, '2026-07-06 10:20:00'),

-- A주차장 방문 차량: 출차 완료, 만료
('9','visit', '102허1004', 'EXPIRED', '2026-07-02 10:30:00', '2026-07-05 10:30:00', 1, '2026-07-02 10:30:00'),

-- B주차장 방문 차량: 현재 주차 중
('20','visit', '201하2001', 'APPROVED', '2026-07-06 11:00:00', '2026-07-09 11:00:00', 1, '2026-07-06 11:00:00'),
('13','visit', '203호2003', 'APPROVED', '2026-07-06 11:10:00', '2026-07-09 11:10:00', 1, '2026-07-06 11:10:00'),
('11','visit', '204하2004', 'APPROVED', '2026-07-06 11:20:00', '2026-07-09 11:20:00', 1, '2026-07-06 11:20:00'),
('7','visit', '201하2005', 'APPROVED', '2026-07-06 11:30:00', '2026-07-09 11:30:00', 1, '2026-07-06 11:30:00'),

-- B주차장 방문 차량: 출차 완료, 만료
('3','visit', '202허2002', 'EXPIRED', '2026-07-02 11:40:00', '2026-07-05 11:40:00', 1, '2026-07-02 11:40:00'),

-- C주차장 방문 차량: 현재 주차 중
('4','visit', '301허3001', 'APPROVED', '2026-07-06 12:00:00', '2026-07-09 12:00:00', 1, '2026-07-06 12:00:00'),
('19','visit', '301허3003', 'APPROVED', '2026-07-06 12:10:00', '2026-07-09 12:10:00', 1, '2026-07-06 12:10:00'),
('15','visit', '303하3004', 'APPROVED', '2026-07-06 12:20:00', '2026-07-09 12:20:00', 1, '2026-07-06 12:20:00'),
('8','visit', '304허3005', 'APPROVED', '2026-07-06 12:30:00', '2026-07-09 12:30:00', 1, '2026-07-06 12:30:00'),

-- C주차장 방문 차량: 출차 완료, 만료
('10','visit', '302호3002', 'EXPIRED', '2026-07-02 12:40:00', '2026-07-05 12:40:00', 1, '2026-07-02 12:40:00'),

-- D 지상주차장 방문 차량: 현재 주차 중
('18','visit', '357하2468', 'APPROVED', '2026-07-06 13:00:00', '2026-07-09 13:00:00', 1, '2026-07-06 13:00:00'),
('12','visit', '403허4001', 'APPROVED', '2026-07-06 13:10:00', '2026-07-09 13:10:00', 1, '2026-07-06 13:10:00'),
('3','visit', '404호4002', 'APPROVED', '2026-07-06 13:20:00', '2026-07-09 13:20:00', 1, '2026-07-06 13:20:00'),
('6','visit', '401호4003', 'APPROVED', '2026-07-06 13:30:00', '2026-07-09 13:30:00', 1, '2026-07-06 13:30:00'),
('17','visit', '401호4004', 'APPROVED', '2026-07-06 13:40:00', '2026-07-09 13:40:00', 1, '2026-07-06 13:40:00'),

-- D 지상주차장 방문 차량: 출차 완료, 만료
('14','visit', '402하4005', 'EXPIRED', '2026-07-02 13:50:00', '2026-07-05 13:50:00', 1, '2026-07-02 13:50:00'),

--[알림용 차량 더미데이터]
-- 방문 차량: 만료 후 5~8일 경과, 아직 출차 안 함
('7','visit', '501하5001', 'EXPIRED', '2026-06-25 09:00:00', '2026-06-28 09:00:00', 1, '2026-06-25 09:00:00'), -- 8일 경과
('17','visit', '502허5002', 'EXPIRED', '2026-06-26 10:00:00', '2026-06-29 10:00:00', 1, '2026-06-26 10:00:00'), -- 7일 경과
('10','visit', '503호5003', 'EXPIRED', '2026-06-28 11:00:00', '2026-07-01 11:00:00', 1, '2026-06-28 11:00:00'), -- 5일 경과

-- 방문 차량: 만료 후 2~3일 경과, 아직 출차 안 함
('4','visit', '504하5004', 'EXPIRED', '2026-06-30 12:00:00', '2026-07-03 12:00:00', 1, '2026-06-30 12:00:00'), -- 3일 경과
('20','visit', '505허5005', 'EXPIRED', '2026-07-01 13:00:00', '2026-07-04 13:00:00', 1, '2026-07-01 13:00:00'), -- 2일 경과

-- 일반 입주민 차량: 만료 후 약 7일 경과, 아직 출차 안 함
('4','normal', '601가6001', 'EXPIRED', '2026-05-30 00:00:00', '2026-06-29 23:59:59', 1, '2026-05-30 09:00:00'),
('19','normal', '602나6002', 'EXPIRED', '2026-05-30 00:00:00', '2026-06-29 23:59:59', 1, '2026-05-30 09:10:00');




-- =====================================================
-- 6. CAMERA DATA
-- =====================================================
INSERT INTO camera_data
    (camera_no, vehicle_no, car_no, image_path, recognition_state, confidence_score, direction)
VALUES
-- [A주차장]
-- 등록차량: 주차 중
(1, 1, '345다1234', '/carPlateImg/a1.jpg', TRUE, 99.0, 'IN'),
(1, 2, '456라5678', '/carPlateImg/a2.jpg', TRUE, 98.2, 'IN'),
(1, 3, '234나7890', '/carPlateImg/a3.jpg', TRUE, 97.8, 'IN'),
(3, 4, '234나7891', '/carPlateImg/a4.jpg', TRUE, 96.9, 'IN'),

-- 등록차량: 출차 완료
(1, 5, '123가4567', '/carPlateImg/a5.jpg', TRUE, 99.1, 'IN'),
(3, 5, '123가4567', '/carPlateImg/a6.jpg', TRUE, 98.5, 'OUT'),

-- 방문 등록 차량: 주차 중
(1, 14, '101하1001', '/carPlateImg/a7.jpg', TRUE, 98.7, 'IN'),
(3, 15, '101하1002', '/carPlateImg/a8.jpg', TRUE, 97.9, 'IN'),
(1, 16, '103호1003', '/carPlateImg/a9.jpg', TRUE, 99.0, 'IN'),

-- 방문 등록 차량: 출차 완료
(2, 17, '102허1004', '/carPlateImg/a10.jpg', TRUE, 96.8, 'IN'),
(3, 17, '102허1004', '/carPlateImg/a11.jpg', TRUE, 96.1, 'OUT'),

-- [B주차장]
-- 등록차량: 현재 주차 중
(4, 6, '678바3456', '/carPlateImg/b1.jpg', TRUE, 98.7, 'IN'),
(4, 7, '789사7890', '/carPlateImg/b2.jpg', TRUE, 97.5, 'IN'),

-- 등록차량: 입차 후 출차
(4, 8, '567마9012', '/carPlateImg/b3.jpg', TRUE, 97.4, 'IN'),
(6, 8, '567마9012', '/carPlateImg/b4.jpg', TRUE, 96.8, 'OUT'),

-- 방문 등록 차량: 주차 중
(4, 18, '201하2001', '/carPlateImg/b5.jpg', TRUE, 98.4, 'IN'),
(4, 19, '203호2003', '/carPlateImg/b6.jpg', TRUE, 97.2, 'IN'),
(5, 20, '204하2004', '/carPlateImg/b7.jpg', TRUE, 98.1, 'IN'),
(6, 21, '201하2005', '/carPlateImg/b8.jpg', TRUE, 97.5, 'IN'),

-- 방문 등록 차량: 출차 완료
(5, 22, '202허2002', '/carPlateImg/b9.jpg', TRUE, 95.9, 'IN'),
(6, 22, '202허2002', '/carPlateImg/b10.jpg', TRUE, 96.4, 'OUT'),

-- [C주차장]
-- 등록차량: 주차 중
(7, 9, '912자6789', '/carPlateImg/c1jpg', TRUE, 97.6, 'IN'),
(7, 10, '135차2468', '/carPlateImg/c2.jpg', TRUE, 96.5, 'IN'),

-- 등록차량: 입차 후 출차
(7, 11, '891아2345', '/carPlateImg/c3.jpg', TRUE, 99.2, 'IN'),
(9, 11, '891아2345', '/carPlateImg/c4.jpg', TRUE, 98.1, 'OUT'),

-- 방문 등록 차량: 주차 중
(7, 23, '301허3001', '/carPlateImg/c5.jpg', TRUE, 99.2, 'IN'),
(7, 24, '301허3003', '/carPlateImg/c6.jpg', TRUE, 98.0, 'IN'),
(7, 25, '303하3004', '/carPlateImg/c7.jpg', TRUE, 97.8, 'IN'),
(7, 26, '304허3005', '/carPlateImg/c8.jpg', TRUE, 95.5, 'IN'),

-- 방문 등록 차량: 출차 완료
(7, 27, '302호3002', '/carPlateImg/c9.jpg', TRUE, 96.7, 'IN'),
(9, 27, '302호3002', '/carPlateImg/c10.jpg', TRUE, 96.0, 'OUT'),

-- [D 지상주차장]
-- 등록차량
(10, 12, '246카1357', '/carPlateImg/d1.jpg', TRUE, 98.9, 'IN'),
(10, 13, '246카1358', '/carPlateImg/d2.jpg', TRUE, 98.0, 'IN'),

-- 방문 등록 차량: 주차 중
(10, 28, '357하2468', '/carPlateImg/d3.jpg', TRUE, 97.2, 'IN'),
(10, 29, '403허4001', '/carPlateImg/d4.jpg', TRUE, 98.3, 'IN'),
(10, 30, '404호4002', '/carPlateImg/d5.jpg', TRUE, 97.1, 'IN'),
(10, 31, '401호4003', '/carPlateImg/d6.jpg', TRUE, 98.6, 'IN'),
(10, 32, '401호4004', '/carPlateImg/d7.jpg', TRUE, 97.7, 'IN'),

-- 방문 등록 차량: 출차 완료
(10, 33, '402하4005', '/carPlateImg/d8.jpg', TRUE, 96.9, 'IN'),
(10, 33, '402하4005', '/carPlateImg/d9.jpg', TRUE, 96.2, 'OUT'),

-- 알림용 카메라데이터
(1, 34, '501하5001', '/carPlateImg/alert_a1.jpg', TRUE, 98.8, 'IN'),
(1, 39, '601가6001', '/carPlateImg/alert_a2.jpg', TRUE, 97.9, 'IN'),
(4, 35, '502허5002', '/carPlateImg/alert_b1.jpg', TRUE, 98.4, 'IN'),
(4, 37, '504하5004', '/carPlateImg/alert_b2.jpg', TRUE, 96.7, 'IN'),
(7, 36, '503호5003', '/carPlateImg/alert_c1.jpg', TRUE, 97.6, 'IN'),
(7, 40, '602나6002', '/carPlateImg/alert_c2.jpg', TRUE, 98.1, 'IN'),
(10, 38, '505허5005', '/carPlateImg/alert_d1.jpg', TRUE, 96.9, 'IN');
-- =====================================================
-- 7. CAR LOG
-- =====================================================

-- 방문 차량 등록 시간 = vehicle_car.start_date
--방문 차량 만료 시간 = vehicle_car.end_date
--현재/마지막 감지 시간 = car_log.in_time 또는 camera_data.capture_time
--만료 초과 시간 = 현재/감지 시간 - vehicle_car.end_date

-- =====================================================
INSERT INTO car_log
    (camera_data_no, vehicle_car_no, in_gate_no, in_time, out_gate_no, out_time)
VALUES

-- A주차장  1번 게이트
    -- 일반 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 1 AND direction = 'IN' LIMIT 1), 1, 1, '2026-07-12 08:10:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 2 AND direction = 'IN' LIMIT 1), 2, 1, '2026-07-12 08:25:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 3 AND direction = 'IN' LIMIT 1), 3, 1, '2026-07-12 08:40:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 4 AND direction = 'IN' LIMIT 1), 4, 1, '2026-07-12 08:55:00', NULL, NULL),

    -- 일반 등록 차량: 출차 완료
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 5 AND direction = 'IN' LIMIT 1), 5, 1, '2026-07-12 09:10:00', 2, '2026-07-12 11:20:00'),

    -- 방문 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 14 AND direction = 'IN' LIMIT 1), 14, 1, '2026-07-06 10:05:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 15 AND direction = 'IN' LIMIT 1), 15, 1, '2026-07-06 10:15:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 16 AND direction = 'IN' LIMIT 1), 16, 1, '2026-07-06 10:25:00', NULL, NULL),

    -- 방문 등록 차량: 출차 완료
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 17 AND direction = 'IN' LIMIT 1), 17, 1, '2026-07-02 10:40:00', 2, '2026-07-02 12:10:00'),

-- =====================================================

-- B주차장 3번 게이트
    -- 일반 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 6 AND direction = 'IN' LIMIT 1), 6, 3, '2026-07-12 09:00:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 7 AND direction = 'IN' LIMIT 1), 7, 3, '2026-07-12 09:15:00', NULL, NULL),

    -- 일반 등록 차량: 입차 후 출차
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 8 AND direction = 'IN' LIMIT 1), 8, 3, '2026-07-12 09:30:00', 4, '2026-07-12 13:00:00'),

    -- 방문 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 18 AND direction = 'IN' LIMIT 1), 18, 3, '2026-07-06 11:05:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 19 AND direction = 'IN' LIMIT 1), 19, 3, '2026-07-06 11:15:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 20 AND direction = 'IN' LIMIT 1), 20, 3, '2026-07-06 11:25:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 21 AND direction = 'IN' LIMIT 1), 21, 3, '2026-07-06 11:35:00', NULL, NULL),

    -- 방문 등록 차량: 출차 완료
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 22 AND direction = 'IN' LIMIT 1), 22, 3, '2026-07-02 11:45:00', 4, '2026-07-02 14:20:00'),

-- =====================================================

-- C주차장 5번 게이트
    -- 일반 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 9 AND direction = 'IN' LIMIT 1), 9, 5, '2026-07-12 10:00:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 10 AND direction = 'IN' LIMIT 1), 10, 5, '2026-07-12 10:15:00', NULL, NULL),

    -- 일반 등록 차량: 입차 후 출차
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 11 AND direction = 'IN' LIMIT 1), 11, 5, '2026-07-12 10:30:00', 6, '2026-07-12 15:10:00'),

    -- 방문 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 23 AND direction = 'IN' LIMIT 1), 23, 5, '2026-07-06 12:05:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 24 AND direction = 'IN' LIMIT 1), 24, 5, '2026-07-06 12:15:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 25 AND direction = 'IN' LIMIT 1), 25, 5, '2026-07-06 12:25:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 26 AND direction = 'IN' LIMIT 1), 26, 5, '2026-07-06 12:35:00', NULL, NULL),

    -- 방문 등록 차량: 출차 완료
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 27 AND direction = 'IN' LIMIT 1), 27, 5, '2026-07-02 12:45:00', 6, '2026-07-02 16:00:00'),

-- =====================================================

-- D 지상주차장 7번 게이트
    -- 일반 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 12 AND direction = 'IN' LIMIT 1), 12, 7, '2026-07-12 11:00:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 13 AND direction = 'IN' LIMIT 1), 13, 7, '2026-07-12 11:15:00', NULL, NULL),

    -- 방문 등록 차량: 현재 주차 중
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 28 AND direction = 'IN' LIMIT 1), 28, 7, '2026-07-06 13:05:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 29 AND direction = 'IN' LIMIT 1), 29, 7, '2026-07-06 13:15:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 30 AND direction = 'IN' LIMIT 1), 30, 7, '2026-07-06 13:25:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 31 AND direction = 'IN' LIMIT 1), 31, 7, '2026-07-06 13:35:00', NULL, NULL),
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 32 AND direction = 'IN' LIMIT 1), 32, 7, '2026-07-06 13:45:00', NULL, NULL),

    -- 방문 등록 차량: 출차 완료
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 33 AND direction = 'IN' LIMIT 1), 33, 7, '2026-07-02 13:55:00', 7, '2026-07-02 17:30:00'),

-- =====================================================
--[알림용 로그 더미데이터]
    -- 방문 차량: 만료 후 5~8일 경과
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 34 AND direction = 'IN' LIMIT 1), 34, 1, '2026-06-25 09:10:00', NULL, NULL), -- A-IN
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 35 AND direction = 'IN' LIMIT 1), 35, 3, '2026-06-26 10:10:00', NULL, NULL), -- B-IN
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 36 AND direction = 'IN' LIMIT 1), 36, 5, '2026-06-28 11:10:00', NULL, NULL), -- C-IN

    -- 방문 차량: 만료 후 2~3일 경과
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 37 AND direction = 'IN' LIMIT 1), 37, 3, '2026-06-30 12:10:00', NULL, NULL), -- B-IN
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 38 AND direction = 'IN' LIMIT 1), 38, 7, '2026-07-01 13:10:00', NULL, NULL), -- D-BOTH

    -- 일반 입주민 차량: 만료 후 약 7일 경과
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 39 AND direction = 'IN' LIMIT 1), 39, 1, '2026-06-30 08:30:00', NULL, NULL), -- A-IN
    ((SELECT camera_data_no FROM camera_data WHERE vehicle_no = 40 AND direction = 'IN' LIMIT 1), 40, 5, '2026-06-30 09:00:00', NULL, NULL); -- C-IN


-- =====================================================
-- 8. NOTICE
-- =====================================================
INSERT INTO notice (parking_no, car_log_no, entry_at, stay_days, alert_stat)
VALUES
-- 방문 차량: 만료 후 5~8일 경과
(1, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 34 AND out_time IS NULL LIMIT 1), '2026-06-25 09:10:00', 8, 'Unresolved'),
(2, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 35 AND out_time IS NULL LIMIT 1), '2026-06-26 10:10:00', 7, 'Unresolved'),
(3, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 36 AND out_time IS NULL LIMIT 1), '2026-06-28 11:10:00', 5, 'Unresolved'),

-- 방문 차량: 만료 후 2~3일 경과
(2, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 37 AND out_time IS NULL LIMIT 1), '2026-06-30 12:10:00', 3, 'Unresolved'),
(4, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 38 AND out_time IS NULL LIMIT 1), '2026-07-01 13:10:00', 2, 'Unresolved'),

-- 일반 입주민 차량: 만료 후 약 7일 경과
(1, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 39 AND out_time IS NULL LIMIT 1), '2026-06-30 08:30:00', 7, 'Unresolved'),
(3, (SELECT car_log_no FROM car_log WHERE vehicle_car_no = 40 AND out_time IS NULL LIMIT 1), '2026-06-30 09:00:00', 7, 'Unresolved');


COMMIT;