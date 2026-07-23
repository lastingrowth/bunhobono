BEGIN;

-- 기존 데이터를 모두 비우고 PK 번호를 1부터 다시 시작합니다.
TRUNCATE TABLE
    trash_bin,
    notice,
    vehicle_nt,
    car_log,
    camera_data,
    camera,
    vehicle_car,
    gate,
    parking,
    member_archive,
    member
RESTART IDENTITY CASCADE;

-- =====================================================
-- 1. 회원
-- 전체 세대: 8개 동 × 20세대 = 160세대
-- 402동은 전체 빈 세대로 유지
-- =====================================================


INSERT INTO member
    (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone,
     role, create_at, delete_at, mem_status)
VALUES
    -- << 관리자 >> 
    -- ACTIVE: 근무
    ('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', NULL, 'ACTIVE'),
    ('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '경비원', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', NULL, 'ACTIVE'),
    -- ON_LEAVE: 휴직
    ('admin3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '휴직자', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', NULL, 'ON_LEAVE'),
    -- INACTIVE: 퇴사
    ('admin4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '퇴사자', '010-1111-1111', 'ADMIN', '2025-01-01 09:00:00', TIMESTAMP '2025-12-31 18:00:00', 'INACTIVE'),


    -- << 입주민 >> 
    -- [101동]
    -- 거주
    ('res1',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 101,  '마틴',             '010-2222-0101', 'RESIDENT', TIMESTAMP '2025-04-08 10:00:00', NULL,                              'ACTIVE'),
    -- 가입 승인 대기
    ('res2',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 201,  '제임스',           '010-2222-0102', 'PENDING',  TIMESTAMP '2026-07-18 11:20:00', NULL,                              'ACTIVE'),
    -- 전출 신청
    ('res3',         '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 301,  '오드리',           '010-2222-0103', 'RESIDENT', TIMESTAMP '2025-06-12 10:00:00', TIMESTAMP '2026-07-15 18:20:00', 'WITHDRAW_PENDING'),
    -- 전출 완료 / 빈 세대
    ('unit_101_401', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 401,  '미등록',           '미등록',        'RESIDENT', TIMESTAMP '2025-01-21 10:00:00', TIMESTAMP '2026-05-20 14:00:00', 'EMPTY'),

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
    ('res40',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1002, '미등록',     '미등록',        'RESIDENT', TIMESTAMP '2025-10-06 10:00:00', NULL,                              'ACTIVE'),

    -- [201동]
    -- 거주 15세대 / 가입 승인 대기 5세대
    ('res42',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 201,  '배리앨런',       '010-3000-0040', 'RESIDENT', TIMESTAMP '2025-02-17 10:00:00', NULL,                              'ACTIVE'),
    ('res43',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 301,  '아서커리',       '010-3000-0041', 'RESIDENT', TIMESTAMP '2025-03-03 10:00:00', NULL,                              'ACTIVE'),
    ('res41',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 101,  '다이애나프린스', '010-3000-0039', 'RESIDENT', TIMESTAMP '2025-02-03 10:00:00', NULL,                              'ACTIVE'),
    ('res44',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 401,  '할조던',         '010-3000-0042', 'RESIDENT', TIMESTAMP '2025-03-17 10:00:00', NULL,                              'ACTIVE'),
    ('res45',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 501,  '딕그레이슨',     '010-3000-0043', 'RESIDENT', TIMESTAMP '2025-03-31 10:00:00', NULL,                              'ACTIVE'),

    -- 가입 승인 대기
    ('res46',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 601,  '셀리나카일',     '010-3000-0044', 'PENDING',  TIMESTAMP '2026-07-16 09:20:00', NULL,                              'ACTIVE'),
    ('res47',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 701,  '할리퀸',         '010-3000-0045', 'PENDING',  TIMESTAMP '2026-07-17 10:20:00', NULL,                              'ACTIVE'),
    ('res48',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 801,  '존콘스탄틴',     '010-3000-0046', 'PENDING',  TIMESTAMP '2026-07-18 11:20:00', NULL,                              'ACTIVE'),
    ('res49',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 901,  '고죠사토루',     '010-3000-0047', 'PENDING',  TIMESTAMP '2026-07-19 12:20:00', NULL,                              'ACTIVE'),
    ('res50',        '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 1001, '이타도리유지',   '010-3000-0048', 'PENDING',  TIMESTAMP '2026-07-20 13:20:00', NULL,                              'ACTIVE'),

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
    ('res140',       '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 1002, '루이지',           '010-3000-0135', 'RESIDENT', TIMESTAMP '2025-10-21 10:00:00', NULL, 'ACTIVE'),

    -- [402동]
    -- 빈 세대 20세대
    ('unit_402_101',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 101,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-01-10 10:00:00', TIMESTAMP '2025-12-01 10:00:00', 'EMPTY'),
    ('unit_402_201',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 201,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-01-24 10:00:00', TIMESTAMP '2025-12-15 10:00:00', 'EMPTY'),
    ('unit_402_301',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 301,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-02-07 10:00:00', TIMESTAMP '2026-01-05 10:00:00', 'EMPTY'),
    ('unit_402_401',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 401,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-02-21 10:00:00', TIMESTAMP '2026-01-19 10:00:00', 'EMPTY'),
    ('unit_402_501',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 501,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-03-07 10:00:00', TIMESTAMP '2026-02-02 10:00:00', 'EMPTY'),
    ('unit_402_601',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 601,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-03-21 10:00:00', TIMESTAMP '2026-02-16 10:00:00', 'EMPTY'),
    ('unit_402_701',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 701,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-04-04 10:00:00', TIMESTAMP '2026-03-02 10:00:00', 'EMPTY'),
    ('unit_402_801',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 801,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-04-18 10:00:00', TIMESTAMP '2026-03-16 10:00:00', 'EMPTY'),
    ('unit_402_901',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 901,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-05-02 10:00:00', TIMESTAMP '2026-03-30 10:00:00', 'EMPTY'),
    ('unit_402_1001', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 1001, '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-05-16 10:00:00', TIMESTAMP '2026-04-13 10:00:00', 'EMPTY'),
    ('unit_402_102',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 102,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-05-30 10:00:00', TIMESTAMP '2026-04-27 10:00:00', 'EMPTY'),
    ('unit_402_202',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 202,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-06-13 10:00:00', TIMESTAMP '2026-05-11 10:00:00', 'EMPTY'),
    ('unit_402_302',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 302,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-06-27 10:00:00', TIMESTAMP '2026-05-25 10:00:00', 'EMPTY'),
    ('unit_402_402',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 402,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-07-11 10:00:00', TIMESTAMP '2026-06-08 10:00:00', 'EMPTY'),
    ('unit_402_502',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 502,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-07-25 10:00:00', TIMESTAMP '2026-06-22 10:00:00', 'EMPTY'),
    ('unit_402_602',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 602,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-08-08 10:00:00', TIMESTAMP '2026-07-01 10:00:00', 'EMPTY'),
    ('unit_402_702',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 702,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-08-22 10:00:00', TIMESTAMP '2026-07-04 10:00:00', 'EMPTY'),
    ('unit_402_802',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 802,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-09-05 10:00:00', TIMESTAMP '2026-07-08 10:00:00', 'EMPTY'),
    ('unit_402_902',  '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 902,  '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-09-19 10:00:00', TIMESTAMP '2026-07-12 10:00:00', 'EMPTY'),
    ('unit_402_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 1002, '미등록', '미등록', 'RESIDENT', TIMESTAMP '2025-10-03 10:00:00', TIMESTAMP '2026-07-16 10:00:00', 'EMPTY');


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
-- 2. 주차장
-- =====================================================
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
    ('A 주차장', 100, 'A 지하 주차장'),
    ('B 주차장', 80,  'B 지하 주차장'),
    ('C 주차장', 100, 'C 지하 주차장'),
    ('D 주차장', 50,  '지상 주차장');


-- =====================================================
-- 3. 게이트
-- =====================================================
INSERT INTO gate (parking_no, gate_name, gate_type)
VALUES
    (1, 'A-IN',  'In'),
    (1, 'A-OUT', 'Out'),
    (2, 'B-IN',  'In'),
    (2, 'B-OUT', 'Out'),
    (3, 'C-IN',  'In'),
    (3, 'C-OUT', 'Out'),
    (4, 'D-IN',  'In'),
    (4, 'D-OUT', 'Out');

 -- =====================================================
-- 4. 카메라
-- =====================================================
INSERT INTO camera (gate_no, camera_name, camera_type, install_date)
VALUES
    (1, 'CAM-A1', 'In',   DATE '2025-01-01'),
    (2, 'CAM-A2', 'Out',  DATE '2025-01-01'),
    (3, 'CAM-B1', 'In',   DATE '2025-01-03'),
    (4, 'CAM-B2', 'Out',  DATE '2025-01-03'),
    (5, 'CAM-C1', 'In',   DATE '2025-01-05'),
    (6, 'CAM-C2', 'Out',  DATE '2025-01-05'),
    (7, 'CAM-D1', 'In',   DATE '2025-02-05'),
    (8, 'CAM-D2', 'Out',  DATE '2025-02-05');

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
    -- 필수 9대: 입주민 7대, 방문 2대. 등록만 하고 입출차 기록에는 사용하지 않는다.
    ('normal', '222하5233', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '335 days', 5,  CURRENT_TIMESTAMP - INTERVAL '30 days'),
    ('normal', '26무3111',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP + INTERVAL '340 days', 9,  CURRENT_TIMESTAMP - INTERVAL '25 days'),
    ('normal', '41소2593',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP + INTERVAL '345 days', 10, CURRENT_TIMESTAMP - INTERVAL '20 days'),
    ('normal', '47조2603',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP + INTERVAL '347 days', 11, CURRENT_TIMESTAMP - INTERVAL '18 days'),
    ('normal', '81라7385',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '350 days', 12, CURRENT_TIMESTAMP - INTERVAL '15 days'),
    ('normal', '95마7152',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP + INTERVAL '353 days', 13, CURRENT_TIMESTAMP - INTERVAL '12 days'),
    ('normal', '67모4231',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '355 days', 14, CURRENT_TIMESTAMP - INTERVAL '10 days'),
    ('visit',  '222마2574', 'APPROVED', CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP, 15, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('visit',  '55오0359',  'APPROVED', CURRENT_TIMESTAMP - INTERVAL '5 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', 16, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- 현재 주차 및 통계용 입주민 차량 48대. 과거 만료 행은 두지 않고 새 1년 등록만 둔다.
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
SELECT 'normal', dp.car_no, 'APPROVED',
       CURRENT_TIMESTAMP - INTERVAL '30 days',
       CURRENT_TIMESTAMP + INTERVAL '335 days',
       9 + ((dp.plate_no - 1) % 100), CURRENT_TIMESTAMP - INTERVAL '30 days'
FROM demo_plate dp
WHERE dp.plate_no BETWEEN 1 AND 48;

-- 현재 주차 방문차량 9대. 1번은 오전 6~8시 승인 후 만료·미출차 상황이다.
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
SELECT 'visit', dp.car_no, 'APPROVED',
       CASE WHEN dp.plate_no = 49 THEN CURRENT_DATE + TIME '06:15:00'
            ELSE CURRENT_TIMESTAMP - INTERVAL '30 minutes' END,
       CASE WHEN dp.plate_no = 49 THEN CURRENT_DATE + TIME '07:15:00'
            ELSE CURRENT_TIMESTAMP + INTERVAL '4 hours 30 minutes' END,
       30 + (dp.plate_no - 48),
       CASE WHEN dp.plate_no = 49 THEN CURRENT_DATE + TIME '06:00:00'
            ELSE CURRENT_TIMESTAMP - INTERVAL '1 hour' END
FROM demo_plate dp
WHERE dp.plate_no BETWEEN 49 AND 57;

-- 방문승인 대기 알림 3건.
INSERT INTO vehicle_car
    (vehicle_type, car_no, vehicle_status, start_date, end_date, member_no, approved_at)
SELECT 'visit', dp.car_no, 'WAITING',
       CURRENT_TIMESTAMP - ((72 - dp.plate_no) * INTERVAL '5 minutes'),
       CURRENT_TIMESTAMP + INTERVAL '6 hours',
       50 + (dp.plate_no - 69), NULL
FROM demo_plate dp
WHERE dp.plate_no BETWEEN 69 AND 71;

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
       CURRENT_DATE - INTERVAL '1 day' + TIME '20:00:00' + (g * INTERVAL '4 minutes'),
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
       CURRENT_DATE + TIME '06:00:00' + (g * INTERVAL '3 minutes')
FROM generate_series(1, 40) AS g;

INSERT INTO demo_event
-- [DEMO RATIO] Completed records: each gate gets 1 visitor record.
SELECT 'BASE-V-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 48 + g), 'VISIT',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_DATE - INTERVAL '1 day' + TIME '21:00:00' + (g * INTERVAL '4 minutes'),
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
       CURRENT_DATE + TIME '06:10:00' + (g * INTERVAL '9 minutes')
FROM generate_series(1, 4) AS g;

INSERT INTO demo_event
-- [DEMO RATIO] Completed records: each gate gets 1 unknown record.
SELECT 'BASE-U-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 57 + g), 'UNKNOWN',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_DATE - INTERVAL '1 day' + TIME '22:00:00' + (g * INTERVAL '4 minutes'),
       (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
       CURRENT_DATE + TIME '06:20:00' + (g * INTERVAL '9 minutes')
FROM generate_series(1, 4) AS g;

-- 09시 이후 현재 시각 사이에 입주민 48대가 입차한다.
-- [DEMO RATIO] Each gate receives 12 currently parked resident vehicles.
INSERT INTO demo_event
SELECT 'NOW-R-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = g), 'REGISTERED',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_DATE + TIME '09:00:00'
         + ((CURRENT_TIMESTAMP - (CURRENT_DATE + TIME '09:00:00')) * (g::NUMERIC / 49)),
       NULL, NULL
FROM generate_series(1, 48) AS g;

-- 방문 8대 중 1대는 06:30 입차 후 만료, 나머지는 09시 이후 입차한다.
-- [DEMO RATIO] Each gate receives 2 currently parked visitor vehicles.
INSERT INTO demo_event
SELECT 'NOW-V-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 48 + g), 'VISIT',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CASE WHEN g = 1 THEN CURRENT_DATE + TIME '06:30:00'
            ELSE CURRENT_DATE + TIME '09:00:00'
              + ((CURRENT_TIMESTAMP - (CURRENT_DATE + TIME '09:00:00')) * (g::NUMERIC / 9)) END,
       NULL, NULL
FROM generate_series(1, 8) AS g;

-- 미등록 4대도 09시 이후 현재 시각 사이에 입차한다.
-- [DEMO RATIO] Each gate receives 1 currently parked unknown vehicle.
-- Resident : non-resident is 12 : 3 (4:1) at every gate.
INSERT INTO demo_event
SELECT 'NOW-U-' || g,
       (SELECT car_no FROM demo_plate WHERE plate_no = 57 + g), 'UNKNOWN',
       (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
       CURRENT_DATE + TIME '09:00:00'
         + ((CURRENT_TIMESTAMP - (CURRENT_DATE + TIME '09:00:00')) * (g::NUMERIC / 5)),
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
           CASE WHEN (o.image_no % 13) = 0 THEN FALSE ELSE TRUE END,
           CASE WHEN (o.image_no % 13) = 0 THEN 91.40 ELSE 98.20 END,
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
     in_gate_no, in_time, out_gate_no, out_time, snapshot_car_no)
SELECT vc.vehicle_car_no, cin.camera_data_no, cout.camera_data_no,
       e.in_gate_no, e.in_time, e.out_gate_no, e.out_time, e.car_no
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
-- 8. 알림 관리: 미확인/확인/처리완료를 모두 제공한다.
-- =====================================================
INSERT INTO notice
    (car_log_no, detect_at, stay_days, alert_stat,
     handled_by_member_no, handled_at, snapshot_car_log_no,
     snapshot_registered_car_no, snapshot_captured_car_no,
     snapshot_car_kind, snapshot_parking_name, snapshot_in_time)
SELECT cl.car_log_no,
       CURRENT_TIMESTAMP - (x.age_hours * INTERVAL '1 hour'),
       x.stay_days, x.alert_stat,
       CASE WHEN x.alert_stat = 'Unresolved' THEN NULL ELSE 1 END,
       CASE WHEN x.alert_stat = 'Unresolved' THEN NULL
            ELSE CURRENT_TIMESTAMP - ((x.age_hours - 1) * INTERVAL '1 hour') END,
       cl.car_log_no, vc.car_no, cl.snapshot_car_no, x.car_kind,
       p.parking_name, cl.in_time
FROM (VALUES
    ('225하2171','VISIT',  'Unresolved',1,2),
    ('103호3307','UNKNOWN','Unresolved',2,3),
    ('143모8849','VISIT',  'Checked',   2,8),
    ('40거2054', 'UNKNOWN','Resolved',  3,9),
    ('91어6511', 'VISIT',  'Resolved',  3,16),
    ('48나8278', 'UNKNOWN','Resolved',  4,20)
) AS x(car_no, car_kind, alert_stat, stay_days, age_hours)
JOIN LATERAL (
    SELECT * FROM car_log cl0
    WHERE cl0.snapshot_car_no = x.car_no
    ORDER BY cl0.in_time DESC LIMIT 1
) cl ON TRUE
LEFT JOIN vehicle_car vc ON vc.vehicle_car_no = cl.vehicle_car_no
JOIN gate g ON g.gate_no = cl.in_gate_no
JOIN parking p ON p.parking_no = g.parking_no;

-- =====================================================
-- 9. 통계 전용 지난 입출차 기록
-- statistics_scope 주석 필드는 통계 화면에서 용도별 중복 집계를 막는다.
-- =====================================================

-- 최근 22일 시간대별 평균: 50,50,36,10,10,10,42,50,50 패턴.
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT 'CAR_LOG', 20000 + ROW_NUMBER() OVER (ORDER BY d, g),
       jsonb_build_object(
           'car_log_no', 20000 + ROW_NUMBER() OVER (ORDER BY d, g),
           'vehicle_car_no', NULL,
           'camera_data_no', NULL,
           'in_gate_no', (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
           'in_time',
               CASE WHEN g <= 32
                    THEN (CURRENT_DATE - d - 1) + TIME '16:20:00' + (g * INTERVAL '3 minutes')
                    ELSE (CURRENT_DATE - d - 1) + TIME '18:00:00' + ((g - 32) * INTERVAL '20 minutes') END,
           'out_gate_no', (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
           'out_time',
               CASE WHEN g <= 14
                    THEN (CURRENT_DATE - d) + TIME '05:00:00' + (g * INTERVAL '3 minutes')
                    WHEN g <= 40
                    THEN (CURRENT_DATE - d) + TIME '06:00:00' + ((g - 14) * INTERVAL '6 minutes')
                    ELSE (CURRENT_DATE - d) + TIME '18:30:00' + ((g - 40) * INTERVAL '12 minutes') END,
           'snapshot_car_no', dp.car_no,
           'captured_car_no', dp.car_no,
           'car_kind', 'REGISTERED',
           'statistics_scope', 'HOURLY',
           'statistics_date', (CURRENT_DATE - d)
       ),
       'SCHEDULED', CURRENT_TIMESTAMP - (d * INTERVAL '1 hour'),
       CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM generate_series(1,22) AS d
CROSS JOIN generate_series(1,50) AS g
JOIN demo_plate dp ON dp.plate_no = 1 + ((g - 1) % 48);

-- 같은 통계일의 저녁 귀가 50대: 18시에 32대, 21시에 50대가 돌아온다.
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT 'CAR_LOG', 23000 + ROW_NUMBER() OVER (ORDER BY d, g),
       jsonb_build_object(
           'car_log_no', 23000 + ROW_NUMBER() OVER (ORDER BY d, g),
           'vehicle_car_no', NULL,
           'camera_data_no', NULL,
           'in_gate_no', (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
           'in_time',
               CASE WHEN g <= 32
                    THEN (CURRENT_DATE - d) + TIME '16:20:00' + (g * INTERVAL '3 minutes')
                    ELSE (CURRENT_DATE - d) + TIME '18:00:00' + ((g - 32) * INTERVAL '10 minutes') END,
           'out_gate_no', (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
           'out_time', (CURRENT_DATE - d + 1) + TIME '00:30:00',
           'snapshot_car_no', dp.car_no,
           'captured_car_no', dp.car_no,
           'car_kind', 'REGISTERED',
           'statistics_scope', 'HOURLY',
           'statistics_date', (CURRENT_DATE - d)
       ),
       'SCHEDULED', CURRENT_TIMESTAMP - (d * INTERVAL '1 hour'),
       CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM generate_series(1,22) AS d
CROSS JOIN generate_series(1,50) AS g
JOIN demo_plate dp ON dp.plate_no = 1 + ((g - 1) % 48);

-- 최근 22일 주간/월간 비교 및 평균 주차시간용 기록.
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT 'CAR_LOG', 30000 + ROW_NUMBER() OVER (ORDER BY d, kind_order, g),
       jsonb_build_object(
           'car_log_no', 30000 + ROW_NUMBER() OVER (ORDER BY d, kind_order, g),
           'vehicle_car_no', NULL,
           'camera_data_no', NULL,
           'in_gate_no', (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
           'in_time', (CURRENT_DATE - d) + TIME '09:05:00' + (g * INTERVAL '21 minutes'),
           'out_gate_no', (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
           'out_time', (CURRENT_DATE - d) + TIME '09:05:00' + (g * INTERVAL '21 minutes') + stay_interval,
           'snapshot_car_no', dp.car_no,
           'captured_car_no', dp.car_no,
           'car_kind', car_kind,
           'statistics_scope', 'ENTRY_AVERAGE'
       ),
       'SCHEDULED', CURRENT_TIMESTAMP - (d * INTERVAL '1 hour'),
       CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM generate_series(1,22) AS d
CROSS JOIN LATERAL (VALUES
    (1,'REGISTERED',24,INTERVAL '3 hours',1),
    (2,'VISIT',       9,INTERVAL '4 hours 30 minutes',49),
    (3,'UNKNOWN',    11,INTERVAL '1 hour 30 minutes',58)
) kind(kind_order,car_kind,amount,stay_interval,first_plate)
CROSS JOIN LATERAL generate_series(1,kind.amount) AS g
JOIN demo_plate dp
  ON dp.plate_no = kind.first_plate + ((g - 1) % kind.amount);

-- 2026년 1~6월 연간 입차 비교용 지난 기록.
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT 'CAR_LOG', 50000 + ROW_NUMBER() OVER (ORDER BY m, kind_order, g),
       jsonb_build_object(
           'car_log_no', 50000 + ROW_NUMBER() OVER (ORDER BY m, kind_order, g),
           'vehicle_car_no', NULL,
           'camera_data_no', NULL,
           'in_gate_no', (ARRAY[1,3,5,7])[1 + ((g - 1) % 4)],
           'in_time', MAKE_TIMESTAMP(2026,m,1 + ((g * 3) % 25),8 + (g % 8),10,0),
           'out_gate_no', (ARRAY[2,4,6,8])[1 + ((g - 1) % 4)],
           'out_time', MAKE_TIMESTAMP(2026,m,1 + ((g * 3) % 25),8 + (g % 8),10,0) + stay_interval,
           'snapshot_car_no', dp.car_no,
           'captured_car_no', dp.car_no,
           'car_kind', car_kind,
           'statistics_scope', 'ENTRY_AVERAGE'
       ),
       'SCHEDULED', CURRENT_TIMESTAMP - INTERVAL '1 day',
       CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM generate_series(1,6) AS m
CROSS JOIN LATERAL (VALUES
    (1,'REGISTERED',18 + (m * 2),INTERVAL '3 hours',1),
    (2,'VISIT',       7 + m,      INTERVAL '4 hours 30 minutes',49),
    (3,'UNKNOWN',     9 + m,      INTERVAL '1 hour 30 minutes',58)
) kind(kind_order,car_kind,amount,stay_interval,first_plate)
CROSS JOIN LATERAL generate_series(1,kind.amount) AS g
JOIN demo_plate dp
  ON dp.plate_no = kind.first_plate + ((g - 1) %
      CASE WHEN kind.car_kind='REGISTERED' THEN 48
           WHEN kind.car_kind='VISIT' THEN 9 ELSE 11 END);

-- =====================================================
-- 10. 일반 지난 기록: 페이지 크기 10 기준 추가 4페이지(36건)
-- 복원 화면에서도 필드 형식이 맞도록 유형별 JSON 구조를 사용한다.
-- =====================================================
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
SELECT
    CASE WHEN g <= 12 THEN 'CAMERA_DATA'
         WHEN g <= 24 THEN 'CAR_LOG'
         ELSE 'NOTICE' END,
    10000 + g,
    CASE WHEN g <= 12 THEN jsonb_build_object(
        'camera_data_no',10000+g,'camera_no',1,'vehicle_car_no',NULL,
        'car_no','88아'||LPAD(g::TEXT,4,'0'),'ocr_car_no','88아'||LPAD(g::TEXT,4,'0'),
        'capture_time',(CURRENT_TIMESTAMP - ((40+g) * INTERVAL '1 day')),
        'image_path',NULL,'crop_image_path',NULL,'recognition_state',TRUE,'confidence_score',97.5)
      WHEN g <= 24 THEN jsonb_build_object(
        'car_log_no',10000+g,'vehicle_car_no',NULL,'camera_data_no',NULL,
        'in_gate_no',1,'in_time',(CURRENT_TIMESTAMP - ((40+g) * INTERVAL '1 day')),
        'out_gate_no',2,'out_time',(CURRENT_TIMESTAMP - ((40+g) * INTERVAL '1 day') + INTERVAL '90 minutes'),
        'free_time',NULL,'snapshot_car_no','88아'||LPAD(g::TEXT,4,'0'),
        'captured_car_no','88아'||LPAD(g::TEXT,4,'0'))
      ELSE jsonb_build_object(
        'notice_no',10000+g,'car_log_no',NULL,
        'detect_at',(CURRENT_TIMESTAMP - ((40+g) * INTERVAL '1 day')),
        'stay_days',2,'alert_stat','Resolved','handled_by_member_no',1,
        'handled_at',(CURRENT_TIMESTAMP - ((39+g) * INTERVAL '1 day')),
        'snapshot_car_log_no',NULL,'snapshot_registered_car_no',NULL,
        'snapshot_captured_car_no','88아'||LPAD(g::TEXT,4,'0'),
        'snapshot_car_kind','UNKNOWN','snapshot_parking_name','A 주차장',
        'snapshot_in_time',(CURRENT_TIMESTAMP - ((42+g) * INTERVAL '1 day')))
    END,
    CASE WHEN (g % 3) = 0 THEN 'MANUAL' ELSE 'SCHEDULED' END,
    CURRENT_TIMESTAMP - (g * INTERVAL '2 hours'),
    CURRENT_TIMESTAMP + INTERVAL '30 days' - (g * INTERVAL '2 hours')
FROM generate_series(1, 36) AS g;

SELECT setval(pg_get_serial_sequence('vehicle_car','vehicle_car_no'), MAX(vehicle_car_no), TRUE) FROM vehicle_car;
SELECT setval(pg_get_serial_sequence('camera_data','camera_data_no'), MAX(camera_data_no), TRUE) FROM camera_data;
SELECT setval(pg_get_serial_sequence('car_log','car_log_no'), MAX(car_log_no), TRUE) FROM car_log;
SELECT setval(pg_get_serial_sequence('notice','notice_no'), MAX(notice_no), TRUE) FROM notice;
SELECT setval(pg_get_serial_sequence('trash_bin','trash_no'), MAX(trash_no), TRUE) FROM trash_bin;

COMMIT;
