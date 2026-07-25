BEGIN;

-- 공지사항 테이블.
CREATE TABLE IF NOT EXISTS board (
    board_no SERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    image_path VARCHAR(500),
    image_name VARCHAR(255),
    image_type VARCHAR(100),
    start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_board_period
        CHECK (end_at IS NULL OR end_at >= start_at)
);


DROP INDEX IF EXISTS idx_board_display_order;

ALTER TABLE board
DROP COLUMN IF EXISTS display_order;

-- 게시기간 조회와 최신 등록순 조회에 사용하는 인덱스를 생성.
CREATE INDEX IF NOT EXISTS idx_board_active_period
ON board (active, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_board_created_at
ON board (created_at DESC, board_no DESC);

COMMIT;