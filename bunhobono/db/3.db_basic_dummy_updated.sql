BEGIN;
-- 기존 데이터를 모두 비우고 SERIAL 번호를 1부터 다시 시작한다.
TRUNCATE TABLE
    trash_bin,
    notice,
    board,
    camera_pdm,
    gate_pdm,
    robot_pdm,
    robot_log,
    robot_task,
    robot,
    parking_space,
    car_log,
    camera_data,
    camera,
    vehicle_car,
    gate,
    parking,
    member_archive,
    member,
    apartment_unit,
    faq,
    fee_rule
RESTART IDENTITY CASCADE;

INSERT INTO apartment_unit (dong, ho, unit_status)
SELECT
    d.dong,
    (f.floor_no * 100) + l.line_no,
    'EMPTY'
FROM unnest(ARRAY[101, 102, 201, 202, 301, 302, 401, 402]) AS d(dong)
CROSS JOIN generate_series(1, 10) AS f(floor_no)
CROSS JOIN generate_series(1, 2) AS l(line_no);

CREATE TEMP TABLE legacy_member_seed (
    seed_order SERIAL PRIMARY KEY,
    login_id VARCHAR(30),
    login_pwd VARCHAR(100),
    mem_dong INT,
    mem_ho INT,
    mem_name VARCHAR(30),
    mem_phone VARCHAR(30),
    role VARCHAR(30),
    create_at TIMESTAMP,
    delete_at TIMESTAMP,
    mem_status VARCHAR(30)
);

INSERT INTO legacy_member_seed
    (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone,
     role, create_at, delete_at, mem_status)
