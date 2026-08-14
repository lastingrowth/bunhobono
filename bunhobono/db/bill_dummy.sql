BEGIN;


-- =====================================================
-- KIOSK BILLING AND B1 RESIDENT EXIT TEST DUMMY
-- B2 키오스크에서 차량 조회, 정산서 생성, 결제 완료,
-- 관리자 정산 목록의 출차 가능 상태 변경을 확인한다.
-- B1 키오스크에서는 입주민 차량의 로봇 출차 작업 생성을 확인한다.
-- 같은 파일을 다시 실행할 수 있도록 기존 테스트 데이터만 먼저 삭제한다.
-- =====================================================

DELETE FROM robot_task
WHERE car_log_no IN (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no IN (
        '299가1101',
        '299가1102',
        '299가1103'
    )
);

UPDATE parking_space
SET car_log_no = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE car_log_no IN (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no IN (
        '299가1101',
        '299가1102',
        '299가1103'
    )
);

DELETE FROM bill
WHERE car_log_no IN (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no IN (
        '299가1101',
        '299가1102',
        '299가1103',
        '299가1201',
        '299가1202',
        '299가1203',
        '299가1204',
        '299가1205'
    )
);

DELETE FROM car_log
WHERE snapshot_car_no IN (
    '299가1101',
    '299가1102',
    '299가1103',
    '299가1201',
    '299가1202',
    '299가1203',
    '299가1204',
    '299가1205'
);

DELETE FROM camera_data
WHERE cam_note IN (
    'B1-RESIDENT-EXIT-TEST-1',
    'B1-RESIDENT-EXIT-TEST-2',
    'B1-RESIDENT-EXIT-TEST-3',
    'B2-BILLING-TEST-VISIT-1',
    'B2-BILLING-TEST-VISIT-2',
    'B2-BILLING-TEST-VISIT-3',
    'B2-BILLING-TEST-UNKNOWN-1',
    'B2-BILLING-TRASH-TEST-1'
);


-- B1 입주민 차량 3대를 현재 주차 중인 서로 다른 일반 주차면에 배정한다.
WITH test_car (
    car_no,
    in_time,
    gate_code,
    image_path,
    crop_image_path,
    cam_note
) AS (
    VALUES
        (
            '299가1101',
            CURRENT_TIMESTAMP - INTERVAL '4 hours',
            'B1-IN-1',
            'camera-data/img_000118.jpeg',
            'camera-data/crop/img_000118.jpg',
            'B1-RESIDENT-EXIT-TEST-1'
        ),
        (
            '299가1102',
            CURRENT_TIMESTAMP - INTERVAL '3 hours',
            'B1-IN-1',
            'camera-data/img_000374.jpeg',
            'camera-data/crop/img_000374.jpg',
            'B1-RESIDENT-EXIT-TEST-2'
        ),
        (
            '299가1103',
            CURRENT_TIMESTAMP - INTERVAL '2 hours',
            'B1-IN-1',
            'camera-data/img_000160.jpeg',
            'camera-data/crop/img_000160.jpg',
            'B1-RESIDENT-EXIT-TEST-3'
        )
), inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        camera.camera_no,
        NULL,
        test_car.car_no,
        test_car.car_no,
        test_car.in_time,
        test_car.image_path,
        test_car.crop_image_path,
        TRUE,
        99.00,
        test_car.cam_note
    FROM test_car
    JOIN gate
        ON gate.gate_code = test_car.gate_code
    JOIN camera
        ON camera.gate_no = gate.gate_no
       AND camera.camera_type = 'In'
    RETURNING camera_data_no, car_no, capture_time, cam_note
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        NULL,
        inserted_camera.camera_data_no,
        gate.gate_no,
        inserted_camera.capture_time,
        0,
        inserted_camera.car_no,
        'REGISTERED'
    FROM inserted_camera
    JOIN gate
        ON gate.gate_code = 'B1-IN-1'
    RETURNING car_log_no, snapshot_car_no
), empty_b1_space AS (
    SELECT
        parking_space.space_no,
        ROW_NUMBER() OVER (
            ORDER BY parking_space.space_no
        ) AS row_no
    FROM parking_space
    JOIN parking
        ON parking.parking_no = parking_space.parking_no
    WHERE parking.parking_code = 'B1'
      AND parking_space.space_type = 'PARKING'
      AND parking_space.car_log_no IS NULL
      AND parking_space.active = TRUE
    ORDER BY parking_space.space_no
    LIMIT 3
), numbered_log AS (
    SELECT
        inserted_log.car_log_no,
        ROW_NUMBER() OVER (
            ORDER BY inserted_log.snapshot_car_no
        ) AS row_no
    FROM inserted_log
)
UPDATE parking_space
SET car_log_no = numbered_log.car_log_no,
    updated_at = CURRENT_TIMESTAMP
