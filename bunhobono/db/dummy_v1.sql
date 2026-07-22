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
-- 4. 차량
-- normal: 입주민 차량
-- visit : 방문 차량
-- =====================================================
INSERT INTO vehicle_car
    (vehicle_type, car_no, alias_car_no, vehicle_status, start_date, end_date, member_no, approved_at)
VALUES
    -- =====================================================
    -- 입주민 차량 normal 90대
    -- 차량 등록일은 회원 가입일 이후로 설정
    -- end_date는 start_date 기준 1년 후
    -- =====================================================
    ('normal', '222하5233', NULL, 'APPROVED', TIMESTAMP '2025-04-09 09:00:00', TIMESTAMP '2026-04-09 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res1'),  TIMESTAMP '2025-04-09 10:00:00'),
    ('normal', '26무3111',  NULL, 'APPROVED', TIMESTAMP '2025-04-10 09:00:00', TIMESTAMP '2026-04-10 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res1'),  TIMESTAMP '2025-04-10 10:00:00'),

    -- res2는 가입 승인 대기, res3은 전출 신청, unit_101_401은 빈 세대라 차량 등록 제외

    ('normal', '41소2593',  NULL, 'APPROVED', TIMESTAMP '2025-02-15 09:00:00', TIMESTAMP '2026-02-15 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res5'),  TIMESTAMP '2025-02-15 10:00:00'),
    ('normal', '47조2603',  NULL, 'APPROVED', TIMESTAMP '2025-03-04 09:00:00', TIMESTAMP '2026-03-04 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res6'),  TIMESTAMP '2025-03-04 10:00:00'),
    ('normal', '817라7385', NULL, 'APPROVED', TIMESTAMP '2025-03-28 09:00:00', TIMESTAMP '2026-03-28 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res7'),  TIMESTAMP '2025-03-28 10:00:00'),
    ('normal', '95마7152',  NULL, 'APPROVED', TIMESTAMP '2025-04-20 09:00:00', TIMESTAMP '2026-04-20 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res8'),  TIMESTAMP '2025-04-20 10:00:00'),
    ('normal', '67모4231',  NULL, 'APPROVED', TIMESTAMP '2025-05-12 09:00:00', TIMESTAMP '2026-05-12 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res9'),  TIMESTAMP '2025-05-12 10:00:00'),
    ('normal', '222마2574', NULL, 'APPROVED', TIMESTAMP '2025-06-03 09:00:00', TIMESTAMP '2026-06-03 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res10'), TIMESTAMP '2025-06-03 10:00:00'),
    ('normal', '55오0359',  NULL, 'APPROVED', TIMESTAMP '2025-06-24 09:00:00', TIMESTAMP '2026-06-24 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res11'), TIMESTAMP '2025-06-24 10:00:00'),
    ('normal', '80가1010',  NULL, 'APPROVED', TIMESTAMP '2025-07-16 09:00:00', TIMESTAMP '2026-07-16 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res12'), TIMESTAMP '2025-07-16 10:00:00'),

    ('normal', '12나1013', NULL, 'APPROVED', TIMESTAMP '2025-08-05 09:00:00', TIMESTAMP '2026-08-05 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res13'), TIMESTAMP '2025-08-05 10:00:00'),
    ('normal', '18다1014', NULL, 'APPROVED', TIMESTAMP '2025-08-27 09:00:00', TIMESTAMP '2026-08-27 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res14'), TIMESTAMP '2025-08-27 10:00:00'),
    ('normal', '25라1015', NULL, 'APPROVED', TIMESTAMP '2025-09-18 09:00:00', TIMESTAMP '2026-09-18 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res15'), TIMESTAMP '2025-09-18 10:00:00'),
    ('normal', '32마1016', NULL, 'APPROVED', TIMESTAMP '2025-10-10 09:00:00', TIMESTAMP '2026-10-10 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res16'), TIMESTAMP '2025-10-10 10:00:00'),
    ('normal', '39바1017', NULL, 'APPROVED', TIMESTAMP '2025-11-02 09:00:00', TIMESTAMP '2026-11-02 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res17'), TIMESTAMP '2025-11-02 10:00:00'),
    ('normal', '46사1018', NULL, 'APPROVED', TIMESTAMP '2025-11-24 09:00:00', TIMESTAMP '2026-11-24 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res18'), TIMESTAMP '2025-11-24 10:00:00'),
    ('normal', '53아1019', NULL, 'APPROVED', TIMESTAMP '2025-12-13 09:00:00', TIMESTAMP '2026-12-13 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res19'), TIMESTAMP '2025-12-13 10:00:00'),
    ('normal', '60자1020', NULL, 'APPROVED', TIMESTAMP '2026-01-09 09:00:00', TIMESTAMP '2027-01-09 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res20'), TIMESTAMP '2026-01-09 10:00:00'),

    ('normal', '67차1021', NULL, 'APPROVED', TIMESTAMP '2025-01-14 09:00:00', TIMESTAMP '2026-01-14 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res21'), TIMESTAMP '2025-01-14 10:00:00'),
    ('normal', '74카1022', NULL, 'APPROVED', TIMESTAMP '2025-01-28 09:00:00', TIMESTAMP '2026-01-28 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res22'), TIMESTAMP '2025-01-28 10:00:00'),
    ('normal', '81타1023', NULL, 'APPROVED', TIMESTAMP '2025-02-11 09:00:00', TIMESTAMP '2026-02-11 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res23'), TIMESTAMP '2025-02-11 10:00:00'),
    ('normal', '88파1024', NULL, 'APPROVED', TIMESTAMP '2025-02-25 09:00:00', TIMESTAMP '2026-02-25 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res24'), TIMESTAMP '2025-02-25 10:00:00'),
    ('normal', '95하1025', NULL, 'APPROVED', TIMESTAMP '2025-03-11 09:00:00', TIMESTAMP '2026-03-11 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res25'), TIMESTAMP '2025-03-11 10:00:00'),
    ('normal', '12거1026', NULL, 'APPROVED', TIMESTAMP '2025-03-25 09:00:00', TIMESTAMP '2026-03-25 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res26'), TIMESTAMP '2025-03-25 10:00:00'),
    ('normal', '19너1027', NULL, 'APPROVED', TIMESTAMP '2025-04-08 09:00:00', TIMESTAMP '2026-04-08 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res27'), TIMESTAMP '2025-04-08 10:00:00'),
    ('normal', '26더1028', NULL, 'APPROVED', TIMESTAMP '2025-04-22 09:00:00', TIMESTAMP '2026-04-22 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res28'), TIMESTAMP '2025-04-22 10:00:00'),
    ('normal', '33러1029', NULL, 'APPROVED', TIMESTAMP '2025-05-06 09:00:00', TIMESTAMP '2026-05-06 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res29'), TIMESTAMP '2025-05-06 10:00:00'),
    ('normal', '40머1030', NULL, 'APPROVED', TIMESTAMP '2025-05-20 09:00:00', TIMESTAMP '2026-05-20 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res30'), TIMESTAMP '2025-05-20 10:00:00'),

    ('normal', '47버1031', NULL, 'APPROVED', TIMESTAMP '2025-06-03 09:00:00', TIMESTAMP '2026-06-03 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res31'), TIMESTAMP '2025-06-03 10:00:00'),
    ('normal', '54서1032', NULL, 'APPROVED', TIMESTAMP '2025-06-17 09:00:00', TIMESTAMP '2026-06-17 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res32'), TIMESTAMP '2025-06-17 10:00:00'),
    ('normal', '61어1033', NULL, 'APPROVED', TIMESTAMP '2025-07-01 09:00:00', TIMESTAMP '2026-07-01 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res33'), TIMESTAMP '2025-07-01 10:00:00'),
    ('normal', '68저1034', NULL, 'APPROVED', TIMESTAMP '2025-07-15 09:00:00', TIMESTAMP '2026-07-15 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res34'), TIMESTAMP '2025-07-15 10:00:00'),
    ('normal', '75처1035', NULL, 'APPROVED', TIMESTAMP '2025-07-29 09:00:00', TIMESTAMP '2026-07-29 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res35'), TIMESTAMP '2025-07-29 10:00:00'),
    ('normal', '82커1036', NULL, 'APPROVED', TIMESTAMP '2025-08-12 09:00:00', TIMESTAMP '2026-08-12 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res36'), TIMESTAMP '2025-08-12 10:00:00'),
    ('normal', '89터1037', NULL, 'APPROVED', TIMESTAMP '2025-08-26 09:00:00', TIMESTAMP '2026-08-26 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res37'), TIMESTAMP '2025-08-26 10:00:00'),
    ('normal', '96퍼1038', NULL, 'APPROVED', TIMESTAMP '2025-09-09 09:00:00', TIMESTAMP '2026-09-09 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res38'), TIMESTAMP '2025-09-09 10:00:00'),
    ('normal', '13고1039', NULL, 'APPROVED', TIMESTAMP '2025-09-23 09:00:00', TIMESTAMP '2026-09-23 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res39'), TIMESTAMP '2025-09-23 10:00:00'),
    ('normal', '20노1040', NULL, 'APPROVED', TIMESTAMP '2025-10-07 09:00:00', TIMESTAMP '2026-10-07 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res40'), TIMESTAMP '2025-10-07 10:00:00'),

    ('normal', '27도1041', NULL, 'APPROVED', TIMESTAMP '2025-02-04 09:00:00', TIMESTAMP '2026-02-04 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res41'), TIMESTAMP '2025-02-04 10:00:00'),
    ('normal', '34로1042', NULL, 'APPROVED', TIMESTAMP '2025-02-18 09:00:00', TIMESTAMP '2026-02-18 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res42'), TIMESTAMP '2025-02-18 10:00:00'),
    ('normal', '41모1043', NULL, 'APPROVED', TIMESTAMP '2025-03-04 09:00:00', TIMESTAMP '2026-03-04 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res43'), TIMESTAMP '2025-03-04 10:00:00'),
    ('normal', '48보1044', NULL, 'APPROVED', TIMESTAMP '2025-03-18 09:00:00', TIMESTAMP '2026-03-18 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res44'), TIMESTAMP '2025-03-18 10:00:00'),
    ('normal', '55소1045', NULL, 'APPROVED', TIMESTAMP '2025-04-01 09:00:00', TIMESTAMP '2026-04-01 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res45'), TIMESTAMP '2025-04-01 10:00:00'),

    -- res46~res50은 가입 승인 대기라 차량 등록 제외

    ('normal', '97후1051', NULL, 'APPROVED', TIMESTAMP '2025-04-15 09:00:00', TIMESTAMP '2026-04-15 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res51'), TIMESTAMP '2025-04-15 10:00:00'),
    ('normal', '14구1052', NULL, 'APPROVED', TIMESTAMP '2025-04-29 09:00:00', TIMESTAMP '2026-04-29 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res52'), TIMESTAMP '2025-04-29 10:00:00'),
    ('normal', '21누1053', NULL, 'APPROVED', TIMESTAMP '2025-05-13 09:00:00', TIMESTAMP '2026-05-13 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res53'), TIMESTAMP '2025-05-13 10:00:00'),
    ('normal', '28두1054', NULL, 'APPROVED', TIMESTAMP '2025-05-27 09:00:00', TIMESTAMP '2026-05-27 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res54'), TIMESTAMP '2025-05-27 10:00:00'),
    ('normal', '35루1055', NULL, 'APPROVED', TIMESTAMP '2025-06-10 09:00:00', TIMESTAMP '2026-06-10 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res55'), TIMESTAMP '2025-06-10 10:00:00'),
    ('normal', '42무1056', NULL, 'APPROVED', TIMESTAMP '2025-06-24 09:00:00', TIMESTAMP '2026-06-24 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res56'), TIMESTAMP '2025-06-24 10:00:00'),
    ('normal', '49부1057', NULL, 'APPROVED', TIMESTAMP '2025-07-08 09:00:00', TIMESTAMP '2026-07-08 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res57'), TIMESTAMP '2025-07-08 10:00:00'),
    ('normal', '56수1058', NULL, 'APPROVED', TIMESTAMP '2025-07-22 09:00:00', TIMESTAMP '2026-07-22 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res58'), TIMESTAMP '2025-07-22 10:00:00'),
    ('normal', '63우1059', NULL, 'APPROVED', TIMESTAMP '2025-08-05 09:00:00', TIMESTAMP '2026-08-05 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res59'), TIMESTAMP '2025-08-05 10:00:00'),
    ('normal', '70주1060', NULL, 'APPROVED', TIMESTAMP '2025-08-19 09:00:00', TIMESTAMP '2026-08-19 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res60'), TIMESTAMP '2025-08-19 10:00:00'),

    ('normal', '77추1061', NULL, 'APPROVED', TIMESTAMP '2025-01-17 09:00:00', TIMESTAMP '2026-01-17 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res61'), TIMESTAMP '2025-01-17 10:00:00'),
    ('normal', '84크1062', NULL, 'APPROVED', TIMESTAMP '2025-01-31 09:00:00', TIMESTAMP '2026-01-31 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res62'), TIMESTAMP '2025-01-31 10:00:00'),
    ('normal', '91투1063', NULL, 'APPROVED', TIMESTAMP '2025-02-14 09:00:00', TIMESTAMP '2026-02-14 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res63'), TIMESTAMP '2025-02-14 10:00:00'),
    ('normal', '98프1064', NULL, 'APPROVED', TIMESTAMP '2025-02-28 09:00:00', TIMESTAMP '2026-02-28 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res64'), TIMESTAMP '2025-02-28 10:00:00'),
    ('normal', '15그1065', NULL, 'APPROVED', TIMESTAMP '2025-03-14 09:00:00', TIMESTAMP '2026-03-14 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res65'), TIMESTAMP '2025-03-14 10:00:00'),
    ('normal', '22느1066', NULL, 'APPROVED', TIMESTAMP '2025-03-28 09:00:00', TIMESTAMP '2026-03-28 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res66'), TIMESTAMP '2025-03-28 10:00:00'),
    ('normal', '29드1067', NULL, 'APPROVED', TIMESTAMP '2025-04-11 09:00:00', TIMESTAMP '2026-04-11 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res67'), TIMESTAMP '2025-04-11 10:00:00'),
    ('normal', '36르1068', NULL, 'APPROVED', TIMESTAMP '2025-04-25 09:00:00', TIMESTAMP '2026-04-25 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res68'), TIMESTAMP '2025-04-25 10:00:00'),
    ('normal', '43므1069', NULL, 'APPROVED', TIMESTAMP '2025-05-09 09:00:00', TIMESTAMP '2026-05-09 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res69'), TIMESTAMP '2025-05-09 10:00:00'),
    ('normal', '50브1070', NULL, 'APPROVED', TIMESTAMP '2025-05-23 09:00:00', TIMESTAMP '2026-05-23 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res70'), TIMESTAMP '2025-05-23 10:00:00'),

    ('normal', '57스1071', NULL, 'APPROVED', TIMESTAMP '2025-06-06 09:00:00', TIMESTAMP '2026-06-06 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res71'), TIMESTAMP '2025-06-06 10:00:00'),
    ('normal', '64으1072', NULL, 'APPROVED', TIMESTAMP '2025-06-20 09:00:00', TIMESTAMP '2026-06-20 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res72'), TIMESTAMP '2025-06-20 10:00:00'),
    ('normal', '71즈1073', NULL, 'APPROVED', TIMESTAMP '2025-07-04 09:00:00', TIMESTAMP '2026-07-04 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res73'), TIMESTAMP '2025-07-04 10:00:00'),
    ('normal', '78츠1074', NULL, 'APPROVED', TIMESTAMP '2025-07-18 09:00:00', TIMESTAMP '2026-07-18 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res74'), TIMESTAMP '2025-07-18 10:00:00'),
    ('normal', '85흐1075', NULL, 'APPROVED', TIMESTAMP '2025-08-01 09:00:00', TIMESTAMP '2026-08-01 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res75'), TIMESTAMP '2025-08-01 10:00:00'),
    ('normal', '92기1076', NULL, 'APPROVED', TIMESTAMP '2025-08-15 09:00:00', TIMESTAMP '2026-08-15 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res76'), TIMESTAMP '2025-08-15 10:00:00'),
    ('normal', '99니1077', NULL, 'APPROVED', TIMESTAMP '2025-08-29 09:00:00', TIMESTAMP '2026-08-29 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res77'), TIMESTAMP '2025-08-29 10:00:00'),
    ('normal', '16디1078', NULL, 'APPROVED', TIMESTAMP '2025-09-12 09:00:00', TIMESTAMP '2026-09-12 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res78'), TIMESTAMP '2025-09-12 10:00:00'),
    ('normal', '23리1079', NULL, 'APPROVED', TIMESTAMP '2025-09-26 09:00:00', TIMESTAMP '2026-09-26 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res79'), TIMESTAMP '2025-09-26 10:00:00'),
    ('normal', '30미1080', NULL, 'APPROVED', TIMESTAMP '2025-10-10 09:00:00', TIMESTAMP '2026-10-10 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res80'), TIMESTAMP '2025-10-10 10:00:00'),

    ('normal', '37비1081', NULL, 'APPROVED', TIMESTAMP '2025-01-21 09:00:00', TIMESTAMP '2026-01-21 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res81'), TIMESTAMP '2025-01-21 10:00:00'),
    ('normal', '44시1082', NULL, 'APPROVED', TIMESTAMP '2025-02-04 09:00:00', TIMESTAMP '2026-02-04 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res82'), TIMESTAMP '2025-02-04 10:00:00'),
    ('normal', '51이1083', NULL, 'APPROVED', TIMESTAMP '2025-02-18 09:00:00', TIMESTAMP '2026-02-18 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res83'), TIMESTAMP '2025-02-18 10:00:00'),
    ('normal', '58치1084', NULL, 'APPROVED', TIMESTAMP '2025-03-04 09:00:00', TIMESTAMP '2026-03-04 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res84'), TIMESTAMP '2025-03-04 10:00:00'),
    ('normal', '65키1085', NULL, 'APPROVED', TIMESTAMP '2025-03-18 09:00:00', TIMESTAMP '2026-03-18 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res85'), TIMESTAMP '2025-03-18 10:00:00'),
    ('normal', '72피1086', NULL, 'APPROVED', TIMESTAMP '2025-04-01 09:00:00', TIMESTAMP '2026-04-01 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res86'), TIMESTAMP '2025-04-01 10:00:00'),
    ('normal', '79히1087', NULL, 'APPROVED', TIMESTAMP '2025-04-15 09:00:00', TIMESTAMP '2026-04-15 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res87'), TIMESTAMP '2025-04-15 10:00:00'),
    ('normal', '86고1088', NULL, 'APPROVED', TIMESTAMP '2025-04-29 09:00:00', TIMESTAMP '2026-04-29 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res88'), TIMESTAMP '2025-04-29 10:00:00'),
    ('normal', '93노1089', NULL, 'APPROVED', TIMESTAMP '2025-05-13 09:00:00', TIMESTAMP '2026-05-13 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res89'), TIMESTAMP '2025-05-13 10:00:00'),
    ('normal', '10도1090', NULL, 'APPROVED', TIMESTAMP '2025-05-27 09:00:00', TIMESTAMP '2026-05-27 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res90'), TIMESTAMP '2025-05-27 10:00:00'),

    -- =====================================================
    -- 방문 차량 visit APPROVED 20대
    -- 현재 날짜 2026-07-22 기준 아직 유효한 방문 차량
    -- =====================================================
    ('visit',  '71거2001', NULL, 'APPROVED', TIMESTAMP '2026-07-22 08:00:00', TIMESTAMP '2026-07-22 22:00:00', (SELECT member_no FROM member WHERE login_id = 'res1'),  TIMESTAMP '2026-07-22 08:10:00'),
    ('visit',  '72너2002', NULL, 'APPROVED', TIMESTAMP '2026-07-22 09:00:00', TIMESTAMP '2026-07-22 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res5'),  TIMESTAMP '2026-07-22 09:10:00'),
    ('visit',  '73더2003', NULL, 'APPROVED', TIMESTAMP '2026-07-22 10:00:00', TIMESTAMP '2026-07-22 20:00:00', (SELECT member_no FROM member WHERE login_id = 'res6'),  TIMESTAMP '2026-07-22 10:10:00'),
    ('visit',  '74러2004', NULL, 'APPROVED', TIMESTAMP '2026-07-22 11:00:00', TIMESTAMP '2026-07-22 23:00:00', (SELECT member_no FROM member WHERE login_id = 'res7'),  TIMESTAMP '2026-07-22 11:10:00'),
    ('visit',  '75머2005', NULL, 'APPROVED', TIMESTAMP '2026-07-22 12:00:00', TIMESTAMP '2026-07-22 21:00:00', (SELECT member_no FROM member WHERE login_id = 'res8'),  TIMESTAMP '2026-07-22 12:10:00'),
    ('visit',  '76버2006', NULL, 'APPROVED', TIMESTAMP '2026-07-22 13:00:00', TIMESTAMP '2026-07-23 01:00:00', (SELECT member_no FROM member WHERE login_id = 'res9'),  TIMESTAMP '2026-07-22 13:10:00'),
    ('visit',  '77서2007', NULL, 'APPROVED', TIMESTAMP '2026-07-22 14:00:00', TIMESTAMP '2026-07-23 02:00:00', (SELECT member_no FROM member WHERE login_id = 'res10'), TIMESTAMP '2026-07-22 14:10:00'),
    ('visit',  '78어2008', NULL, 'APPROVED', TIMESTAMP '2026-07-22 15:00:00', TIMESTAMP '2026-07-23 00:00:00', (SELECT member_no FROM member WHERE login_id = 'res11'), TIMESTAMP '2026-07-22 15:10:00'),
    ('visit',  '79저2009', NULL, 'APPROVED', TIMESTAMP '2026-07-22 16:00:00', TIMESTAMP '2026-07-23 04:00:00', (SELECT member_no FROM member WHERE login_id = 'res12'), TIMESTAMP '2026-07-22 16:10:00'),
    ('visit',  '80처2010', NULL, 'APPROVED', TIMESTAMP '2026-07-22 17:00:00', TIMESTAMP '2026-07-23 05:00:00', (SELECT member_no FROM member WHERE login_id = 'res13'), TIMESTAMP '2026-07-22 17:10:00'),

    ('visit',  '81커2011', NULL, 'APPROVED', TIMESTAMP '2026-07-21 09:00:00', TIMESTAMP '2026-07-23 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res14'), TIMESTAMP '2026-07-21 09:10:00'),
    ('visit',  '82터2012', NULL, 'APPROVED', TIMESTAMP '2026-07-21 10:00:00', TIMESTAMP '2026-07-23 10:00:00', (SELECT member_no FROM member WHERE login_id = 'res15'), TIMESTAMP '2026-07-21 10:10:00'),
    ('visit',  '83퍼2013', NULL, 'APPROVED', TIMESTAMP '2026-07-21 11:00:00', TIMESTAMP '2026-07-23 11:00:00', (SELECT member_no FROM member WHERE login_id = 'res16'), TIMESTAMP '2026-07-21 11:10:00'),
    ('visit',  '84고2014', NULL, 'APPROVED', TIMESTAMP '2026-07-21 12:00:00', TIMESTAMP '2026-07-23 12:00:00', (SELECT member_no FROM member WHERE login_id = 'res17'), TIMESTAMP '2026-07-21 12:10:00'),
    ('visit',  '85노2015', NULL, 'APPROVED', TIMESTAMP '2026-07-21 13:00:00', TIMESTAMP '2026-07-23 13:00:00', (SELECT member_no FROM member WHERE login_id = 'res18'), TIMESTAMP '2026-07-21 13:10:00'),
    ('visit',  '86도2016', NULL, 'APPROVED', TIMESTAMP '2026-07-21 14:00:00', TIMESTAMP '2026-07-23 14:00:00', (SELECT member_no FROM member WHERE login_id = 'res19'), TIMESTAMP '2026-07-21 14:10:00'),
    ('visit',  '87로2017', NULL, 'APPROVED', TIMESTAMP '2026-07-21 15:00:00', TIMESTAMP '2026-07-23 15:00:00', (SELECT member_no FROM member WHERE login_id = 'res20'), TIMESTAMP '2026-07-21 15:10:00'),
    ('visit',  '88모2018', NULL, 'APPROVED', TIMESTAMP '2026-07-21 16:00:00', TIMESTAMP '2026-07-23 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res21'), TIMESTAMP '2026-07-21 16:10:00'),
    ('visit',  '89보2019', NULL, 'APPROVED', TIMESTAMP '2026-07-21 17:00:00', TIMESTAMP '2026-07-23 17:00:00', (SELECT member_no FROM member WHERE login_id = 'res22'), TIMESTAMP '2026-07-21 17:10:00'),
    ('visit',  '90소2020', NULL, 'APPROVED', TIMESTAMP '2026-07-21 18:00:00', TIMESTAMP '2026-07-23 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res23'), TIMESTAMP '2026-07-21 18:10:00'),

    -- =====================================================
    -- 방문 차량 visit WAITING 5대
    -- 승인 전이므로 approved_at은 NULL
    -- =====================================================
    ('visit',  '91오3001', NULL, 'WAITING',  TIMESTAMP '2026-07-23 09:00:00', TIMESTAMP '2026-07-23 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res24'), NULL),
    ('visit',  '92조3002', NULL, 'WAITING',  TIMESTAMP '2026-07-23 10:00:00', TIMESTAMP '2026-07-23 20:00:00', (SELECT member_no FROM member WHERE login_id = 'res25'), NULL),
    ('visit',  '93하3003', NULL, 'WAITING',  TIMESTAMP '2026-07-23 11:00:00', TIMESTAMP '2026-07-24 00:00:00', (SELECT member_no FROM member WHERE login_id = 'res26'), NULL),
    ('visit',  '94가3004', NULL, 'WAITING',  TIMESTAMP '2026-07-24 09:00:00', TIMESTAMP '2026-07-24 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res27'), NULL),
    ('visit',  '95나3005', NULL, 'WAITING',  TIMESTAMP '2026-07-24 10:00:00', TIMESTAMP '2026-07-24 22:00:00', (SELECT member_no FROM member WHERE login_id = 'res28'), NULL),

    -- =====================================================
    -- 방문 차량 visit EXPIRED 8대
    -- end_date가 2026-07-22보다 과거라 만료 상태
    -- 이 중 3대는 car_log에서 주차중, 5대는 출차완료로 연결 예정
    -- =====================================================
    ('visit',  '96다4001', NULL, 'EXPIRED',  TIMESTAMP '2026-07-20 09:00:00', TIMESTAMP '2026-07-20 21:00:00', (SELECT member_no FROM member WHERE login_id = 'res29'), TIMESTAMP '2026-07-20 09:10:00'),
    ('visit',  '97라4002', NULL, 'EXPIRED',  TIMESTAMP '2026-07-20 10:00:00', TIMESTAMP '2026-07-20 22:00:00', (SELECT member_no FROM member WHERE login_id = 'res30'), TIMESTAMP '2026-07-20 10:10:00'),
    ('visit',  '98마4003', NULL, 'EXPIRED',  TIMESTAMP '2026-07-20 11:00:00', TIMESTAMP '2026-07-20 23:00:00', (SELECT member_no FROM member WHERE login_id = 'res31'), TIMESTAMP '2026-07-20 11:10:00'),
    ('visit',  '99바4004', NULL, 'EXPIRED',  TIMESTAMP '2026-07-19 09:00:00', TIMESTAMP '2026-07-19 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res32'), TIMESTAMP '2026-07-19 09:10:00'),
    ('visit',  '10사4005', NULL, 'EXPIRED',  TIMESTAMP '2026-07-19 10:00:00', TIMESTAMP '2026-07-19 19:00:00', (SELECT member_no FROM member WHERE login_id = 'res33'), TIMESTAMP '2026-07-19 10:10:00'),
    ('visit',  '11아4006', NULL, 'EXPIRED',  TIMESTAMP '2026-07-18 09:00:00', TIMESTAMP '2026-07-18 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res34'), TIMESTAMP '2026-07-18 09:10:00'),
    ('visit',  '12자4007', NULL, 'EXPIRED',  TIMESTAMP '2026-07-18 10:00:00', TIMESTAMP '2026-07-18 19:00:00', (SELECT member_no FROM member WHERE login_id = 'res35'), TIMESTAMP '2026-07-18 10:10:00'),
    ('visit',  '13차4008', NULL, 'EXPIRED',  TIMESTAMP '2026-07-17 09:00:00', TIMESTAMP '2026-07-17 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res36'), TIMESTAMP '2026-07-17 09:10:00');


