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
-- 총 8동. 각 동 별 20세대
-- =====================================================
INSERT INTO member
    (login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone,
     role, create_at, delete_at, mem_status)
VALUES
    -- << 관리자 >> 
    -- 근무
    ('admin1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자', '010-1111-1111', 'ADMIN', NOW(), NULL, 'ACTIVE'),
    ('admin2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '경비원', '010-1111-1111', 'ADMIN', NOW(), NULL, 'ACTIVE'),
    -- 휴직
    ('admin3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '관리자', '010-1111-1111', 'ADMIN', NOW(), NULL, 'ON_LEAVE'),
    -- 퇴사
    ('admin4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 0, 0, '경비원', '010-1111-1111', 'ADMIN', NOW(), NULL, 'INACTIVE'),


    -- << 입주민 >> 
    -- [101동]
    -- 거주
    ('res1', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 101, '마틴', '010-2222-0101', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    -- 가입 승인 대기
    ('res2', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 201, '제임스', '010-2222-0102', 'PENDING', NOW(), NULL, 'ACTIVE'),
    -- 전출 신청
    ('res3', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 301, '오드리', '010-2222-0103', 'RESIDENT', NOW(), NULL, 'WITHDRAW_PENDING'),
    -- 전출 완료 / 빈 세대
    ('res4', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 401, '닉', '010-2222-0104', 'RESIDENT', NOW(), NULL, 'EMPTY'),
    
    ('res5', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 501, '칼', '010-2222-0201', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res6', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 601, '찰스', '010-2222-0202', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res7', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 701, '마이클', '010-2222-0203', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res8', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 801, '케빈', '010-2222-0204', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res9', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 901, '오스틴', '010-2222-0301', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res10', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1001, '토니스타크', '010-3000-0010', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res11', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 102, '토르', '010-3000-0011', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res12', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 202, '로키', '010-3000-0012', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res13', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 302, '스티브로저스', '010-3000-0013', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res14', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 402, '나타샤로마노프', '010-3000-0014', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res15', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 502, '클린트바튼', '010-3000-0015', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res16', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 602, '브루스배너', '010-3000-0016', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res17', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 702, '피터파커', '010-3000-0017', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res18', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 802, '스티븐스트레인지', '010-3000-0018', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res19', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 902, '완다막시모프', '010-3000-0019', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res0', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 101, 1002, '티파록하트', '010-3000-0152', 'RESIDENT', NOW(), NOW(), 'ACTIVE'),

    -- 102동: 등록 회원 19세대 + 빈집 1세대
    ('res20', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 101, '비전', '010-3000-0020', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res21', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 201, '샘윌슨', '010-3000-0021', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res22', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 301, '버키반즈', '010-3000-0022', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res23', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 401, '티찰라', '010-3000-0023', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res24', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 501, '캐럴댄버스', '010-3000-0024', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res25', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 601, '스콧랭', '010-3000-0025', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res26', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 701, '호프반다인', '010-3000-0026', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res27', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 801, '피터퀼', '010-3000-0027', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res28', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 901, '가모라', '010-3000-0028', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res29', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1001, '드랙스', '010-3000-0029', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res30', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 102, '로켓', '010-3000-0030', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res31', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 202, '그루트', '010-3000-0031', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res32', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 302, '네뷸라', '010-3000-0032', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res33', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 402, '웨이드윌슨', '010-3000-0033', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res34', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 502, '로건', '010-3000-0034', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res35', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 602, '찰스자비에', '010-3000-0035', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res36', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 702, '에릭렌셔', '010-3000-0036', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res37', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 802, '브루스웨인', '010-3000-0037', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res38', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 902, '클라크켄트', '010-3000-0038', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_102_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 102, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY'),

    -- 201동: 등록 회원 19세대 + 빈집 1세대
    ('res39', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 101, '다이애나프린스', '010-3000-0039', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res40', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 201, '배리앨런', '010-3000-0040', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res41', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 301, '아서커리', '010-3000-0041', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res42', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 401, '할조던', '010-3000-0042', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res43', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 501, '딕그레이슨', '010-3000-0043', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res44', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 601, '셀리나카일', '010-3000-0044', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res45', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 701, '할리퀸', '010-3000-0045', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res46', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 801, '존콘스탄틴', '010-3000-0046', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res47', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 901, '고죠사토루', '010-3000-0047', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res48', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 1001, '이타도리유지', '010-3000-0048', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res49', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 102, '후시구로메구미', '010-3000-0049', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res50', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 202, '쿠기사키노바라', '010-3000-0050', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res51', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 302, '료멘스쿠나', '010-3000-0051', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res52', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 402, '나나미켄토', '010-3000-0052', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res53', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 502, '이에이리쇼코', '010-3000-0053', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res54', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 602, '게토스구루', '010-3000-0054', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res55', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 702, '하타케카카시', '010-3000-0055', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res56', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 802, '우즈마키나루토', '010-3000-0056', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res57', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 902, '우치하사스케', '010-3000-0057', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_201_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 201, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY'),

    -- 202동: 등록 회원 19세대 + 빈집 1세대
    ('res58', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 101, '하루노사쿠라', '010-3000-0058', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res59', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 201, '지라이야', '010-3000-0059', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res60', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 301, '츠나데', '010-3000-0060', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res61', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 401, '우치하이타치', '010-3000-0061', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res62', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 501, '가아라', '010-3000-0062', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res63', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 601, '록리', '010-3000-0063', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res64', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 701, '휴우가히나타', '010-3000-0064', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res65', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 801, '몽키디루피', '010-3000-0065', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res66', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 901, '롤로노아조로', '010-3000-0066', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res67', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 1001, '나미', '010-3000-0067', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res68', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 102, '상디', '010-3000-0068', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res69', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 202, '니코로빈', '010-3000-0069', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res70', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 302, '우솝', '010-3000-0070', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res71', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 402, '토니토니쵸파', '010-3000-0071', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res72', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 502, '프랑키', '010-3000-0072', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res73', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 602, '브룩', '010-3000-0073', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res74', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 702, '징베', '010-3000-0074', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res75', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 802, '샹크스', '010-3000-0075', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res76', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 902, '트라팔가로', '010-3000-0076', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_202_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 202, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY'),

    -- 301동: 등록 회원 19세대 + 빈집 1세대
    ('res77', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 101, '포트거스에이스', '010-3000-0077', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res78', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 201, '사보', '010-3000-0078', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res79', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 301, '손오공', '010-3000-0079', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res80', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 401, '베지터', '010-3000-0080', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res81', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 501, '손오반', '010-3000-0081', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res82', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 601, '피콜로', '010-3000-0082', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res83', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 701, '트랭크스', '010-3000-0083', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res84', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 801, '부르마', '010-3000-0084', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res85', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 901, '크리링', '010-3000-0085', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res86', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 1001, '프리저', '010-3000-0086', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res87', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 102, '비루스', '010-3000-0087', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res88', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 202, '카마도탄지로', '010-3000-0088', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res89', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 302, '카마도네즈코', '010-3000-0089', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res90', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 402, '아가츠마젠이츠', '010-3000-0090', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res91', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 502, '하시비라이노스케', '010-3000-0091', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res92', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 602, '렌고쿠쿄주로', '010-3000-0092', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res93', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 702, '토미오카기유', '010-3000-0093', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res94', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 802, '코쵸우시노부', '010-3000-0094', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res95', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 902, '우즈이텐겐', '010-3000-0095', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_301_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 301, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY'),

    -- 302동: 등록 회원 19세대 + 빈집 1세대
    ('res96', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 101, '토키토무이치로', '010-3000-0096', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res97', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 201, '칸로지미츠리', '010-3000-0097', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res98', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 301, '이구로오바나이', '010-3000-0098', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res99', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 401, '에렌예거', '010-3000-0099', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res100', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 501, '미카사아커만', '010-3000-0100', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res101', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 601, '아르민알레르토', '010-3000-0101', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res102', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 701, '리바이아커만', '010-3000-0102', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res103', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 801, '한지조에', '010-3000-0103', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res104', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 901, '엘빈스미스', '010-3000-0104', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res105', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 1001, '라이너브라운', '010-3000-0105', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res106', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 102, '애니레온하트', '010-3000-0106', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res107', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 202, '미도리야이즈쿠', '010-3000-0107', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res108', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 302, '바쿠고카츠키', '010-3000-0108', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res109', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 402, '토도로키쇼토', '010-3000-0109', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res110', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 502, '올마이트', '010-3000-0110', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res111', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 602, '아이자와쇼타', '010-3000-0111', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res112', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 702, '우라라카오챠코', '010-3000-0112', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res113', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 802, '야가미라이토', '010-3000-0113', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res114', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 902, '엘로우라이트', '010-3000-0114', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_302_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 302, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY'),

    -- 401동: 등록 회원 19세대 + 빈집 1세대
    ('res115', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 101, '류크', '010-3000-0115', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res116', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 201, '에드워드엘릭', '010-3000-0116', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res117', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 301, '알폰스엘릭', '010-3000-0117', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res118', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 401, '로이머스탱', '010-3000-0118', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res119', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 501, '스파이크스피겔', '010-3000-0119', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res120', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 601, '제트블랙', '010-3000-0120', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res121', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 701, '페이발렌타인', '010-3000-0121', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res122', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 801, '키리토', '010-3000-0122', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res123', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 901, '아스나', '010-3000-0123', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res124', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 1001, '렘', '010-3000-0124', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res125', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 102, '에밀리아', '010-3000-0125', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res126', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 202, '아냐포저', '010-3000-0126', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res127', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 302, '로이드포저', '010-3000-0127', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res128', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 402, '요르포저', '010-3000-0128', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res129', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 502, '프리렌', '010-3000-0129', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res130', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 602, '페른', '010-3000-0130', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res131', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 702, '슈타르크', '010-3000-0131', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res132', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 802, '링크', '010-3000-0132', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res133', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 902, '젤다', '010-3000-0133', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_401_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 401, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY'),

    -- 402동: 등록 회원 19세대 + 빈집 1세대
    ('res134', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 101, '마리오', '010-3000-0134', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res135', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 201, '루이지', '010-3000-0135', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res136', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 301, '피치', '010-3000-0136', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res137', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 401, '커비', '010-3000-0137', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res138', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 501, '소닉', '010-3000-0138', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res139', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 601, '테일즈', '010-3000-0139', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res140', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 701, '크레토스', '010-3000-0140', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res141', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 801, '아트레우스', '010-3000-0141', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res142', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 901, '조엘밀러', '010-3000-0142', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res143', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 1001, '엘리윌리엄스', '010-3000-0143', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res144', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 102, '아서모건', '010-3000-0144', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res145', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 202, '존마스턴', '010-3000-0145', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res146', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 302, '게롤트', '010-3000-0146', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res147', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 402, '시리', '010-3000-0147', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res148', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 502, '라라크로프트', '010-3000-0148', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res149', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 602, '네이선드레이크', '010-3000-0149', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res150', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 702, '마스터치프', '010-3000-0150', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('res151', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 802, '클라우드스트라이프', '010-3000-0151', 'RESIDENT', NOW(), NULL, 'ACTIVE'),
    ('unit_402_902', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 902, '미등록', '미등록', 'RESIDENT', NOW(), NULL, 'EMPTY'),
    ('unit_402_1002', '$2a$10$4HZzIIhKHAc3Bmy1t8vdKeoI9fWfl/.a3Il8qR7qp7sdLvE4ZkXU6', 402, 1002, '미등록', '미등록', 'RESIDENT', NOW(), NOW(), 'EMPTY');


-- =====================================================
-- 2. 주차장
-- 기존과 동일한 주차장 4개
-- =====================================================
INSERT INTO parking (parking_name, parking_spaces, parking_location)
VALUES
    ('A 주차장', 100, 'A 지하 주차장'),
    ('B 주차장', 80, 'B 지하 주차장'),
    ('C 주차장', 100, 'C 지하 주차장'),
    ('D 주차장', 50, '지상 주차장');


-- =====================================================
-- 3. 게이트
-- 기존과 동일한 게이트 8개
-- =====================================================
INSERT INTO gate (parking_no, gate_name, gate_type)
VALUES
    (1, 'A-IN', 'In'),
    (1, 'A-OUT', 'Out'),
    (2, 'B-IN', 'In'),
    (2, 'B-OUT', 'Out'),
    (3, 'C-IN', 'In'),
    (3, 'C-OUT', 'Out'),
    (4, 'D-IN', 'In'),
    (4, 'D-OUT', 'Out');

-- =====================================================
-- 5. 카메라
-- 기존과 동일한 카메라 8개
-- =====================================================
INSERT INTO camera (gate_no, camera_name, camera_type, install_date)
VALUES
    (1, 'CAM-A1', 'In',   DATE '2025-01-01'),
    (2, 'CAM-A2', 'Out',  DATE '2025-01-01'),
    (3, 'CAM-B1', 'In',   DATE '2025-01-03'),
    (4, 'CAM-B2', 'Out',  DATE '2025-01-03'),
    (5, 'CAM-C1', 'In',   DATE '2025-01-05'),
    (6, 'CAM-C2', 'Out',  DATE '2025-01-05'),
    (7, 'CAM-D1', 'In',  DATE '2025-02-05'),
    (8, 'CAM-D2', 'Out',  DATE '2025-02-05');


    
-- =====================================================
-- 4. 차량
-- =====================================================
-- 입주민 등록 차량 더미
-- vehicle_type: normal
-- vehicle_status: APPROVED
-- start_date는 차량마다 다르게 지정
-- end_date는 실제 프론트 구조처럼 start_date + 등록 개월 수로 생성
 INSERT INTO vehicle_car
    (vehicle_type, car_no, alias_car_no, vehicle_status, start_date, end_date, member_no, approved_at)
VALUES
    ('normal', '222하5233', NULL, 'APPROVED', '2026-07-01 09:00:00', '2026-08-01 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res1'), '2026-07-01 10:00:00'),
    ('normal', '26무3111', NULL, 'APPROVED', '2026-07-03 10:00:00', '2026-10-03 10:00:00', (SELECT member_no FROM member WHERE login_id = 'res2'), '2026-07-03 11:00:00'),
    ('normal', '41소2593', NULL, 'APPROVED', '2026-07-05 11:00:00', '2027-01-05 11:00:00', (SELECT member_no FROM member WHERE login_id = 'res3'), '2026-07-05 12:00:00'),
    ('normal', '47조2603', NULL, 'APPROVED', '2026-07-07 12:00:00', '2027-07-07 12:00:00', (SELECT member_no FROM member WHERE login_id = 'res4'), '2026-07-07 13:00:00'),
    ('normal', '817라7385', NULL, 'APPROVED', '2026-07-09 13:00:00', '2026-08-09 13:00:00', (SELECT member_no FROM member WHERE login_id = 'res5'), '2026-07-09 14:00:00'),
    ('normal', '95마7152', NULL, 'APPROVED', '2026-07-11 14:00:00', '2026-10-11 14:00:00', (SELECT member_no FROM member WHERE login_id = 'res6'), '2026-07-11 15:00:00'),
    ('normal', '67모4231', NULL, 'APPROVED', '2026-07-13 15:00:00', '2027-01-13 15:00:00', (SELECT member_no FROM member WHERE login_id = 'res7'), '2026-07-13 16:00:00'),
    ('normal', '222마2574', NULL, 'APPROVED', '2026-07-15 16:00:00', '2027-07-15 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res8'), '2026-07-15 17:00:00'),
    ('normal', '55오0359', NULL, 'APPROVED', '2026-07-17 17:00:00', '2026-10-17 17:00:00', (SELECT member_no FROM member WHERE login_id = 'res9'), '2026-07-17 18:00:00'),
    ('normal', '80가1010', NULL, 'APPROVED', '2026-05-06 09:45:00', '2026-08-06 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res10'), '2026-05-06 10:45:00'),
    ('normal', '87나1011', NULL, 'APPROVED', '2026-05-11 12:00:00', '2026-08-11 12:00:00', (SELECT member_no FROM member WHERE login_id = 'res11'), '2026-05-11 13:00:00'),
    ('normal', '94다1012', NULL, 'APPROVED', '2026-02-13 14:00:00', '2027-02-13 14:00:00', (SELECT member_no FROM member WHERE login_id = 'res12'), '2026-02-13 15:00:00'),
    ('normal', '11라1013', NULL, 'APPROVED', '2026-06-21 09:45:00', '2026-12-21 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res13'), '2026-06-21 10:45:00'),
    ('normal', '18마1014', NULL, 'APPROVED', '2026-06-04 16:30:00', '2026-09-04 16:30:00', (SELECT member_no FROM member WHERE login_id = 'res14'), '2026-06-04 17:30:00'),
    ('normal', '25거1015', NULL, 'APPROVED', '2026-07-05 18:30:00', '2026-08-05 18:30:00', (SELECT member_no FROM member WHERE login_id = 'res15'), '2026-07-05 19:30:00'),
    ('normal', '32너1016', NULL, 'APPROVED', '2026-06-16 08:45:00', '2026-09-16 08:45:00', (SELECT member_no FROM member WHERE login_id = 'res16'), '2026-06-16 09:45:00'),
    ('normal', '39더1017', NULL, 'APPROVED', '2026-03-24 10:45:00', '2027-03-24 10:45:00', (SELECT member_no FROM member WHERE login_id = 'res17'), '2026-03-24 11:45:00'),
    ('normal', '46러1018', NULL, 'APPROVED', '2026-04-02 11:30:00', '2026-10-02 11:30:00', (SELECT member_no FROM member WHERE login_id = 'res18'), '2026-04-02 12:30:00'),
    ('normal', '53머1019', NULL, 'APPROVED', '2026-06-17 16:45:00', '2026-09-17 16:45:00', (SELECT member_no FROM member WHERE login_id = 'res19'), '2026-06-17 17:45:00'),
    ('normal', '60버1020', NULL, 'APPROVED', '2026-07-15 08:00:00', '2027-01-15 08:00:00', (SELECT member_no FROM member WHERE login_id = 'res20'), '2026-07-15 09:00:00'),
    ('normal', '67서1021', NULL, 'APPROVED', '2025-12-24 09:00:00', '2026-12-24 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res21'), '2025-12-24 10:00:00'),
    ('normal', '74어1022', NULL, 'APPROVED', '2025-10-10 08:00:00', '2026-10-10 08:00:00', (SELECT member_no FROM member WHERE login_id = 'res22'), '2025-10-10 09:00:00'),
    ('normal', '81저1023', NULL, 'APPROVED', '2026-07-12 08:45:00', '2026-08-12 08:45:00', (SELECT member_no FROM member WHERE login_id = 'res23'), '2026-07-12 09:45:00'),
    ('normal', '88고1024', NULL, 'APPROVED', '2026-05-07 11:45:00', '2026-08-07 11:45:00', (SELECT member_no FROM member WHERE login_id = 'res24'), '2026-05-07 12:45:00'),
    ('normal', '95노1025', NULL, 'APPROVED', '2026-04-28 14:15:00', '2026-10-28 14:15:00', (SELECT member_no FROM member WHERE login_id = 'res25'), '2026-04-28 15:15:00'),
    ('normal', '12도1026', NULL, 'APPROVED', '2026-07-10 11:45:00', '2026-10-10 11:45:00', (SELECT member_no FROM member WHERE login_id = 'res26'), '2026-07-10 12:45:00'),
    ('normal', '19로1027', NULL, 'APPROVED', '2026-05-16 08:30:00', '2026-08-16 08:30:00', (SELECT member_no FROM member WHERE login_id = 'res27'), '2026-05-16 09:30:00'),
    ('normal', '26모1028', NULL, 'APPROVED', '2026-05-10 18:15:00', '2027-05-10 18:15:00', (SELECT member_no FROM member WHERE login_id = 'res28'), '2026-05-10 19:15:00'),
    ('normal', '33보1029', NULL, 'APPROVED', '2025-11-14 15:45:00', '2026-11-14 15:45:00', (SELECT member_no FROM member WHERE login_id = 'res29'), '2025-11-14 16:45:00'),
    ('normal', '40소1030', NULL, 'APPROVED', '2026-05-16 12:30:00', '2026-08-16 12:30:00', (SELECT member_no FROM member WHERE login_id = 'res30'), '2026-05-16 13:30:00'),
    ('normal', '47오1031', NULL, 'APPROVED', '2026-07-08 09:15:00', '2026-08-08 09:15:00', (SELECT member_no FROM member WHERE login_id = 'res31'), '2026-07-08 10:15:00'),
    ('normal', '54조1032', NULL, 'APPROVED', '2026-05-28 09:30:00', '2026-11-28 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res32'), '2026-05-28 10:30:00'),
    ('normal', '61구1033', NULL, 'APPROVED', '2026-07-08 08:45:00', '2026-08-08 08:45:00', (SELECT member_no FROM member WHERE login_id = 'res33'), '2026-07-08 09:45:00'),
    ('normal', '68누1034', NULL, 'APPROVED', '2026-06-29 08:00:00', '2026-07-29 08:00:00', (SELECT member_no FROM member WHERE login_id = 'res34'), '2026-06-29 09:00:00'),
    ('normal', '75두1035', NULL, 'APPROVED', '2026-06-12 09:30:00', '2026-09-12 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res35'), '2026-06-12 10:30:00'),
    ('normal', '82루1036', NULL, 'APPROVED', '2026-06-29 09:30:00', '2026-09-29 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res36'), '2026-06-29 10:30:00'),
    ('normal', '89무1037', NULL, 'APPROVED', '2026-05-31 08:45:00', '2027-05-31 08:45:00', (SELECT member_no FROM member WHERE login_id = 'res37'), '2026-05-31 09:45:00'),
    ('normal', '96부1038', NULL, 'APPROVED', '2026-02-07 12:45:00', '2027-02-07 12:45:00', (SELECT member_no FROM member WHERE login_id = 'res38'), '2026-02-07 13:45:00'),
    ('normal', '13수1039', NULL, 'APPROVED', '2026-07-13 11:30:00', '2026-10-13 11:30:00', (SELECT member_no FROM member WHERE login_id = 'res39'), '2026-07-13 12:30:00'),
    ('normal', '20우1040', NULL, 'APPROVED', '2026-01-21 09:45:00', '2027-01-21 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res40'), '2026-01-21 10:45:00'),
    ('normal', '27주1041', NULL, 'APPROVED', '2026-07-01 10:00:00', '2026-08-01 10:00:00', (SELECT member_no FROM member WHERE login_id = 'res41'), '2026-07-01 11:00:00'),
    ('normal', '34하1042', NULL, 'APPROVED', '2025-10-27 09:00:00', '2026-10-27 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res42'), '2025-10-27 10:00:00'),
    ('normal', '41허1043', NULL, 'APPROVED', '2026-05-04 14:30:00', '2026-11-04 14:30:00', (SELECT member_no FROM member WHERE login_id = 'res43'), '2026-05-04 15:30:00'),
    ('normal', '48호1044', NULL, 'APPROVED', '2026-03-17 17:30:00', '2027-03-17 17:30:00', (SELECT member_no FROM member WHERE login_id = 'res44'), '2026-03-17 18:30:00'),
    ('normal', '55가1045', NULL, 'APPROVED', '2026-07-02 14:30:00', '2026-08-02 14:30:00', (SELECT member_no FROM member WHERE login_id = 'res45'), '2026-07-02 15:30:00'),
    ('normal', '62나1046', NULL, 'APPROVED', '2026-06-17 13:15:00', '2026-09-17 13:15:00', (SELECT member_no FROM member WHERE login_id = 'res46'), '2026-06-17 14:15:00'),
    ('normal', '69다1047', NULL, 'APPROVED', '2026-02-25 14:00:00', '2026-08-25 14:00:00', (SELECT member_no FROM member WHERE login_id = 'res47'), '2026-02-25 15:00:00'),
    ('normal', '76라1048', NULL, 'APPROVED', '2026-07-04 13:45:00', '2026-08-04 13:45:00', (SELECT member_no FROM member WHERE login_id = 'res48'), '2026-07-04 14:45:00'),
    ('normal', '83마1049', NULL, 'APPROVED', '2026-07-18 08:00:00', '2026-08-18 08:00:00', (SELECT member_no FROM member WHERE login_id = 'res49'), '2026-07-18 09:00:00'),
    ('normal', '90거1050', NULL, 'APPROVED', '2026-05-30 08:15:00', '2026-08-30 08:15:00', (SELECT member_no FROM member WHERE login_id = 'res50'), '2026-05-30 09:15:00'),
    ('normal', '97너1051', NULL, 'APPROVED', '2026-05-25 09:15:00', '2026-08-25 09:15:00', (SELECT member_no FROM member WHERE login_id = 'res51'), '2026-05-25 10:15:00'),
    ('normal', '14더1052', NULL, 'APPROVED', '2026-05-16 13:15:00', '2026-08-16 13:15:00', (SELECT member_no FROM member WHERE login_id = 'res52'), '2026-05-16 14:15:00'),
    ('normal', '21러1053', NULL, 'APPROVED', '2026-07-07 11:45:00', '2026-08-07 11:45:00', (SELECT member_no FROM member WHERE login_id = 'res53'), '2026-07-07 12:45:00'),
    ('normal', '28머1054', NULL, 'APPROVED', '2026-05-30 14:30:00', '2026-08-30 14:30:00', (SELECT member_no FROM member WHERE login_id = 'res54'), '2026-05-30 15:30:00'),
    ('normal', '35버1055', NULL, 'APPROVED', '2026-07-09 15:00:00', '2026-08-09 15:00:00', (SELECT member_no FROM member WHERE login_id = 'res55'), '2026-07-09 16:00:00'),
    ('normal', '42서1056', NULL, 'APPROVED', '2026-02-12 11:15:00', '2027-02-12 11:15:00', (SELECT member_no FROM member WHERE login_id = 'res56'), '2026-02-12 12:15:00'),
    ('normal', '49어1057', NULL, 'APPROVED', '2026-05-13 17:00:00', '2026-11-13 17:00:00', (SELECT member_no FROM member WHERE login_id = 'res57'), '2026-05-13 18:00:00'),
    ('normal', '56저1058', NULL, 'APPROVED', '2026-07-10 16:00:00', '2026-08-10 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res58'), '2026-07-10 17:00:00'),
    ('normal', '63고1059', NULL, 'APPROVED', '2026-05-16 11:45:00', '2026-08-16 11:45:00', (SELECT member_no FROM member WHERE login_id = 'res59'), '2026-05-16 12:45:00'),
    ('normal', '70노1060', NULL, 'APPROVED', '2026-03-03 13:45:00', '2026-09-03 13:45:00', (SELECT member_no FROM member WHERE login_id = 'res60'), '2026-03-03 14:45:00'),
    ('normal', '77도1061', NULL, 'APPROVED', '2026-06-25 15:30:00', '2026-07-25 15:30:00', (SELECT member_no FROM member WHERE login_id = 'res61'), '2026-06-25 16:30:00'),
    ('normal', '84로1062', NULL, 'APPROVED', '2026-03-20 11:15:00', '2026-09-20 11:15:00', (SELECT member_no FROM member WHERE login_id = 'res62'), '2026-03-20 12:15:00'),
    ('normal', '91모1063', NULL, 'APPROVED', '2026-05-06 13:30:00', '2026-08-06 13:30:00', (SELECT member_no FROM member WHERE login_id = 'res63'), '2026-05-06 14:30:00'),
    ('normal', '98보1064', NULL, 'APPROVED', '2026-07-14 09:30:00', '2026-08-14 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res64'), '2026-07-14 10:30:00'),
    ('normal', '15소1065', NULL, 'APPROVED', '2026-06-24 16:00:00', '2026-12-24 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res65'), '2026-06-24 17:00:00'),
    ('normal', '22오1066', NULL, 'APPROVED', '2026-06-01 09:15:00', '2026-09-01 09:15:00', (SELECT member_no FROM member WHERE login_id = 'res66'), '2026-06-01 10:15:00'),
    ('normal', '29조1067', NULL, 'APPROVED', '2026-07-14 17:30:00', '2026-08-14 17:30:00', (SELECT member_no FROM member WHERE login_id = 'res67'), '2026-07-14 18:30:00'),
    ('normal', '36구1068', NULL, 'APPROVED', '2026-03-20 16:00:00', '2026-09-20 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res68'), '2026-03-20 17:00:00'),
    ('normal', '43누1069', NULL, 'APPROVED', '2026-05-16 13:30:00', '2026-08-16 13:30:00', (SELECT member_no FROM member WHERE login_id = 'res69'), '2026-05-16 14:30:00'),
    ('normal', '50두1070', NULL, 'APPROVED', '2026-03-04 08:45:00', '2026-09-04 08:45:00', (SELECT member_no FROM member WHERE login_id = 'res70'), '2026-03-04 09:45:00'),
    ('normal', '57루1071', NULL, 'APPROVED', '2026-02-03 18:45:00', '2027-02-03 18:45:00', (SELECT member_no FROM member WHERE login_id = 'res71'), '2026-02-03 19:45:00'),
    ('normal', '64무1072', NULL, 'APPROVED', '2026-05-17 12:45:00', '2026-08-17 12:45:00', (SELECT member_no FROM member WHERE login_id = 'res72'), '2026-05-17 13:45:00'),
    ('normal', '71부1073', NULL, 'APPROVED', '2026-06-30 08:15:00', '2026-07-30 08:15:00', (SELECT member_no FROM member WHERE login_id = 'res73'), '2026-06-30 09:15:00'),
    ('normal', '78수1074', NULL, 'APPROVED', '2026-07-08 14:15:00', '2026-08-08 14:15:00', (SELECT member_no FROM member WHERE login_id = 'res74'), '2026-07-08 15:15:00'),
    ('normal', '85우1075', NULL, 'APPROVED', '2026-07-10 10:45:00', '2026-10-10 10:45:00', (SELECT member_no FROM member WHERE login_id = 'res75'), '2026-07-10 11:45:00'),
    ('normal', '92주1076', NULL, 'APPROVED', '2026-03-12 09:30:00', '2026-09-12 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res76'), '2026-03-12 10:30:00'),
    ('normal', '99하1077', NULL, 'APPROVED', '2026-02-19 10:30:00', '2026-08-19 10:30:00', (SELECT member_no FROM member WHERE login_id = 'res77'), '2026-02-19 11:30:00'),
    ('normal', '16허1078', NULL, 'APPROVED', '2026-04-24 18:30:00', '2026-10-24 18:30:00', (SELECT member_no FROM member WHERE login_id = 'res78'), '2026-04-24 19:30:00'),
    ('normal', '23호1079', NULL, 'APPROVED', '2025-09-21 15:45:00', '2026-09-21 15:45:00', (SELECT member_no FROM member WHERE login_id = 'res79'), '2025-09-21 16:45:00'),
    ('normal', '30가1080', NULL, 'APPROVED', '2026-03-28 08:30:00', '2027-03-28 08:30:00', (SELECT member_no FROM member WHERE login_id = 'res80'), '2026-03-28 09:30:00'),
    ('normal', '37나1081', NULL, 'APPROVED', '2026-07-07 12:30:00', '2026-10-07 12:30:00', (SELECT member_no FROM member WHERE login_id = 'res81'), '2026-07-07 13:30:00'),
    ('normal', '44다1082', NULL, 'APPROVED', '2026-02-11 12:15:00', '2027-02-11 12:15:00', (SELECT member_no FROM member WHERE login_id = 'res82'), '2026-02-11 13:15:00'),
    ('normal', '51라1083', NULL, 'APPROVED', '2026-07-09 16:30:00', '2026-08-09 16:30:00', (SELECT member_no FROM member WHERE login_id = 'res83'), '2026-07-09 17:30:00'),
    ('normal', '58마1084', NULL, 'APPROVED', '2026-04-09 18:45:00', '2027-04-09 18:45:00', (SELECT member_no FROM member WHERE login_id = 'res84'), '2026-04-09 19:45:00'),
    ('normal', '65거1085', NULL, 'APPROVED', '2026-07-06 13:00:00', '2026-08-06 13:00:00', (SELECT member_no FROM member WHERE login_id = 'res85'), '2026-07-06 14:00:00'),
    ('normal', '72너1086', NULL, 'APPROVED', '2026-06-26 15:00:00', '2026-07-26 15:00:00', (SELECT member_no FROM member WHERE login_id = 'res86'), '2026-06-26 16:00:00'),
    ('normal', '79더1087', NULL, 'APPROVED', '2025-11-09 11:00:00', '2026-11-09 11:00:00', (SELECT member_no FROM member WHERE login_id = 'res87'), '2025-11-09 12:00:00'),
    ('normal', '86러1088', NULL, 'APPROVED', '2026-06-28 09:45:00', '2026-07-28 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res88'), '2026-06-28 10:45:00'),
    ('normal', '93머1089', NULL, 'APPROVED', '2026-05-27 10:30:00', '2026-11-27 10:30:00', (SELECT member_no FROM member WHERE login_id = 'res89'), '2026-05-27 11:30:00'),
    ('normal', '10버1090', NULL, 'APPROVED', '2026-03-03 12:00:00', '2026-09-03 12:00:00', (SELECT member_no FROM member WHERE login_id = 'res90'), '2026-03-03 13:00:00'),
    ('normal', '17서1091', NULL, 'APPROVED', '2026-03-03 09:30:00', '2026-09-03 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res91'), '2026-03-03 10:30:00'),
    ('normal', '24어1092', NULL, 'APPROVED', '2026-06-03 12:15:00', '2026-09-03 12:15:00', (SELECT member_no FROM member WHERE login_id = 'res92'), '2026-06-03 13:15:00'),
    ('normal', '31저1093', NULL, 'APPROVED', '2026-06-22 14:15:00', '2027-06-22 14:15:00', (SELECT member_no FROM member WHERE login_id = 'res93'), '2026-06-22 15:15:00'),
    ('normal', '38고1094', NULL, 'APPROVED', '2026-05-28 17:30:00', '2026-08-28 17:30:00', (SELECT member_no FROM member WHERE login_id = 'res94'), '2026-05-28 18:30:00'),
    ('normal', '45노1095', NULL, 'APPROVED', '2026-06-18 15:45:00', '2026-09-18 15:45:00', (SELECT member_no FROM member WHERE login_id = 'res95'), '2026-06-18 16:45:00'),
    ('normal', '52도1096', NULL, 'APPROVED', '2026-05-04 16:00:00', '2026-11-04 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res96'), '2026-05-04 17:00:00'),
    ('normal', '59로1097', NULL, 'APPROVED', '2026-04-06 09:00:00', '2026-10-06 09:00:00', (SELECT member_no FROM member WHERE login_id = 'res97'), '2026-04-06 10:00:00'),
    ('normal', '66모1098', NULL, 'APPROVED', '2026-05-07 09:45:00', '2027-05-07 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res98'), '2026-05-07 10:45:00'),
    ('normal', '73보1099', NULL, 'APPROVED', '2026-05-09 13:15:00', '2026-08-09 13:15:00', (SELECT member_no FROM member WHERE login_id = 'res99'), '2026-05-09 14:15:00'),
    ('normal', '80소1100', NULL, 'APPROVED', '2026-07-18 10:45:00', '2027-01-18 10:45:00', (SELECT member_no FROM member WHERE login_id = 'res100'), '2026-07-18 11:45:00'),
    ('normal', '87오1101', NULL, 'APPROVED', '2026-06-01 10:00:00', '2026-12-01 10:00:00', (SELECT member_no FROM member WHERE login_id = 'res101'), '2026-06-01 11:00:00'),
    ('normal', '94조1102', NULL, 'APPROVED', '2025-09-24 15:30:00', '2026-09-24 15:30:00', (SELECT member_no FROM member WHERE login_id = 'res102'), '2025-09-24 16:30:00'),
    ('normal', '11구1103', NULL, 'APPROVED', '2026-02-25 11:15:00', '2026-08-25 11:15:00', (SELECT member_no FROM member WHERE login_id = 'res103'), '2026-02-25 12:15:00'),
    ('normal', '18누1104', NULL, 'APPROVED', '2026-07-13 13:30:00', '2026-08-13 13:30:00', (SELECT member_no FROM member WHERE login_id = 'res104'), '2026-07-13 14:30:00'),
    ('normal', '25두1105', NULL, 'APPROVED', '2026-03-22 12:15:00', '2026-09-22 12:15:00', (SELECT member_no FROM member WHERE login_id = 'res105'), '2026-03-22 13:15:00'),
    ('normal', '32루1106', NULL, 'APPROVED', '2026-07-01 08:45:00', '2026-08-01 08:45:00', (SELECT member_no FROM member WHERE login_id = 'res106'), '2026-07-01 09:45:00'),
    ('normal', '39무1107', NULL, 'APPROVED', '2026-03-10 12:30:00', '2026-09-10 12:30:00', (SELECT member_no FROM member WHERE login_id = 'res107'), '2026-03-10 13:30:00'),
    ('normal', '46부1108', NULL, 'APPROVED', '2026-06-27 09:15:00', '2026-07-27 09:15:00', (SELECT member_no FROM member WHERE login_id = 'res108'), '2026-06-27 10:15:00'),
    ('normal', '53수1109', NULL, 'APPROVED', '2026-06-17 13:15:00', '2026-12-17 13:15:00', (SELECT member_no FROM member WHERE login_id = 'res109'), '2026-06-17 14:15:00'),
    ('normal', '60우1110', NULL, 'APPROVED', '2026-04-01 18:45:00', '2027-04-01 18:45:00', (SELECT member_no FROM member WHERE login_id = 'res110'), '2026-04-01 19:45:00'),
    ('normal', '67주1111', NULL, 'APPROVED', '2026-07-15 17:15:00', '2026-08-15 17:15:00', (SELECT member_no FROM member WHERE login_id = 'res111'), '2026-07-15 18:15:00'),
    ('normal', '74하1112', NULL, 'APPROVED', '2026-07-12 18:30:00', '2026-08-12 18:30:00', (SELECT member_no FROM member WHERE login_id = 'res112'), '2026-07-12 19:30:00'),
    ('normal', '81허1113', NULL, 'APPROVED', '2026-02-14 12:45:00', '2027-02-14 12:45:00', (SELECT member_no FROM member WHERE login_id = 'res113'), '2026-02-14 13:45:00'),
    ('normal', '88호1114', NULL, 'APPROVED', '2026-06-24 18:30:00', '2027-06-24 18:30:00', (SELECT member_no FROM member WHERE login_id = 'res114'), '2026-06-24 19:30:00'),
    ('normal', '95가1115', NULL, 'APPROVED', '2026-07-05 18:45:00', '2026-08-05 18:45:00', (SELECT member_no FROM member WHERE login_id = 'res115'), '2026-07-05 19:45:00'),
    ('normal', '12나1116', NULL, 'APPROVED', '2026-07-04 09:45:00', '2026-08-04 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res116'), '2026-07-04 10:45:00'),
    ('normal', '19다1117', NULL, 'APPROVED', '2026-07-10 18:00:00', '2027-07-10 18:00:00', (SELECT member_no FROM member WHERE login_id = 'res117'), '2026-07-10 19:00:00'),
    ('normal', '26라1118', NULL, 'APPROVED', '2026-07-17 11:45:00', '2026-08-17 11:45:00', (SELECT member_no FROM member WHERE login_id = 'res118'), '2026-07-17 12:45:00'),
    ('normal', '33마1119', NULL, 'APPROVED', '2026-05-15 15:45:00', '2026-08-15 15:45:00', (SELECT member_no FROM member WHERE login_id = 'res119'), '2026-05-15 16:45:00'),
    ('normal', '40거1120', NULL, 'APPROVED', '2026-03-20 17:45:00', '2026-09-20 17:45:00', (SELECT member_no FROM member WHERE login_id = 'res120'), '2026-03-20 18:45:00'),
    ('normal', '47너1121', NULL, 'APPROVED', '2026-07-19 13:15:00', '2026-10-19 13:15:00', (SELECT member_no FROM member WHERE login_id = 'res121'), '2026-07-19 14:15:00'),
    ('normal', '54더1122', NULL, 'APPROVED', '2026-05-11 15:45:00', '2026-11-11 15:45:00', (SELECT member_no FROM member WHERE login_id = 'res122'), '2026-05-11 16:45:00'),
    ('normal', '61러1123', NULL, 'APPROVED', '2026-02-13 13:15:00', '2026-08-13 13:15:00', (SELECT member_no FROM member WHERE login_id = 'res123'), '2026-02-13 14:15:00'),
    ('normal', '68머1124', NULL, 'APPROVED', '2026-06-06 11:30:00', '2026-09-06 11:30:00', (SELECT member_no FROM member WHERE login_id = 'res124'), '2026-06-06 12:30:00'),
    ('normal', '75버1125', NULL, 'APPROVED', '2026-06-25 16:45:00', '2026-09-25 16:45:00', (SELECT member_no FROM member WHERE login_id = 'res125'), '2026-06-25 17:45:00'),
    ('normal', '82서1126', NULL, 'APPROVED', '2026-05-06 09:30:00', '2026-08-06 09:30:00', (SELECT member_no FROM member WHERE login_id = 'res126'), '2026-05-06 10:30:00'),
    ('normal', '89어1127', NULL, 'APPROVED', '2026-06-25 11:45:00', '2026-09-25 11:45:00', (SELECT member_no FROM member WHERE login_id = 'res127'), '2026-06-25 12:45:00'),
    ('normal', '96저1128', NULL, 'APPROVED', '2026-05-21 12:00:00', '2026-08-21 12:00:00', (SELECT member_no FROM member WHERE login_id = 'res128'), '2026-05-21 13:00:00'),
    ('normal', '13고1129', NULL, 'APPROVED', '2026-03-25 08:30:00', '2026-09-25 08:30:00', (SELECT member_no FROM member WHERE login_id = 'res129'), '2026-03-25 09:30:00'),
    ('normal', '20노1130', NULL, 'APPROVED', '2026-06-09 14:45:00', '2026-09-09 14:45:00', (SELECT member_no FROM member WHERE login_id = 'res130'), '2026-06-09 15:45:00'),
    ('normal', '27도1131', NULL, 'APPROVED', '2026-07-06 10:15:00', '2026-08-06 10:15:00', (SELECT member_no FROM member WHERE login_id = 'res131'), '2026-07-06 11:15:00'),
    ('normal', '34로1132', NULL, 'APPROVED', '2026-06-27 11:30:00', '2026-12-27 11:30:00', (SELECT member_no FROM member WHERE login_id = 'res132'), '2026-06-27 12:30:00'),
    ('normal', '41모1133', NULL, 'APPROVED', '2026-03-03 12:45:00', '2026-09-03 12:45:00', (SELECT member_no FROM member WHERE login_id = 'res133'), '2026-03-03 13:45:00'),
    ('normal', '48보1134', NULL, 'APPROVED', '2026-07-18 11:15:00', '2026-08-18 11:15:00', (SELECT member_no FROM member WHERE login_id = 'res134'), '2026-07-18 12:15:00'),
    ('normal', '55소1135', NULL, 'APPROVED', '2026-03-18 10:45:00', '2026-09-18 10:45:00', (SELECT member_no FROM member WHERE login_id = 'res135'), '2026-03-18 11:45:00'),
    ('normal', '62오1136', NULL, 'APPROVED', '2026-05-03 15:00:00', '2026-11-03 15:00:00', (SELECT member_no FROM member WHERE login_id = 'res136'), '2026-05-03 16:00:00'),
    ('normal', '69조1137', NULL, 'APPROVED', '2026-04-04 08:30:00', '2027-04-04 08:30:00', (SELECT member_no FROM member WHERE login_id = 'res137'), '2026-04-04 09:30:00'),
    ('normal', '76구1138', NULL, 'APPROVED', '2026-06-05 17:45:00', '2026-09-05 17:45:00', (SELECT member_no FROM member WHERE login_id = 'res138'), '2026-06-05 18:45:00'),
    ('normal', '83누1139', NULL, 'APPROVED', '2026-04-16 10:00:00', '2026-10-16 10:00:00', (SELECT member_no FROM member WHERE login_id = 'res139'), '2026-04-16 11:00:00'),
    ('normal', '90두1140', NULL, 'APPROVED', '2026-05-21 13:30:00', '2026-08-21 13:30:00', (SELECT member_no FROM member WHERE login_id = 'res140'), '2026-05-21 14:30:00'),
    ('normal', '97루1141', NULL, 'APPROVED', '2026-05-24 15:15:00', '2026-08-24 15:15:00', (SELECT member_no FROM member WHERE login_id = 'res141'), '2026-05-24 16:15:00'),
    ('normal', '14무1142', NULL, 'APPROVED', '2026-06-19 14:45:00', '2026-12-19 14:45:00', (SELECT member_no FROM member WHERE login_id = 'res142'), '2026-06-19 15:45:00'),
    ('normal', '21부1143', NULL, 'APPROVED', '2026-06-02 16:30:00', '2027-06-02 16:30:00', (SELECT member_no FROM member WHERE login_id = 'res143'), '2026-06-02 17:30:00'),
    ('normal', '28수1144', NULL, 'APPROVED', '2026-06-21 11:00:00', '2027-06-21 11:00:00', (SELECT member_no FROM member WHERE login_id = 'res144'), '2026-06-21 12:00:00'),
    ('normal', '35우1145', NULL, 'APPROVED', '2025-11-19 17:45:00', '2026-11-19 17:45:00', (SELECT member_no FROM member WHERE login_id = 'res145'), '2025-11-19 18:45:00'),
    ('normal', '42주1146', NULL, 'APPROVED', '2026-06-26 10:30:00', '2026-09-26 10:30:00', (SELECT member_no FROM member WHERE login_id = 'res146'), '2026-06-26 11:30:00'),
    ('normal', '49하1147', NULL, 'APPROVED', '2026-06-20 15:15:00', '2026-12-20 15:15:00', (SELECT member_no FROM member WHERE login_id = 'res147'), '2026-06-20 16:15:00'),
    ('normal', '56허1148', NULL, 'APPROVED', '2026-05-14 16:00:00', '2026-08-14 16:00:00', (SELECT member_no FROM member WHERE login_id = 'res148'), '2026-05-14 17:00:00'),
    ('normal', '63호1149', NULL, 'APPROVED', '2026-07-02 09:45:00', '2026-10-02 09:45:00', (SELECT member_no FROM member WHERE login_id = 'res149'), '2026-07-02 10:45:00'),
    ('normal', '70가1150', NULL, 'APPROVED', '2026-07-05 12:45:00', '2026-08-05 12:45:00', (SELECT member_no FROM member WHERE login_id = 'res150'), '2026-07-05 13:45:00'),
    ('normal', '77나1151', NULL, 'APPROVED', '2026-07-09 10:15:00', '2026-10-09 10:15:00', (SELECT member_no FROM member WHERE login_id = 'res151'), '2026-07-09 11:15:00'),
    ('normal', '84다1152', NULL, 'APPROVED', '2026-05-21 17:45:00', '2026-08-21 17:45:00', (SELECT member_no FROM member WHERE login_id = 'res152'), '2026-05-21 18:45:00');

