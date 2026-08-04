BEGIN;

-- 키오스크 장비 정보
CREATE TABLE IF NOT EXISTS kiosk (
    kiosk_no BIGSERIAL PRIMARY KEY,             -- 키오스크 내부 고유번호

    kiosk_name VARCHAR(100) NOT NULL,           -- 관리자 화면에 표시할 키오스크 이름
    -- B1 키오스크1
    -- B1 키오스크2
    -- B2 키오스크1
    -- B2 키오스크2

    kiosk_type VARCHAR(30) NOT NULL             -- 키오스크 타입 (입주민,미등록/방문)
        CHECK (kiosk_type IN (
            'RESIDENT',
            'NON_RESIDENT'
        )),

    parking_no INT NOT NULL,                   -- 키오스크가 설치된 주차장 번호 (B1,B2)

    active BOOLEAN NOT NULL DEFAULT TRUE,      -- 현재 사용 여부

    -- 키오스크가 설치된 주차장이 먼저 삭제되지 않도록 제한한다.
    CONSTRAINT fk_kiosk_parking
        FOREIGN KEY (parking_no)
        REFERENCES parking(parking_no)
        ON DELETE RESTRICT
);

COMMIT;