VALUES
    -- << 관리자 >> 
    -- ACTIVE: 근무
    ('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', NULL, NULL, '관리자', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', NULL, 'ACTIVE'),
    ('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', NULL, NULL, '경비원', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', NULL, 'ACTIVE'),
    -- ON_LEAVE: 휴직
    ('admin3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', NULL, NULL, '휴직자', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', NULL, 'ON_LEAVE'),
    -- INACTIVE: 퇴사
    ('admin4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', NULL, NULL, '퇴사자', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', TIMESTAMP '2025-12-31 18:00:00', 'INACTIVE'),


    -- << 입주민 >> 
    -- [101동]
    -- 거주
    ('res1',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 101,  '마틴',             '010-2222-0101', 'RESIDENT', TIMESTAMP '2025-04-08 10:00:00', NULL,                              'ACTIVE'),
    -- 가입 승인 대기
    ('res2',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 201,  '제임스',           '010-2222-0102', 'RESIDENT', TIMESTAMP '2026-07-18 11:20:00', NULL,                              'PENDING'),
    -- 전출 신청
    ('res3',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 301,  '오드리',           '010-2222-0103', 'RESIDENT', TIMESTAMP '2025-06-12 10:00:00', TIMESTAMP '2026-07-15 18:20:00', 'WITHDRAW_PENDING'),
    -- 101동 401호 빈 세대는 apartment_unit에만 존재한다.
    ('res5',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 501,  '칼',               '010-2222-0201', 'RESIDENT', TIMESTAMP '2025-02-14 10:00:00', NULL,                              'ACTIVE'),
    ('res6',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 601,  '찰스',             '010-2222-0202', 'RESIDENT', TIMESTAMP '2025-03-03 10:00:00', NULL,                              'ACTIVE'),
    ('res7',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 701,  '마이클',           '010-2222-0203', 'RESIDENT', TIMESTAMP '2025-03-27 10:00:00', NULL,                              'ACTIVE'),
    ('res8',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 801,  '케빈',             '010-2222-0204', 'RESIDENT', TIMESTAMP '2025-04-19 10:00:00', NULL,                              'ACTIVE'),
    ('res9',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 901,  '오스틴',           '010-2222-0301', 'RESIDENT', TIMESTAMP '2025-05-11 10:00:00', NULL,                              'ACTIVE'),
    ('res10',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1001, '토니스타크',       '010-3000-0010', 'RESIDENT', TIMESTAMP '2025-06-02 10:00:00', NULL,                              'ACTIVE'),
    ('res11',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 102,  '토르',             '010-3000-0011', 'RESIDENT', TIMESTAMP '2025-06-23 10:00:00', NULL,                              'ACTIVE'),
    ('res12',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 202,  '로키',             '010-3000-0012', 'RESIDENT', TIMESTAMP '2025-07-15 10:00:00', NULL,                              'ACTIVE'),
    ('res13',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 302,  '스티브로저스',     '010-3000-0013', 'RESIDENT', TIMESTAMP '2025-08-04 10:00:00', NULL,                              'ACTIVE'),
    ('res14',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 402,  '나타샤로마노프',   '010-3000-0014', 'RESIDENT', TIMESTAMP '2025-08-26 10:00:00', NULL,                              'ACTIVE'),
    ('res15',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 502,  '클린트바튼',       '010-3000-0015', 'RESIDENT', TIMESTAMP '2025-09-17 10:00:00', NULL,                              'ACTIVE'),
    ('res16',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 602,  '브루스배너',       '010-3000-0016', 'RESIDENT', TIMESTAMP '2025-10-09 10:00:00', NULL,                              'ACTIVE'),
    ('res17',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 702,  '피터파커',         '010-3000-0017', 'RESIDENT', TIMESTAMP '2025-11-01 10:00:00', NULL,                              'ACTIVE'),
    ('res18',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 802,  '스티븐스트레인지', '010-3000-0018', 'RESIDENT', TIMESTAMP '2025-11-23 10:00:00', NULL,                              'ACTIVE'),
    ('res19',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 902,  '완다막시모프',     '010-3000-0019', 'RESIDENT', TIMESTAMP '2025-12-12 10:00:00', NULL,                              'ACTIVE'),
    ('res20',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1002, '티파록하트',       '010-3000-0152', 'RESIDENT', TIMESTAMP '2026-01-08 10:00:00', NULL,                              'ACTIVE'),

    -- [102동]
    -- 입주민만
    ('res21',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 101,  '비전',       '010-3000-0020', 'RESIDENT', TIMESTAMP '2025-01-13 10:00:00', NULL,                              'ACTIVE'),
    ('res22',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 201,  '샘윌슨',     '010-3000-0021', 'RESIDENT', TIMESTAMP '2025-01-27 10:00:00', NULL,                              'ACTIVE'),
    ('res23',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 301,  '버키반즈',   '010-3000-0022', 'RESIDENT', TIMESTAMP '2025-02-10 10:00:00', NULL,                              'ACTIVE'),
    ('res24',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 401,  '티찰라',     '010-3000-0023', 'RESIDENT', TIMESTAMP '2025-02-24 10:00:00', NULL,                              'ACTIVE'),
    ('res25',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 501,  '캐럴댄버스', '010-3000-0024', 'RESIDENT', TIMESTAMP '2025-03-10 10:00:00', NULL,                              'ACTIVE'),
    ('res26',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 601,  '스콧랭',     '010-3000-0025', 'RESIDENT', TIMESTAMP '2025-03-24 10:00:00', NULL,                              'ACTIVE'),
    ('res27',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 701,  '호프반다인', '010-3000-0026', 'RESIDENT', TIMESTAMP '2025-04-07 10:00:00', NULL,                              'ACTIVE'),
    ('res28',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 801,  '피터퀼',     '010-3000-0027', 'RESIDENT', TIMESTAMP '2025-04-21 10:00:00', NULL,                              'ACTIVE'),
    ('res29',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 901,  '가모라',     '010-3000-0028', 'RESIDENT', TIMESTAMP '2025-05-05 10:00:00', NULL,                              'ACTIVE'),
    ('res30',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1001, '드랙스',     '010-3000-0029', 'RESIDENT', TIMESTAMP '2025-05-19 10:00:00', NULL,                              'ACTIVE'),
    ('res31',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 102,  '로켓',       '010-3000-0030', 'RESIDENT', TIMESTAMP '2025-06-02 10:00:00', NULL,                              'ACTIVE'),
    ('res32',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 202,  '그루트',     '010-3000-0031', 'RESIDENT', TIMESTAMP '2025-06-16 10:00:00', NULL,                              'ACTIVE'),
    ('res33',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 302,  '네뷸라',     '010-3000-0032', 'RESIDENT', TIMESTAMP '2025-06-30 10:00:00', NULL,                              'ACTIVE'),
    ('res34',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 402,  '웨이드윌슨', '010-3000-0033', 'RESIDENT', TIMESTAMP '2025-07-14 10:00:00', NULL,                              'ACTIVE'),
    ('res35',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 502,  '로건',       '010-3000-0034', 'RESIDENT', TIMESTAMP '2025-07-28 10:00:00', NULL,                              'ACTIVE'),
    ('res36',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 602,  '찰스자비에', '010-3000-0035', 'RESIDENT', TIMESTAMP '2025-08-11 10:00:00', NULL,                              'ACTIVE'),
    ('res37',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 702,  '에릭렌셔',   '010-3000-0036', 'RESIDENT', TIMESTAMP '2025-08-25 10:00:00', NULL,                              'ACTIVE'),
    ('res38',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 802,  '브루스웨인', '010-3000-0037', 'RESIDENT', TIMESTAMP '2025-09-08 10:00:00', NULL,                              'ACTIVE'),
    ('res39',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 902,  '클라크켄트', '010-3000-0038', 'RESIDENT', TIMESTAMP '2025-09-22 10:00:00', NULL,                              'ACTIVE'),
    -- [201동]
    -- 거주 15세대 / 가입 승인 대기 5세대
    ('res42',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 201,  '배리앨런',       '010-3000-0040', 'RESIDENT', TIMESTAMP '2025-02-17 10:00:00', NULL,                              'ACTIVE'),
    ('res43',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 301,  '아서커리',       '010-3000-0041', 'RESIDENT', TIMESTAMP '2025-03-03 10:00:00', NULL,                              'ACTIVE'),
    ('res41',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 101,  '다이애나프린스', '010-3000-0039', 'RESIDENT', TIMESTAMP '2025-02-03 10:00:00', NULL,                              'ACTIVE'),
    ('res44',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 401,  '할조던',         '010-3000-0042', 'RESIDENT', TIMESTAMP '2025-03-17 10:00:00', NULL,                              'ACTIVE'),
    ('res45',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 501,  '딕그레이슨',     '010-3000-0043', 'RESIDENT', TIMESTAMP '2025-03-31 10:00:00', NULL,                              'ACTIVE'),

    -- 가입 승인 대기
    ('res46',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 601,  '셀리나카일',     '010-3000-0044', 'RESIDENT', TIMESTAMP '2026-07-16 09:20:00', NULL,                              'PENDING'),
    ('res47',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 701,  '할리퀸',         '010-3000-0045', 'RESIDENT', TIMESTAMP '2026-07-17 10:20:00', NULL,                              'PENDING'),
    ('res48',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 801,  '존콘스탄틴',     '010-3000-0046', 'RESIDENT', TIMESTAMP '2026-07-18 11:20:00', NULL,                              'PENDING'),
    ('res49',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 901,  '고죠사토루',     '010-3000-0047', 'RESIDENT', TIMESTAMP '2026-07-19 12:20:00', NULL,                              'PENDING'),
    ('res50',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 1001, '이타도리유지',   '010-3000-0048', 'RESIDENT', TIMESTAMP '2026-07-20 13:20:00', NULL,                              'PENDING'),

    ('res51',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 102,  '후시구로메구미', '010-3000-0049', 'RESIDENT', TIMESTAMP '2025-04-14 10:00:00', NULL,                              'ACTIVE'),
    ('res52',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 202,  '쿠기사키노바라', '010-3000-0050', 'RESIDENT', TIMESTAMP '2025-04-28 10:00:00', NULL,                              'ACTIVE'),
    ('res53',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 302,  '료멘스쿠나',     '010-3000-0051', 'RESIDENT', TIMESTAMP '2025-05-12 10:00:00', NULL,                              'ACTIVE'),
    ('res54',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 402,  '나나미켄토',     '010-3000-0052', 'RESIDENT', TIMESTAMP '2025-05-26 10:00:00', NULL,                              'ACTIVE'),
    ('res55',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 502,  '이에이리쇼코',   '010-3000-0053', 'RESIDENT', TIMESTAMP '2025-06-09 10:00:00', NULL,                              'ACTIVE'),
    ('res56',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 602,  '게토스구루',     '010-3000-0054', 'RESIDENT', TIMESTAMP '2025-06-23 10:00:00', NULL,                              'ACTIVE'),
    ('res57',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 702,  '하타케카카시',   '010-3000-0055', 'RESIDENT', TIMESTAMP '2025-07-07 10:00:00', NULL,                              'ACTIVE'),
    ('res58',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 802,  '우즈마키나루토', '010-3000-0056', 'RESIDENT', TIMESTAMP '2025-07-21 10:00:00', NULL,                              'ACTIVE'),
    ('res59',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 902,  '우치하사스케',   '010-3000-0057', 'RESIDENT', TIMESTAMP '2025-08-04 10:00:00', NULL,                              'ACTIVE'),
    ('res60',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 1002, '하루노사쿠라',   '010-3000-0058', 'RESIDENT', TIMESTAMP '2025-08-18 10:00:00', NULL,                              'ACTIVE'),

    -- [202동]
    -- 거주 20세대
    ('res61',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 101,  '하루노사쿠라',   '010-3000-0058', 'RESIDENT', TIMESTAMP '2025-01-16 10:00:00', NULL, 'ACTIVE'),
    ('res62',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 201,  '지라이야',       '010-3000-0059', 'RESIDENT', TIMESTAMP '2025-01-30 10:00:00', NULL, 'ACTIVE'),
    ('res63',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 301,  '츠나데',         '010-3000-0060', 'RESIDENT', TIMESTAMP '2025-02-13 10:00:00', NULL, 'ACTIVE'),
    ('res64',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 401,  '우치하이타치',   '010-3000-0061', 'RESIDENT', TIMESTAMP '2025-02-27 10:00:00', NULL, 'ACTIVE'),
    ('res65',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 501,  '가아라',         '010-3000-0062', 'RESIDENT', TIMESTAMP '2025-03-13 10:00:00', NULL, 'ACTIVE'),
    ('res66',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 601,  '록리',           '010-3000-0063', 'RESIDENT', TIMESTAMP '2025-03-27 10:00:00', NULL, 'ACTIVE'),
    ('res67',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 701,  '휴우가히나타',   '010-3000-0064', 'RESIDENT', TIMESTAMP '2025-04-10 10:00:00', NULL, 'ACTIVE'),
    ('res68',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 801,  '몽키디루피',     '010-3000-0065', 'RESIDENT', TIMESTAMP '2025-04-24 10:00:00', NULL, 'ACTIVE'),
    ('res69',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 901,  '롤로노아조로',   '010-3000-0066', 'RESIDENT', TIMESTAMP '2025-05-08 10:00:00', NULL, 'ACTIVE'),
    ('res70',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 1001, '나미',           '010-3000-0067', 'RESIDENT', TIMESTAMP '2025-05-22 10:00:00', NULL, 'ACTIVE'),
    ('res71',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 102,  '상디',           '010-3000-0068', 'RESIDENT', TIMESTAMP '2025-06-05 10:00:00', NULL, 'ACTIVE'),
    ('res72',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 202,  '니코로빈',       '010-3000-0069', 'RESIDENT', TIMESTAMP '2025-06-19 10:00:00', NULL, 'ACTIVE'),
    ('res73',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 302,  '우솝',           '010-3000-0070', 'RESIDENT', TIMESTAMP '2025-07-03 10:00:00', NULL, 'ACTIVE'),
    ('res74',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 402,  '토니토니쵸파',   '010-3000-0071', 'RESIDENT', TIMESTAMP '2025-07-17 10:00:00', NULL, 'ACTIVE'),
    ('res75',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 502,  '프랑키',         '010-3000-0072', 'RESIDENT', TIMESTAMP '2025-07-31 10:00:00', NULL, 'ACTIVE'),
    ('res76',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 602,  '브룩',           '010-3000-0073', 'RESIDENT', TIMESTAMP '2025-08-14 10:00:00', NULL, 'ACTIVE'),
    ('res77',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 702,  '징베',           '010-3000-0074', 'RESIDENT', TIMESTAMP '2025-08-28 10:00:00', NULL, 'ACTIVE'),
    ('res78',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 802,  '샹크스',         '010-3000-0075', 'RESIDENT', TIMESTAMP '2025-09-11 10:00:00', NULL, 'ACTIVE'),
    ('res79',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 902,  '트라팔가로',     '010-3000-0076', 'RESIDENT', TIMESTAMP '2025-09-25 10:00:00', NULL, 'ACTIVE'),
    ('res80',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 1002, '포트거스에이스', '010-3000-0077', 'RESIDENT', TIMESTAMP '2025-10-09 10:00:00', NULL, 'ACTIVE'),

    -- [301동]
    -- 거주 20세대
    ('res81',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 101,  '포트거스에이스',   '010-3000-0077', 'RESIDENT', TIMESTAMP '2025-01-20 10:00:00', NULL, 'ACTIVE'),
    ('res82',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 201,  '사보',             '010-3000-0078', 'RESIDENT', TIMESTAMP '2025-02-03 10:00:00', NULL, 'ACTIVE'),
    ('res83',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 301,  '손오공',           '010-3000-0079', 'RESIDENT', TIMESTAMP '2025-02-17 10:00:00', NULL, 'ACTIVE'),
    ('res84',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 401,  '베지터',           '010-3000-0080', 'RESIDENT', TIMESTAMP '2025-03-03 10:00:00', NULL, 'ACTIVE'),
    ('res85',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 501,  '손오반',           '010-3000-0081', 'RESIDENT', TIMESTAMP '2025-03-17 10:00:00', NULL, 'ACTIVE'),
    ('res86',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 601,  '피콜로',           '010-3000-0082', 'RESIDENT', TIMESTAMP '2025-03-31 10:00:00', NULL, 'ACTIVE'),
    ('res87',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 701,  '트랭크스',         '010-3000-0083', 'RESIDENT', TIMESTAMP '2025-04-14 10:00:00', NULL, 'ACTIVE'),
    ('res88',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 801,  '부르마',           '010-3000-0084', 'RESIDENT', TIMESTAMP '2025-04-28 10:00:00', NULL, 'ACTIVE'),
    ('res89',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 901,  '크리링',           '010-3000-0085', 'RESIDENT', TIMESTAMP '2025-05-12 10:00:00', NULL, 'ACTIVE'),
    ('res90',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 1001, '프리저',           '010-3000-0086', 'RESIDENT', TIMESTAMP '2025-05-26 10:00:00', NULL, 'ACTIVE'),
    ('res91',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 102,  '비루스',           '010-3000-0087', 'RESIDENT', TIMESTAMP '2025-06-09 10:00:00', NULL, 'ACTIVE'),
    ('res92',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 202,  '카마도탄지로',     '010-3000-0088', 'RESIDENT', TIMESTAMP '2025-06-23 10:00:00', NULL, 'ACTIVE'),
    ('res93',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 302,  '카마도네즈코',     '010-3000-0089', 'RESIDENT', TIMESTAMP '2025-07-07 10:00:00', NULL, 'ACTIVE'),
    ('res94',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 402,  '아가츠마젠이츠',   '010-3000-0090', 'RESIDENT', TIMESTAMP '2025-07-21 10:00:00', NULL, 'ACTIVE'),
    ('res95',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 502,  '하시비라이노스케', '010-3000-0091', 'RESIDENT', TIMESTAMP '2025-08-04 10:00:00', NULL, 'ACTIVE'),
    ('res96',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 602,  '렌고쿠쿄주로',     '010-3000-0092', 'RESIDENT', TIMESTAMP '2025-08-18 10:00:00', NULL, 'ACTIVE'),
    ('res97',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 702,  '토미오카기유',     '010-3000-0093', 'RESIDENT', TIMESTAMP '2025-09-01 10:00:00', NULL, 'ACTIVE'),
    ('res98',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 802,  '코쵸우시노부',     '010-3000-0094', 'RESIDENT', TIMESTAMP '2025-09-15 10:00:00', NULL, 'ACTIVE'),
    ('res99',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 902,  '우즈이텐겐',       '010-3000-0095', 'RESIDENT', TIMESTAMP '2025-09-29 10:00:00', NULL, 'ACTIVE'),
    ('res100',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 1002, '토키토무이치로',   '010-3000-0096', 'RESIDENT', TIMESTAMP '2025-10-13 10:00:00', NULL, 'ACTIVE'),

    -- [302동]
    -- 거주 15세대 / 전출 신청 5세대
    ('res101',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 101,  '토키토무이치로', '010-3000-0096', 'RESIDENT', TIMESTAMP '2025-01-24 10:00:00', NULL,                              'ACTIVE'),
    ('res102',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 201,  '칸로지미츠리',   '010-3000-0097', 'RESIDENT', TIMESTAMP '2025-02-07 10:00:00', NULL,                              'ACTIVE'),
    ('res103',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 301,  '이구로오바나이', '010-3000-0098', 'RESIDENT', TIMESTAMP '2025-02-21 10:00:00', NULL,                              'ACTIVE'),
    ('res104',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 401,  '에렌예거',       '010-3000-0099', 'RESIDENT', TIMESTAMP '2025-03-07 10:00:00', NULL,                              'ACTIVE'),
    ('res105',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 501,  '미카사아커만',   '010-3000-0100', 'RESIDENT', TIMESTAMP '2025-03-21 10:00:00', NULL,                              'ACTIVE'),
    ('res106',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 601,  '아르민알레르토', '010-3000-0101', 'RESIDENT', TIMESTAMP '2025-04-04 10:00:00', NULL,                              'ACTIVE'),
    ('res107',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 701,  '리바이아커만',   '010-3000-0102', 'RESIDENT', TIMESTAMP '2025-04-18 10:00:00', NULL,                              'ACTIVE'),
    ('res108',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 801,  '한지조에',       '010-3000-0103', 'RESIDENT', TIMESTAMP '2025-05-02 10:00:00', NULL,                              'ACTIVE'),
    ('res109',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 901,  '엘빈스미스',     '010-3000-0104', 'RESIDENT', TIMESTAMP '2025-05-16 10:00:00', NULL,                              'ACTIVE'),
    ('res110',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 1001, '라이너브라운',   '010-3000-0105', 'RESIDENT', TIMESTAMP '2025-05-30 10:00:00', NULL,                              'ACTIVE'),
    ('res111',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 102,  '애니레온하트',   '010-3000-0106', 'RESIDENT', TIMESTAMP '2025-06-13 10:00:00', NULL,                              'ACTIVE'),
    ('res112',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 202,  '미도리야이즈쿠', '010-3000-0107', 'RESIDENT', TIMESTAMP '2025-06-27 10:00:00', NULL,                              'ACTIVE'),
    ('res113',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 302,  '바쿠고카츠키',   '010-3000-0108', 'RESIDENT', TIMESTAMP '2025-07-11 10:00:00', NULL,                              'ACTIVE'),
    ('res114',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 402,  '토도로키쇼토',   '010-3000-0109', 'RESIDENT', TIMESTAMP '2025-07-25 10:00:00', NULL,                              'ACTIVE'),
    ('res115',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 502,  '올마이트',       '010-3000-0110', 'RESIDENT', TIMESTAMP '2025-08-08 10:00:00', NULL,                              'ACTIVE'),

    -- 전출 신청
    ('res116',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 602,  '아이자와쇼타',   '010-3000-0111', 'RESIDENT', TIMESTAMP '2025-08-22 10:00:00', TIMESTAMP '2026-07-13 10:30:00', 'WITHDRAW_PENDING'),
    ('res117',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 702,  '우라라카오챠코', '010-3000-0112', 'RESIDENT', TIMESTAMP '2025-09-05 10:00:00', TIMESTAMP '2026-07-14 11:30:00', 'WITHDRAW_PENDING'),
    ('res118',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 802,  '야가미라이토',   '010-3000-0113', 'RESIDENT', TIMESTAMP '2025-09-19 10:00:00', TIMESTAMP '2026-07-15 12:30:00', 'WITHDRAW_PENDING'),
    ('res119',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 902,  '엘로우라이트',   '010-3000-0114', 'RESIDENT', TIMESTAMP '2025-10-03 10:00:00', TIMESTAMP '2026-07-16 13:30:00', 'WITHDRAW_PENDING'),
    ('res120',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 1002, '류크',           '010-3000-0115', 'RESIDENT', TIMESTAMP '2025-10-17 10:00:00', TIMESTAMP '2026-07-17 14:30:00', 'WITHDRAW_PENDING'),

    -- [401동]
    -- 거주 20세대
    ('res121',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 101,  '에드워드엘릭',     '010-3000-0116', 'RESIDENT', TIMESTAMP '2025-01-28 10:00:00', NULL, 'ACTIVE'),
    ('res122',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 201,  '알폰스엘릭',       '010-3000-0117', 'RESIDENT', TIMESTAMP '2025-02-11 10:00:00', NULL, 'ACTIVE'),
    ('res123',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 301,  '로이머스탱',       '010-3000-0118', 'RESIDENT', TIMESTAMP '2025-02-25 10:00:00', NULL, 'ACTIVE'),
    ('res124',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 401,  '스파이크스피겔',   '010-3000-0119', 'RESIDENT', TIMESTAMP '2025-03-11 10:00:00', NULL, 'ACTIVE'),
    ('res125',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 501,  '제트블랙',         '010-3000-0120', 'RESIDENT', TIMESTAMP '2025-03-25 10:00:00', NULL, 'ACTIVE'),
    ('res126',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 601,  '페이발렌타인',     '010-3000-0121', 'RESIDENT', TIMESTAMP '2025-04-08 10:00:00', NULL, 'ACTIVE'),
    ('res127',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 701,  '키리토',           '010-3000-0122', 'RESIDENT', TIMESTAMP '2025-04-22 10:00:00', NULL, 'ACTIVE'),
    ('res128',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 801,  '아스나',           '010-3000-0123', 'RESIDENT', TIMESTAMP '2025-05-06 10:00:00', NULL, 'ACTIVE'),
    ('res129',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 901,  '렘',               '010-3000-0124', 'RESIDENT', TIMESTAMP '2025-05-20 10:00:00', NULL, 'ACTIVE'),
    ('res130',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 1001, '에밀리아',         '010-3000-0125', 'RESIDENT', TIMESTAMP '2025-06-03 10:00:00', NULL, 'ACTIVE'),
    ('res131',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 102,  '아냐포저',         '010-3000-0126', 'RESIDENT', TIMESTAMP '2025-06-17 10:00:00', NULL, 'ACTIVE'),
    ('res132',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 202,  '로이드포저',       '010-3000-0127', 'RESIDENT', TIMESTAMP '2025-07-01 10:00:00', NULL, 'ACTIVE'),
    ('res133',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 302,  '요르포저',         '010-3000-0128', 'RESIDENT', TIMESTAMP '2025-07-15 10:00:00', NULL, 'ACTIVE'),
    ('res134',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 402,  '프리렌',           '010-3000-0129', 'RESIDENT', TIMESTAMP '2025-07-29 10:00:00', NULL, 'ACTIVE'),
    ('res135',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 502,  '페른',             '010-3000-0130', 'RESIDENT', TIMESTAMP '2025-08-12 10:00:00', NULL, 'ACTIVE'),
    ('res136',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 602,  '슈타르크',         '010-3000-0131', 'RESIDENT', TIMESTAMP '2025-08-26 10:00:00', NULL, 'ACTIVE'),
    ('res137',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 702,  '링크',             '010-3000-0132', 'RESIDENT', TIMESTAMP '2025-09-09 10:00:00', NULL, 'ACTIVE'),
    ('res138',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 802,  '젤다',             '010-3000-0133', 'RESIDENT', TIMESTAMP '2025-09-23 10:00:00', NULL, 'ACTIVE'),
    ('res139',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 902,  '마리오',           '010-3000-0134', 'RESIDENT', TIMESTAMP '2025-10-07 10:00:00', NULL, 'ACTIVE'),
    ('res140',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 1002, '루이지',           '010-3000-0135', 'RESIDENT', TIMESTAMP '2025-10-21 10:00:00', NULL, 'ACTIVE');




-- 관리자는 unit_no를 NULL로 연결하고 입주민은 실제 세대의 unit_no를 참조한다.
-- 기존 번호를 유지하여 차량 등 나머지 더미의 member_no 참조를 보존한다.
INSERT INTO member (
    member_no,
    login_id,
    login_pwd,
    unit_no,
    mem_name,
    mem_phone,
    role,
    mem_status,
    create_at,
    delete_at
)
SELECT
    CASE
        WHEN seed.seed_order >= 39 THEN seed.seed_order + 2
        WHEN seed.seed_order >= 8 THEN seed.seed_order + 1
        ELSE seed.seed_order
    END,
    seed.login_id,
    seed.login_pwd,
    CASE WHEN seed.role = 'ADMIN' THEN NULL ELSE au.apartment_unit_no END,
    seed.mem_name,
    seed.mem_phone,
    seed.role,
    seed.mem_status,
    seed.create_at,
    seed.delete_at
FROM legacy_member_seed seed
LEFT JOIN apartment_unit au
    ON au.dong = seed.mem_dong
   AND au.ho = seed.mem_ho;

-- 기존 다른 더미가 참조하는 회원 번호와 충돌하지 않도록 다음 발급 번호만 유지한다.
SELECT setval(pg_get_serial_sequence('member', 'member_no'), 164, TRUE);

UPDATE apartment_unit au
SET unit_status = 'OCCUPIED'
WHERE EXISTS (
    SELECT 1
    FROM member m
WHERE m.unit_no = au.apartment_unit_no
      AND m.role = 'RESIDENT'
      AND m.mem_status IN ('PENDING', 'ACTIVE', 'WITHDRAW_PENDING')
);

DROP TABLE legacy_member_seed;

-- =====================================================
-- 1-1. 전출 이력
-- 현재 빈 세대의 과거 이력과 새 입주민이 들어온 세대의 과거 이력을 함께 둔다.
-- =====================================================
INSERT INTO member_archive
    (original_member_no, login_id, mem_name, mem_phone, role, mem_status,
     mem_dong, mem_ho, create_at, delete_at, archived_at)
VALUES
    -- 현재 빈 세대의 과거 이력
    (8,   'old_res_101_401', '닉',       '010-2222-0104', 'RESIDENT', 'WITHDRAW_PENDING', 101, 401, TIMESTAMP '2025-01-21 10:00:00', TIMESTAMP '2026-05-20 14:00:00', TIMESTAMP '2026-05-20 14:10:00'),
    (145, 'old_res_402_101', '마리오',   '010-3000-0134', 'RESIDENT', 'WITHDRAW_PENDING', 402, 101, TIMESTAMP '2025-01-10 10:00:00', TIMESTAMP '2025-12-01 10:00:00', TIMESTAMP '2025-12-01 10:10:00'),
    (146, 'old_res_402_201', '루이지',   '010-3000-0135', 'RESIDENT', 'WITHDRAW_PENDING', 402, 201, TIMESTAMP '2025-01-24 10:00:00', TIMESTAMP '2025-12-15 10:00:00', TIMESTAMP '2025-12-15 10:10:00'),
    (147, 'old_res_402_301', '피치',     '010-3000-0136', 'RESIDENT', 'WITHDRAW_PENDING', 402, 301, TIMESTAMP '2025-02-07 10:00:00', TIMESTAMP '2026-01-05 10:00:00', TIMESTAMP '2026-01-05 10:10:00'),
    (148, 'old_res_402_401', '커비',     '010-3000-0137', 'RESIDENT', 'WITHDRAW_PENDING', 402, 401, TIMESTAMP '2025-02-21 10:00:00', TIMESTAMP '2026-01-19 10:00:00', TIMESTAMP '2026-01-19 10:10:00'),
    (149, 'old_res_402_501', '소닉',     '010-3000-0138', 'RESIDENT', 'WITHDRAW_PENDING', 402, 501, TIMESTAMP '2025-03-07 10:00:00', TIMESTAMP '2026-02-02 10:00:00', TIMESTAMP '2026-02-02 10:10:00'),

    -- 새 입주민이 이미 들어온 세대의 과거 이력
    (9,   'old_res_101_501', '이전거주자101501', '010-7100-1501', 'RESIDENT', 'WITHDRAW_PENDING', 101, 501, TIMESTAMP '2025-01-15 10:00:00', TIMESTAMP '2025-11-20 16:00:00', TIMESTAMP '2025-11-20 16:10:00'),
    (12,  'old_res_101_801', '이전거주자101801', '010-7100-1801', 'RESIDENT', 'WITHDRAW_PENDING', 101, 801, TIMESTAMP '2025-02-01 10:00:00', TIMESTAMP '2026-01-25 19:00:00', TIMESTAMP '2026-01-25 19:10:00'),
    (25,  'old_res_102_101', '이전거주자102101', '010-7100-2101', 'RESIDENT', 'WITHDRAW_PENDING', 102, 101, TIMESTAMP '2025-03-03 10:00:00', TIMESTAMP '2026-02-18 15:00:00', TIMESTAMP '2026-02-18 15:10:00'),
    (45,  'old_res_201_101', '이전거주자201101', '010-7200-1101', 'RESIDENT', 'WITHDRAW_PENDING', 201, 101, TIMESTAMP '2025-04-04 10:00:00', TIMESTAMP '2026-03-10 11:00:00', TIMESTAMP '2026-03-10 11:10:00'),
    (85,  'old_res_301_101', '이전거주자301101', '010-7300-1101', 'RESIDENT', 'WITHDRAW_PENDING', 301, 101, TIMESTAMP '2025-05-05 10:00:00', TIMESTAMP '2026-04-12 13:00:00', TIMESTAMP '2026-04-12 13:10:00'),
    (125, 'old_res_401_101', '이전거주자401101', '010-7400-1101', 'RESIDENT', 'WITHDRAW_PENDING', 401, 101, TIMESTAMP '2025-06-06 10:00:00', TIMESTAMP '2026-05-14 17:00:00', TIMESTAMP '2026-05-14 17:10:00');

-- =====================================================
-- PARKING DUMMY DATA
-- 지하 1층과 지하 2층을 각각 별도 주차구역으로 관리한다.
-- =====================================================
INSERT INTO parking (
    parking_code,
    parking_name,
    parking_type,
    parking_spaces,
    parking_location
)
VALUES
	(
		'SURFACE',
		'BONO 아파트 지상 출입구', -- 지상 게이트 관리용 구역
		'SURFACE',
    	0,                          -- 지상에는 실제 주차면이 없음
    	'BONO 아파트 지상'
	),
    (
        'B1',
        'BONO 아파트 지하 1층 주차장', -- 주차장 이름
        'ROBOT',
        100,                           -- 지하 1층 주차 가능 대수
        'BONO 아파트 지하 1층'         -- 주차장 위치
    ),
    (
        'B2',
        'BONO 아파트 지하 2층 주차장', -- 주차장 이름
        'GENERAL',
        100,                           -- 지하 2층 주차 가능 대수
        'BONO 아파트 지하 2층'         -- 주차장 위치
    );

-- =====================================================
-- KIOSK DUMMY DATA
-- 각 층에 키오스크 2대씩 설치한다.
-- =====================================================
INSERT INTO kiosk (parking_no, model_name, kiosk_location, install_date)
VALUES
    (2, 'BONO-KIOSK-V1', '지하 1층 A구역', '2026-08-01'),
    (2, 'BONO-KIOSK-V1', '지하 1층 B구역', '2026-08-01'),
    (3, 'BONO-KIOSK-V1', '지하 2층 A구역', '2026-08-01'),
    (3, 'BONO-KIOSK-V1', '지하 2층 B구역', '2026-08-01');

-- =====================================================
-- 3. 게이트
-- =====================================================
INSERT INTO gate (parking_no, gate_code, gate_name, gate_type, gate_area)
VALUES
    -- 지상 정문
    (1, 'MAIN-IN',  'GROUND-MAIN-IN',  'In',  'SITE'), -- 지상 정문 입차 게이트
    (1, 'MAIN-OUT', 'GROUND-MAIN-OUT', 'Out', 'SITE'), -- 지상 정문 출차 게이트
    -- 지상 후문
    (1, 'REAR-IN',  'GROUND-REAR-IN',  'In',  'SITE'), -- 지상 후문 입차 게이트
    (1, 'REAR-OUT', 'GROUND-REAR-OUT', 'Out', 'SITE'), -- 지상 후문 출차 게이트
    -- 지하 1층 A구역 입구
    (2, 'B1-IN-1',  'B1-A-IN',  'In',  'B1'), -- 지하 1층 A구역 입차 게이트
    (2, 'B1-OUT-1', 'B1-A-OUT', 'Out', 'B1'), -- 지하 1층 A구역 출차 게이트
    -- 지하 1층 B구역 입구
    (2, 'B1-IN-2',  'B1-B-IN',  'In',  'B1'), -- 지하 1층 B구역 입차 게이트
    (2, 'B1-OUT-2', 'B1-B-OUT', 'Out', 'B1'), -- 지하 1층 B구역 출차 게이트
    -- 지하 2층 A구역 입구
    (3, 'B2-IN-1',  'B2-A-IN',  'In',  'B2'), -- 지하 2층 A구역 입차 게이트
    (3, 'B2-OUT-1', 'B2-A-OUT', 'Out', 'B2'), -- 지하 2층 A구역 출차 게이트
    -- 지하 2층 B구역 입구
    (3, 'B2-IN-2',  'B2-B-IN',  'In',  'B2'), -- 지하 2층 B구역 입차 게이트
    (3, 'B2-OUT-2', 'B2-B-OUT', 'Out', 'B2'); -- 지하 2층 B구역 출차 게이트

-- =====================================================
-- 4. 카메라
-- =====================================================
INSERT INTO camera (gate_no, camera_name, camera_type, install_date)
VALUES
    -- 지상 정문
    (1,  'CAM-GROUND-MAIN-IN',  'In',  DATE '2025-01-01'), -- 지상 정문 입차 카메라
    (2,  'CAM-GROUND-MAIN-OUT', 'Out', DATE '2025-01-01'), -- 지상 정문 출차 카메라
    -- 지상 후문
    (3,  'CAM-GROUND-REAR-IN',  'In',  DATE '2025-01-01'), -- 지상 후문 입차 카메라
    (4,  'CAM-GROUND-REAR-OUT', 'Out', DATE '2025-01-01'), -- 지상 후문 출차 카메라
    -- 지하 1층 A구역
    (5,  'CAM-B1-A-IN',         'In',  DATE '2025-01-03'), -- 지하 1층 A구역 입차 카메라
    (6,  'CAM-B1-A-OUT',        'Out', DATE '2025-01-03'), -- 지하 1층 A구역 출차 카메라
    -- 지하 1층 B구역
    (7,  'CAM-B1-B-IN',         'In',  DATE '2025-01-03'), -- 지하 1층 B구역 입차 카메라
    (8,  'CAM-B1-B-OUT',        'Out', DATE '2025-01-03'), -- 지하 1층 B구역 출차 카메라
    -- 지하 2층 A구역
    (9,  'CAM-B2-A-IN',         'In',  DATE '2025-01-05'), -- 지하 2층 A구역 입차 카메라
    (10, 'CAM-B2-A-OUT',        'Out', DATE '2025-01-05'), -- 지하 2층 A구역 출차 카메라
    -- 지하 2층 B구역
    (11, 'CAM-B2-B-IN',         'In',  DATE '2025-01-05'), -- 지하 2층 B구역 입차 카메라
    (12, 'CAM-B2-B-OUT',        'Out', DATE '2025-01-05'); -- 지하 2층 B구역 출차 카메라

-- =====================================================
-- 4-1. 주차면
-- B1 로봇 주차면과 입·출차 대기면을 생성한다.
-- =====================================================
INSERT INTO parking_space (
    parking_no,
    space_code,
    space_type
)
SELECT
    parking_no,
    'B1-P' || LPAD(no::TEXT, 3, '0'),
    'PARKING'
FROM parking
CROSS JOIN generate_series(1, 100) AS numbers(no)
WHERE parking_code = 'B1';

-- B1 A·B 구역별 입차 대기면 2개와 출차 대기면 3개를 생성한다.
INSERT INTO parking_space (
    parking_no,
    gate_no,
    space_code,
    space_type
)
SELECT
    parking.parking_no,
    gate.gate_no,
    spaces.space_code,
    spaces.space_type
FROM (
    VALUES
        ('B1-IN-1',  'B1-IN1-01',  'ENTRY_WAIT'),
        ('B1-IN-1',  'B1-IN1-02',  'ENTRY_WAIT'),
        ('B1-OUT-1', 'B1-OUT1-01', 'EXIT_WAIT'),
        ('B1-OUT-1', 'B1-OUT1-02', 'EXIT_WAIT'),
        ('B1-OUT-1', 'B1-OUT1-03', 'EXIT_WAIT'),
        ('B1-IN-2',  'B1-IN2-01',  'ENTRY_WAIT'),
        ('B1-IN-2',  'B1-IN2-02',  'ENTRY_WAIT'),
        ('B1-OUT-2', 'B1-OUT2-01', 'EXIT_WAIT'),
        ('B1-OUT-2', 'B1-OUT2-02', 'EXIT_WAIT'),
        ('B1-OUT-2', 'B1-OUT2-03', 'EXIT_WAIT')
) AS spaces(gate_code, space_code, space_type)
JOIN gate
    ON gate.gate_code = spaces.gate_code
JOIN parking
    ON parking.parking_no = gate.parking_no;

-- =====================================================
-- 4-2. 로봇
-- 4개 세트에 A·B 로봇을 각각 한 대씩 배치한다.
-- =====================================================
INSERT INTO robot (
    robot_code,
    set_no,
    set_position,
    robot_status,
    battery_level
)
VALUES
    ('ROBOT-01A', 1, 'A', 'STANDBY', 100),
    ('ROBOT-01B', 1, 'B', 'STANDBY', 100),
    ('ROBOT-02A', 2, 'A', 'STANDBY', 100),
    ('ROBOT-02B', 2, 'B', 'STANDBY', 100),
    ('ROBOT-03A', 3, 'A', 'STANDBY', 100),
    ('ROBOT-03B', 3, 'B', 'STANDBY', 100),
    ('ROBOT-04A', 4, 'A', 'STANDBY', 100),
    ('ROBOT-04B', 4, 'B', 'STANDBY', 100);

-- =====================================================
-- 5. 시연용 차량
-- 회원~카메라(1~4절)는 전달받은 원본을 그대로 유지한다.
-- =====================================================
CREATE TEMP TABLE demo_plate (
    plate_no INT PRIMARY KEY,
    car_no VARCHAR(50) UNIQUE NOT NULL,
    image_file TEXT NOT NULL,
    crop_file TEXT NOT NULL,
    ocr_car_no VARCHAR(50)
) ON COMMIT DROP;

-- val.txt를 실제 .jpeg 파일명과 대조한 공유 이미지 목록.
INSERT INTO demo_plate(plate_no, car_no, image_file, crop_file) VALUES
                    (1, '49도1839', 'img_000747.jpeg', 'img_000747.jpeg'),
                    (2, '34다8346', 'img_000605.jpeg', 'img_000605.jpeg'),
                    (3, '299러4344', 'img_000542.jpeg', 'img_000542.jpeg'),
                    (4, '06러3795', 'img_000031.jpeg', 'img_000031.jpeg'),
                    (5, '210고4056', 'img_000345.jpeg', 'img_000345.jpeg'),
                    (6, '188부1972', 'img_000282.jpeg', 'img_000282.jpeg'),
                    (7, '55소7745', 'img_000788.jpeg', 'img_000788.jpeg'),
                    (8, '180하1107', 'img_000271.jpeg', 'img_000271.jpeg'),
                    (9, '170로6099', 'img_000234.jpeg', 'img_000234.jpeg'),
                    (10, '50우0386', 'img_000759.jpeg', 'img_000759.jpeg'),
                    (11, '87머7056', 'img_000956.jpeg', 'img_000956.jpeg'),
                    (12, '93더1306', 'img_000984.jpeg', 'img_000984.jpeg'),
                    (13, '156누8346', 'img_000197.jpeg', 'img_000197.jpeg'),
                    (14, '52소6756', 'img_000774.jpeg', 'img_000774.jpeg'),
                    (15, '163가7411', 'img_000217.jpeg', 'img_000217.jpeg'),
                    (16, '314부6765', 'img_000571.jpeg', 'img_000571.jpeg'),
                    (17, '120무6377', 'img_000095.jpeg', 'img_000095.jpeg'),
                    (18, '55무0825', 'img_000787.jpeg', 'img_000787.jpeg'),
                    (19, '243보2032', 'img_000446.jpeg', 'img_000446.jpeg'),
                    (20, '07두7942', 'img_000033.jpeg', 'img_000033.jpeg'),
                    (21, '35수2784', 'img_000624.jpeg', 'img_000624.jpeg'),
                    (22, '96오5139', 'img_000993.jpeg', 'img_000993.jpeg'),
                    (23, '14나6164', 'img_000176.jpeg', 'img_000176.jpeg'),
                    (24, '143하2621', 'img_000162.jpeg', 'img_000162.jpeg'),
                    (25, '22나6912', 'img_000395.jpeg', 'img_000395.jpeg'),
                    (26, '83마0327', 'img_000931.jpeg', 'img_000931.jpeg'),
                    (27, '145주5974', 'img_000168.jpeg', 'img_000168.jpeg'),
                    (28, '129조1193', 'img_000118.jpeg', 'img_000118.jpeg'),
                    (29, '181서3569', 'img_000272.jpeg', 'img_000272.jpeg'),
                    (30, '204도6527', 'img_000323.jpeg', 'img_000323.jpeg'),
                    (31, '145어2319', 'img_000167.jpeg', 'img_000167.jpeg'),
                    (32, '127루3517', 'img_000113.jpeg', 'img_000113.jpeg'),
                    (33, '204도8991', 'img_000324.jpeg', 'img_000324.jpeg'),
                    (34, '229하7128', 'img_000391.jpeg', 'img_000391.jpeg'),
                    (35, '341저6026', 'img_000599.jpeg', 'img_000599.jpeg'),
                    (36, '161머8942', 'img_000215.jpeg', 'img_000215.jpeg'),
                    (37, '62누4783', 'img_000827.jpeg', 'img_000827.jpeg'),
                    (38, '128모5622', 'img_000115.jpeg', 'img_000115.jpeg'),
                    (39, '93나0823', 'img_000983.jpeg', 'img_000983.jpeg'),
                    (40, '24거1096', 'img_000460.jpeg', 'img_000460.jpeg'),
                    (41, '163저8578', 'img_000218.jpeg', 'img_000218.jpeg'),
                    (42, '41저1645', 'img_000702.jpeg', 'img_000702.jpeg'),
                    (43, '216러7763', 'img_000353.jpeg', 'img_000353.jpeg'),
                    (44, '90러2980', 'img_000971.jpeg', 'img_000971.jpeg'),
                    (45, '43소0198', 'img_000719.jpeg', 'img_000719.jpeg'),
                    (46, '62구3638', 'img_000825.jpeg', 'img_000825.jpeg'),
                    (47, '52너2284', 'img_000769.jpeg', 'img_000769.jpeg'),
                    (48, '142머5623', 'img_000157.jpeg', 'img_000157.jpeg'),
                    (49, '225하2171', 'img_000374.jpeg', 'img_000374.jpeg'),
                    (50, '143모8849', 'img_000160.jpeg', 'img_000160.jpeg'),
                    (51, '91어6511', 'img_000975.jpeg', 'img_000975.jpeg'),
                    (52, '41서5534', 'img_000699.jpeg', 'img_000699.jpeg'),
                    (53, '308소1608', 'img_000559.jpeg', 'img_000559.jpeg'),
                    (54, '131보2915', 'img_000128.jpeg', 'img_000128.jpeg'),
                    (55, '87마5686', 'img_000955.jpeg', 'img_000955.jpeg'),
                    (56, '257러4242', 'img_000480.jpeg', 'img_000480.jpeg'),
                    (57, '168러5334', 'img_000224.jpeg', 'img_000224.jpeg'),
                    (58, '103호3307', 'img_000058.jpeg', 'img_000058.jpeg'),
                    (59, '40거2054', 'img_000687.jpeg', 'img_000687.jpeg'),
                    (60, '48나8278', 'img_000739.jpeg', 'img_000739.jpeg'),
                    (61, '117어3971', 'img_000082.jpeg', 'img_000082.jpeg'),
                    (62, '166누1189', 'img_000221.jpeg', 'img_000221.jpeg'),
                    (63, '727고6666', 'img_000876.jpeg', 'img_000876.jpeg'),
                    (64, '251도4009', 'img_000470.jpeg', 'img_000470.jpeg'),
                    (65, '50소1546', 'img_000755.jpeg', 'img_000755.jpeg'),
                    (66, '184두3996', 'img_000275.jpeg', 'img_000275.jpeg'),
                    (67, '97조9295', 'img_000999.jpeg', 'img_000999.jpeg'),
                    (68, '48가8873', 'img_000736.jpeg', 'img_000736.jpeg'),
                    (69, '230호2607', 'img_000409.jpeg', 'img_000409.jpeg'),
                    (70, '13나1643', 'img_000143.jpeg', 'img_000143.jpeg'),
                    (71, '301나7718', 'img_000552.jpeg', 'img_000552.jpeg');

INSERT INTO vehicle_car
(vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
VALUES
    -- 필수 9대
    ('normal', '222하5233', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '335 days', 5,  CURRENT_TIMESTAMP - INTERVAL '30 days'),
    -- res1 만료 임박 표시 확인용: 실행 시점부터 정확히 10일 후 만료
    ('normal', '99보9999',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '355 days', CURRENT_TIMESTAMP + INTERVAL '10 days', 5, CURRENT_TIMESTAMP - INTERVAL '355 days'),
    ('normal', '26무3111',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '27 days', CURRENT_TIMESTAMP + INTERVAL '338 days', 9,  CURRENT_TIMESTAMP - INTERVAL '27 days'),
    ('normal', '41소2593',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '24 days', CURRENT_TIMESTAMP + INTERVAL '341 days', 10, CURRENT_TIMESTAMP - INTERVAL '24 days'),
    ('normal', '47조2603',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '21 days', CURRENT_TIMESTAMP + INTERVAL '344 days', 11, CURRENT_TIMESTAMP - INTERVAL '21 days'),
    ('normal', '81라7385',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP + INTERVAL '347 days', 12, CURRENT_TIMESTAMP - INTERVAL '18 days'),
    ('normal', '95마7152',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '350 days', 13, CURRENT_TIMESTAMP - INTERVAL '15 days'),
    ('normal', '817라7385',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '350 days', 13, CURRENT_TIMESTAMP - INTERVAL '15 days'),

    -- 시연용 더미
    ('normal', '40두9797',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '11 days', CURRENT_TIMESTAMP + INTERVAL '354 days', 82, CURRENT_TIMESTAMP - INTERVAL '11 days'),
    ('normal', '40무3111',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '355 days', 71, CURRENT_TIMESTAMP - INTERVAL '10 days'),
    ('normal', '826수4755', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '9 days',  CURRENT_TIMESTAMP + INTERVAL '356 days', 10, CURRENT_TIMESTAMP - INTERVAL '9 days'),
    ('normal',  '93로6277',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '8 days',  CURRENT_TIMESTAMP + INTERVAL '357 days', 11, CURRENT_TIMESTAMP - INTERVAL '8 days'),
    ('normal', '18나8473',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '7 days',  CURRENT_TIMESTAMP + INTERVAL '358 days', 62, CURRENT_TIMESTAMP - INTERVAL '7 days'),
    ('normal', '101하2613', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '6 days',  CURRENT_TIMESTAMP + INTERVAL '359 days', 13, CURRENT_TIMESTAMP - INTERVAL '6 days'),
    ('normal', '18보6535',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '5 days',  CURRENT_TIMESTAMP + INTERVAL '360 days', 14, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    ('normal', '22어5609',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '4 days',  CURRENT_TIMESTAMP + INTERVAL '361 days', 74, CURRENT_TIMESTAMP - INTERVAL '4 days'),
	('normal', '80오0473',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '3 days',  CURRENT_TIMESTAMP + INTERVAL '362 days', 75, CURRENT_TIMESTAMP - INTERVAL '3 days'),

    -- 방문 차량(가장 최근)
    ('normal',  '31조2923',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP + INTERVAL '20 hours', 15, CURRENT_TIMESTAMP - INTERVAL '6 hours'),
    ('normal',  '222마2574', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP + INTERVAL '22 hours', 16, CURRENT_TIMESTAMP - INTERVAL '3 hours');

-- 현재 주차 및 통계용 입주민 차량 48대. 과거 만료 행은 두지 않고 새 1년 등록만 둔다.
INSERT INTO vehicle_car
(vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
SELECT 'normal', dp.car_no, 'APPROVED',
       CURRENT_TIMESTAMP - INTERVAL '30 days',
    CURRENT_TIMESTAMP + INTERVAL '335 days',
    CASE
           WHEN 9 + ((dp.plate_no - 1) % 100) >= 40
           THEN 10 + ((dp.plate_no - 1) % 100)
           ELSE 9 + ((dp.plate_no - 1) % 100)
       END,
       CURRENT_TIMESTAMP - INTERVAL '30 days'
FROM demo_plate dp
WHERE dp.plate_no BETWEEN 1 AND 48;

-- 현재 주차 방문차량 9대. 1번은 오전 6~8시 승인 후 만료·미출차 상황이다.
INSERT INTO vehicle_car
(vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
SELECT 'visit', dp.car_no, 'APPROVED',
       CASE
           WHEN dp.plate_no = 49
               THEN CURRENT_TIMESTAMP - INTERVAL '2 hours'
           WHEN dp.plate_no = 57
               THEN CURRENT_TIMESTAMP - INTERVAL '3 hours'
           ELSE CURRENT_TIMESTAMP - INTERVAL '30 minutes'
       END,
       CASE
           WHEN dp.plate_no = 49
               THEN CURRENT_TIMESTAMP - INTERVAL '1 hour'
           WHEN dp.plate_no = 57
               THEN CURRENT_TIMESTAMP + INTERVAL '2 hours'
           ELSE CURRENT_TIMESTAMP + INTERVAL '4 hours 30 minutes'
       END,
       30 + (dp.plate_no - 48),
       CASE
           WHEN dp.plate_no = 49
               THEN CURRENT_TIMESTAMP - INTERVAL '2 hours 15 minutes'
           WHEN dp.plate_no = 57
               THEN CURRENT_TIMESTAMP - INTERVAL '4 hours'
           ELSE CURRENT_TIMESTAMP - INTERVAL '1 hour'
       END
FROM demo_plate dp
WHERE dp.plate_no BETWEEN 49 AND 57;

-- =====================================================
-- 6. 입출차 사건 임시표
-- 사건 1건은 car_log 1건, 입차 camera_data 1건을 만든다.
-- 출차 완료 사건은 출차 camera_data를 1건 더 만든다.
-- =====================================================
CREATE TEMP TABLE demo_event (
    event_key TEXT PRIMARY KEY,
    car_no VARCHAR(50) NOT NULL,
    car_kind VARCHAR(20) NOT NULL,
    in_gate_no INT NOT NULL,
    in_time TIMESTAMP NOT NULL,
    out_gate_no INT,
    out_time TIMESTAMP
) ON COMMIT DROP;

-- 전날 완료 기록도 게이트별 등록 차량 비율이 낮아지지 않도록 48건을 고르게 배치한다.
-- 시간대별 평균 그래프의 정확한 곡선은 아래 HOURLY 통계 스냅샷에서 별도로 제공한다.
INSERT INTO demo_event
-- [DEMO RATIO] Completed records: each gate gets 10 resident records.
SELECT 'BASE-R-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = g), 'REGISTERED',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - INTERVAL '20 hours' - (g * INTERVAL '4 minutes'),
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - INTERVAL '8 hours' - (g * INTERVAL '3 minutes')
FROM generate_series(1, 40) AS g;

INSERT INTO demo_event
-- [DEMO RATIO] Completed records: each gate gets 1 visitor record.
SELECT 'BASE-V-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 48 + g), 'VISIT',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - INTERVAL '18 hours' - (g * INTERVAL '4 minutes'),
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - INTERVAL '6 hours' - (g * INTERVAL '9 minutes')
FROM generate_series(1, 4) AS g;

INSERT INTO demo_event
-- [DEMO RATIO] Completed records: each gate gets 1 unknown record.
SELECT 'BASE-U-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 57 + g), 'UNKNOWN',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - INTERVAL '16 hours' - (g * INTERVAL '4 minutes'),
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - INTERVAL '4 hours' - (g * INTERVAL '9 minutes')
FROM generate_series(1, 4) AS g;

-- 09시 이후 현재 시각 사이에 입주민 48대가 입차한다.
-- [DEMO RATIO] Each gate receives 12 currently parked resident vehicles.
INSERT INTO demo_event
SELECT 'NOW-R-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = g), 'REGISTERED',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_TIMESTAMP - ((49 - g) * INTERVAL '5 minutes'),
       NULL, NULL
FROM generate_series(1, 48) AS g;

-- 방문 8대 중 1대는 06:30 입차 후 만료, 나머지는 09시 이후 입차한다.
-- [DEMO RATIO] Each gate receives 2 currently parked visitor vehicles.
INSERT INTO demo_event
SELECT 'NOW-V-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 48 + g), 'VISIT',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CASE
            WHEN g = 1 THEN CURRENT_TIMESTAMP - INTERVAL '30 hours'
            WHEN g = 2 THEN CURRENT_TIMESTAMP - INTERVAL '55 hours'
            WHEN g = 3 THEN CURRENT_TIMESTAMP - INTERVAL '80 hours'
            ELSE CURRENT_TIMESTAMP - ((9 - g) * INTERVAL '7 minutes')
       END,
       NULL, NULL
FROM generate_series(1, 8) AS g;

-- 미등록 4대도 09시 이후 현재 시각 사이에 입차한다.
-- [DEMO RATIO] Each gate receives 1 currently parked unknown vehicle.
-- Resident : non-resident is 12 : 3 (4:1) at every gate.
INSERT INTO demo_event
SELECT 'NOW-U-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 57 + g), 'UNKNOWN',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CASE
            WHEN g = 1 THEN CURRENT_TIMESTAMP - INTERVAL '50 hours'
            WHEN g = 2 THEN CURRENT_TIMESTAMP - INTERVAL '75 hours'
            WHEN g = 3 THEN CURRENT_TIMESTAMP - INTERVAL '100 hours'
            ELSE CURRENT_TIMESTAMP - ((5 - g) * INTERVAL '20 minutes')
       END,
       NULL, NULL
FROM generate_series(1, 4) AS g;

-- =====================================================
-- 7. 카메라 데이터와 카로그 연결
-- 모든 카로그에 입차 이미지가 있고, 출차 완료 로그에는 출차 이미지도 있다.
-- 시간순으로 camera_data를 넣어 최신 촬영의 PK가 가장 크도록 한다.
-- =====================================================
CREATE TEMP TABLE demo_capture (
    capture_key TEXT PRIMARY KEY,
    event_key TEXT NOT NULL,
    camera_no INT NOT NULL,
    car_no VARCHAR(50) NOT NULL,
    capture_time TIMESTAMP NOT NULL,
    capture_side VARCHAR(3) NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_capture
SELECT event_key || '-IN', event_key, in_gate_no, car_no, in_time, 'IN'
FROM demo_event;

INSERT INTO demo_capture
SELECT event_key || '-OUT', event_key, out_gate_no, car_no, out_time, 'OUT'
FROM demo_event
WHERE out_time IS NOT NULL;

CREATE TEMP TABLE demo_capture_link (
    capture_key TEXT PRIMARY KEY,
    camera_data_no INT NOT NULL
) ON COMMIT DROP;

WITH ordered AS (
    SELECT dc.*,
           ROW_NUMBER() OVER (ORDER BY dc.capture_time, dc.capture_key) AS image_no
    FROM demo_capture dc
), inserted AS (
INSERT INTO camera_data
(camera_no, vehicle_car_no, car_no, ocr_car_no, capture_time,
 image_path, crop_image_path, recognition_state, confidence_score, cam_note)
SELECT o.camera_no, vc.vehicle_car_no, o.car_no,
       COALESCE(dp.ocr_car_no, o.car_no), o.capture_time,
       'camera-data/' || dp.image_file,
       'camera-data/crop/' || REPLACE(dp.crop_file, '.jpeg', '.jpg'),
       TRUE,
       CASE WHEN (o.image_no % 13) = 0 THEN 95.40 ELSE 98.20 END,
       o.capture_key
FROM ordered o
         JOIN demo_plate dp ON dp.car_no = o.car_no
         LEFT JOIN vehicle_car vc ON vc.car_no = o.car_no
ORDER BY o.capture_time, o.capture_key
    RETURNING camera_data_no, cam_note
)
INSERT INTO demo_capture_link(capture_key, camera_data_no)
SELECT cam_note, camera_data_no FROM inserted;

INSERT INTO car_log
(vehicle_car_no, camera_data_no, out_camera_data_no,
 in_gate_no, in_time, out_gate_no, out_time,
 snapshot_car_no, snapshot_car_kind)
SELECT vc.vehicle_car_no, cin.camera_data_no, cout.camera_data_no,
       e.in_gate_no, e.in_time, e.out_gate_no, e.out_time,
       e.car_no, e.car_kind
FROM demo_event e
         LEFT JOIN vehicle_car vc ON vc.car_no = e.car_no
         JOIN demo_capture_link cin ON cin.capture_key = e.event_key || '-IN'
         LEFT JOIN demo_capture_link cout ON cout.capture_key = e.event_key || '-OUT';

-- =====================================================
-- res1 현재 주차 위치 확인용
-- 만료 임박 차량 99보9999를 B1-P023 주차면에 배정한다.
-- =====================================================
WITH inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        5,
        vc.vehicle_car_no,
        vc.car_no,
        vc.car_no,
        CURRENT_TIMESTAMP - INTERVAL '45 minutes',
        TRUE,
        99.00,
        'RES1-PARKING-DEMO'
    FROM vehicle_car vc
    WHERE vc.member_no = 5
      AND vc.car_no = '99보9999'
    RETURNING camera_data_no, vehicle_car_no, car_no, capture_time
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        vehicle_car_no,
        camera_data_no,
        5,
        capture_time,
        car_no,
        'REGISTERED'
    FROM inserted_camera
    RETURNING car_log_no
)
UPDATE parking_space
SET car_log_no = (SELECT car_log_no FROM inserted_log),
    updated_at = CURRENT_TIMESTAMP
WHERE space_code = 'B1-P023'
  AND car_log_no IS NULL;

-- =====================================================
-- 공지·알림 더미 데이터
-- =====================================================
-- 관리자 차량 알림
INSERT INTO notice (
    notice_type,
    car_log_no,
    camera_data_no,
    detect_at,
    due_at,
    alert_stat,
    snapshot_car_log_no,
    snapshot_camera_data_no,
    snapshot_registered_car_no,
    snapshot_captured_car_no,
    snapshot_car_kind,
    snapshot_parking_name,
    snapshot_in_time,
    snapshot_image_path,
    snapshot_confidence_score
)
SELECT
    o.notice_type,
    o.car_log_no,
    NULL,
    o.due_at + INTERVAL '1 minute',
    o.due_at,
    'Unresolved',
    o.car_log_no,
    o.snapshot_camera_data_no,
    o.snapshot_registered_car_no,
    o.snapshot_captured_car_no,
    o.snapshot_car_kind,
    o.snapshot_parking_name,
    o.snapshot_in_time,
    o.snapshot_image_path,
    o.snapshot_confidence_score
FROM notice_overstay o
WHERE CURRENT_TIMESTAMP >= o.due_at
  AND o.snapshot_captured_car_no IN (
      '225하2171',
      '143모8849',
      '91어6511',
      '103호3307',
      '40거2054',
      '48나8278'
  )
ON CONFLICT DO NOTHING;

-- 네 차량은 초과 알림이 발생한 뒤 출차한 상태로 만든다.
-- 이 중 두 건은 처리완료, 두 건은 관리자가 처리할 수 있는 미처리 상태다.
WITH exit_targets AS (
    SELECT
        n.notice_no,
        cl.car_log_no,
        cl.vehicle_car_no,
        cl.snapshot_car_no AS car_no,
        cl.in_gate_no + 1 AS out_gate_no,
        n.due_at + INTERVAL '2 hours' AS out_time
    FROM notice n
    JOIN car_log cl
        ON n.car_log_no = cl.car_log_no
    WHERE n.snapshot_captured_car_no IN (
        '143모8849',
        '91어6511',
        '40거2054',
        '48나8278'
    )
      AND cl.out_time IS NULL
), inserted_exit_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        et.out_gate_no,
        et.vehicle_car_no,
        et.car_no,
        COALESCE(dp.ocr_car_no, et.car_no),
        et.out_time,
        'camera-data/' || dp.image_file,
        'camera-data/crop/'
            || REPLACE(dp.crop_file, '.jpeg', '.jpg'),
        TRUE,
        98.20,
        'NOTICE-OUT-' || et.notice_no
    FROM exit_targets et
    JOIN demo_plate dp
        ON dp.car_no = et.car_no
    RETURNING camera_data_no, cam_note
)
UPDATE car_log cl
SET out_gate_no = et.out_gate_no,
    out_time = et.out_time,
    out_camera_data_no = iec.camera_data_no
FROM exit_targets et
JOIN inserted_exit_camera iec
    ON iec.cam_note = 'NOTICE-OUT-' || et.notice_no
WHERE cl.car_log_no = et.car_log_no;

UPDATE notice n
SET alert_stat = 'Resolved',
    handled_by_member_no = (
        SELECT member_no
        FROM member
        WHERE login_id = 'admin1'
        LIMIT 1
    ),
    handled_at = cl.out_time + INTERVAL '10 minutes'
FROM car_log cl
WHERE n.car_log_no = cl.car_log_no
  AND n.snapshot_captured_car_no IN (
      '143모8849',
      '40거2054'
  );

-- =====================================================
-- OCR 확인 및 입차기록 없는 출차 시도 알림
-- =====================================================
INSERT INTO camera_data (camera_no, vehicle_car_no, car_no, ocr_car_no, capture_time, image_path, crop_image_path, recognition_state, confidence_score, cam_note)
VALUES
    -- OCR 오인식 미처리 확인용
    (1, NULL, '12가3456', '12가3458', CURRENT_TIMESTAMP - INTERVAL '45 minutes', 'camera-data/car1.jpeg', 'camera-data/crop/car1.jpg', FALSE, 62.40, 'NOTICE-OCR-OPEN'),

    -- OCR 오인식 처리 완료 확인용
    (3, NULL, '34나5678', '34나5679', CURRENT_TIMESTAMP - INTERVAL '90 minutes', 'camera-data/car2.jpeg', 'camera-data/crop/car2.jpg', FALSE, 62.40, 'NOTICE-OCR-DONE'),

    -- 입차 기록 없는 출차 미처리 확인용
    (2, NULL, '56다7890', '56다7890', CURRENT_TIMESTAMP - INTERVAL '30 minutes', 'camera-data/car3.jpeg', 'camera-data/crop/car3.jpg', TRUE, 98.10, 'NOTICE-EXIT-OPEN'),

    -- 입차 기록 없는 출차 처리 완료 확인용
    (4, NULL, '78라9012', '78라9012', CURRENT_TIMESTAMP - INTERVAL '75 minutes', 'camera-data/car4.jpeg', 'camera-data/crop/car4.jpg', TRUE, 98.10, 'NOTICE-EXIT-DONE');

INSERT INTO notice (notice_type, car_log_no, camera_data_no, detect_at, due_at, alert_stat, handled_by_member_no, handled_at, snapshot_car_log_no, snapshot_camera_data_no,
    snapshot_registered_car_no, snapshot_captured_car_no, snapshot_car_kind, snapshot_parking_name, snapshot_in_time, snapshot_image_path, snapshot_confidence_score)
SELECT
    x.notice_type,
    NULL,
    cd.camera_data_no,
    cd.capture_time,
    NULL,
    x.alert_stat,
    CASE
        WHEN x.alert_stat = 'Resolved' THEN (
            SELECT member_no
            FROM member
            WHERE login_id = 'admin1'
            LIMIT 1
        )
        ELSE NULL
    END,
    CASE
        WHEN x.alert_stat = 'Resolved'
            THEN cd.capture_time + INTERVAL '10 minutes'
        ELSE NULL
    END,
    NULL,
    cd.camera_data_no,
    NULL,
    COALESCE(cd.ocr_car_no, cd.car_no),
    'UNKNOWN',
    p.parking_name,
    NULL,
    cd.image_path,
    cd.confidence_score
FROM (VALUES
    ('NOTICE-OCR-OPEN',  'OCR_REVIEW',         'Unresolved'),
    ('NOTICE-OCR-DONE',  'OCR_REVIEW',         'Resolved'),
    ('NOTICE-EXIT-OPEN', 'EXIT_WITHOUT_ENTRY', 'Unresolved'),
    ('NOTICE-EXIT-DONE', 'EXIT_WITHOUT_ENTRY', 'Resolved')
) AS x(event_key, notice_type, alert_stat)
JOIN camera_data cd
    ON cd.cam_note = x.event_key
JOIN camera c
    ON cd.camera_no = c.camera_no
JOIN gate g
    ON c.gate_no = g.gate_no
JOIN parking p
    ON g.parking_no = p.parking_no
ON CONFLICT DO NOTHING;

-- =====================================================
-- 게시판 공지
-- =====================================================

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

-- =====================================================
-- 삭제·보관 더미 데이터
-- =====================================================
CREATE TEMP TABLE demo_stats_event (
    stat_no SERIAL PRIMARY KEY,
    car_no VARCHAR(50) NOT NULL,
    car_kind VARCHAR(20) NOT NULL,
    in_gate_no INT NOT NULL,
    in_time TIMESTAMP NOT NULL,
    out_gate_no INT NOT NULL,
    out_time TIMESTAMP NOT NULL
) ON COMMIT DROP;

-- 올해 1월부터 지난달까지 월별 18~22일, 날짜별 약 33~39건을 생성한다.
INSERT INTO demo_stats_event
(car_no, car_kind, in_gate_no, in_time, out_gate_no, out_time)
SELECT dp.car_no, kind.car_kind,
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)], event_time,
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)], event_time + kind.stay_interval
FROM generate_series(
         DATE_TRUNC('year', CURRENT_DATE)::date,
         (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month')::date,
         INTERVAL '1 month'
     ) AS months(month_start)
CROSS JOIN LATERAL generate_series(1,18 + (EXTRACT(MONTH FROM month_start)::int % 5)) AS days(day_no)
CROSS JOIN LATERAL (VALUES
    ('REGISTERED',28 + ((EXTRACT(MONTH FROM month_start)::int + day_no) % 5),INTERVAL '3 hours',1,48),
    ('VISIT',      3 + ((EXTRACT(MONTH FROM month_start)::int + day_no) % 3),INTERVAL '4 hours 30 minutes',49,9),
    ('UNKNOWN',    1 + ((EXTRACT(MONTH FROM month_start)::int + day_no) % 2),INTERVAL '1 hour 30 minutes',58,11)
) kind(car_kind,amount,stay_interval,first_plate,plate_count)
CROSS JOIN LATERAL generate_series(1,kind.amount) AS series(g)
CROSS JOIN LATERAL (
    SELECT month_start::date + ((day_no - 1) * INTERVAL '1 day')
           + TIME '00:05:00' + (g * INTERVAL '21 minutes') AS event_time
) event
JOIN demo_plate dp ON dp.plate_no = kind.first_plate + ((g - 1) % kind.plate_count)
WHERE event_time < CURRENT_DATE - INTERVAL '22 days';

-- 7월을 포함한 최근 22일 데이터도 같은 비율로 생성한다.
INSERT INTO demo_stats_event
(car_no, car_kind, in_gate_no, in_time, out_gate_no, out_time)
SELECT dp.car_no, kind.car_kind,
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)], event_time,
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)], event_time + kind.stay_interval
FROM generate_series(1,22) AS days(day_no)
CROSS JOIN LATERAL (VALUES
    ('REGISTERED',28 + (day_no % 5),INTERVAL '3 hours',1,48),
    ('VISIT',      3 + (day_no % 3),INTERVAL '4 hours 30 minutes',49,9),
    ('UNKNOWN',    1 + ((day_no + 1) % 2),INTERVAL '1 hour 30 minutes',58,11)
) kind(car_kind,amount,stay_interval,first_plate,plate_count)
CROSS JOIN LATERAL generate_series(1,kind.amount) AS series(g)
CROSS JOIN LATERAL (
    SELECT (CURRENT_DATE - day_no) + TIME '00:05:00' + (g * INTERVAL '21 minutes') AS event_time
) event
JOIN demo_plate dp ON dp.plate_no = kind.first_plate + ((g - 1) % kind.plate_count);

