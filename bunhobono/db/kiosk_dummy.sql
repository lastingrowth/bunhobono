BEGIN;

TRUNCATE TABLE kiosk RESTART IDENTITY CASCADE;

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
