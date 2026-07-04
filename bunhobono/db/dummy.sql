BEGIN;

-- =====================================================
-- 1. MEMBER
-- =====================================================
INSERT INTO member (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone, role, create_at, mem_status)
VALUES
('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자1', '010-1111-1111', 'ADMIN', NOW(), '거주'),
('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자2', '010-1111-2222', 'ADMIN', NOW(), '거주'),
('res1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1001, '입주민1', '010-2222-1111', 'RESIDENT', NOW(), '거주'),
('res2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1002, '입주민2', '010-2222-2222', 'RESIDENT', NOW(), '거주');


-- =====================================================
-- 2. PARKING
-- =====================================================
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
('A주차장', '100', 'A동 지하'),
('B주차장', '80', 'B동 지하'),
('C주차장', '120', 'C동 지하'),
('D주차장', '60', 'D동 지하');


-- =====================================================
-- 3. GATE
-- =====================================================
INSERT INTO gate (parking_no, gate_name, gate_type)
VALUES
(1, 'A-IN', 'In'),
(1, 'A-OUT', 'Out'),
(2, 'B-IN', 'In'),
(2, 'B-OUT', 'Out');


-- =====================================================
-- 4. VEHICLE
-- =====================================================
INSERT INTO vehicle_car (vehicle_type, car_no, vehicle_status, start_date, end_date, approved_no, approved_at)
VALUES
('normal', '11가1111', 'APPROVED', NOW(), NOW() + INTERVAL '30 day', 1, NOW()),
('normal', '22나2222', 'APPROVED', NOW(), NOW() + INTERVAL '30 day', 2, NOW()),
('visit', '33다3333', 'WAITING', NULL, NULL, NULL, NULL),
('normal', '44라4444', 'APPROVED', NOW(), NOW() + INTERVAL '30 day', 1, NOW());


-- =====================================================
-- 5. CAR LOG
-- =====================================================
INSERT INTO car_log (vehicle_car_no, in_gate_no, in_time, out_gate_no, out_time, free_time)
VALUES
(1, 1, NOW() - INTERVAL '2 hour', 2, NOW(), 30),
(2, 3, NOW() - INTERVAL '3 hour', 4, NOW(), 30),
(3, 1, NOW() - INTERVAL '4 hour', 2, NOW(), 30),
(4, 3, NOW() - INTERVAL '5 hour', 4, NOW(), 30);


-- =====================================================
-- 6. CAMERA
-- =====================================================
INSERT INTO camera (parking_no, gate_no, camera_name, camera_type, install_date)
VALUES
(1, 1, 'CAM-A1', 'In', '2025-01-01'),
(1, 2, 'CAM-A2', 'Out', '2025-01-02'),
(2, 3, 'CAM-B1', 'In', '2025-01-03'),
(2, 4, 'CAM-B2', 'Out', '2025-01-04');


-- =====================================================
-- 7. CAMERA DATA
-- =====================================================
INSERT INTO camera_data (camera_no, vehicle_no, car_no, image_path, recognition_state, confidence_score)
VALUES
(1, 1, '11가1111', '/img/1.jpg', TRUE, 99.1),
(2, 2, '22나2222', '/img/2.jpg', TRUE, 98.5),
(3, 3, '33다3333', '/img/3.jpg', TRUE, 97.0),
(4, 4, '44라4444', '/img/4.jpg', TRUE, 96.2);


-- =====================================================
-- 8. NOTICE
-- =====================================================
INSERT INTO notice (parking_no, plate_no, entry_at, stay_days, alert_stat)
VALUES
(1, '11가1111', NOW() - INTERVAL '5 day', 5, 'Unresolved'),
(2, '22나2222', NOW() - INTERVAL '6 day', 6, 'Checked'),
(3, '33다3333', NOW() - INTERVAL '7 day', 7, 'Resolved'),
(4, '44라4444', NOW() - INTERVAL '3 day', 3, 'Unresolved');


-- =====================================================
-- 9. WRONG CAR
-- =====================================================
INSERT INTO wrong_car (car_log_no, plate_no, reason_type, description)
VALUES
(1, '99가9999', 'Unpaid', '미납 차량'),
(2, '88나8888', 'Unregistered', '미등록 차량'),
(3, '77다7777', 'Suspicious', '의심 차량'),
(4, '66라6666', 'Other', '기타');


-- =====================================================
-- 10. FEE POLICY
-- =====================================================
INSERT INTO parking_fee_policy (parking_no, vehicle_type, free_minutes, unit_minutes, unit_fee, daily_max_fee)
VALUES
(1, 'RESIDENT', 30, 10, 1000, 10000),
(2, 'VISITOR', 20, 10, 1500, 12000),
(3, 'RESIDENT', 60, 10, 800, 8000),
(4, 'VISITOR', 30, 10, 2000, 15000);


-- =====================================================
-- 11. CHARGE
-- =====================================================
INSERT INTO parking_charge (car_log_no, fee_policy_no, charge_type, amount, payer_type, payer_no, status)
VALUES
(1, 1, 'VIOLATION', 5000, 'RESIDENT', 1, 'PENDING'),
(2, 2, 'VIOLATION', 7000, 'VISITOR', 2, 'PENDING'),
(3, 3, 'MONTHLY', 30000, 'RESIDENT', 3, 'PENDING'),
(4, 4, 'VIOLATION', 9000, 'VISITOR', 4, 'PENDING');


-- =====================================================
-- 12. PAYMENT
-- =====================================================
INSERT INTO parking_payment (charge_no, payment_amount, payment_method, payment_status, transaction_no)
VALUES
(1, 5000, 'CARD', 'SUCCESS', 'T001'),
(2, 7000, 'CARD', 'SUCCESS', 'T002'),
(3, 30000, 'AUTO', 'SUCCESS', 'T003'),
(4, 9000, 'CARD', 'FAIL', 'T004');

COMMIT;