-- =====================================================
-- 5. 카메라
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
-- 6. 카메라 데이터
-- =====================================================
-- camera_data는 카메라가 차량을 촬영하고 OCR로 번호를 읽은 원본 기록입니다.
-- 이후 car_log에서 camera_data_no와 out_camera_data_no로 입차/출차를 연결합니다.
--
-- camera_no 기준
-- 1: A 주차장 입차 / 2: A 주차장 출차
-- 3: B 주차장 입차 / 4: B 주차장 출차
-- 5: C 주차장 입차 / 6: C 주차장 출차
-- 7: D 주차장 입차 / 8: D 주차장 출차
--
-- 필수 차량번호는 모두 입차 + 출차 쌍으로 작성했습니다.
-- 통계 확인을 위해 최근 7일, 월별, 시간대별 패턴이 보이도록 시간을 분산했습니다.
-- =====================================================
INSERT INTO camera_data
    (camera_no, vehicle_car_no, car_no, ocr_car_no, capture_time, image_path, crop_image_path, recognition_state, confidence_score)
VALUES
    -- =====================================================
    -- 필수 차량 출차완료 데이터
    -- car_log에서 입차/출차가 모두 연결될 차량입니다.
    -- =====================================================
    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '222하5233'), '222하5233', '222하5233', TIMESTAMP '2026-07-16 08:10:00', '/carPlateImg/a-in-222하5233.jpg',    '/carPlateCrop/a-in-222하5233.jpg',    true, 99.10),
    (2, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '222하5233'), '222하5233', '222하5233', TIMESTAMP '2026-07-16 18:20:00', '/carPlateImg/a-out-222하5233.jpg',   '/carPlateCrop/a-out-222하5233.jpg',   true, 98.70),

    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '26무3111'),  '26무3111',  '26무3111',  TIMESTAMP '2026-07-16 08:35:00', '/carPlateImg/b-in-26무3111.jpg',     '/carPlateCrop/b-in-26무3111.jpg',     true, 98.90),
    (4, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '26무3111'),  '26무3111',  '26무3111',  TIMESTAMP '2026-07-16 17:55:00', '/carPlateImg/b-out-26무3111.jpg',    '/carPlateCrop/b-out-26무3111.jpg',    true, 98.40),

    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '41소2593'),  '41소2593',  '41소2593',  TIMESTAMP '2026-07-17 07:55:00', '/carPlateImg/c-in-41소2593.jpg',     '/carPlateCrop/c-in-41소2593.jpg',     true, 97.80),
    (6, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '41소2593'),  '41소2593',  '41소2593',  TIMESTAMP '2026-07-17 18:05:00', '/carPlateImg/c-out-41소2593.jpg',    '/carPlateCrop/c-out-41소2593.jpg',    true, 98.20),

    (7, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '47조2603'),  '47조2603',  '47조2603',  TIMESTAMP '2026-07-17 09:12:00', '/carPlateImg/d-in-47조2603.jpg',     '/carPlateCrop/d-in-47조2603.jpg',     true, 96.90),
    (8, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '47조2603'),  '47조2603',  '47조2603',  TIMESTAMP '2026-07-17 20:10:00', '/carPlateImg/d-out-47조2603.jpg',    '/carPlateCrop/d-out-47조2603.jpg',    true, 97.30),

    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '817라7385'), '817라7385', '817라7385', TIMESTAMP '2026-07-18 08:05:00', '/carPlateImg/a-in-817라7385.jpg',    '/carPlateCrop/a-in-817라7385.jpg',    true, 98.10),
    (2, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '817라7385'), '817라7385', '817라7385', TIMESTAMP '2026-07-18 16:40:00', '/carPlateImg/a-out-817라7385.jpg',   '/carPlateCrop/a-out-817라7385.jpg',   true, 97.70),

    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마7152'),  '95마7152',  '95마7152',  TIMESTAMP '2026-07-18 08:45:00', '/carPlateImg/b-in-95마7152.jpg',     '/carPlateCrop/b-in-95마7152.jpg',     true, 98.60),
    (4, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마7152'),  '95마7152',  '95마7152',  TIMESTAMP '2026-07-18 19:25:00', '/carPlateImg/b-out-95마7152.jpg',    '/carPlateCrop/b-out-95마7152.jpg',    true, 98.00),

    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '67모4231'),  '67모4231',  '67모4231',  TIMESTAMP '2026-07-19 07:50:00', '/carPlateImg/c-in-67모4231.jpg',     '/carPlateCrop/c-in-67모4231.jpg',     true, 97.90),
    (6, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '67모4231'),  '67모4231',  '67모4231',  TIMESTAMP '2026-07-19 18:35:00', '/carPlateImg/c-out-67모4231.jpg',    '/carPlateCrop/c-out-67모4231.jpg',    true, 97.60),

    (7, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '222마2574'), '222마2574', '222마2574', TIMESTAMP '2026-07-19 09:30:00', '/carPlateImg/d-in-222마2574.jpg',    '/carPlateCrop/d-in-222마2574.jpg',    true, 96.80),
    (8, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '222마2574'), '222마2574', '222마2574', TIMESTAMP '2026-07-19 21:05:00', '/carPlateImg/d-out-222마2574.jpg',   '/carPlateCrop/d-out-222마2574.jpg',   true, 97.20),

    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '55오0359'),  '55오0359',  '55오0359',  TIMESTAMP '2026-07-20 08:20:00', '/carPlateImg/a-in-55오0359.jpg',     '/carPlateCrop/a-in-55오0359.jpg',     true, 98.30),
    (2, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '55오0359'),  '55오0359',  '55오0359',  TIMESTAMP '2026-07-20 18:15:00', '/carPlateImg/a-out-55오0359.jpg',    '/carPlateCrop/a-out-55오0359.jpg',    true, 98.10),

    -- 이전에 같이 언급된 차량이라 출차완료 데이터로 포함했습니다.
    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '80가1010'),  '80가1010',  '80가1010',  TIMESTAMP '2026-07-20 09:10:00', '/carPlateImg/b-in-80가1010.jpg',     '/carPlateCrop/b-in-80가1010.jpg',     true, 97.50),
    (4, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '80가1010'),  '80가1010',  '80가1010',  TIMESTAMP '2026-07-20 17:45:00', '/carPlateImg/b-out-80가1010.jpg',    '/carPlateCrop/b-out-80가1010.jpg',    true, 97.00),

    -- =====================================================
    -- 최근 7일 통계용 입주민 차량 데이터
    -- 낮에는 출차가 많고, 저녁에는 입차가 많아 보이도록 시간대를 나눴습니다.
    -- =====================================================
    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '87누1011'),  '87누1011',  '87누1011',  TIMESTAMP '2026-07-21 07:40:00', '/carPlateImg/c-out-87누1011.jpg',    '/carPlateCrop/c-out-87누1011.jpg',    true, 96.80),
    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '94다1012'),  '94다1012',  '94다1012',  TIMESTAMP '2026-07-21 19:10:00', '/carPlateImg/c-in-94다1012.jpg',     '/carPlateCrop/c-in-94다1012.jpg',     true, 97.40),
    (7, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '11라1013'),  '11라1013',  '11라1013',  TIMESTAMP '2026-07-21 19:35:00', '/carPlateImg/d-in-11라1013.jpg',     '/carPlateCrop/d-in-11라1013.jpg',     true, 96.90),
    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '18마1014'),  '18마1014',  '18마1014',  TIMESTAMP '2026-07-22 07:30:00', '/carPlateImg/a-out-18마1014.jpg',    '/carPlateCrop/a-out-18마1014.jpg',    true, 97.10),
    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '25거1015'),  '25거1015',  '25거1015',  TIMESTAMP '2026-07-22 20:05:00', '/carPlateImg/b-in-25거1015.jpg',     '/carPlateCrop/b-in-25거1015.jpg',     true, 98.20),

    -- =====================================================
    -- 방문 차량 통계용 데이터
    -- visit 차량은 vehicle_car에 존재해야 입주민/방문 비교 통계에서 방문 차량으로 계산됩니다.
    -- =====================================================
    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '71거1001'), '71거1001', '71거1001', TIMESTAMP '2026-07-16 10:20:00', '/carPlateImg/a-in-71거1001.jpg',  '/carPlateCrop/a-in-71거1001.jpg',  true, 96.70),
    (2, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '71거1001'), '71거1001', '71거1001', TIMESTAMP '2026-07-16 14:40:00', '/carPlateImg/a-out-71거1001.jpg', '/carPlateCrop/a-out-71거1001.jpg', true, 96.20),

    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '72너1002'), '72너1002', '72너1002', TIMESTAMP '2026-07-17 11:10:00', '/carPlateImg/b-in-72너1002.jpg',  '/carPlateCrop/b-in-72너1002.jpg',  true, 95.90),
    (4, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '72너1002'), '72너1002', '72너1002', TIMESTAMP '2026-07-17 16:20:00', '/carPlateImg/b-out-72너1002.jpg', '/carPlateCrop/b-out-72너1002.jpg', true, 95.40),

    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '73더1003'), '73더1003', '73더1003', TIMESTAMP '2026-07-18 12:30:00', '/carPlateImg/c-in-73더1003.jpg',  '/carPlateCrop/c-in-73더1003.jpg',  true, 96.10),
    (6, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '73더1003'), '73더1003', '73더1003', TIMESTAMP '2026-07-18 15:50:00', '/carPlateImg/c-out-73더1003.jpg', '/carPlateCrop/c-out-73더1003.jpg', true, 95.80),

    (7, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '74러1004'), '74러1004', '74러1004', TIMESTAMP '2026-07-19 13:00:00', '/carPlateImg/d-in-74러1004.jpg',  '/carPlateCrop/d-in-74러1004.jpg',  true, 94.90),
    (8, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '74러1004'), '74러1004', '74러1004', TIMESTAMP '2026-07-19 17:35:00', '/carPlateImg/d-out-74러1004.jpg', '/carPlateCrop/d-out-74러1004.jpg', true, 95.20),

    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '75머1005'), '75머1005', '75머1005', TIMESTAMP '2026-07-20 10:50:00', '/carPlateImg/a-in-75머1005.jpg',  '/carPlateCrop/a-in-75머1005.jpg',  true, 96.30),
    (2, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '75머1005'), '75머1005', '75머1005', TIMESTAMP '2026-07-20 13:25:00', '/carPlateImg/a-out-75머1005.jpg', '/carPlateCrop/a-out-75머1005.jpg', true, 96.00),

    -- =====================================================
    -- 현재 주차중인 입주민 차량
    -- 출차 데이터가 없으므로 car_log에서 out_time을 NULL로 둘 예정입니다.
    -- 시간대별 평균 주차 대수에서 밤 시간 주차 차량으로 보이게 됩니다.
    -- =====================================================
    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '32나1016'), '32나1016', '32나1016', TIMESTAMP '2026-07-21 19:20:00', '/carPlateImg/b-in-32나1016.jpg', '/carPlateCrop/b-in-32나1016.jpg', true, 97.60),
    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '39다1017'), '39다1017', '39다1017', TIMESTAMP '2026-07-21 20:10:00', '/carPlateImg/c-in-39다1017.jpg', '/carPlateCrop/c-in-39다1017.jpg', true, 97.10),
    (7, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '46라1018'), '46라1018', '46라1018', TIMESTAMP '2026-07-21 21:00:00', '/carPlateImg/d-in-46라1018.jpg', '/carPlateCrop/d-in-46라1018.jpg', true, 96.80),
    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '53머1019'), '53머1019', '53머1019', TIMESTAMP '2026-07-21 21:35:00', '/carPlateImg/a-in-53머1019.jpg', '/carPlateCrop/a-in-53머1019.jpg', true, 97.90),

    -- =====================================================
    -- 방문 만료 + 현재 주차중 데이터
    -- notice에서 "방문 장기주차" 또는 "주차중 방문 만료"로 연결할 대상입니다.
    -- 출차 데이터는 일부러 만들지 않습니다.
    -- =====================================================
    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '91가9001'), '91가9001', '91가9001', TIMESTAMP '2026-07-21 08:30:00', '/carPlateImg/c-in-91가9001.jpg', '/carPlateCrop/c-in-91가9001.jpg', true, 96.20),
    (7, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '92나9002'), '92나9002', '92나9002', TIMESTAMP '2026-07-21 09:10:00', '/carPlateImg/d-in-92나9002.jpg', '/carPlateCrop/d-in-92나9002.jpg', true, 95.80),
    (1, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '93다9003'), '93다9003', '93다9003', TIMESTAMP '2026-07-21 10:00:00', '/carPlateImg/a-in-93다9003.jpg', '/carPlateCrop/a-in-93다9003.jpg', true, 94.90),

    -- =====================================================
    -- 방문 만료 + 출차완료 데이터
    -- 만료 이력은 남지만 현재 주차중 알림 대상은 아닙니다.
    -- =====================================================
    (3, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '94라9004'), '94라9004', '94라9004', TIMESTAMP '2026-07-20 09:00:00', '/carPlateImg/b-in-94라9004.jpg',  '/carPlateCrop/b-in-94라9004.jpg',  true, 97.00),
    (4, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '94라9004'), '94라9004', '94라9004', TIMESTAMP '2026-07-20 12:20:00', '/carPlateImg/b-out-94라9004.jpg', '/carPlateCrop/b-out-94라9004.jpg', true, 96.80),

    (5, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마9005'), '95마9005', '95마9005', TIMESTAMP '2026-07-19 11:00:00', '/carPlateImg/c-in-95마9005.jpg',  '/carPlateCrop/c-in-95마9005.jpg',  true, 96.50),
    (6, (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마9005'), '95마9005', '95마9005', TIMESTAMP '2026-07-19 14:45:00', '/carPlateImg/c-out-95마9005.jpg', '/carPlateCrop/c-out-95마9005.jpg', true, 96.10),

    -- =====================================================
    -- 미등록 차량 데이터
    -- vehicle_car_no가 NULL이므로 등록 차량 테이블과 연결되지 않습니다.
    -- 비등록 차량 추이와 비등록 장기주차 알림 확인용입니다.
    -- =====================================================
    (1, NULL, '77라7777', '77라7777', TIMESTAMP '2026-07-16 09:25:00', '/carPlateImg/a-in-77라7777.jpg',  '/carPlateCrop/a-in-77라7777.jpg',  true, 91.30),
    (2, NULL, '77라7777', '77라7777', TIMESTAMP '2026-07-16 11:40:00', '/carPlateImg/a-out-77라7777.jpg', '/carPlateCrop/a-out-77라7777.jpg', true, 91.80),

    (3, NULL, '88태9001', '88태9001', TIMESTAMP '2026-07-17 10:10:00', '/carPlateImg/b-in-88태9001.jpg',  '/carPlateCrop/b-in-88태9001.jpg',  true, 93.40),
    (4, NULL, '88태9001', '88태9001', TIMESTAMP '2026-07-17 13:30:00', '/carPlateImg/b-out-88태9001.jpg', '/carPlateCrop/b-out-88태9001.jpg', true, 92.90),

    (5, NULL, '12다3456', '12다3456', TIMESTAMP '2026-07-18 11:20:00', '/carPlateImg/c-in-12다3456.jpg',  '/carPlateCrop/c-in-12다3456.jpg',  true, 92.10),
    (6, NULL, '12다3456', '12다3456', TIMESTAMP '2026-07-18 14:40:00', '/carPlateImg/c-out-12다3456.jpg', '/carPlateCrop/c-out-12다3456.jpg', true, 92.80),

    -- =====================================================
    -- 오래 주차중인 미등록 차량
    -- notice에서 "비등록 장기주차 알림"으로 연결할 대상입니다.
    -- 출차 데이터는 일부러 만들지 않습니다.
    -- =====================================================
    (7, NULL, '99하9999', '99하9999', TIMESTAMP '2026-07-20 22:10:00', '/carPlateImg/d-in-99하9999.jpg', '/carPlateCrop/d-in-99하9999.jpg', true, 90.70),
    (1, NULL, '66호6666', '66호6666', TIMESTAMP '2026-07-21 23:05:00', '/carPlateImg/a-in-66호6666.jpg', '/carPlateCrop/a-in-66호6666.jpg', true, 89.90),

    -- =====================================================
    -- 월별 통계 확인용 과거 데이터
    -- 현재 차량 정보와 직접 연결하지 않는 과거 기록이므로 vehicle_car_no는 NULL로 둡니다.
    -- 잘못된 FK 연결을 만들지 않기 위한 처리입니다.
    -- =====================================================
    (1, NULL, '20월0001', '20월0001', TIMESTAMP '2026-01-12 08:20:00', '/carPlateImg/stat-202601-in-01.jpg',  '/carPlateCrop/stat-202601-in-01.jpg',  true, 94.20),
    (2, NULL, '20월0001', '20월0001', TIMESTAMP '2026-01-12 18:10:00', '/carPlateImg/stat-202601-out-01.jpg', '/carPlateCrop/stat-202601-out-01.jpg', true, 93.90),

    (3, NULL, '20월0002', '20월0002', TIMESTAMP '2026-02-14 09:10:00', '/carPlateImg/stat-202602-in-01.jpg',  '/carPlateCrop/stat-202602-in-01.jpg',  true, 94.80),
    (4, NULL, '20월0002', '20월0002', TIMESTAMP '2026-02-14 17:40:00', '/carPlateImg/stat-202602-out-01.jpg', '/carPlateCrop/stat-202602-out-01.jpg', true, 94.10),

    (5, NULL, '20월0003', '20월0003', TIMESTAMP '2026-03-18 10:30:00', '/carPlateImg/stat-202603-in-01.jpg',  '/carPlateCrop/stat-202603-in-01.jpg',  true, 95.20),
    (6, NULL, '20월0003', '20월0003', TIMESTAMP '2026-03-18 16:20:00', '/carPlateImg/stat-202603-out-01.jpg', '/carPlateCrop/stat-202603-out-01.jpg', true, 94.70),

    (7, NULL, '20월0004', '20월0004', TIMESTAMP '2026-04-20 11:00:00', '/carPlateImg/stat-202604-in-01.jpg',  '/carPlateCrop/stat-202604-in-01.jpg',  true, 95.60),
    (8, NULL, '20월0004', '20월0004', TIMESTAMP '2026-04-20 15:30:00', '/carPlateImg/stat-202604-out-01.jpg', '/carPlateCrop/stat-202604-out-01.jpg', true, 95.00),

    (1, NULL, '20월0005', '20월0005', TIMESTAMP '2026-05-22 08:40:00', '/carPlateImg/stat-202605-in-01.jpg',  '/carPlateCrop/stat-202605-in-01.jpg',  true, 96.00),
    (2, NULL, '20월0005', '20월0005', TIMESTAMP '2026-05-22 18:50:00', '/carPlateImg/stat-202605-out-01.jpg', '/carPlateCrop/stat-202605-out-01.jpg', true, 95.50),

    (3, NULL, '20월0006', '20월0006', TIMESTAMP '2026-06-24 09:20:00', '/carPlateImg/stat-202606-in-01.jpg',  '/carPlateCrop/stat-202606-in-01.jpg',  true, 96.30),
    (4, NULL, '20월0006', '20월0006', TIMESTAMP '2026-06-24 17:10:00', '/carPlateImg/stat-202606-out-01.jpg', '/carPlateCrop/stat-202606-out-01.jpg', true, 95.90);

-- 등록 차량으로 연결되어야 하는 필수 차량이 NULL로 들어갔는지 확인
SELECT
    car_no,
    vehicle_car_no,
    capture_time
FROM camera_data
WHERE car_no IN (
    '222하5233',
    '26무3111',
    '41소2593',
    '47조2603',
    '817라7385',
    '95마7152',
    '67모4231',
    '222마2574',
    '55오0359',
    '80가1010'
)
ORDER BY capture_time;

-- =====================================================
-- 7. 입출차 로그
-- =====================================================
-- car_log는 실제 주차 상태를 판단하는 기준 데이터입니다.
--
-- camera_data_no     : 입차 카메라 데이터
-- out_camera_data_no : 출차 카메라 데이터
--
-- out_time이 NULL이면 현재 주차중입니다.
-- out_time이 있으면 출차완료입니다.
--
-- free_time은 무료 주차 시간입니다.
-- 입주민 차량은 0, 방문/미등록 차량은 180분 기준으로 넣었습니다.
-- =====================================================
INSERT INTO car_log
    (vehicle_car_no, camera_data_no, out_camera_data_no, in_gate_no, in_time, out_gate_no, out_time, free_time, snapshot_car_no)
VALUES
    -- =====================================================
    -- 필수 차량 출차완료 데이터
    -- 반드시 넣어야 한다고 했던 차량번호는 모두 출차완료 상태입니다.
    -- =====================================================
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '222하5233'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '222하5233' AND capture_time = TIMESTAMP '2026-07-16 08:10:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '222하5233' AND capture_time = TIMESTAMP '2026-07-16 18:20:00'),
        1,
        TIMESTAMP '2026-07-16 08:10:00',
        2,
        TIMESTAMP '2026-07-16 18:20:00',
        0,
        '222하5233'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '26무3111'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '26무3111' AND capture_time = TIMESTAMP '2026-07-16 08:35:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '26무3111' AND capture_time = TIMESTAMP '2026-07-16 17:55:00'),
        3,
        TIMESTAMP '2026-07-16 08:35:00',
        4,
        TIMESTAMP '2026-07-16 17:55:00',
        0,
        '26무3111'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '41소2593'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '41소2593' AND capture_time = TIMESTAMP '2026-07-17 07:55:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '41소2593' AND capture_time = TIMESTAMP '2026-07-17 18:05:00'),
        5,
        TIMESTAMP '2026-07-17 07:55:00',
        6,
        TIMESTAMP '2026-07-17 18:05:00',
        0,
        '41소2593'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '47조2603'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '47조2603' AND capture_time = TIMESTAMP '2026-07-17 09:12:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '47조2603' AND capture_time = TIMESTAMP '2026-07-17 20:10:00'),
        7,
        TIMESTAMP '2026-07-17 09:12:00',
        8,
        TIMESTAMP '2026-07-17 20:10:00',
        0,
        '47조2603'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '817라7385'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '817라7385' AND capture_time = TIMESTAMP '2026-07-18 08:05:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '817라7385' AND capture_time = TIMESTAMP '2026-07-18 16:40:00'),
        1,
        TIMESTAMP '2026-07-18 08:05:00',
        2,
        TIMESTAMP '2026-07-18 16:40:00',
        0,
        '817라7385'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마7152'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '95마7152' AND capture_time = TIMESTAMP '2026-07-18 08:45:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '95마7152' AND capture_time = TIMESTAMP '2026-07-18 19:25:00'),
        3,
        TIMESTAMP '2026-07-18 08:45:00',
        4,
        TIMESTAMP '2026-07-18 19:25:00',
        0,
        '95마7152'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '67모4231'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '67모4231' AND capture_time = TIMESTAMP '2026-07-19 07:50:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '67모4231' AND capture_time = TIMESTAMP '2026-07-19 18:35:00'),
        5,
        TIMESTAMP '2026-07-19 07:50:00',
        6,
        TIMESTAMP '2026-07-19 18:35:00',
        0,
        '67모4231'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '222마2574'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '222마2574' AND capture_time = TIMESTAMP '2026-07-19 09:30:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '222마2574' AND capture_time = TIMESTAMP '2026-07-19 21:05:00'),
        7,
        TIMESTAMP '2026-07-19 09:30:00',
        8,
        TIMESTAMP '2026-07-19 21:05:00',
        0,
        '222마2574'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '55오0359'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '55오0359' AND capture_time = TIMESTAMP '2026-07-20 08:20:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '55오0359' AND capture_time = TIMESTAMP '2026-07-20 18:15:00'),
        1,
        TIMESTAMP '2026-07-20 08:20:00',
        2,
        TIMESTAMP '2026-07-20 18:15:00',
        0,
        '55오0359'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '80가1010'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '80가1010' AND capture_time = TIMESTAMP '2026-07-20 09:10:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '80가1010' AND capture_time = TIMESTAMP '2026-07-20 17:45:00'),
        3,
        TIMESTAMP '2026-07-20 09:10:00',
        4,
        TIMESTAMP '2026-07-20 17:45:00',
        0,
        '80가1010'
    ),

    -- =====================================================
    -- 현재 주차중인 입주민 차량
    -- 밤 시간대 평균 주차 대수를 높게 보이게 하는 데이터입니다.
    -- out_camera_data_no, out_gate_no, out_time은 NULL입니다.
    -- =====================================================
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '94다1012'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '94다1012' AND capture_time = TIMESTAMP '2026-07-21 19:10:00'),
        NULL,
        5,
        TIMESTAMP '2026-07-21 19:10:00',
        NULL,
        NULL,
        0,
        '94다1012'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '11라1013'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '11라1013' AND capture_time = TIMESTAMP '2026-07-21 19:35:00'),
        NULL,
        7,
        TIMESTAMP '2026-07-21 19:35:00',
        NULL,
        NULL,
        0,
        '11라1013'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '25거1015'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '25거1015' AND capture_time = TIMESTAMP '2026-07-22 20:05:00'),
        NULL,
        3,
        TIMESTAMP '2026-07-22 20:05:00',
        NULL,
        NULL,
        0,
        '25거1015'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '32나1016'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '32나1016' AND capture_time = TIMESTAMP '2026-07-21 19:20:00'),
        NULL,
        3,
        TIMESTAMP '2026-07-21 19:20:00',
        NULL,
        NULL,
        0,
        '32나1016'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '39다1017'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '39다1017' AND capture_time = TIMESTAMP '2026-07-21 20:10:00'),
        NULL,
        5,
        TIMESTAMP '2026-07-21 20:10:00',
        NULL,
        NULL,
        0,
        '39다1017'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '46라1018'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '46라1018' AND capture_time = TIMESTAMP '2026-07-21 21:00:00'),
        NULL,
        7,
        TIMESTAMP '2026-07-21 21:00:00',
        NULL,
        NULL,
        0,
        '46라1018'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '53머1019'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '53머1019' AND capture_time = TIMESTAMP '2026-07-21 21:35:00'),
        NULL,
        1,
        TIMESTAMP '2026-07-21 21:35:00',
        NULL,
        NULL,
        0,
        '53머1019'
    ),

    -- =====================================================
    -- 방문 차량 출차완료 데이터
    -- 최근 7일 입주민/방문 차량 입차 비교 통계에 사용됩니다.
    -- =====================================================
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '71거1001'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '71거1001' AND capture_time = TIMESTAMP '2026-07-16 10:20:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '71거1001' AND capture_time = TIMESTAMP '2026-07-16 14:40:00'),
        1,
        TIMESTAMP '2026-07-16 10:20:00',
        2,
        TIMESTAMP '2026-07-16 14:40:00',
        180,
        '71거1001'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '72너1002'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '72너1002' AND capture_time = TIMESTAMP '2026-07-17 11:10:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '72너1002' AND capture_time = TIMESTAMP '2026-07-17 16:20:00'),
        3,
        TIMESTAMP '2026-07-17 11:10:00',
        4,
        TIMESTAMP '2026-07-17 16:20:00',
        180,
        '72너1002'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '73더1003'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '73더1003' AND capture_time = TIMESTAMP '2026-07-18 12:30:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '73더1003' AND capture_time = TIMESTAMP '2026-07-18 15:50:00'),
        5,
        TIMESTAMP '2026-07-18 12:30:00',
        6,
        TIMESTAMP '2026-07-18 15:50:00',
        180,
        '73더1003'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '74러1004'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '74러1004' AND capture_time = TIMESTAMP '2026-07-19 13:00:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '74러1004' AND capture_time = TIMESTAMP '2026-07-19 17:35:00'),
        7,
        TIMESTAMP '2026-07-19 13:00:00',
        8,
        TIMESTAMP '2026-07-19 17:35:00',
        180,
        '74러1004'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '75머1005'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '75머1005' AND capture_time = TIMESTAMP '2026-07-20 10:50:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '75머1005' AND capture_time = TIMESTAMP '2026-07-20 13:25:00'),
        1,
        TIMESTAMP '2026-07-20 10:50:00',
        2,
        TIMESTAMP '2026-07-20 13:25:00',
        180,
        '75머1005'
    ),

    -- =====================================================
    -- 방문 만료 + 현재 주차중 데이터
    -- notice에서 "주차중 방문 만료 차량"으로 연결할 대상입니다.
    -- =====================================================
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '91가9001'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '91가9001' AND capture_time = TIMESTAMP '2026-07-21 08:30:00'),
        NULL,
        5,
        TIMESTAMP '2026-07-21 08:30:00',
        NULL,
        NULL,
        180,
        '91가9001'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '92나9002'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '92나9002' AND capture_time = TIMESTAMP '2026-07-21 09:10:00'),
        NULL,
        7,
        TIMESTAMP '2026-07-21 09:10:00',
        NULL,
        NULL,
        180,
        '92나9002'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '93다9003'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '93다9003' AND capture_time = TIMESTAMP '2026-07-21 10:00:00'),
        NULL,
        1,
        TIMESTAMP '2026-07-21 10:00:00',
        NULL,
        NULL,
        180,
        '93다9003'
    ),

    -- =====================================================
    -- 방문 만료 + 출차완료 데이터
    -- 만료 차량이지만 현재 주차중은 아니므로 알림 대상에서 제외할 수 있습니다.
    -- =====================================================
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '94라9004'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '94라9004' AND capture_time = TIMESTAMP '2026-07-20 09:00:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '94라9004' AND capture_time = TIMESTAMP '2026-07-20 12:20:00'),
        3,
        TIMESTAMP '2026-07-20 09:00:00',
        4,
        TIMESTAMP '2026-07-20 12:20:00',
        180,
        '94라9004'
    ),
    (
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마9005'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '95마9005' AND capture_time = TIMESTAMP '2026-07-19 11:00:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '95마9005' AND capture_time = TIMESTAMP '2026-07-19 14:45:00'),
        5,
        TIMESTAMP '2026-07-19 11:00:00',
        6,
        TIMESTAMP '2026-07-19 14:45:00',
        180,
        '95마9005'
    ),

    -- =====================================================
    -- 미등록 차량 출차완료 데이터
    -- vehicle_car_no가 NULL입니다.
    -- =====================================================
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '77라7777' AND capture_time = TIMESTAMP '2026-07-16 09:25:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '77라7777' AND capture_time = TIMESTAMP '2026-07-16 11:40:00'),
        1,
        TIMESTAMP '2026-07-16 09:25:00',
        2,
        TIMESTAMP '2026-07-16 11:40:00',
        180,
        '77라7777'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '88태9001' AND capture_time = TIMESTAMP '2026-07-17 10:10:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '88태9001' AND capture_time = TIMESTAMP '2026-07-17 13:30:00'),
        3,
        TIMESTAMP '2026-07-17 10:10:00',
        4,
        TIMESTAMP '2026-07-17 13:30:00',
        180,
        '88태9001'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '12다3456' AND capture_time = TIMESTAMP '2026-07-18 11:20:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '12다3456' AND capture_time = TIMESTAMP '2026-07-18 14:40:00'),
        5,
        TIMESTAMP '2026-07-18 11:20:00',
        6,
        TIMESTAMP '2026-07-18 14:40:00',
        180,
        '12다3456'
    ),

    -- =====================================================
    -- 오래 주차중인 미등록 차량
    -- notice에서 "비등록 장기주차 알림"으로 연결할 대상입니다.
    -- =====================================================
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '99하9999' AND capture_time = TIMESTAMP '2026-07-20 22:10:00'),
        NULL,
        7,
        TIMESTAMP '2026-07-20 22:10:00',
        NULL,
        NULL,
        180,
        '99하9999'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '66호6666' AND capture_time = TIMESTAMP '2026-07-21 23:05:00'),
        NULL,
        1,
        TIMESTAMP '2026-07-21 23:05:00',
        NULL,
        NULL,
        180,
        '66호6666'
    ),

    -- =====================================================
    -- 월별 통계 확인용 과거 기록
    -- 실제 현재 차량과 연결하지 않는 과거 기록이므로 vehicle_car_no는 NULL입니다.
    -- =====================================================
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0001' AND capture_time = TIMESTAMP '2026-01-12 08:20:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0001' AND capture_time = TIMESTAMP '2026-01-12 18:10:00'),
        1,
        TIMESTAMP '2026-01-12 08:20:00',
        2,
        TIMESTAMP '2026-01-12 18:10:00',
        180,
        '20월0001'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0002' AND capture_time = TIMESTAMP '2026-02-14 09:10:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0002' AND capture_time = TIMESTAMP '2026-02-14 17:40:00'),
        3,
        TIMESTAMP '2026-02-14 09:10:00',
        4,
        TIMESTAMP '2026-02-14 17:40:00',
        180,
        '20월0002'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0003' AND capture_time = TIMESTAMP '2026-03-18 10:30:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0003' AND capture_time = TIMESTAMP '2026-03-18 16:20:00'),
        5,
        TIMESTAMP '2026-03-18 10:30:00',
        6,
        TIMESTAMP '2026-03-18 16:20:00',
        180,
        '20월0003'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0004' AND capture_time = TIMESTAMP '2026-04-20 11:00:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0004' AND capture_time = TIMESTAMP '2026-04-20 15:30:00'),
        7,
        TIMESTAMP '2026-04-20 11:00:00',
        8,
        TIMESTAMP '2026-04-20 15:30:00',
        180,
        '20월0004'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0005' AND capture_time = TIMESTAMP '2026-05-22 08:40:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0005' AND capture_time = TIMESTAMP '2026-05-22 18:50:00'),
        1,
        TIMESTAMP '2026-05-22 08:40:00',
        2,
        TIMESTAMP '2026-05-22 18:50:00',
        180,
        '20월0005'
    ),
    (
        NULL,
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0006' AND capture_time = TIMESTAMP '2026-06-24 09:20:00'),
        (SELECT camera_data_no FROM camera_data WHERE car_no = '20월0006' AND capture_time = TIMESTAMP '2026-06-24 17:10:00'),
        3,
        TIMESTAMP '2026-06-24 09:20:00',
        4,
        TIMESTAMP '2026-06-24 17:10:00',
        180,
        '20월0006'
    );

