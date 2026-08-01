BEGIN;

TRUNCATE TABLE payment, bill, fee_rule RESTART IDENTITY CASCADE;

INSERT INTO fee_rule (
    rule_name,
    unit_minutes,
    unit_fee,
    daily_max_fee,
    active
) VALUES
    ('방문·미등록 공통 요금', 10, 500, 20000, TRUE);

WITH target_log AS (
    SELECT
        cl.car_log_no,
        cl.snapshot_car_no,
        cl.snapshot_car_kind,
        ROW_NUMBER() OVER (
            ORDER BY cl.in_time DESC NULLS LAST, cl.car_log_no DESC
        ) AS row_no
    FROM car_log cl
    WHERE cl.snapshot_car_kind IN ('VISIT', 'UNKNOWN')
      AND cl.snapshot_car_no IS NOT NULL
      AND cl.in_time IS NOT NULL
    ORDER BY cl.in_time DESC NULLS LAST, cl.car_log_no DESC
    LIMIT 10
), calculated AS (
    SELECT
        t.*,
        CASE
            WHEN t.snapshot_car_kind = 'VISIT' THEN 10 + (t.row_no * 5)::INT
            ELSE 60 + (t.row_no * 15)::INT
        END AS charge_minutes
    FROM target_log t
)
INSERT INTO bill (
    car_log_no,
    fee_rule_no,
    snapshot_car_no,
    snapshot_car_kind,
    charge_minutes,
    bill_amount,
    bill_status,
    issued_at
)
SELECT
    c.car_log_no,
    (SELECT fee_rule_no FROM fee_rule WHERE rule_name = '방문·미등록 공통 요금'),
    c.snapshot_car_no,
    c.snapshot_car_kind,
    c.charge_minutes,
    CEIL(c.charge_minutes::NUMERIC / 10) * 500,
    CASE WHEN c.row_no IN (4, 9) THEN 'UNPAID' ELSE 'PAID' END,
    CURRENT_TIMESTAMP - ((11 - c.row_no) * INTERVAL '10 minutes')
FROM calculated c
ORDER BY c.row_no;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM kiosk
        WHERE kiosk_type = 'NON_RESIDENT'
          AND active = TRUE
    ) THEN
        RAISE EXCEPTION '활성화된 NON_RESIDENT 키오스크가 필요합니다.';
    END IF;
END
$$;

WITH bill_target AS (
    SELECT
        b.*,
        ROW_NUMBER() OVER (ORDER BY b.bill_no) AS row_no
    FROM bill b
    ORDER BY b.bill_no
    LIMIT 10
), kiosk_target AS (
    SELECT
        k.kiosk_no,
        ROW_NUMBER() OVER (ORDER BY k.kiosk_no) AS row_no
    FROM kiosk k
    WHERE k.kiosk_type = 'NON_RESIDENT'
      AND k.active = TRUE
), kiosk_count AS (
    SELECT COUNT(*) AS total_count
    FROM kiosk_target
)
INSERT INTO payment (
    bill_no,
    kiosk_no,
    transaction_id,
    paid_amount,
    payment_status,
    paid_at,
    cancelled_at
)
SELECT
    b.bill_no,
    k.kiosk_no,
    'DEMO-TXN-' || LPAD(b.bill_no::TEXT, 4, '0'),
    b.bill_amount,
    CASE WHEN b.bill_no IN (4, 9) THEN 'CANCELLED' ELSE 'COMPLETED' END,
    b.issued_at + INTERVAL '2 minutes',
    CASE
        WHEN b.bill_no IN (4, 9)
        THEN b.issued_at + INTERVAL '7 minutes'
        ELSE NULL
    END
FROM bill_target b
CROSS JOIN kiosk_count kc
JOIN kiosk_target k
    ON k.row_no = MOD(b.row_no - 1, kc.total_count) + 1
ORDER BY b.bill_no;

COMMIT;