FROM numbered_log
JOIN empty_b1_space
    ON empty_b1_space.row_no = numbered_log.row_no
WHERE parking_space.space_no = empty_b1_space.space_no;


-- B2 입차 카메라 촬영정보를 생성한다.
WITH test_car (
    car_no,
    car_kind,
    free_time,
    in_time,
    gate_code,
    image_path,
    crop_image_path,
    cam_note
) AS (
    VALUES
        (
            '299가1201',
            'VISIT',
            1440,
            CURRENT_TIMESTAMP - INTERVAL '5 minutes',
            'B2-IN-1',
            'camera-data/img_000374.jpeg',
            'camera-data/crop/img_000374.jpg',
            'B2-BILLING-TEST-VISIT-1'
        ),
        (
            '299가1202',
            'VISIT',
            1440,
            CURRENT_TIMESTAMP - INTERVAL '12 hours',
            'B2-IN-2',
            'camera-data/img_000160.jpeg',
            'camera-data/crop/img_000160.jpg',
            'B2-BILLING-TEST-VISIT-2'
        ),
        (
            '299가1203',
            'VISIT',
            1440,
            CURRENT_TIMESTAMP - INTERVAL '26 hours',
            'B2-IN-1',
            'camera-data/img_000975.jpeg',
            'camera-data/crop/img_000975.jpg',
            'B2-BILLING-TEST-VISIT-3'
        ),
        (
            '299가1204',
            'UNKNOWN',
            0,
            CURRENT_TIMESTAMP - INTERVAL '2 hours',
            'B2-IN-2',
            'camera-data/img_000975.jpeg',
            'camera-data/crop/img_000975.jpg',
            'B2-BILLING-TEST-UNKNOWN-1'
        )
), inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        camera.camera_no,
        NULL,
        test_car.car_no,
        test_car.car_no,
        test_car.in_time,
        test_car.image_path,
        test_car.crop_image_path,
        TRUE,
        99.00,
        test_car.cam_note
    FROM test_car
    JOIN gate
        ON gate.gate_code = test_car.gate_code
    JOIN camera
        ON camera.gate_no = gate.gate_no
       AND camera.camera_type = 'In'
    RETURNING camera_data_no, car_no, capture_time, cam_note
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        NULL,
        inserted_camera.camera_data_no,
        gate.gate_no,
        inserted_camera.capture_time,
        test_car.free_time,
        test_car.car_no,
        test_car.car_kind
    FROM inserted_camera
    JOIN test_car
        ON test_car.cam_note = inserted_camera.cam_note
    JOIN gate
        ON gate.gate_code = test_car.gate_code
    RETURNING car_log_no
), selected_rule AS (
    SELECT fee_rule_no
    FROM fee_rule
    WHERE effective_from <= CURRENT_TIMESTAMP
      AND (
          effective_to IS NULL
          OR effective_to > CURRENT_TIMESTAMP
      )
    ORDER BY effective_from DESC, fee_rule_no DESC
    LIMIT 1
)
INSERT INTO bill (
    car_log_no,
    fee_rule_no,
    kiosk_no,
    charge_minutes,
    bill_amount,
    bill_status
)
SELECT
    inserted_log.car_log_no,
    selected_rule.fee_rule_no,
    NULL,
    0,
    0,
    'UNPAID'
