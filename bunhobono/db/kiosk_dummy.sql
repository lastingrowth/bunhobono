BEGIN;

TRUNCATE TABLE kiosk RESTART IDENTITY CASCADE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM parking WHERE parking_no = 1)
       OR NOT EXISTS (SELECT 1 FROM parking WHERE parking_no = 2) THEN
        RAISE EXCEPTION 'parking_no 1과 2가 먼저 등록되어야 합니다.';
    END IF;
END
$$;

INSERT INTO kiosk (
    kiosk_name,
    kiosk_type,
    parking_no,
    active
) VALUES
    ('B1 입주민 키오스크 1', 'RESIDENT',     1, TRUE),
    ('B1 입주민 키오스크 2', 'RESIDENT',     1, TRUE),
    ('B2 방문·미등록 키오스크 1', 'NON_RESIDENT', 2, TRUE),
    ('B2 방문·미등록 키오스크 2', 'NON_RESIDENT', 2, TRUE);

COMMIT;
