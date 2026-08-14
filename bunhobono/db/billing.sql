BEGIN;

DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS fee_rule;

-- =====================================================
-- FEE RULE
-- 방문차량의 무료시간 종료 후와 미등록차량의 입차 직후부터 적용할
-- 주차요금 계산 규칙을 관리한다.
-- 요금 계산과 값 검증은 백엔드에서 처리한다.
-- 사용 중인 규칙은 직접 수정하지 않고 비활성화한 뒤
-- 새로운 규칙을 등록하여 기존 정산 기록의 계산 기준을 보존한다.
-- =====================================================
CREATE TABLE fee_rule (

    fee_rule_no SERIAL PRIMARY KEY,                         -- 요금 규칙 고유번호

    rule_name VARCHAR(100) NOT NULL UNIQUE,                 -- 요금 규칙을 구분하기 위한 이름

    unit_minutes INT NOT NULL,                              -- 요금이 한 번 부과되는 시간 단위(분)

    unit_fee NUMERIC(12, 0) NOT NULL,                       -- 시간 단위마다 부과되는 금액

    daily_max_fee NUMERIC(12, 0),                           -- 과금 24시간당 부과할 수 있는 최대요금
                                                             -- 최대요금 제한이 없으면 NULL

    active BOOLEAN NOT NULL DEFAULT TRUE,                   -- 현재 요금 규칙 사용 여부

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,                           -- 요금 규칙 등록 시각

	effective_from TIMESTAMP NOT NULL                     	-- 요금 규칙 적용 시작시각
);


-- =====================================================
-- BILL
-- 차량 입출차 기록별 주차요금과 결제 상태를 관리한다.
-- 별도의 결제 테이블을 만들지 않고 토스페이먼츠 테스트 결제 결과를
-- 정산서에 함께 저장한다.
-- =====================================================
CREATE TABLE bill (

    bill_no SERIAL PRIMARY KEY,                             -- 정산서 고유번호

    car_log_no INT NOT NULL UNIQUE,                         -- 정산 대상 차량 입출차 기록 고유번호
                                                             -- 입출차 기록 한 건당 정산서 한 건만 생성

    fee_rule_no INT NOT NULL,                               -- 정산에 적용된 요금 규칙 고유번호

    kiosk_no INT,                                           -- 결제가 진행된 키오스크 고유번호
                                                             -- 미결제 또는 요금 면제 정산이면 NULL 가능

    charge_minutes INT NOT NULL,                            -- 무료시간을 제외하고 요금 계산에 적용한 시간(분)

    bill_amount NUMERIC(12, 0) NOT NULL,                    -- 백엔드에서 계산한 최종 정산금액

    bill_status VARCHAR(20) NOT NULL
        DEFAULT 'UNPAID',                                   -- 정산 상태
                                                             -- UNPAID: 미결제
                                                             -- PAID: 결제완료

    payment_order_id VARCHAR(64) UNIQUE,                    -- 백엔드에서 생성한 토스페이먼츠 주문번호
                                                             -- 결제를 요청하기 전에는 NULL 가능

    payment_key VARCHAR(200) UNIQUE,                        -- 토스페이먼츠에서 발급한 결제 고유키
                                                             -- 결제 승인 전에는 NULL

    payment_method VARCHAR(30),                             -- 승인된 결제수단
                                                             -- 카드·간편결제 등
                                                             -- 결제 승인 전에는 NULL

    issued_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,                          -- 정산서 생성 시각

    paid_at TIMESTAMP,                                      -- 결제 승인 완료 시각
                                                             -- 미결제 상태이면 NULL

    CONSTRAINT fk_bill_car_log
        FOREIGN KEY (car_log_no)                            -- 정산서를 차량 입출차 기록과 연결
        REFERENCES car_log(car_log_no)
        ON DELETE RESTRICT,                                 -- 정산서가 존재하면 입출차 기록 삭제 제한

    CONSTRAINT fk_bill_fee_rule
        FOREIGN KEY (fee_rule_no)                           -- 정산서를 적용된 요금 규칙과 연결
        REFERENCES fee_rule(fee_rule_no)
        ON DELETE RESTRICT,                                 -- 사용된 요금 규칙 삭제 제한

    CONSTRAINT fk_bill_kiosk
        FOREIGN KEY (kiosk_no)                              -- 정산서를 결제가 진행된 키오스크와 연결
        REFERENCES kiosk(kiosk_no)
        ON DELETE RESTRICT                                  -- 정산 기록이 있으면 키오스크 삭제 제한
);


-- 미결제·결제완료 상태별 정산 목록을 생성 시각 역순으로 조회하는 데 사용한다.
CREATE INDEX idx_bill_status_issued_at
    ON bill(bill_status, issued_at DESC);


-- 키오스크별 결제 내역을 조회하는 데 사용한다.
CREATE INDEX idx_bill_kiosk_no
    ON bill(kiosk_no);


-- 완료된 정산 내역을 결제 시각 역순으로 조회하는 데 사용한다.
CREATE INDEX idx_bill_paid_at
    ON bill(paid_at DESC);


COMMIT;