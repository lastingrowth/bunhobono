DO $$
DECLARE
    v_camera_no INT;
    v_gate_no INT;

    v_normal_1 INT;
    v_normal_2 INT;
    v_normal_3 INT;
    v_visit_1 INT;
    v_visit_2 INT;
    v_visit_expired INT;

    v_camera_data_no INT;
    v_car_log_no INT;

    v_day DATE;
    v_month INT;
BEGIN
    /*
     * 기존 통계 테스트 더미 삭제
     * car_no가 STAT- 로 시작하는 데이터만 지운다.
     */
    DELETE FROM notice
    WHERE snapshot_captured_car_no LIKE 'STAT-%'
       OR snapshot_registered_car_no LIKE 'STAT-%'
       OR car_log_no IN (
            SELECT car_log_no
            FROM car_log
            WHERE snapshot_car_no LIKE 'STAT-%'
       );

    DELETE FROM car_log
    WHERE snapshot_car_no LIKE 'STAT-%'
       OR vehicle_car_no IN (
            SELECT vehicle_car_no
            FROM vehicle_car
            WHERE car_no LIKE 'STAT-%'
       );

    DELETE FROM camera_data
    WHERE car_no LIKE 'STAT-%'
       OR ocr_car_no LIKE 'STAT-%';

    DELETE FROM vehicle_car
    WHERE car_no LIKE 'STAT-%';

    /*
     * 기존 카메라/게이트 중 하나를 사용한다.
     * 카메라 데이터와 입출차 로그는 FK 때문에 camera_no, gate_no가 필요하다.
     */
    SELECT c.camera_no, g.gate_no
    INTO v_camera_no, v_gate_no
    FROM camera c
    JOIN gate g ON c.gate_no = g.gate_no
    ORDER BY c.camera_no
    LIMIT 1;

    IF v_camera_no IS NULL OR v_gate_no IS NULL THEN
        RAISE EXCEPTION 'camera / gate 데이터가 필요합니다. 카메라와 게이트 더미를 먼저 넣어주세요.';
    END IF;

    /*
     * 입주민 차량 더미
     */
    INSERT INTO vehicle_car (
        vehicle_type,
        car_no,
        vehicle_status,
        start_date,
        end_date,
        approved_at
    )
    VALUES
        ('normal', 'STAT-RES-001', 'APPROVED', '2026-01-01 00:00:00', '2026-12-31 23:59:59', CURRENT_TIMESTAMP)
    RETURNING vehicle_car_no INTO v_normal_1;

    INSERT INTO vehicle_car (
        vehicle_type,
        car_no,
        vehicle_status,
        start_date,
        end_date,
        approved_at
    )
    VALUES
        ('normal', 'STAT-RES-002', 'APPROVED', '2026-01-01 00:00:00', '2026-12-31 23:59:59', CURRENT_TIMESTAMP)
    RETURNING vehicle_car_no INTO v_normal_2;

    INSERT INTO vehicle_car (
        vehicle_type,
        car_no,
        vehicle_status,
        start_date,
        end_date,
        approved_at
    )
    VALUES
        ('normal', 'STAT-RES-003', 'APPROVED', '2026-01-01 00:00:00', '2026-12-31 23:59:59', CURRENT_TIMESTAMP)
    RETURNING vehicle_car_no INTO v_normal_3;

    /*
     * 방문 차량 더미
     */
    INSERT INTO vehicle_car (
        vehicle_type,
        car_no,
        vehicle_status,
        start_date,
        end_date,
        approved_at
    )
    VALUES
        ('visit', 'STAT-VISIT-001', 'APPROVED', '2026-07-20 08:00:00', '2026-07-20 18:00:00', CURRENT_TIMESTAMP)
    RETURNING vehicle_car_no INTO v_visit_1;

    INSERT INTO vehicle_car (
        vehicle_type,
        car_no,
        vehicle_status,
        start_date,
        end_date,
        approved_at
    )
    VALUES
        ('visit', 'STAT-VISIT-002', 'APPROVED', '2026-07-19 08:00:00', '2026-07-19 18:00:00', CURRENT_TIMESTAMP)
    RETURNING vehicle_car_no INTO v_visit_2;

    /*
     * 방문 만료 주의용 차량
     * 입차 후 하루는 안 지났지만, 등록 가능 시간이 이미 만료된 상태를 만든다.
     * realEndDate = car_log.in_time + (vehicle.end_date - vehicle.start_date)
     */
    INSERT INTO vehicle_car (
        vehicle_type,
        car_no,
        vehicle_status,
        start_date,
        end_date,
        approved_at
    )
    VALUES
        ('visit', 'STAT-VISIT-EXP', 'APPROVED', '2026-07-20 08:00:00', '2026-07-20 10:00:00', CURRENT_TIMESTAMP)
    RETURNING vehicle_car_no INTO v_visit_expired;

    /*
     * 현재 주차중 데이터
     * 도넛 그래프와 시간대별 주차 대수 확인용
     */

    -- 입주민 현재 주차 1
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        v_normal_1,
        'STAT-RES-001',
        'STAT-RES-001',
        '2026-07-20 06:30:00',
        '/stat-dummy/res-001.jpg',
        '/stat-dummy/res-001-crop.jpg',
        TRUE,
        98.10
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        v_normal_1,
        v_camera_data_no,
        v_gate_no,
        '2026-07-20 06:30:00',
        'STAT-RES-001'
    );

    -- 입주민 현재 주차 2
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        v_normal_2,
        'STAT-RES-002',
        'STAT-RES-002',
        '2026-07-20 09:20:00',
        '/stat-dummy/res-002.jpg',
        '/stat-dummy/res-002-crop.jpg',
        TRUE,
        97.20
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        v_normal_2,
        v_camera_data_no,
        v_gate_no,
        '2026-07-20 09:20:00',
        'STAT-RES-002'
    );

    -- 방문 차량 현재 주차
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        v_visit_1,
        'STAT-VISIT-001',
        'STAT-VISIT-001',
        '2026-07-20 11:00:00',
        '/stat-dummy/visit-001.jpg',
        '/stat-dummy/visit-001-crop.jpg',
        TRUE,
        95.30
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        v_visit_1,
        v_camera_data_no,
        v_gate_no,
        '2026-07-20 11:00:00',
        'STAT-VISIT-001'
    );

    -- 미등록 차량 현재 주차
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        NULL,
        'STAT-UNKNOWN-001',
        'STAT-UNKNOWN-001',
        '2026-07-20 10:10:00',
        '/stat-dummy/unknown-001.jpg',
        '/stat-dummy/unknown-001-crop.jpg',
        TRUE,
        91.70
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        NULL,
        v_camera_data_no,
        v_gate_no,
        '2026-07-20 10:10:00',
        'STAT-UNKNOWN-001'
    );

    /*
     * 방문 만료 주의용 현재 주차 로그
     * 2026-07-20 08:30 입차 + 2시간 허용 = 10:30 만료
     * 현재 시간이 2026-07-20 기준이면 만료 주의 수에 잡힌다.
     */
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        v_visit_expired,
        'STAT-VISIT-EXP',
        'STAT-VISIT-EXP',
        '2026-07-20 08:30:00',
        '/stat-dummy/visit-exp.jpg',
        '/stat-dummy/visit-exp-crop.jpg',
        TRUE,
        94.80
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        v_visit_expired,
        v_camera_data_no,
        v_gate_no,
        '2026-07-20 08:30:00',
        'STAT-VISIT-EXP'
    );

    /*
     * 평균 주차 시간 확인용 출차 완료 데이터
     */

    -- 방문 차량 4시간 45분
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        v_visit_2,
        'STAT-VISIT-002',
        'STAT-VISIT-002',
        '2026-07-18 09:00:00',
        '/stat-dummy/visit-002.jpg',
        '/stat-dummy/visit-002-crop.jpg',
        TRUE,
        96.40
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        out_gate_no,
        out_time,
        snapshot_car_no
    )
    VALUES (
        v_visit_2,
        v_camera_data_no,
        v_gate_no,
        '2026-07-18 09:00:00',
        v_gate_no,
        '2026-07-18 13:45:00',
        'STAT-VISIT-002'
    );

    -- 미등록 차량 11시간 30분
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        NULL,
        'STAT-UNKNOWN-002',
        'STAT-UNKNOWN-002',
        '2026-07-17 08:00:00',
        '/stat-dummy/unknown-002.jpg',
        '/stat-dummy/unknown-002-crop.jpg',
        TRUE,
        90.50
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        out_gate_no,
        out_time,
        snapshot_car_no
    )
    VALUES (
        NULL,
        v_camera_data_no,
        v_gate_no,
        '2026-07-17 08:00:00',
        v_gate_no,
        '2026-07-17 19:30:00',
        'STAT-UNKNOWN-002'
    );

    /*
     * 주간 / 월간 그래프 확인용 데이터
     * 2026-07-14 ~ 2026-07-20
     */
    FOR v_day IN
        SELECT generate_series('2026-07-14'::date, '2026-07-20'::date, '1 day'::interval)::date
    LOOP
        -- 입주민 입차
        INSERT INTO camera_data (
            camera_no,
            vehicle_car_no,
            car_no,
            ocr_car_no,
            capture_time,
            image_path,
            crop_image_path,
            recognition_state,
            confidence_score
        )
        VALUES (
            v_camera_no,
            v_normal_3,
            'STAT-RES-003',
            'STAT-RES-003',
            v_day + TIME '08:00:00',
            '/stat-dummy/weekly-res.jpg',
            '/stat-dummy/weekly-res-crop.jpg',
            TRUE,
            97.00
        )
        RETURNING camera_data_no INTO v_camera_data_no;

        INSERT INTO car_log (
            vehicle_car_no,
            camera_data_no,
            in_gate_no,
            in_time,
            out_gate_no,
            out_time,
            snapshot_car_no
        )
        VALUES (
            v_normal_3,
            v_camera_data_no,
            v_gate_no,
            v_day + TIME '08:00:00',
            v_gate_no,
            v_day + TIME '09:30:00',
            'STAT-RES-003'
        );

        -- 방문 입차
        INSERT INTO camera_data (
            camera_no,
            vehicle_car_no,
            car_no,
            ocr_car_no,
            capture_time,
            image_path,
            crop_image_path,
            recognition_state,
            confidence_score
        )
        VALUES (
            v_camera_no,
            v_visit_1,
            'STAT-VISIT-001',
            'STAT-VISIT-001',
            v_day + TIME '10:00:00',
            '/stat-dummy/weekly-visit.jpg',
            '/stat-dummy/weekly-visit-crop.jpg',
            TRUE,
            95.00
        )
        RETURNING camera_data_no INTO v_camera_data_no;

        INSERT INTO car_log (
            vehicle_car_no,
            camera_data_no,
            in_gate_no,
            in_time,
            out_gate_no,
            out_time,
            snapshot_car_no
        )
        VALUES (
            v_visit_1,
            v_camera_data_no,
            v_gate_no,
            v_day + TIME '10:00:00',
            v_gate_no,
            v_day + TIME '12:00:00',
            'STAT-VISIT-001'
        );

        -- 홀수 날짜에는 미등록 입차도 추가해서 비입주민 막대가 달라지게 만든다.
        IF EXTRACT(DAY FROM v_day)::INT % 2 = 1 THEN
            INSERT INTO camera_data (
                camera_no,
                vehicle_car_no,
                car_no,
                ocr_car_no,
                capture_time,
                image_path,
                crop_image_path,
                recognition_state,
                confidence_score
            )
            VALUES (
                v_camera_no,
                NULL,
                'STAT-UNKNOWN-W' || TO_CHAR(v_day, 'DD'),
                'STAT-UNKNOWN-W' || TO_CHAR(v_day, 'DD'),
                v_day + TIME '14:00:00',
                '/stat-dummy/weekly-unknown.jpg',
                '/stat-dummy/weekly-unknown-crop.jpg',
                TRUE,
                89.00
            )
            RETURNING camera_data_no INTO v_camera_data_no;

            INSERT INTO car_log (
                vehicle_car_no,
                camera_data_no,
                in_gate_no,
                in_time,
                out_gate_no,
                out_time,
                snapshot_car_no
            )
            VALUES (
                NULL,
                v_camera_data_no,
                v_gate_no,
                v_day + TIME '14:00:00',
                v_gate_no,
                v_day + TIME '16:00:00',
                'STAT-UNKNOWN-W' || TO_CHAR(v_day, 'DD')
            );
        END IF;
    END LOOP;

    /*
     * 연간 그래프 확인용 데이터
     * 1월 ~ 12월에 입주민/비입주민 데이터가 하나씩 들어가도록 만든다.
     */
    FOR v_month IN 1..12 LOOP
        INSERT INTO camera_data (
            camera_no,
            vehicle_car_no,
            car_no,
            ocr_car_no,
            capture_time,
            image_path,
            crop_image_path,
            recognition_state,
            confidence_score
        )
        VALUES (
            v_camera_no,
            v_normal_3,
            'STAT-RES-003',
            'STAT-RES-003',
            MAKE_TIMESTAMP(2026, v_month, 10, 8, 0, 0),
            '/stat-dummy/year-res.jpg',
            '/stat-dummy/year-res-crop.jpg',
            TRUE,
            97.00
        )
        RETURNING camera_data_no INTO v_camera_data_no;

        INSERT INTO car_log (
            vehicle_car_no,
            camera_data_no,
            in_gate_no,
            in_time,
            out_gate_no,
            out_time,
            snapshot_car_no
        )
        VALUES (
            v_normal_3,
            v_camera_data_no,
            v_gate_no,
            MAKE_TIMESTAMP(2026, v_month, 10, 8, 0, 0),
            v_gate_no,
            MAKE_TIMESTAMP(2026, v_month, 10, 10, 0, 0),
            'STAT-RES-003'
        );

        INSERT INTO camera_data (
            camera_no,
            vehicle_car_no,
            car_no,
            ocr_car_no,
            capture_time,
            image_path,
            crop_image_path,
            recognition_state,
            confidence_score
        )
        VALUES (
            v_camera_no,
            v_visit_1,
            'STAT-VISIT-001',
            'STAT-VISIT-001',
            MAKE_TIMESTAMP(2026, v_month, 15, 11, 0, 0),
            '/stat-dummy/year-visit.jpg',
            '/stat-dummy/year-visit-crop.jpg',
            TRUE,
            95.00
        )
        RETURNING camera_data_no INTO v_camera_data_no;

        INSERT INTO car_log (
            vehicle_car_no,
            camera_data_no,
            in_gate_no,
            in_time,
            out_gate_no,
            out_time,
            snapshot_car_no
        )
        VALUES (
            v_visit_1,
            v_camera_data_no,
            v_gate_no,
            MAKE_TIMESTAMP(2026, v_month, 15, 11, 0, 0),
            v_gate_no,
            MAKE_TIMESTAMP(2026, v_month, 15, 13, 0, 0),
            'STAT-VISIT-001'
        );
    END LOOP;

    /*
     * 장기주차 알림 확인용 데이터
     * 통계 오른쪽 위 카드와 알림 관리 이동 확인용
     */

    -- 방문 장기주차 알림
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        v_visit_2,
        'STAT-VISIT-002',
        'STAT-VISIT-002',
        '2026-07-18 07:00:00',
        '/stat-dummy/notice-visit.jpg',
        '/stat-dummy/notice-visit-crop.jpg',
        TRUE,
        94.00
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        v_visit_2,
        v_camera_data_no,
        v_gate_no,
        '2026-07-18 07:00:00',
        'STAT-VISIT-002'
    )
    RETURNING car_log_no INTO v_car_log_no;

    INSERT INTO notice (
        car_log_no,
        detect_at,
        stay_days,
        alert_stat,
        snapshot_car_log_no,
        snapshot_registered_car_no,
        snapshot_captured_car_no,
        snapshot_car_kind,
        snapshot_parking_name,
        snapshot_in_time
    )
    VALUES (
        v_car_log_no,
        '2026-07-19 07:10:00',
        1,
        'Unresolved',
        v_car_log_no,
        'STAT-VISIT-002',
        'STAT-VISIT-002',
        'VISIT',
        '통계 더미 주차장',
        '2026-07-18 07:00:00'
    );

    -- 비등록 장기주차 알림
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score
    )
    VALUES (
        v_camera_no,
        NULL,
        'STAT-UNKNOWN-NOTICE',
        'STAT-UNKNOWN-NOTICE',
        '2026-07-18 06:00:00',
        '/stat-dummy/notice-unknown.jpg',
        '/stat-dummy/notice-unknown-crop.jpg',
        TRUE,
        88.00
    )
    RETURNING camera_data_no INTO v_camera_data_no;

    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no
    )
    VALUES (
        NULL,
        v_camera_data_no,
        v_gate_no,
        '2026-07-18 06:00:00',
        'STAT-UNKNOWN-NOTICE'
    )
    RETURNING car_log_no INTO v_car_log_no;

    INSERT INTO notice (
        car_log_no,
        detect_at,
        stay_days,
        alert_stat,
        snapshot_car_log_no,
        snapshot_registered_car_no,
        snapshot_captured_car_no,
        snapshot_car_kind,
        snapshot_parking_name,
        snapshot_in_time
    )
    VALUES (
        v_car_log_no,
        '2026-07-19 06:10:00',
        1,
        'Unresolved',
        v_car_log_no,
        NULL,
        'STAT-UNKNOWN-NOTICE',
        'UNKNOWN',
        '통계 더미 주차장',
        '2026-07-18 06:00:00'
    );
END $$;