-- =====================================================
-- 8. 장기 주차 / 미등록 알림
-- =====================================================
-- notice는 관리자에게 보여줄 장기주차 / 미등록 차량 알림입니다.
--
-- alert_stat
-- Unresolved : 미처리
-- Checked    : 확인
-- Resolved   : 처리완료
--
-- car_log_no는 자동 생성 번호이므로 직접 숫자를 넣지 않고
-- snapshot_car_no와 in_time으로 찾아 연결합니다.
-- =====================================================
INSERT INTO notice
    (car_log_no, detect_at, stay_days, alert_stat, handled_by_member_no, handled_at,
     snapshot_car_log_no, snapshot_registered_car_no, snapshot_captured_car_no,
     snapshot_car_kind, snapshot_parking_name, snapshot_in_time)
VALUES
    -- =====================================================
    -- 방문 장기주차 알림
    -- 방문 차량이 만료되었고 아직 출차하지 않은 상태입니다.
    -- 통계 화면의 "방문 장기주차 알림"에 사용할 수 있습니다.
    -- =====================================================
    (
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '91가9001' AND in_time = TIMESTAMP '2026-07-21 08:30:00'),
        TIMESTAMP '2026-07-22 08:30:00',
        1,
        'Unresolved',
        NULL,
        NULL,
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '91가9001' AND in_time = TIMESTAMP '2026-07-21 08:30:00'),
        '91가9001',
        '91가9001',
        'VISIT',
        'C 주차장',
        TIMESTAMP '2026-07-21 08:30:00'
    ),
    (
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '92나9002' AND in_time = TIMESTAMP '2026-07-21 09:10:00'),
        TIMESTAMP '2026-07-22 09:10:00',
        1,
        'Checked',
        (SELECT member_no FROM member WHERE login_id = 'admin1'),
        TIMESTAMP '2026-07-22 09:30:00',
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '92나9002' AND in_time = TIMESTAMP '2026-07-21 09:10:00'),
        '92나9002',
        '92나9002',
        'VISIT',
        'D 주차장',
        TIMESTAMP '2026-07-21 09:10:00'
    ),

    -- =====================================================
    -- 주차중 방문 만료 차량
    -- 방문 등록 시간이 이미 만료되었지만 아직 출차하지 않은 차량입니다.
    -- 통계 화면의 "주차중 방문 만료 차량"에 사용할 수 있습니다.
    -- =====================================================
    (
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '93다9003' AND in_time = TIMESTAMP '2026-07-21 10:00:00'),
        TIMESTAMP '2026-07-22 10:00:00',
        1,
        'Unresolved',
        NULL,
        NULL,
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '93다9003' AND in_time = TIMESTAMP '2026-07-21 10:00:00'),
        '93다9003',
        '93다9003',
        'VISIT',
        'A 주차장',
        TIMESTAMP '2026-07-21 10:00:00'
    ),

    -- =====================================================
    -- 비등록 장기주차 알림
    -- vehicle_car_no가 NULL인 미등록 차량이 출차하지 않은 상태입니다.
    -- 통계 화면의 "비등록 장기주차 알림"에 사용할 수 있습니다.
    -- =====================================================
    (
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '99하9999' AND in_time = TIMESTAMP '2026-07-20 22:10:00'),
        TIMESTAMP '2026-07-22 00:10:00',
        2,
        'Unresolved',
        NULL,
        NULL,
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '99하9999' AND in_time = TIMESTAMP '2026-07-20 22:10:00'),
        NULL,
        '99하9999',
        'UNKNOWN',
        'D 주차장',
        TIMESTAMP '2026-07-20 22:10:00'
    ),
    (
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '66호6666' AND in_time = TIMESTAMP '2026-07-21 23:05:00'),
        TIMESTAMP '2026-07-22 23:05:00',
        1,
        'Unresolved',
        NULL,
        NULL,
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '66호6666' AND in_time = TIMESTAMP '2026-07-21 23:05:00'),
        NULL,
        '66호6666',
        'UNKNOWN',
        'A 주차장',
        TIMESTAMP '2026-07-21 23:05:00'
    ),

    -- =====================================================
    -- 처리 완료된 알림 예시
    -- 화면에서 처리완료 상태 디자인과 필터 확인용입니다.
    -- 이미 출차한 방문 만료 차량이므로 Resolved 상태로 둡니다.
    -- =====================================================
    (
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '94라9004' AND in_time = TIMESTAMP '2026-07-20 09:00:00'),
        TIMESTAMP '2026-07-21 09:00:00',
        1,
        'Resolved',
        (SELECT member_no FROM member WHERE login_id = 'admin1'),
        TIMESTAMP '2026-07-21 09:20:00',
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '94라9004' AND in_time = TIMESTAMP '2026-07-20 09:00:00'),
        '94라9004',
        '94라9004',
        'VISIT',
        'B 주차장',
        TIMESTAMP '2026-07-20 09:00:00'
    );

