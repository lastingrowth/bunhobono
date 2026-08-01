BEGIN;

-- =====================================================
-- 1. 주차요금 규칙
-- =====================================================
CREATE TABLE IF NOT EXISTS fee_rule (
    fee_rule_no SERIAL PRIMARY KEY,             -- 요금 규칙 번호
    rule_name VARCHAR(100) NOT NULL UNIQUE,     -- 요금 규칙명

    unit_minutes INT NOT NULL                   -- 요금 계산단위
        CHECK (unit_minutes > 0),

    unit_fee NUMERIC(12, 0) NOT NULL            -- 단위시간당 요금
        CHECK (unit_fee >= 0),

    daily_max_fee NUMERIC(12, 0)                -- 일 최대요금
        CHECK (
            daily_max_fee IS NULL
            OR daily_max_fee > 0
        ),

    active BOOLEAN NOT NULL DEFAULT TRUE        -- 사용 여부
);


-- =====================================================
-- 2. 결제 명세서
-- =====================================================
CREATE TABLE IF NOT EXISTS bill (
    bill_no BIGSERIAL PRIMARY KEY,              -- 명세서 번호

    car_log_no INT,                             -- 입출차 기록 번호
    fee_rule_no INT NOT NULL,                   -- 적용 요금 규칙

    snapshot_car_no VARCHAR(50) NOT NULL,       -- 차량번호

    snapshot_car_kind VARCHAR(20) NOT NULL      -- 차량 종류
        CHECK (snapshot_car_kind IN (
            'VISIT',                            -- 방문차량
            'UNKNOWN'                           -- 미등록차량
        )),

    charge_minutes INT NOT NULL                 -- 실제 과금시간
        CHECK (charge_minutes >= 0),

    bill_amount NUMERIC(12, 0) NOT NULL         -- 결제 금액
        CHECK (bill_amount >= 0),

    bill_status VARCHAR(20) NOT NULL
        DEFAULT 'UNPAID'                        -- 명세서 상태
        CHECK (bill_status IN (
            'UNPAID',                           -- 결제 전
            'PAID',                             -- 결제 완료
            'CANCELLED'                         -- 명세서 취소
        )),

    issued_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,              -- 생성 시각

    -- 입출차 기록이 삭제되어도 명세서는 유지한다.
    CONSTRAINT fk_bill_car_log
        FOREIGN KEY (car_log_no)
        REFERENCES car_log(car_log_no)
        ON DELETE SET NULL,

    -- 사용된 요금 규칙은 삭제할 수 없다.
    CONSTRAINT fk_bill_fee_rule
        FOREIGN KEY (fee_rule_no)
        REFERENCES fee_rule(fee_rule_no)
        ON DELETE RESTRICT
);

-- 입출차 기록별 명세서를 빠르게 조회한다.
CREATE INDEX IF NOT EXISTS idx_bill_car_log
    ON bill(car_log_no);

-- 같은 입출차 기록에는 미결제 명세서를 한 건만 허용한다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_bill_unpaid_car_log
    ON bill(car_log_no)
    WHERE bill_status = 'UNPAID'
      AND car_log_no IS NOT NULL;


-- =====================================================
-- 3. 카드 결제
-- =====================================================
CREATE TABLE IF NOT EXISTS payment (
    payment_no BIGSERIAL PRIMARY KEY,           -- 결제 번호

    bill_no BIGINT NOT NULL,                    -- 명세서 번호
    kiosk_no BIGINT NOT NULL,                   -- 키오스크 번호

    transaction_id VARCHAR(100) NOT NULL UNIQUE,-- 카드 거래번호

    paid_amount NUMERIC(12, 0) NOT NULL         -- 실제 결제 금액
        CHECK (paid_amount > 0),

    payment_status VARCHAR(20) NOT NULL
        DEFAULT 'COMPLETED'                     -- 결제 상태 (결제 완료/결제 취소)
        CHECK (payment_status IN (
            'COMPLETED',                       
            'CANCELLED'                       
        )),

    paid_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,              -- 정산 시각

    cancelled_at TIMESTAMP,                     -- 취소 시각

    -- 정산 후 10분 이내 출차 여부는 Spring에서 정산 시각과 출차 시각을 비교한다.
    -- 취소된 결제만 취소 시각을 기록한다.
    CONSTRAINT chk_payment_cancelled_at
        CHECK (
            (payment_status = 'COMPLETED'
                AND cancelled_at IS NULL)
            OR
            (payment_status = 'CANCELLED'
                AND cancelled_at IS NOT NULL)
        ),

    -- 결제된 명세서는 삭제할 수 없다.
    CONSTRAINT fk_payment_bill
        FOREIGN KEY (bill_no)
        REFERENCES bill(bill_no)
        ON DELETE RESTRICT,

    -- 결제 이력이 있는 키오스크는 삭제할 수 없다.
    CONSTRAINT fk_payment_kiosk
        FOREIGN KEY (kiosk_no)
        REFERENCES kiosk(kiosk_no)
        ON DELETE RESTRICT
);


-- =====================================================
-- 4. 중복 결제 방지
-- =====================================================

-- 명세서별 완료 결제는 한 건만 허용한다.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_completed_bill
    ON payment(bill_no)
    WHERE payment_status = 'COMPLETED';


COMMIT;