-- 출차 후 3개월 이내인 기록의 입차·출차 카메라 데이터는 현재 테이블에 둔다.
INSERT INTO camera_data
(camera_data_no, camera_no, vehicle_car_no, car_no, ocr_car_no, capture_time,
 image_path, crop_image_path, recognition_state, confidence_score, cam_note)
SELECT 100000 + (e.stat_no * 2) + cap.offset_no,
       cap.camera_no, NULL, e.car_no, e.car_no, cap.capture_time,
       'camera-data/' || dp.image_file,
       'camera-data/crop/' || REPLACE(dp.crop_file, '.jpeg', '.jpg'),
       TRUE, 96.10 + ((e.stat_no + cap.offset_no) % 8) * 0.35,
       'STAT-' || e.stat_no || '-' || cap.capture_side
FROM demo_stats_event e
JOIN demo_plate dp ON dp.car_no = e.car_no
CROSS JOIN LATERAL (VALUES
    (0,e.in_gate_no,e.in_time,'IN'),
    (1,e.out_gate_no,e.out_time,'OUT')
) cap(offset_no,camera_no,capture_time,capture_side)
WHERE e.out_time >= CURRENT_TIMESTAMP - INTERVAL '3 months';

-- 출차 후 3개월이 지난 기록의 카메라 데이터는 스케줄러 이동 형태로 지난 기록에 둔다.
INSERT INTO trash_bin
(data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT 'CAMERA_DATA', 100000 + (e.stat_no * 2) + cap.offset_no,
       jsonb_build_object(
           'camera_data_no',100000 + (e.stat_no * 2) + cap.offset_no,
           'camera_no',cap.camera_no,'vehicle_car_no',NULL,
           'car_no',e.car_no,'ocr_car_no',e.car_no,'capture_time',cap.capture_time,
           'image_path','camera-data/' || dp.image_file,
           'crop_image_path','camera-data/crop/' || REPLACE(dp.crop_file, '.jpeg', '.jpg'),
           'recognition_state',TRUE,
           'confidence_score',96.10 + ((e.stat_no + cap.offset_no) % 8) * 0.35,
           'cam_note',NULL
       ),
       'SCHEDULED', e.out_time + INTERVAL '3 months', CURRENT_TIMESTAMP + INTERVAL '1 year'
FROM demo_stats_event e
JOIN demo_plate dp ON dp.car_no = e.car_no
CROSS JOIN LATERAL (VALUES
    (0,e.in_gate_no,e.in_time,'IN'),
    (1,e.out_gate_no,e.out_time,'OUT')
) cap(offset_no,camera_no,capture_time,capture_side)
WHERE e.out_time < CURRENT_TIMESTAMP - INTERVAL '3 months';

-- 아직 3개월이 지나지 않은 완료 기록은 현재 car_log에 둔다.
INSERT INTO car_log
(vehicle_car_no, camera_data_no, out_camera_data_no,
 in_gate_no, in_time, out_gate_no, out_time, free_time,
 snapshot_car_no, snapshot_car_kind)
SELECT NULL, 100000 + (e.stat_no * 2), 100000 + (e.stat_no * 2) + 1,
       e.in_gate_no, e.in_time, e.out_gate_no, e.out_time,
       NULL, e.car_no, e.car_kind
FROM demo_stats_event e
WHERE e.out_time >= CURRENT_TIMESTAMP - INTERVAL '3 months';

-- 출차 후 3개월이 지난 완료 기록은 지난 기록에 둔다.
INSERT INTO trash_bin
(data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT 'CAR_LOG', 50000 + e.stat_no,
       jsonb_build_object(
           'car_log_no',50000 + e.stat_no,'vehicle_car_no',NULL,
           'camera_data_no',100000 + (e.stat_no * 2),
           'out_camera_data_no',100000 + (e.stat_no * 2) + 1,
           'in_gate_no',e.in_gate_no,'in_time',e.in_time,
           'out_gate_no',e.out_gate_no,'out_time',e.out_time,'free_time',NULL,
           'snapshot_car_no',e.car_no,'captured_car_no',e.car_no,
           'snapshot_car_kind',e.car_kind,'statistics_scope','ENTRY_AVERAGE'
       ),
       'SCHEDULED', e.out_time + INTERVAL '3 months', CURRENT_TIMESTAMP + INTERVAL '1 year'
FROM demo_stats_event e
WHERE e.out_time < CURRENT_TIMESTAMP - INTERVAL '3 months';


-- =====================================================
-- 일반 지난 기록: 카메라 데이터 12건 + 처리 완료 알림 12건
-- 통계용 CAR_LOG와 중복되지 않도록 별도 번호를 사용한다.
-- =====================================================
INSERT INTO trash_bin
(data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT CASE WHEN g <= 12 THEN 'CAMERA_DATA' ELSE 'NOTICE' END,
       10000 + g,
       CASE WHEN g <= 12 THEN jsonb_build_object(
           'camera_data_no',10000+g,'camera_no',1,'vehicle_car_no',NULL,
           'car_no','88아'||LPAD(g::TEXT,4,'0'),'ocr_car_no','88아'||LPAD(g::TEXT,4,'0'),
           'capture_time',CURRENT_TIMESTAMP - ((100+g) * INTERVAL '1 day'),
           'image_path',NULL,'crop_image_path',NULL,'recognition_state',TRUE,'confidence_score',97.5)
       ELSE jsonb_build_object(
           'notice_no',10000+g,
           'notice_type',CASE WHEN g % 2 = 0
                              THEN 'OCR_REVIEW'
                              ELSE 'EXIT_WITHOUT_ENTRY' END,
           'car_log_no',NULL,
           'camera_data_no',NULL,
           'detect_at',CURRENT_TIMESTAMP - ((100+g) * INTERVAL '1 day'),
           'due_at',NULL,
           'alert_stat','Resolved',
           'handled_by_member_no',1,
           'handled_at',CURRENT_TIMESTAMP - ((99+g) * INTERVAL '1 day'),
           'snapshot_car_log_no',NULL,
           'snapshot_camera_data_no',10000+g,
           'snapshot_registered_car_no',NULL,
           'snapshot_captured_car_no','88아'||LPAD(g::TEXT,4,'0'),
           'snapshot_car_kind','UNKNOWN','snapshot_parking_name','A 주차장',
           'snapshot_in_time',NULL,
           'snapshot_image_path',NULL,
           'snapshot_confidence_score',97.5)
       END,
       'SCHEDULED',
       CURRENT_TIMESTAMP - (g * INTERVAL '2 hours'),
       CURRENT_TIMESTAMP + INTERVAL '30 days' - (g * INTERVAL '2 hours')
FROM generate_series(1,24) AS g;

-- =====================================================
-- res1 자동 보관 문의 확인용
-- 답변 완료 후 3개월이 지나 스케줄러가 trash_bin으로 이동한 상태를 재현한다.
-- 입주민 화면에서는 일반 답변 완료 문의와 동일하게 표시한다.
-- =====================================================
INSERT INTO trash_bin
(data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT
    'INQUIRY',
    900001,
    jsonb_build_object(
        'inquiry_no', 900001,
        'member_no', resident.member_no,
        'root_inquiry_no', NULL,
        'category', 'PARKING',
        'title', '지난 주차 이용 내역 문의',
        'content', '이전에 이용한 주차 기록의 입출차 시간을 확인하고 싶습니다.',
        'status', 'ANSWERED',
        'answer_content', '확인 결과 정상적으로 입차 및 출차 처리된 기록입니다.',
        'answered_by', admin_member.member_no,
        'answered_at', CURRENT_TIMESTAMP - INTERVAL '4 months',
        'created_at', CURRENT_TIMESTAMP - INTERVAL '5 months'
    ),
    'SCHEDULED',
    CURRENT_TIMESTAMP - INTERVAL '1 month',
    CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM member resident
CROSS JOIN member admin_member
WHERE resident.login_id = 'res1'
  AND admin_member.login_id = 'admin1';

SELECT setval(pg_get_serial_sequence('vehicle_car','vehicle_car_no'), MAX(vehicle_car_no), TRUE) FROM vehicle_car;
SELECT setval(pg_get_serial_sequence('camera_data','camera_data_no'), MAX(camera_data_no), TRUE) FROM camera_data;
SELECT setval(pg_get_serial_sequence('car_log','car_log_no'), MAX(car_log_no), TRUE) FROM car_log;
SELECT setval(pg_get_serial_sequence('notice','notice_no'), MAX(notice_no), TRUE) FROM notice;
SELECT setval(pg_get_serial_sequence('trash_bin','trash_no'), MAX(trash_no), TRUE) FROM trash_bin;

-- cam_note는 연결용 임시 키로만 사용한다. 최초 더미 비고는 모두 비워 둔다.
UPDATE camera_data
SET cam_note = NULL;
-- =====================================================
-- 초기화 결과 확인
-- =====================================================
SELECT 'member' AS data_name, COUNT(*) AS data_count FROM member
UNION ALL
SELECT 'vehicle_car', COUNT(*) FROM vehicle_car
UNION ALL
SELECT 'camera_data', COUNT(*) FROM camera_data
UNION ALL
SELECT 'car_log', COUNT(*) FROM car_log
UNION ALL
SELECT 'notice', COUNT(*) FROM notice
UNION ALL
SELECT 'trash_bin', COUNT(*) FROM trash_bin
ORDER BY data_name;

SELECT
    notice_type,
    alert_stat,
    COUNT(*) AS notice_count
FROM notice
GROUP BY notice_type, alert_stat
ORDER BY notice_type, alert_stat;

SELECT
    'notice_detail' AS view_name,
    COUNT(*) AS row_count
FROM notice_detail
UNION ALL
SELECT
    'notice_overstay',
    COUNT(*)
FROM notice_overstay;

-- 자주하는 질문 테스트 데이터
INSERT INTO faq (category, question, answer)
VALUES
    ('PARKING', '입주민 차량은 어떻게 등록하나요?', '입주민 차량은 관리실에서 등록할 수 있습니다. 등록이 필요한 경우 관리실에 문의해 주세요.'),
    ('PARKING', '내 차량의 현재 주차 위치는 어디에서 확인하나요?', '입주민 메인 화면의 차량 현황에서 현재 주차 상태와 주차 위치를 확인할 수 있습니다.'),
    ('PARKING', '차량 입출차 내역은 어디에서 확인하나요?', '입주민 화면의 메뉴에서 입출차 내역을 선택하면 내 차량의 최근 입차와 출차 기록을 확인할 수 있습니다.'),
    ('VISIT', '방문차량은 어떻게 등록하나요?', '입주민 화면의 차량 관리에서 방문차량 신청을 선택한 뒤 차량번호, 방문 시작 시간, 방문 시간을 입력해 주세요. 방문 시작은 현재 시간으로부터 1시간 이후로 선택해야 합니다.'),
    ('VISIT', '등록한 방문차량을 취소할 수 있나요?', '아직 입차하지 않은 방문차량만 등록을 취소할 수 있습니다. 차량 관리의 방문차량 목록에서 등록 취소를 선택해 주세요.'),
    ('VISIT', '방문차량 등록 가능 횟수는 어디에서 확인하나요?', '입주민 메인 화면의 차량 현황에서 이번 달에 남아 있는 방문차량 등록 횟수를 확인할 수 있습니다.'),
    ('VISIT', '방문차량의 주차 시간이 초과되면 어떻게 확인하나요?', '방문차량의 주차 시간이 초과되면 차량 알림에서 관련 내용을 확인할 수 있습니다.'),
    ('PAYMENT', '방문차량 추가 등록 횟수를 충전할 수 있나요?', '방문차량 추가 등록 횟수 충전 기능은 현재 준비 중입니다. 추가 등록이 필요한 경우 관리실에 문의해 주세요.'),
    ('ETC', '차량 알림은 어디에서 확인하나요?', '입주민 화면의 메뉴 또는 화면 오른쪽의 차량 알림 버튼을 이용하면 차량 관련 알림을 확인할 수 있습니다.'),
    ('ETC', '자주하는 질문에서 해결하지 못한 내용은 어떻게 문의하나요?', '1:1 문의 화면에서 문의하기 버튼을 선택해 문의를 등록해 주세요. 등록한 문의와 관리자 답변은 내 문의에서 확인할 수 있습니다.');


INSERT INTO board_comment
    (comment_no, board_no, member_no, parent_comment_no, content)
VALUES
    (1,  1, 7,  NULL, '점검 시간에는 엘리베이터를 전혀 이용할 수 없나요?'),                                 -- 게시글 1의 최상위 댓글(1단계)
    (2,  1, 9,  1,    '저도 출근 시간과 겹치는지 확인해야겠네요.'),                                        -- 게시글 1의 1번 댓글에 대한 답글(2단계)
    (3,  1, 10, NULL, '고층에 사시는 분들은 옥상을 통해 옆 동으로 이동해서 엘리베이터를 이용할 수도 있어요.'),   -- 게시글 1의 최상위 댓글(1단계)

    (4,  2, 10, NULL, '단수 시간에 급하게 물을 사용해야 할 경우 이용할 수 있는 시설이 따로 있나요?'),          -- 게시글 2의 최상위 댓글(1단계)
    (5,  2, 5,  4,    '혹시 모르니 미리 사용할 물을 받아두는 게 좋을 것 같아요.'),                          -- 게시글 2의 4번 댓글에 대한 답글(2단계)
    (6,  2, 6,  5,    '지난번 단수 때는 관리동 화장실을 이용할 수 있었어요.'),                              -- 게시글 2의 5번 댓글에 대한 답글(3단계)

    (7,  3, 5,  NULL, '나눔 장터에 판매자로 참여하려면 따로 신청해야 하나요?'),                             -- 게시글 3의 최상위 댓글(1단계)
    (8,  3, 1,  7,    '관리실에서 신청서를 작성하실 수 있습니다.'),                                       -- 게시글 3의 7번 댓글에 대한 답글(2단계)
    (9,  3, 5,  8,    '빠른 답변 감사드립니다.'),                                                      -- 게시글 3의 8번 댓글에 대한 답글(3단계)
    (10, 3, 9,  NULL, '아이들과 함께 참여해도 괜찮은가요?');                                             -- 게시글 3의 최상위 댓글(1단계)

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

-- =====================================================
-- KIOSK BILLING AND B1 RESIDENT EXIT TEST DUMMY
-- B2 키오스크에서 차량 조회, 정산서 생성, 결제 완료,
-- 관리자 정산 목록의 출차 가능 상태 변경을 확인한다.
-- B1 키오스크에서는 입주민 차량의 로봇 출차 작업 생성을 확인한다.
-- 같은 파일을 다시 실행할 수 있도록 기존 테스트 데이터만 먼저 삭제한다.
-- =====================================================

DELETE FROM robot_task
WHERE car_log_no IN (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no IN (
        '299가1101',
        '299가1102',
        '299가1103'
    )
);

UPDATE parking_space
SET car_log_no = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE car_log_no IN (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no IN (
        '299가1101',
        '299가1102',
        '299가1103'
    )
);

DELETE FROM mem_notice
WHERE reference_table = 'bill'
  AND reference_no IN (
    SELECT bill_no
    FROM bill
    WHERE car_log_no IN (
        SELECT car_log_no
        FROM car_log
        WHERE snapshot_car_no IN (
            '299가1101',
            '299가1102',
            '299가1103',
            '299가1201',
            '299가1202',
            '299가1203',
            '299가1204',
            '299가1205'
        )
    )
);

DELETE FROM bill
WHERE car_log_no IN (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no IN (
        '299가1101',
        '299가1102',
        '299가1103',
        '299가1201',
        '299가1202',
        '299가1203',
        '299가1204',
        '299가1205'
    )
);

DELETE FROM car_log
WHERE snapshot_car_no IN (
    '299가1101',
    '299가1102',
    '299가1103',
    '299가1201',
    '299가1202',
    '299가1203',
    '299가1204',
    '299가1205'
);

DELETE FROM camera_data
WHERE cam_note IN (
    'B1-RESIDENT-EXIT-TEST-1',
    'B1-RESIDENT-EXIT-TEST-2',
    'B1-RESIDENT-EXIT-TEST-3',
    'B2-BILLING-TEST-VISIT-1',
    'B2-BILLING-TEST-VISIT-2',
    'B2-BILLING-TEST-VISIT-3',
    'B2-BILLING-TEST-UNKNOWN-1',
    'B2-BILLING-TRASH-TEST-1'
);

-- 1203 차량은 res1이 등록한 방문차량으로 생성하여
-- 정산서 발행 시 res1에게 mem_notice가 생성되도록 연결한다.
DELETE FROM vehicle_car
WHERE car_no = '299가1203';

INSERT INTO vehicle_car (
    vehicle_type,
    car_no,
    vehicle_status,
    start_date,
    end_date,
    member_no,
    approved_at
)
SELECT
    'visit',
    '299가1203',
    'APPROVED',
    CURRENT_TIMESTAMP - INTERVAL '30 hours',
    CURRENT_TIMESTAMP + INTERVAL '6 hours',
    member.member_no,
    CURRENT_TIMESTAMP - INTERVAL '30 hours'
FROM member
WHERE member.login_id = 'res1';


-- B1 입주민 차량 3대를 현재 주차 중인 서로 다른 일반 주차면에 배정한다.
WITH test_car (
    car_no,
    in_time,
    gate_code,
    image_path,
    crop_image_path,
    cam_note
) AS (
    VALUES
        (
            '299가1101',
            CURRENT_TIMESTAMP - INTERVAL '4 hours',
            'B1-IN-1',
            'camera-data/img_000118.jpeg',
            'camera-data/crop/img_000118.jpg',
            'B1-RESIDENT-EXIT-TEST-1'
        ),
        (
            '299가1102',
            CURRENT_TIMESTAMP - INTERVAL '3 hours',
            'B1-IN-1',
            'camera-data/img_000374.jpeg',
            'camera-data/crop/img_000374.jpg',
            'B1-RESIDENT-EXIT-TEST-2'
        ),
        (
            '299가1103',
            CURRENT_TIMESTAMP - INTERVAL '2 hours',
            'B1-IN-1',
            'camera-data/img_000160.jpeg',
            'camera-data/crop/img_000160.jpg',
            'B1-RESIDENT-EXIT-TEST-3'
        )
), inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        camera.camera_no,
        NULL,
        test_car.car_no,
        test_car.car_no,
        test_car.in_time,
        test_car.image_path,
        test_car.crop_image_path,
        TRUE,
        99.00,
        test_car.cam_note
    FROM test_car
    JOIN gate
        ON gate.gate_code = test_car.gate_code
    JOIN camera
        ON camera.gate_no = gate.gate_no
       AND camera.camera_type = 'In'
    RETURNING camera_data_no, car_no, capture_time, cam_note
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        NULL,
        inserted_camera.camera_data_no,
        gate.gate_no,
        inserted_camera.capture_time,
        0,
        inserted_camera.car_no,
        'REGISTERED'
    FROM inserted_camera
    JOIN gate
        ON gate.gate_code = 'B1-IN-1'
    RETURNING car_log_no, snapshot_car_no
), empty_b1_space AS (
    SELECT
        parking_space.space_no,
        ROW_NUMBER() OVER (
            ORDER BY parking_space.space_no
        ) AS row_no
    FROM parking_space
    JOIN parking
        ON parking.parking_no = parking_space.parking_no
    WHERE parking.parking_code = 'B1'
      AND parking_space.space_type = 'PARKING'
      AND parking_space.car_log_no IS NULL
      AND parking_space.active = TRUE
    ORDER BY parking_space.space_no
    LIMIT 3
), numbered_log AS (
    SELECT
        inserted_log.car_log_no,
        ROW_NUMBER() OVER (
            ORDER BY inserted_log.snapshot_car_no
        ) AS row_no
    FROM inserted_log
)
UPDATE parking_space
SET car_log_no = numbered_log.car_log_no,
    updated_at = CURRENT_TIMESTAMP
FROM numbered_log
JOIN empty_b1_space
    ON empty_b1_space.row_no = numbered_log.row_no
WHERE parking_space.space_no = empty_b1_space.space_no;


-- B2 입차 카메라 촬영정보를 생성한다.
WITH test_car (
    car_no,
    car_kind,
    free_time,
    in_time,
    gate_code,
    image_path,
    crop_image_path,
    cam_note
) AS (
    VALUES
        (
            '299가1201',
            'VISIT',
            1440,
            CURRENT_TIMESTAMP - INTERVAL '5 minutes',
            'B2-IN-1',
            'camera-data/img_000374.jpeg',
            'camera-data/crop/img_000374.jpg',
            'B2-BILLING-TEST-VISIT-1'
        ),
        (
            '299가1202',
            'VISIT',
            1440,
            CURRENT_TIMESTAMP - INTERVAL '12 hours',
            'B2-IN-2',
            'camera-data/img_000160.jpeg',
            'camera-data/crop/img_000160.jpg',
            'B2-BILLING-TEST-VISIT-2'
        ),
        (
            '299가1203',
            'VISIT',
            1440,
            CURRENT_TIMESTAMP - INTERVAL '26 hours',
            'B2-IN-1',
            'camera-data/img_000975.jpeg',
            'camera-data/crop/img_000975.jpg',
            'B2-BILLING-TEST-VISIT-3'
        ),
        (
            '299가1204',
            'UNKNOWN',
            0,
            CURRENT_TIMESTAMP - INTERVAL '2 hours',
            'B2-IN-2',
            'camera-data/img_000975.jpeg',
            'camera-data/crop/img_000975.jpg',
            'B2-BILLING-TEST-UNKNOWN-1'
        )
), inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        camera.camera_no,
        (
            SELECT vehicle_car.vehicle_car_no
            FROM vehicle_car
            WHERE vehicle_car.car_no = test_car.car_no
              AND vehicle_car.vehicle_type = 'visit'
            LIMIT 1
        ),
        test_car.car_no,
        test_car.car_no,
        test_car.in_time,
        test_car.image_path,
        test_car.crop_image_path,
        TRUE,
        99.00,
        test_car.cam_note
    FROM test_car
    JOIN gate
        ON gate.gate_code = test_car.gate_code
    JOIN camera
        ON camera.gate_no = gate.gate_no
       AND camera.camera_type = 'In'
    RETURNING
        camera_data_no,
        vehicle_car_no,
        car_no,
        capture_time,
        cam_note
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        inserted_camera.vehicle_car_no,
        inserted_camera.camera_data_no,
        gate.gate_no,
        inserted_camera.capture_time,
        test_car.free_time,
        test_car.car_no,
        test_car.car_kind
    FROM inserted_camera
    JOIN test_car
        ON test_car.cam_note = inserted_camera.cam_note
    JOIN gate
        ON gate.gate_code = test_car.gate_code
    RETURNING car_log_no, snapshot_car_no
), selected_rule AS (
    SELECT fee_rule_no
    FROM fee_rule
    WHERE effective_from <= CURRENT_TIMESTAMP
      AND (
          effective_to IS NULL
          OR effective_to > CURRENT_TIMESTAMP
      )
    ORDER BY effective_from DESC, fee_rule_no DESC
    LIMIT 1
)
INSERT INTO bill (
    car_log_no,
    fee_rule_no,
    kiosk_no,
    charge_minutes,
    bill_amount,
    bill_status
)
SELECT
    inserted_log.car_log_no,
    selected_rule.fee_rule_no,
    NULL,
    0,
    0,
    'UNPAID'
FROM inserted_log
CROSS JOIN selected_rule
WHERE inserted_log.snapshot_car_no <> '299가1203';

-- 1203 방문차량의 미결제 고지서를 미리 생성한다.
-- 더미 실행 직후 res1 알림 화면과 입주민 결제 페이지를 확인할 수 있다.
WITH target_log AS (
    SELECT car_log_no
    FROM car_log
    WHERE snapshot_car_no = '299가1203'
    ORDER BY car_log_no DESC
    LIMIT 1
), selected_rule AS (
    SELECT fee_rule_no, unit_minutes, unit_fee, daily_max_fee
    FROM fee_rule
    WHERE effective_from <= CURRENT_TIMESTAMP
      AND (
          effective_to IS NULL
          OR effective_to > CURRENT_TIMESTAMP
      )
    ORDER BY effective_from DESC, fee_rule_no DESC
    LIMIT 1
)

INSERT INTO bill (
    car_log_no,
    fee_rule_no,
    kiosk_no,
    charge_minutes,
    bill_amount,
    bill_status,
    payment_order_id
)
SELECT
    target_log.car_log_no,
    selected_rule.fee_rule_no,
    NULL,
    120,
    CASE
        WHEN selected_rule.daily_max_fee IS NULL THEN
            CEIL(120.0 / selected_rule.unit_minutes) * selected_rule.unit_fee
        ELSE LEAST(
            CEIL(120.0 / selected_rule.unit_minutes) * selected_rule.unit_fee,
            selected_rule.daily_max_fee
        )
    END,
    'UNPAID',
    'BILL-DUMMY-' || MD5(
        RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT
    )
FROM target_log
CROSS JOIN selected_rule;

INSERT INTO mem_notice (
    recipient_member_no,
    reference_table,
    reference_no,
    notice_type,
    title,
    message
)
SELECT
    member.member_no,
    'bill',
    bill.bill_no,
    'VISIT_PARKING_FEE_ISSUED',
    '방문차량 주차요금 발생',
    '등록하신 방문차량 299가1203에 주차요금 '
        || TO_CHAR(bill.bill_amount, 'FM999,999,999,990')
        || '원이 부과되었습니다.'
FROM bill
JOIN car_log
    ON car_log.car_log_no = bill.car_log_no
JOIN vehicle_car
    ON vehicle_car.vehicle_car_no = car_log.vehicle_car_no
JOIN member
    ON member.member_no = vehicle_car.member_no
WHERE car_log.snapshot_car_no = '299가1203'
  AND member.login_id = 'res1'
ON CONFLICT ON CONSTRAINT uq_mem_notice_reference
DO NOTHING;


-- 관리자 정산 목록에서 직접 지난 기록 이동을 확인할 수 있도록
-- B2 출차와 결제가 모두 완료된 정산서 1건을 생성한다.
WITH inserted_camera AS (
    INSERT INTO camera_data (
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note
    )
    SELECT
        camera.camera_no,
        NULL,
        '299가1205',
        '299가1205',
        CURRENT_TIMESTAMP - INTERVAL '26 hours',
        'camera-data/img_000118.jpeg',
        'camera-data/crop/img_000118.jpg',
        TRUE,
        99.00,
        'B2-BILLING-TRASH-TEST-1'
    FROM gate
    JOIN camera
        ON camera.gate_no = gate.gate_no
       AND camera.camera_type = 'In'
    WHERE gate.gate_code = 'B2-IN-1'
    RETURNING camera_data_no, capture_time
), inserted_log AS (
    INSERT INTO car_log (
        vehicle_car_no,
        camera_data_no,
        in_gate_no,
        in_time,
        out_gate_no,
        out_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        NULL,
        inserted_camera.camera_data_no,
        in_gate.gate_no,
        inserted_camera.capture_time,
        out_gate.gate_no,
        CURRENT_TIMESTAMP - INTERVAL '5 minutes',
        1440,
        '299가1205',
        'VISIT'
    FROM inserted_camera
    JOIN gate in_gate
        ON in_gate.gate_code = 'B2-IN-1'
    JOIN gate out_gate
        ON out_gate.gate_code = 'B2-OUT-1'
    RETURNING car_log_no, out_time
), selected_rule AS (
    SELECT fee_rule_no
    FROM fee_rule
    WHERE effective_from <= CURRENT_TIMESTAMP
      AND (
          effective_to IS NULL
          OR effective_to > CURRENT_TIMESTAMP
      )
    ORDER BY effective_from DESC, fee_rule_no DESC
    LIMIT 1
)
INSERT INTO bill (
    car_log_no,
    fee_rule_no,
    kiosk_no,
    charge_minutes,
    bill_amount,
    bill_status,
    payment_order_id,
    payment_key,
    payment_method,
    issued_at,
    paid_at
)
SELECT
    inserted_log.car_log_no,
    selected_rule.fee_rule_no,
    NULL,
    60,
    2000,
    'PAID',
    'BILLING-TRASH-TEST-299GA1205',
    'BILLING-TRASH-TEST-KEY-299GA1205',
    '카드',
    inserted_log.out_time - INTERVAL '10 minutes',
    inserted_log.out_time - INTERVAL '5 minutes'
FROM inserted_log
CROSS JOIN selected_rule;




-- =====================================================
-- TEST VEHICLES
-- B1 키오스크 번호는 기본 더미 기준 1 또는 2를 사용한다.
-- B2 키오스크 번호는 기본 더미 기준 3 또는 4를 사용한다.
--
-- 299가1101: B1 입주민 차량, 번호 뒤 4자리 1101
--             B1 일반 주차면에 배정되어 로봇 출차 작업 생성 가능
-- 299가1102: B1 입주민 차량, 번호 뒤 4자리 1102
--             B1 일반 주차면에 배정되어 로봇 출차 작업 생성 가능
-- 299가1103: B1 입주민 차량, 번호 뒤 4자리 1103
--             B1 일반 주차면에 배정되어 로봇 출차 작업 생성 가능
--
-- 299가1201: 방금 입차한 방문차량, 24시간 무료, 정산금액 0원 확인용
-- 299가1202: 12시간 주차한 방문차량, 24시간 무료 범위, 정산금액 0원 확인용
-- 299가1203: 26시간 주차한 방문차량, 24시간 초과분 과금 확인용
-- 299가1204: 미등록차량, 무료시간 없음, 입차 직후부터 과금 확인용
-- 299가1205: B2 출차·결제 완료 차량, 관리자 직접 지난 기록 이동 확인용
--
-- 실행 직후 B2 입차 차량의 미결제 정산서가 함께 생성된다.
-- B2 키오스크에서 차량을 선택하면 주차시간에 맞춰 정산금액이 갱신된다.
-- 결제를 완료하면 정산완료 및 30분간 출차 가능으로 바뀐다.
-- =====================================================

COMMIT;