-- =====================================================
-- 9. 차량 알림
-- =====================================================
-- vehicle_nt는 특정 입주민에게 전달되는 차량 관련 알림입니다.
--
-- notification_type
-- ADMIN_REJECTED      : 관리자가 차량 신청을 반려
-- APPROVAL_TIMEOUT    : 승인 대기 시간이 길어짐
-- NO_ENTRY_EXPIRED    : 방문 차량이 입차하지 않은 채 만료
-- VISIT_OVERDUE       : 방문 차량이 만료 후에도 주차중
-- VISIT_OVERDUE_EXIT  : 방문 차량이 만료 후 출차
--
-- read_at이 NULL이면 아직 읽지 않은 알림입니다.
-- =====================================================
INSERT INTO vehicle_nt
    (recipient_member_no, sender_member_no, vehicle_car_no, car_log_no,
     snapshot_car_no, notification_type, message, overdue_minutes, created_at, read_at)
VALUES
    -- =====================================================
    -- 방문 차량이 만료 후에도 주차중인 알림
    -- resident에게 보여줄 수 있는 미확인 알림입니다.
    -- =====================================================
    (
        (SELECT member_no FROM member WHERE login_id = 'res1'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '91가9001'),
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '91가9001' AND in_time = TIMESTAMP '2026-07-21 08:30:00'),
        '91가9001',
        'VISIT_OVERDUE',
        '방문 차량 91가9001이 등록 만료 후에도 주차중입니다.',
        630,
        TIMESTAMP '2026-07-22 08:30:00',
        NULL
    ),
    (
        (SELECT member_no FROM member WHERE login_id = 'res5'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '92나9002'),
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '92나9002' AND in_time = TIMESTAMP '2026-07-21 09:10:00'),
        '92나9002',
        'VISIT_OVERDUE',
        '방문 차량 92나9002이 등록 만료 후에도 주차중입니다.',
        590,
        TIMESTAMP '2026-07-22 09:10:00',
        TIMESTAMP '2026-07-22 09:25:00'
    ),

    -- =====================================================
    -- 방문 차량이 만료 후 출차한 알림
    -- 이미 출차했지만 이력 확인용으로 남기는 알림입니다.
    -- =====================================================
    (
        (SELECT member_no FROM member WHERE login_id = 'res6'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '94라9004'),
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '94라9004' AND in_time = TIMESTAMP '2026-07-20 09:00:00'),
        '94라9004',
        'VISIT_OVERDUE_EXIT',
        '방문 차량 94라9004이 등록 만료 후 출차했습니다.',
        140,
        TIMESTAMP '2026-07-20 12:20:00',
        NULL
    ),
    (
        (SELECT member_no FROM member WHERE login_id = 'res7'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '95마9005'),
        (SELECT car_log_no FROM car_log WHERE snapshot_car_no = '95마9005' AND in_time = TIMESTAMP '2026-07-19 11:00:00'),
        '95마9005',
        'VISIT_OVERDUE_EXIT',
        '방문 차량 95마9005이 등록 만료 후 출차했습니다.',
        165,
        TIMESTAMP '2026-07-19 14:45:00',
        TIMESTAMP '2026-07-19 15:00:00'
    ),

    -- =====================================================
    -- 승인 대기 시간이 오래된 방문 차량 알림
    -- 방문 차량 승인 대기 화면과 연결하기 좋은 데이터입니다.
    -- =====================================================
    (
        (SELECT member_no FROM member WHERE login_id = 'res8'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '72나2001'),
        NULL,
        '72나2001',
        'APPROVAL_TIMEOUT',
        '방문 차량 72나2001의 승인 대기 시간이 길어지고 있습니다.',
        180,
        TIMESTAMP '2026-07-22 11:00:00',
        NULL
    ),
    (
        (SELECT member_no FROM member WHERE login_id = 'res9'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '72나2002'),
        NULL,
        '72나2002',
        'APPROVAL_TIMEOUT',
        '방문 차량 72나2002의 승인 대기 시간이 길어지고 있습니다.',
        240,
        TIMESTAMP '2026-07-22 12:00:00',
        NULL
    ),

    -- =====================================================
    -- 관리자가 반려한 방문 차량 알림
    -- sender_member_no에는 관리자 번호를 넣습니다.
    -- =====================================================
    (
        (SELECT member_no FROM member WHERE login_id = 'res10'),
        (SELECT member_no FROM member WHERE login_id = 'admin1'),
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '72나2003'),
        NULL,
        '72나2003',
        'ADMIN_REJECTED',
        '방문 차량 72나2003 신청이 반려되었습니다.',
        NULL,
        TIMESTAMP '2026-07-22 13:00:00',
        NULL
    ),

    -- =====================================================
    -- 입차하지 않은 채 만료된 방문 차량 알림
    -- 차량은 신청되었지만 실제 카메라 입차 기록이 없는 상태입니다.
    -- =====================================================
    (
        (SELECT member_no FROM member WHERE login_id = 'res11'),
        NULL,
        (SELECT vehicle_car_no FROM vehicle_car WHERE car_no = '91가9006'),
        NULL,
        '91가9006',
        'NO_ENTRY_EXPIRED',
        '방문 차량 91가9006이 입차하지 않은 상태로 등록 시간이 만료되었습니다.',
        0,
        TIMESTAMP '2026-07-22 15:00:00',
        NULL
    );