-- 검증: 각 등록기간이 골고루 들어갔는지 확인
SELECT
    EXTRACT(YEAR FROM AGE(end_date, start_date)) * 12
    + EXTRACT(MONTH FROM AGE(end_date, start_date)) AS period_months,
    COUNT(*)
FROM vehicle_car
WHERE vehicle_type = 'normal'
GROUP BY period_months
ORDER BY period_months;

-- 검증: 등록차량 수와 차량번호 중복 확인
SELECT COUNT(*) AS normal_vehicle_count FROM vehicle_car WHERE vehicle_type = 'normal';
SELECT car_no, COUNT(*) FROM vehicle_car GROUP BY car_no HAVING COUNT(*) > 1;



-- 입주민 방문차량 시연 데이터
-- EXPIRED : res1 ~ res30의 과거 만료 이력 30건
-- WAITING : res1 ~ res10, res31 ~ res40의 승인 대기 20건
-- APPROVED: res11 ~ res20, res41 ~ res50의 승인 완료 20건
--
-- 같은 입주민에게 EXPIRED 이력과 현재 차량이 함께 있을 수 있지만,
-- WAITING/APPROVED 상태의 유효한 방문차량은 회원당 최대 1대만 생성한다.

WITH visit_source AS (
    -- 과거에 승인받았지만 현재는 만료된 방문차량
    SELECT
        n AS member_seq,
        '71거' || LPAD((1000 + n)::TEXT, 4, '0') AS car_no,
        'EXPIRED'::VARCHAR AS vehicle_status,
        date_trunc('hour', LOCALTIMESTAMP)
            - INTERVAL '60 days'
            + n * INTERVAL '1 day'
            + (n % 8) * INTERVAL '1 hour' AS start_date,
        (ARRAY[2, 4, 6, 8, 12])[((n - 1) % 5) + 1] AS duration_hours,
        NULL::TIMESTAMP AS approved_at
    FROM generate_series(1, 30) AS n

    UNION ALL

    -- 아직 관리자의 승인을 기다리는 방문차량
    SELECT
        CASE WHEN n <= 10 THEN n ELSE n + 20 END AS member_seq,
        '72너' || LPAD((2000 + n)::TEXT, 4, '0') AS car_no,
        'WAITING'::VARCHAR AS vehicle_status,
        date_trunc('hour', LOCALTIMESTAMP)
            + INTERVAL '1 day'
            + (n % 7) * INTERVAL '1 day'
            + (n % 8) * INTERVAL '1 hour' AS start_date,
        (ARRAY[2, 4, 6, 8, 12])[((n - 1) % 5) + 1] AS duration_hours,
        NULL::TIMESTAMP AS approved_at
    FROM generate_series(1, 20) AS n

    UNION ALL

    -- 관리자가 승인했으며 아직 입차하지 않은 방문차량
    SELECT
        CASE WHEN n <= 10 THEN n + 10 ELSE n + 30 END AS member_seq,
        '73더' || LPAD((3000 + n)::TEXT, 4, '0') AS car_no,
        'APPROVED'::VARCHAR AS vehicle_status,
        date_trunc('hour', LOCALTIMESTAMP)
            + INTERVAL '2 hours'
            + (n % 5) * INTERVAL '1 day'
            + (n % 6) * INTERVAL '1 hour' AS start_date,
        (ARRAY[2, 4, 6, 8, 12])[((n - 1) % 5) + 1] AS duration_hours,
        LOCALTIMESTAMP - n * INTERVAL '15 minutes' AS approved_at
    FROM generate_series(1, 20) AS n
),
visit_rows AS (
    SELECT
        source.car_no,
        source.vehicle_status,
        source.start_date,
        source.start_date
            + source.duration_hours * INTERVAL '1 hour' AS end_date,
        member.member_no,
        CASE
            WHEN source.vehicle_status = 'EXPIRED'
            THEN source.start_date - INTERVAL '12 hours'
            ELSE source.approved_at
        END AS approved_at
    FROM visit_source source
    JOIN member
        ON member.login_id = 'res' || source.member_seq
)
INSERT INTO vehicle_car (
    vehicle_type,
    car_no,
    alias_car_no,
    vehicle_status,
    start_date,
    end_date,
    member_no,
    approved_at
)
SELECT
    'visit',
    visit_rows.car_no,
    NULL,
    visit_rows.vehicle_status,
    visit_rows.start_date,
    visit_rows.end_date,
    visit_rows.member_no,
    visit_rows.approved_at
FROM visit_rows
WHERE NOT EXISTS (
    SELECT 1
    FROM vehicle_car existing
    WHERE existing.car_no = visit_rows.car_no
);


-- 삽입 결과 확인
SELECT
    vehicle_type,
    vehicle_status,
    COUNT(*) AS vehicle_count
FROM vehicle_car
GROUP BY vehicle_type, vehicle_status
ORDER BY vehicle_type, vehicle_status;


-- =====================================================
-- 6. 카메라 데이터
-- =====================================================

-- =====================================================
-- 7. 입출차 로그
-- =====================================================

-- =====================================================
-- 8. 차량 알림
-- =====================================================

-- =====================================================
-- 9. 장기 주차 / 미등록 알림
-- =====================================================

-- =====================================================
-- 10. 휴지통
-- =====================================================

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
