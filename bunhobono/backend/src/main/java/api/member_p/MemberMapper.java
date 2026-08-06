package api.member_p;

import api.apartmentunit_p.ApartmentUnitDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {

    // =====================================================
    // 1. 회원가입 및 가입 가능한 세대 확인
    // =====================================================

    // 입주민 회원가입은 비어 있는 세대(EMPTY)에만 등록한다.
    // 선택한 세대를 OCCUPIED로 변경한 뒤 해당 unit_no로 실제 회원을 생성한다.
    // 이미 점유된 세대라면 selected_unit 결과가 없으므로 회원도 생성되지 않는다.
    @Insert("""
        WITH selected_unit AS (
            UPDATE apartment_unit
            SET unit_status = 'OCCUPIED'
            WHERE dong = #{dong}
              AND ho = #{ho}
              AND unit_status = 'EMPTY'
            RETURNING apartment_unit_no
        )
        INSERT INTO member (
            login_id,
            login_pwd,
            unit_no,
            mem_name,
            mem_phone,
            email,
            role,
            create_at,
            delete_at,
            mem_status
        )
        SELECT
            #{loginId},
            #{loginPwd},
            selected_unit.apartment_unit_no,
            #{memName},
            #{memPhone},
            #{email},
            #{role},
            CURRENT_TIMESTAMP,
            NULL,
            #{memStatus}
        FROM selected_unit
    """)
    int signup(MemberDTO dto);

    // 관리자는 아파트 세대에 속하지 않으므로 unit_no를 NULL로 등록한다.
    // 관리실을 나타내는 별도의 0동 0호 데이터는 사용하지 않는다.
    @Insert("""
        INSERT INTO member (
            login_id,
            login_pwd,
            unit_no,
            mem_name,
            mem_phone,
            email,
            role,
            create_at,
            delete_at,
            mem_status
        )
        VALUES (
            #{loginId},
            #{loginPwd},
            NULL,
            #{memName},
            #{memPhone},
            #{email},
            'ADMIN',
            CURRENT_TIMESTAMP,
            NULL,
            #{memStatus}
        )
    """)
    int signupAdmin(MemberDTO dto);

    // 회원가입 전에 입력한 아이디가 이미 사용 중인지 확인한다.
    @Select("SELECT EXISTS (SELECT 1 FROM member WHERE login_id = #{loginId})" )
    boolean checkLoginId(String loginId);

    // 이름 또는 아이디와 선택한 연락수단이 모두 일치하는 회원의 아이디를 조회한다.
    @Select("""
        SELECT m.login_id
        FROM member m
        LEFT JOIN apartment_unit au
          ON au.apartment_unit_no = m.unit_no
        WHERE (
                (#{purpose} = 'FIND_ID'
                    AND m.mem_name = #{memName}
                    AND au.dong = #{dong}
                    AND au.ho = #{ho})
                OR (#{purpose} = 'RESET_PASSWORD' AND m.login_id = #{loginId})
              )
          AND (
                (#{channel} = 'PHONE'
                    AND REGEXP_REPLACE(COALESCE(m.mem_phone, ''), '[^0-9]', '', 'g')
                        = REGEXP_REPLACE(COALESCE(#{contact}, ''), '[^0-9]', '', 'g'))
                OR
                (#{channel} = 'EMAIL'
                    AND LOWER(TRIM(COALESCE(m.email, ''))) = LOWER(TRIM(#{contact})))
              )
          AND m.mem_status NOT IN ('WITHDRAWN', 'REJECTED', 'INACTIVE')
        ORDER BY m.member_no DESC
        LIMIT 1
    """)
    String findRecoveryLoginId(MemberDTO.AccountRecoveryRequest request);

    // [email인증] 새 비밀번호가 기존 비밀번호와 같은지 비교할 암호화 비밀번호를 조회한다.
    @Select("""
        SELECT login_pwd
        FROM member
        WHERE login_id = #{loginId}
          AND mem_status NOT IN ('WITHDRAWN', 'REJECTED', 'INACTIVE')
        ORDER BY member_no DESC
        LIMIT 1
    """)
    String findRecoveryPassword(@Param("loginId") String loginId);

    // 계정 복구 인증이 완료된 회원의 비밀번호를 변경한다.
    @Update("""
        UPDATE member
        SET login_pwd = #{encodedPassword}
        WHERE login_id = #{loginId}
          AND mem_status NOT IN ('WITHDRAWN', 'REJECTED', 'INACTIVE')
    """)
    int updateRecoveredPassword(
            @Param("loginId") String loginId,
            @Param("encodedPassword") String encodedPassword
    );

    // 회원가입 화면에서 선택할 수 있는 빈 세대 정보를 조회한다.
    @Select("""
        SELECT
            apartment_unit_no,
            dong,
            ho,
            unit_status
        FROM apartment_unit
        WHERE unit_status = 'EMPTY'
          AND dong IN (101, 102, 201, 202, 301, 302, 401, 402)
          AND (ho / 100) BETWEEN 1 AND 10
          AND MOD(ho, 100) BETWEEN 1 AND 2
        ORDER BY dong, ho
    """)
    List<ApartmentUnitDTO> availableSignupUnits();

    // 같은 세대에 여러 가입 요청이 동시에 처리되지 않도록 빈 세대 행을 잠근다.
    // 조회 결과가 없으면 해당 세대가 존재하지 않거나 이미 점유된 상태다.
    @Select("""
        SELECT apartment_unit_no
        FROM apartment_unit
        WHERE dong = #{dong}
          AND ho = #{ho}
          AND unit_status = 'EMPTY'
        FOR UPDATE
    """)
    Integer lockWithdrawnUnit(@Param("dong") int dong, @Param("ho") int ho);

    // 해당 동·호를 현재 점유하고 있는 입주민이 있는지 확인한다.
    // 가입 승인 대기, 거주 중, 전출 승인 대기 회원은 모두 세대를 점유한다.
    @Select("""
        SELECT COUNT(*)
        FROM member m
        JOIN apartment_unit au
          ON au.apartment_unit_no = m.unit_no
        WHERE au.dong = #{dong}
          AND au.ho = #{ho}
          AND m.role = 'RESIDENT'
          AND m.mem_status IN (
              'PENDING',
              'ACTIVE',
              'WITHDRAW_PENDING'
          )
    """)
    int countActiveMembersAtUnit(@Param("dong") int dong, @Param("ho") int ho);

    // =====================================================
    // 2. 관리자 회원 목록·상세·검색·수정·승인·전출 관리
    // =====================================================

    // 가입일과 회원번호 역순으로 전체 회원 목록을 조회한다.
    @Select("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC) display_no,
            m.*,
            au.dong,
            au.ho,
            m.create_at mem_create_at,
            m.delete_at mem_delete_at,
            EXISTS (
                SELECT 1
                FROM member_archive ma
                WHERE ma.original_member_no = m.member_no
                  AND ma.archived_at > m.create_at
            ) archived
        FROM member m
        LEFT JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no
        ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC
    """)
    List<MemberDTO> list();

    // 회원번호에 해당하는 상세 정보를 조회한다.
    @Select("SELECT m.*, " +
            "au.dong, au.ho, " +
            "m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m " +
            "LEFT JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no " +
            "WHERE m.member_no = #{memberNo}")
    MemberDTO detail(int memberNo);

    // 역할, 이름 또는 동·호수 조건에 맞는 회원을 조회한다.
    @Select("""
        SELECT ROW_NUMBER() OVER (ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC) AS display_no,
               m.*,
               au.dong,
               au.ho,
               m.create_at AS mem_create_at,
               m.delete_at AS mem_delete_at
        FROM member m
        LEFT JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no
        WHERE
            (
                #{type} = 'role'
                AND m.role LIKE CONCAT('%', #{keyword}, '%')
            )
            OR
            (
                #{type} = 'name'
                AND m.mem_name LIKE CONCAT('%', #{keyword}, '%')
            )
            OR
            (
                #{type} = 'dongHo'
                AND au.dong = #{dong}
                AND au.ho = #{ho}
            )
        ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC
    """)
    List<MemberDTO> search(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("dong") Integer dong,
            @Param("ho") Integer ho
    );

    // 연락처, 비밀번호와 상태를 수정하고 전출 신청·퇴사 전환 시 처리일을 기록한다.
    @Update("""
        UPDATE member
        SET mem_phone = #{memPhone},
            login_pwd = #{loginPwd},
            mem_status = #{memStatus},
            delete_at = CASE
                WHEN delete_at IS NULL
                    AND ((UPPER(TRIM(role)) = 'RESIDENT' AND TRIM(#{memStatus}) = 'WITHDRAW_PENDING')
                        OR (UPPER(TRIM(role)) = 'ADMIN' AND TRIM(#{memStatus}) = 'INACTIVE'))
                THEN CURRENT_TIMESTAMP
                ELSE delete_at
            END
        WHERE member_no = #{memberNo}
        """)
    void update(MemberDTO dto);

    // 선택한 승인 대기 입주민의 상태를 정상 거주 상태로 변경한다.
    @Update("""
        <script>
        UPDATE member
        SET mem_status = 'ACTIVE'
        WHERE role = 'RESIDENT'
          AND mem_status = 'PENDING'
          AND member_no IN
        <foreach collection="memberNos" item="memberNo" open="(" separator="," close=")">
            #{memberNo}
        </foreach>
        </script>
    """)
    int approvePendingMembers(@Param("memberNos") List<Integer> memberNos);

    // 전출 확정 전에 회원 정보와 당시 동·호수를 member_archive에 보관한다.
    @Insert("""
        INSERT INTO member_archive (
            original_member_no,
            login_id,
            mem_name,
            mem_phone,
            role,
            mem_status,
            mem_dong,
            mem_ho,
            create_at,
            delete_at
        )
        SELECT
            m.member_no,
            m.login_id,
            m.mem_name,
            m.mem_phone,
            m.role,
            m.mem_status,
            au.dong,
            au.ho,
            m.create_at,
            CURRENT_TIMESTAMP
        FROM member m
        JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no
        WHERE m.member_no = #{memberNo}
    """)
    int saveMemberArchive(@Param("memberNo") int memberNo);

    // 이력 보관 전인 전출 신청 회원을 다시 현재 회원 상태로 복원한다.
    @Update("""
        UPDATE member
        SET mem_status = 'ACTIVE',
            delete_at = NULL
        WHERE member_no = #{memberNo}
        """)
    int restoreWithdrawnMember(@Param("memberNo") int memberNo);

    // 회원을 전출 신청 상태로 변경하고 신청 시각을 기록한다.
    @Update("""
        UPDATE member
        SET mem_status = 'WITHDRAW_PENDING',
            delete_at = CURRENT_TIMESTAMP
        WHERE member_no = #{memberNo}
        """)
    int requestWithdrawnMember(@Param("memberNo") int memberNo);

    // 전출 확정 회원에게 연결된 등록 차량을 삭제한다.
    @Delete("""
        DELETE FROM vehicle_car
        WHERE member_no = #{memberNo}
        """)
    int deleteVehiclesByMemberNo(@Param("memberNo") int memberNo);

    // 전출 회원은 WITHDRAWN 상태로 보존하고 해당 아파트 세대를 빈 세대로 변경한다.
    @Update("""
        WITH withdrawn_member AS (
            UPDATE member
            SET mem_status = 'WITHDRAWN',
                delete_at = CURRENT_TIMESTAMP
            WHERE member_no = #{memberNo}
              AND role = 'RESIDENT'
              AND mem_status = 'WITHDRAW_PENDING'
            RETURNING unit_no
        )
        UPDATE apartment_unit
        SET unit_status = 'EMPTY'
        WHERE apartment_unit_no IN (
            SELECT unit_no
            FROM withdrawn_member
        )
    """)
    int delete(@Param("memberNo") int memberNo);


    // =====================================================
    // 3. 입주민 마이페이지·차량·입출차 조회
    // =====================================================

    // 로그인 아이디에 해당하는 입주민 정보와 현재 동·호수를 조회한다.
    @Select("SELECT m.*, " +
            "au.dong, au.ho, " +
            "m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m " +
            "JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no " +
            "WHERE m.login_id = #{loginId}")
    MemberDTO residentMypage(String loginId);

    // member_no로 회원과 등록 차량을 연결해 본인 차량과 최신 주차 상태를 조회한다.
    @Select("""
        SELECT
            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.approved_at,
            CASE
                WHEN latest_log.car_log_no IS NULL THEN 'NONE'
                WHEN latest_log.out_time IS NULL THEN 'PARKING'
                ELSE 'OUT'
            END AS parking_state,
            latest_log.parking_name
        FROM member m
        JOIN vehicle_car vc ON vc.member_no = m.member_no
        LEFT JOIN LATERAL (
            SELECT
                cl.car_log_no,
                cl.out_time,
                p.parking_name
            FROM car_log cl
            LEFT JOIN gate ig ON cl.in_gate_no = ig.gate_no
            LEFT JOIN parking p ON ig.parking_no = p.parking_no
            WHERE cl.vehicle_car_no = vc.vehicle_car_no
            ORDER BY cl.in_time DESC
            LIMIT 1
        ) latest_log ON TRUE
        WHERE m.login_id = #{loginId}
        ORDER BY vc.vehicle_car_no
        """)
    List<MemberDTO.ResidentVehicle> residentVehicles(String loginId);

    // 본인 등록 차량의 입출차 기록과 입차 게이트의 주차장명을 최신순으로 조회한다.
    @Select("""
        SELECT
            cl.car_log_no,
            cl.car_log_no,
            cl.in_time,
            cl.out_time,
            vc.car_no,
            p.parking_name,
            CASE WHEN cl.out_time IS NULL THEN 'PARKING' ELSE 'OUT' END AS parking_state
        FROM car_log cl
        JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no
        JOIN member m ON vc.member_no = m.member_no
        LEFT JOIN gate ig ON cl.in_gate_no = ig.gate_no
        LEFT JOIN parking p ON ig.parking_no = p.parking_no
        WHERE m.login_id = #{loginId}
        ORDER BY cl.in_time DESC
        """)
    List<MemberDTO.ResidentCarLog> residentCarLogs(String loginId);

    // =====================================================
    // 4. 입주민 정보 수정·비밀번호 확인·탈퇴
    // =====================================================

    // 입주민의 연락처를 수정하고 비밀번호가 전달된 경우에만 함께 변경한다.
    @Update("""
        <script>
        UPDATE member
        SET mem_phone = #{memPhone}
            <if test="loginPwd != null and loginPwd != ''">
                , login_pwd = #{loginPwd}
            </if>
        WHERE login_id = #{loginId}
        </script>
        """)
    void residentMypageEdit(MemberDTO dto);

    // 비밀번호 변경과 회원탈퇴 검증에 사용할 현재 암호화 비밀번호를 조회한다.
    @Select("SELECT login_pwd " +
            "FROM member " +
            "WHERE login_id = #{loginId} " +
            "AND UPPER(TRIM(role)) = 'RESIDENT' " +
            "AND delete_at IS NULL")
    String findResPw(String loginId);

    // 본인 확인이 끝난 입주민의 비밀번호를 새 암호화 값으로 변경한다.
    @Update("UPDATE member " +
            "SET login_pwd = #{encodedPassword} " +
            "WHERE login_id = #{loginId} " +
            "AND UPPER(TRIM(role)) = 'RESIDENT' " +
            "AND delete_at IS NULL")
    int changeResidentPassword(
            @Param("loginId") String loginId,
            @Param("encodedPassword") String encodedPassword
    );

    // 입주민이 직접 전출 신청하면 관리자 승인 대기 상태로 변경하고 신청 시각을 기록한다.
    @Update("UPDATE member " +
            "SET mem_status = 'WITHDRAW_PENDING', " +
            "delete_at = COALESCE(delete_at, CURRENT_TIMESTAMP) " +
            "WHERE login_id = #{loginId} " +
            "AND UPPER(TRIM(role)) = 'RESIDENT' " +
            "AND delete_at IS NULL")
    int residentDelete(String loginId);

}

