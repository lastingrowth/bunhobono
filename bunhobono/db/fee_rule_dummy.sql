BEGIN;


-- 기존 요금 규칙과 이를 참조하는 정산서 데이터를 모두 비우고
-- SERIAL 번호를 1부터 다시 시작한다.
TRUNCATE TABLE
    fee_rule
RESTART IDENTITY CASCADE;


-- =====================================================
-- FEE RULE DUMMY
-- 방문차량의 무료시간 종료 후와 미등록차량의 입차 직후부터 적용할
-- 기본 시간당 주차요금 규칙을 등록한다.
--
-- 방문차량: car_log.free_time에 1,440분을 저장하여 24시간 무료 적용
-- 미등록차량: car_log.free_time에 0분을 저장하여 입차 직후부터 과금
-- =====================================================
INSERT INTO fee_rule (
    rule_name,                -- 요금 규칙명
    unit_minutes,             -- 요금이 한 번 부과되는 시간 단위(분)
    unit_fee,                 -- 시간 단위마다 부과되는 금액
    daily_max_fee,            -- 과금 24시간당 최대요금
    created_at,               -- 요금 규칙 등록 시각
	effective_from,           -- 요금 규칙 적용 시작시각
	effective_to			  -- 요금 규칙 적용 종료시각
)
VALUES (
    '일반 시간당 주차요금',
    30,                       -- 30분 단위
    1000,                     -- 30분당 1,000원
    15000,                    -- 과금 24시간당 최대 15,000원
    CURRENT_TIMESTAMP,
	CURRENT_TIMESTAMP,
	NULL
);


COMMIT;