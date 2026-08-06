-- member를 참조하므로 member보다 먼저 삭제되어야 함.
DROP TABLE IF EXISTS inquiry;

CREATE TABLE inquiry (
    inquiry_no SERIAL PRIMARY KEY,

    -- 문의 작성 입주민
    member_no INT NOT NULL,

    -- 재문의인 경우 최초 문의 번호
    root_inquiry_no INT,

    -- 문의
    category VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,

    -- 처리 상태
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',

    -- 관리자 답변
    answer_content TEXT,
    answered_by INT,
    answered_at TIMESTAMP,

    -- 문의 작성일
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inquiry_member
        FOREIGN KEY (member_no)
        REFERENCES member(member_no),

    CONSTRAINT fk_inquiry_root
        FOREIGN KEY (root_inquiry_no)
        REFERENCES inquiry(inquiry_no),

    CONSTRAINT fk_inquiry_answered_by
        FOREIGN KEY (answered_by)
        REFERENCES member(member_no)
);