BEGIN;

TRUNCATE TABLE notice, car_log, camera_data, camera, vehicle_car, gate, parking, member
RESTART IDENTITY CASCADE;

INSERT INTO member (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone, role, create_at, mem_status) VALUES
('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자1', '010-1111-1111', 'ADMIN', NOW(), '근무'),
('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자2', '010-1111-2222', 'ADMIN', NOW(), '근무'),
('res1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1001, '입주민1', '010-2222-1001', 'RESIDENT', NOW(), '거주'),
('res2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1002, '입주민2', '010-2222-1002', 'RESIDENT', NOW(), '거주');

INSERT INTO parking (parking_name, parking_spaces, parking_location) VALUES
('A 주차장', 100, 'A동 지상'),
('B 주차장', 80, 'B동 지상'),
('C 주차장', 100, 'C동 지상'),
('D 주차장', 50, '지하 주차장');

INSERT INTO gate (parking_no, gate_name, gate_type) VALUES
(1, 'A-IN', 'In'), (1, 'A-OUT', 'Out'),
(2, 'B-IN', 'In'), (2, 'B-OUT', 'Out'),
(3, 'C-IN', 'In'), (3, 'C-OUT', 'Out'),
(4, 'D-BOTH', 'Both');

INSERT INTO vehicle_car (vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at) VALUES
('normal', '12가3456', 'APPROVED', '2026-07-01 00:00:00', NULL, 1, '2026-07-01 09:00:00'),
('normal', '34나5678', 'APPROVED', '2026-07-01 00:00:00', NULL, 1, '2026-07-01 09:05:00'),
('visit', '101마0001', 'APPROVED', '2026-07-06 10:00:00', '2026-07-15 23:59:59', 2, '2026-07-06 10:00:00'),
('visit', '202바0002', 'EXPIRED', '2026-06-25 09:00:00', '2026-06-28 23:59:59', 2, '2026-06-25 09:00:00');

INSERT INTO camera (gate_no, camera_name, camera_type, install_date) VALUES
(1, 'CAM-A1', 'In', '2025-01-01'), (2, 'CAM-A2', 'Out', '2025-01-01'),
(3, 'CAM-B1', 'In', '2025-01-03'), (4, 'CAM-B2', 'Out', '2025-01-03'),
(5, 'CAM-C1', 'In', '2025-01-05'), (6, 'CAM-C2', 'Out', '2025-01-05'),
(7, 'CAM-D1', 'Both', '2025-01-07');

INSERT INTO camera_data (camera_no, vehicle_car_no, car_no, capture_time, image_path, recognition_state, confidence_score) VALUES
(1, 1, '12가3456', '2026-07-12 08:10:00', '/carPlateImg/a1-in.jpg', TRUE, 99.0),
(1, 2, '34나5678', '2026-07-12 08:25:00', '/carPlateImg/a2-in.jpg', TRUE, 98.2),
(3, 3, '101마0001', '2026-07-06 10:05:00', '/carPlateImg/b1-in.jpg', TRUE, 98.7),
(3, 4, '202바0002', '2026-06-25 09:10:00', '/carPlateImg/b2-alert.jpg', TRUE, 97.6),
(1, NULL, '미인식', '2026-07-12 11:00:00', '/carPlateImg/a-unknown.jpg', FALSE, 0.0);

INSERT INTO car_log (vehicle_car_no, camera_data_no, in_gate_no, in_time, out_gate_no, out_time, free_time) VALUES
(1, 1, 1, '2026-07-12 08:10:00', NULL, NULL, 180),
(2, 2, 1, '2026-07-12 08:25:00', NULL, NULL, 180),
(3, 3, 3, '2026-07-06 10:05:00', NULL, NULL, 180),
(4, 4, 3, '2026-06-25 09:10:00', NULL, NULL, 180),
(NULL, 5, 1, '2026-07-12 11:00:00', NULL, NULL, 0);

INSERT INTO notice (car_log_no, detect_at, stay_days, alert_stat, handled_by_member_no, handled_at) VALUES
(4, '2026-07-03 09:10:00', 8, 'Unresolved', NULL, NULL),
(3, '2026-07-09 10:05:00', 3, 'Checked', 2, '2026-07-09 10:20:00');

COMMIT;
