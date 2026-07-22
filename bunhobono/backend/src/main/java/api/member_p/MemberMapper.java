package api.member_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {

    // =====================================================
    // 1. 회원가입 및 가입 가능한 세대 확인
    // =====================================================

    // 입주민 회원가입은 전출 확정 후 비어 있는 세대(EMPTY)에만 저장한다.
    @Update("""
    UPDATE member
    SET login_id = #{loginId},
        login_pwd = #{loginPwd},
        mem_name = #{memName},
        mem_phone = #{memPhone},
        role = #{role},
        mem_status = #{memStatus},
        create_at = CURRENT_TIMESTAMP,
        delete_at = NULL
    WHERE mem_dong = #{memDong}
      AND mem_ho = #{memHo}
      AND UPPER(TRIM(role)) = 'RESIDENT'
      AND TRIM(mem_status) = 'EMPTY'
""")
    int signup(MemberDTO dto);

    // 관리자가 추가한 ADMIN 계정은 기존 세대 행을 사용하지 않고 새 행으로 등록한다.
    @Insert("""
        INSERT INTO member (
            login_id,
            login_pwd,
            mem_dong,
            mem_ho,
            mem_name,
            mem_phone,
            role,
            create_at,
            delete_at,
            mem_status
        ) VALUES (
            #{loginId},
            #{loginPwd},
            #{memDong},
            #{memHo},
            #{memName},
            #{memPhone},
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

    // 전출 확정 후 비어 있고, 현재 사용 중이거나 전출 신청 중인 회원이 없는 세대만 가입 대상으로 조회한다.
    @Select("""
        SELECT DISTINCT empty_unit.mem_dong, empty_unit.mem_ho
        FROM member empty_unit
        WHERE UPPER(TRIM(empty_unit.role)) = 'RESIDENT'
          AND TRIM(empty_unit.mem_status) = 'EMPTY'
          AND empty_unit.mem_dong IN (101, 102, 201, 202, 301, 302, 401, 402)
          AND (empty_unit.mem_ho / 100) BETWEEN 1 AND 10
          AND MOD(empty_unit.mem_ho, 100) BETWEEN 1 AND 2
          AND NOT EXISTS (
              SELECT 1
              FROM member active
              WHERE active.mem_dong = empty_unit.mem_dong
                AND active.mem_ho = empty_unit.mem_ho
                AND UPPER(TRIM(active.role)) IN ('PENDING', 'RESIDENT')
                AND TRIM(active.mem_status) IN ('ACTIVE', 'WITHDRAW_PENDING')
          )
        ORDER BY empty_unit.mem_dong, empty_unit.mem_ho
        """)
    List<MemberDTO> availableSignupUnits();

    // 가입 처리 중 같은 빈 세대 행을 잠가 동시 중복 신청을 방지한다.
    @Select("""
        SELECT member_no
        FROM member
        WHERE mem_dong = #{dong}
          AND mem_ho = #{ho}
          AND UPPER(TRIM(role)) = 'RESIDENT'
          AND TRIM(mem_status) = 'EMPTY'
        ORDER BY member_no DESC
        LIMIT 1
        FOR UPDATE
        """)
    Long lockWithdrawnUnit(@Param("dong") int dong, @Param("ho") int ho);

    // 동일 동·호수에 가입 승인 대기, 현재 회원, 전출 신청 회원이 있는지 확인한다.
    @Select("""
        SELECT COUNT(*)
        FROM member
        WHERE mem_dong = #{dong}
          AND mem_ho = #{ho}
          AND UPPER(TRIM(role)) IN ('PENDING', 'RESIDENT')
          AND TRIM(mem_status) IN ('ACTIVE', 'WITHDRAW_PENDING')
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
        m.create_at mem_create_at,
        m.delete_at mem_delete_at,
        EXISTS (
            SELECT 1
            FROM member_archive ma
            WHERE ma.original_member_no = m.member_no
              AND ma.archived_at > m.create_at
        ) archived
    FROM member m
    ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC
    """)
    List<MemberDTO> list();
    // 회원번호에 해당하는 상세 정보를 조회한다.
    @Select("SELECT m.*, " +
            "m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m WHERE member_no = #{memberNo}")
    MemberDTO detail(int memberNo);

    // 역할, 이름 또는 동·호수 조건에 맞는 회원을 조회한다.
    @Select("""
        SELECT ROW_NUMBER() OVER (ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC) AS display_no,
               m.*,
               m.create_at AS mem_create_at,
               m.delete_at AS mem_delete_at
        from member m
        where
            (
                #{type} = 'role'
                and role like concat('%', #{keyword}, '%')
            )
            or
            (
                #{type} = 'name'
                and mem_name like concat('%', #{keyword}, '%')
            )
            or
            (
                #{type} = 'dongHo'
                and mem_dong = #{dong}
                and mem_ho = #{ho}
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

    // 선택한 승인 대기 회원의 역할을 입주민으로 변경한다.
    @Update("""
        <script>
        UPDATE member
        SET role = 'RESIDENT'
        WHERE UPPER(TRIM(role)) = 'PENDING'
          AND member_no IN
        <foreach collection="memberNos" item="memberNo" open="(" separator="," close=")">
            #{memberNo}
        </foreach>
        </script>
        """)
    int approvePendingMembers(@Param("memberNos") List<Long> memberNos);

    // 전출 확정 전에 현재 회원 정보를 member_archive 이력으로 복사한다.
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
            member_no,
            login_id,
            mem_name,
            mem_phone,
            role,
            mem_status,
            mem_dong,
            mem_ho,
            create_at,
            CURRENT_TIMESTAMP
        FROM member
        WHERE member_no = #{memberNo}
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

    // 전출 확정 후 동·호수 자리를 유지하면서 원본 회원 정보를 빈 세대 상태로 초기화한다.
    @Update("""
        UPDATE member
        SET login_id = CONCAT('unit_', mem_dong, '_', mem_ho),
            login_pwd = 'EMPTY',
            mem_name = '미등록',
            mem_phone = '미등록',
            role = 'RESIDENT',
            mem_status = 'EMPTY',
            delete_at = CURRENT_TIMESTAMP
        WHERE member_no = #{memberNo}
        """)
    int delete(@Param("memberNo") int memberNo);


    // =====================================================
    // 3. 입주민 마이페이지·차량·입출차 조회
    // =====================================================

    // 로그인 아이디에 해당하는 입주민 본인 정보를 조회한다.
    @Select("SELECT m.*, " +
            "m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m WHERE login_id = #{loginId}")
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