-- =====================================================
-- 10. 휴지통
-- =====================================================
-- trash_bin은 삭제된 데이터를 JSONB 형태로 보관하는 테이블입니다.
--
-- data_type
-- CAMERA_DATA : 삭제된 카메라 데이터
-- CAR_LOG     : 삭제된 입출차 로그
-- NOTICE      : 삭제된 알림
--
-- delete_type
-- MANUAL      : 사용자가 직접 삭제
-- SCHEDULED   : 스케줄러가 자동 삭제
--
-- purge_at은 영구 삭제 예정일입니다.
-- =====================================================
INSERT INTO trash_bin
    (data_type, original_no, data_json, delete_type, deleted_at, purge_at)
VALUES
    -- =====================================================
    -- 사용자가 직접 삭제한 카메라 데이터
    -- =====================================================
    (
        'CAMERA_DATA',
        9001,
        '{
            "camera_data_no": 9001,
            "camera_no": 1,
            "vehicle_car_no": null,
            "car_no": "삭제12가3456",
            "ocr_car_no": "삭제12가3456",
            "capture_time": "2026-07-10T09:20:00",
            "image_path": "/carPlateImg/deleted-a-in-01.jpg",
            "crop_image_path": "/carPlateCrop/deleted-a-in-01.jpg",
            "recognition_state": true,
            "confidence_score": 92.40
        }'::jsonb,
        'MANUAL',
        TIMESTAMP '2026-07-12 10:00:00',
        TIMESTAMP '2026-08-11 10:00:00'
    ),
    (
        'CAMERA_DATA',
        9002,
        '{
            "camera_data_no": 9002,
            "camera_no": 5,
            "vehicle_car_no": null,
            "car_no": "삭제77나7777",
            "ocr_car_no": "삭제77나7777",
            "capture_time": "2026-07-11T14:30:00",
            "image_path": "/carPlateImg/deleted-c-in-01.jpg",
            "crop_image_path": "/carPlateCrop/deleted-c-in-01.jpg",
            "recognition_state": true,
            "confidence_score": 89.70
        }'::jsonb,
        'MANUAL',
        TIMESTAMP '2026-07-13 11:00:00',
        TIMESTAMP '2026-08-12 11:00:00'
    ),

    -- =====================================================
    -- 사용자가 직접 삭제한 입출차 로그
    -- =====================================================
    (
        'CAR_LOG',
        9101,
        '{
            "car_log_no": 9101,
            "vehicle_car_no": null,
            "camera_data_no": 9001,
            "out_camera_data_no": null,
            "in_gate_no": 1,
            "in_time": "2026-07-10T09:20:00",
            "out_gate_no": null,
            "out_time": null,
            "free_time": 180,
            "snapshot_car_no": "삭제12가3456"
        }'::jsonb,
        'MANUAL',
        TIMESTAMP '2026-07-12 10:05:00',
        TIMESTAMP '2026-08-11 10:05:00'
    ),
    (
        'CAR_LOG',
        9102,
        '{
            "car_log_no": 9102,
            "vehicle_car_no": null,
            "camera_data_no": 9002,
            "out_camera_data_no": null,
            "in_gate_no": 5,
            "in_time": "2026-07-11T14:30:00",
            "out_gate_no": null,
            "out_time": null,
            "free_time": 180,
            "snapshot_car_no": "삭제77나7777"
        }'::jsonb,
        'MANUAL',
        TIMESTAMP '2026-07-13 11:05:00',
        TIMESTAMP '2026-08-12 11:05:00'
    ),

    -- =====================================================
    -- 처리 완료 후 삭제된 알림
    -- =====================================================
    (
        'NOTICE',
        9201,
        '{
            "notice_no": 9201,
            "car_log_no": 9101,
            "detect_at": "2026-07-11T09:20:00",
            "stay_days": 1,
            "alert_stat": "Resolved",
            "handled_by_member_no": 1,
            "handled_at": "2026-07-12T09:40:00",
            "snapshot_car_log_no": 9101,
            "snapshot_registered_car_no": null,
            "snapshot_captured_car_no": "삭제12가3456",
            "snapshot_car_kind": "UNKNOWN",
            "snapshot_parking_name": "A 주차장",
            "snapshot_in_time": "2026-07-10T09:20:00"
        }'::jsonb,
        'MANUAL',
        TIMESTAMP '2026-07-12 10:10:00',
        TIMESTAMP '2026-08-11 10:10:00'
    ),
    (
        'NOTICE',
        9202,
        '{
            "notice_no": 9202,
            "car_log_no": 9102,
            "detect_at": "2026-07-12T14:30:00",
            "stay_days": 1,
            "alert_stat": "Checked",
            "handled_by_member_no": 1,
            "handled_at": "2026-07-13T10:30:00",
            "snapshot_car_log_no": 9102,
            "snapshot_registered_car_no": null,
            "snapshot_captured_car_no": "삭제77나7777",
            "snapshot_car_kind": "UNKNOWN",
            "snapshot_parking_name": "C 주차장",
            "snapshot_in_time": "2026-07-11T14:30:00"
        }'::jsonb,
        'MANUAL',
        TIMESTAMP '2026-07-13 11:10:00',
        TIMESTAMP '2026-08-12 11:10:00'
    ),

    -- =====================================================
    -- 스케줄러가 자동 삭제한 오래된 알림
    -- 삭제 방식 필터 확인용입니다.
    -- =====================================================
    (
        'NOTICE',
        9203,
        '{
            "notice_no": 9203,
            "car_log_no": 9103,
            "detect_at": "2026-06-01T08:00:00",
            "stay_days": 3,
            "alert_stat": "Resolved",
            "handled_by_member_no": 1,
            "handled_at": "2026-06-01T09:00:00",
            "snapshot_car_log_no": 9103,
            "snapshot_registered_car_no": "스케줄12가1234",
            "snapshot_captured_car_no": "스케줄12가1234",
            "snapshot_car_kind": "VISIT",
            "snapshot_parking_name": "B 주차장",
            "snapshot_in_time": "2026-05-29T08:00:00"
        }'::jsonb,
        'SCHEDULED',
        TIMESTAMP '2026-07-01 00:10:00',
        TIMESTAMP '2026-07-31 00:10:00'
    );

COMMIT;

-- 테이블을 모두 생성한 뒤 애플리케이션 계정에 권한을 부여한다.
GRANT USAGE ON SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bono_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bono_user;

-- 이후 같은 실행 계정으로 생성되는 테이블과 시퀀스에도 자동으로 권한을 부여한다.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO bono_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON SEQUENCES TO bono_user;