FROM inserted_log
CROSS JOIN selected_rule;


-- 관리자 정산 목록에서 직접 지난 기록 이동을 확인할 수 있도록
-- B2 출차와 결제가 모두 완료된 정산서 1건을 생성한다.
WITH inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        camera.camera_no,
        NULL,
        '299가1205',
        '299가1205',
        CURRENT_TIMESTAMP - INTERVAL '26 hours',
        'camera-data/img_000118.jpeg',
        'camera-data/crop/img_000118.jpg',
        TRUE,
        99.00,
        'B2-BILLING-TRASH-TEST-1'
    FROM gate
    JOIN camera
        ON camera.gate_no = gate.gate_no
       AND camera.camera_type = 'In'
    WHERE gate.gate_code = 'B2-IN-1'
    RETURNING camera_data_no, capture_time
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        out_gate_no,
        out_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        NULL,
        inserted_camera.camera_data_no,
        in_gate.gate_no,
        inserted_camera.capture_time,
        out_gate.gate_no,
        CURRENT_TIMESTAMP - INTERVAL '5 minutes',
        1440,
        '299가1205',
        'VISIT'
    FROM inserted_camera
    JOIN gate in_gate
        ON in_gate.gate_code = 'B2-IN-1'
    JOIN gate out_gate
        ON out_gate.gate_code = 'B2-OUT-1'
    RETURNING car_log_no, out_time
), selected_rule AS (
    SELECT fee_rule_no
    FROM fee_rule
    WHERE effective_from <= CURRENT_TIMESTAMP
      AND (
          effective_to IS NULL
          OR effective_to > CURRENT_TIMESTAMP
      )
    ORDER BY effective_from DESC, fee_rule_no DESC
    LIMIT 1
)
INSERT INTO bill (
    car_log_no,
    fee_rule_no,
    kiosk_no,
    charge_minutes,
    bill_amount,
    bill_status,
    payment_order_id,
    payment_key,
    payment_method,
    issued_at,
    paid_at
)
SELECT
    inserted_log.car_log_no,
    selected_rule.fee_rule_no,
    NULL,
    60,
    2000,
    'PAID',
    'BILLING-TRASH-TEST-299GA1205',
    'BILLING-TRASH-TEST-KEY-299GA1205',
    '카드',
    inserted_log.out_time - INTERVAL '10 minutes',
    inserted_log.out_time - INTERVAL '5 minutes'
FROM inserted_log
CROSS JOIN selected_rule;


COMMIT;


-- =====================================================
-- TEST VEHICLES
-- B1 키오스크 번호는 기본 더미 기준 1 또는 2를 사용한다.
-- B2 키오스크 번호는 기본 더미 기준 3 또는 4를 사용한다.
--
-- 299가1101: B1 입주민 차량, 번호 뒤 4자리 1101
--             B1 일반 주차면에 배정되어 로봇 출차 작업 생성 가능
-- 299가1102: B1 입주민 차량, 번호 뒤 4자리 1102
--             B1 일반 주차면에 배정되어 로봇 출차 작업 생성 가능
-- 299가1103: B1 입주민 차량, 번호 뒤 4자리 1103
--             B1 일반 주차면에 배정되어 로봇 출차 작업 생성 가능
--
-- 299가1201: 방금 입차한 방문차량, 24시간 무료, 정산금액 0원 확인용
-- 299가1202: 12시간 주차한 방문차량, 24시간 무료 범위, 정산금액 0원 확인용
-- 299가1203: 26시간 주차한 방문차량, 24시간 초과분 과금 확인용
-- 299가1204: 미등록차량, 무료시간 없음, 입차 직후부터 과금 확인용
-- 299가1205: B2 출차·결제 완료 차량, 관리자 직접 지난 기록 이동 확인용
--
-- 실행 직후 B2 입차 차량의 미결제 정산서가 함께 생성된다.
-- B2 키오스크에서 차량을 선택하면 주차시간에 맞춰 정산금액이 갱신된다.
-- 결제를 완료하면 정산완료 및 30분간 출차 가능으로 바뀐다.
-- =====================================================
