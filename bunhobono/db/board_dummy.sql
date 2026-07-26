BEGIN;

-- 기존 공지사항을 모두 삭제하고 번호를 1번부터 다시 시작한다.
TRUNCATE TABLE board RESTART IDENTITY;

-- 엘리베이터 점검 공지를 등록한다.
INSERT INTO board (
    title,
    content,
    image_path,
    image_name,
    image_type,
    start_at,
    end_at,
    active,
    created_by,
    created_at,
    updated_at
) VALUES (
    '엘리베이터 점검 및 보수 안내',
    $content$
입주민 여러분의 안전하고 편리한 엘리베이터 이용을 위해 점검 및 보수를 실시합니다.

점검기간: 2025. 7. 27.(일) ~ 2025. 7. 31.(금)
점검시간: 10:00 ~ 16:00

7월 27일: 101동·102동
7월 28일: 201동·202동
7월 29일: 301동·302동
7월 30일: 401동·402동

점검 시간에는 엘리베이터 이용이 제한될 수 있으니 입주민 여러분의 양해를 부탁드립니다.
BunhoBono APT
$content$,
    'classpath:board-seed/elevator-inspection-poster.png',
    '엘레베이터점검포스터.png',
    'image/png',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    TRUE,
    'admin1',
    CURRENT_TIMESTAMP - INTERVAL '2 minutes',
    CURRENT_TIMESTAMP - INTERVAL '2 minutes'
);

-- 아파트 단수 공지를 등록한다.
INSERT INTO board (
    title,
    content,
    image_path,
    image_name,
    image_type,
    start_at,
    end_at,
    active,
    created_by,
    created_at,
    updated_at
) VALUES (
    '아파트 단수 안내',
    $content$
구내 수도 설비 점검으로 인해 동별 단수가 진행될 예정입니다.

단수기간: 2025. 7. 26.(월) ~ 2025. 7. 29.(목)
단수시간: 09:00 ~ 17:00

7월 26일: 101동·102동
7월 27일: 201동·202동
7월 28일: 301동·302동
7월 29일: 401동·402동

작업 진행 상황에 따라 단수 시간이 변경될 수 있으니 필요한 물을 미리 받아두시기 바랍니다.
BunhoBono APT
$content$,
    'classpath:board-seed/water-outage-poster.png',
    '단수안내포스터.png',
    'image/png',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    TRUE,
    'admin1',
    CURRENT_TIMESTAMP - INTERVAL '1 minute',
    CURRENT_TIMESTAMP - INTERVAL '1 minute'
);

-- 알뜰 나눔 장터 공지를 등록한다.
INSERT INTO board (
    title,
    content,
    image_path,
    image_name,
    image_type,
    start_at,
    end_at,
    active,
    created_by,
    created_at,
    updated_at
) VALUES (
    '알뜰 나눔 장터 행사 안내',
    $content$
입주민이 함께 나누고 소통하는 알뜰 나눔 장터를 개최합니다.

행사기간: 2025. 7. 31.(금) ~ 2025. 8. 2.(일)
행사시간: 11:00 ~ 18:00
행사장소: 아파트 내 별빛공원

도서, 의류, 생활용품과 장난감 등 이웃과 나누고 싶은 물품을 준비해 주세요.
입주민 여러분의 많은 참여를 부탁드립니다.
BunhoBono APT
$content$,
    'classpath:board-seed/sharing-market-poster.png',
    '알뜰장터포스터.png',
    'image/png',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    TRUE,
    'admin1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

COMMIT;
