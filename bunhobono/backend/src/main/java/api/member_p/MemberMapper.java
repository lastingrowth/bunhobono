package api.member_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 회원가입
    // 외부 회원가입은 PENDING 역할로 저장하고 가입일은 실제 가입 시각을 기록한다.
    @Update("""
    UPDATE member
    SET login_id = #{loginId},
        login_pwd = #{loginPwd},
        mem_name = #{memName},
        mem_phone = #{memPhone},
        role = 'PENDING',
        mem_status = '거주',
        create_at = CURRENT_TIMESTAMP,
        delete_at = NULL
    WHERE mem_dong = #{memDong}
      AND mem_ho = #{memHo}
      AND UPPER(TRIM(role)) = 'RESIDENT'
      AND TRIM(mem_status) = '전출'
""")
    int signup(MemberDTO dto);

    // 전출 이력이 있고 현재 거주·승인대기 회원이 없는 세대만 공개 회원가입 대상으로 조회한다.
    @Select("""
        SELECT DISTINCT departed.mem_dong, departed.mem_ho
        FROM member departed
        WHERE UPPER(TRIM(departed.role)) = 'RESIDENT'
          AND TRIM(departed.mem_status) = '전출'
          AND departed.mem_dong IN (101, 102, 201, 202, 301, 302, 401, 402)
          AND (departed.mem_ho / 100) BETWEEN 1 AND 15
          AND MOD(departed.mem_ho, 100) BETWEEN 1 AND 4
          AND NOT EXISTS (
              SELECT 1
              FROM member active
              WHERE active.mem_dong = departed.mem_dong
                AND active.mem_ho = departed.mem_ho
                AND UPPER(TRIM(active.role)) IN ('PENDING', 'RESIDENT')
                AND TRIM(active.mem_status) <> '전출'
                AND active.delete_at IS NULL
          )
        ORDER BY departed.mem_dong, departed.mem_ho
        """)
    List<MemberDTO> availableSignupUnits();

    // 가입 처리 중 같은 전출 세대 행을 잠가 동시 중복 신청을 방지한다.
    @Select("""
        SELECT member_no
        FROM member
        WHERE mem_dong = #{dong}
          AND mem_ho = #{ho}
          AND UPPER(TRIM(role)) = 'RESIDENT'
          AND TRIM(mem_status) = '전출'
        ORDER BY delete_at DESC NULLS LAST, member_no DESC
        LIMIT 1
        FOR UPDATE
        """)
    Long lockWithdrawnUnit(@Param("dong") int dong, @Param("ho") int ho);

    @Select("""
        SELECT COUNT(*)
        FROM member
        WHERE mem_dong = #{dong}
          AND mem_ho = #{ho}
          AND UPPER(TRIM(role)) IN ('PENDING', 'RESIDENT')
          AND TRIM(mem_status) <> '전출'
          AND delete_at IS NULL
        """)
    int countActiveMembersAtUnit(@Param("dong") int dong, @Param("ho") int ho);

    // 회원목록
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC) AS display_no, " +
            "m.*, m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m ORDER BY m.create_at DESC NULLS LAST, m.member_no DESC")
    List<MemberDTO> list();

    // 회원상세
    @Select("SELECT m.*, " +
            "m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m WHERE member_no = #{memberNo}")
    MemberDTO detail(int memberNo);

    // 회원검색
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

    // 관리자는 연락처, 비밀번호, 회원 상태만 수정하며 탈퇴일 생성을 유지한다.
    @Update("""
        UPDATE member
        SET mem_phone = #{memPhone},
            login_pwd = #{loginPwd},
            mem_status = #{memStatus},
            delete_at = CASE
                WHEN delete_at IS NULL
                    AND ((UPPER(TRIM(role)) = 'RESIDENT' AND TRIM(#{memStatus}) = '전출')
                        OR (UPPER(TRIM(role)) = 'ADMIN' AND TRIM(#{memStatus}) = '퇴사'))
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

    // 탈퇴 처리된 선택 회원을 영구 삭제한다.
    @Delete("""
        <script>
        DELETE FROM member
        WHERE delete_at IS NOT NULL
          AND member_no IN
        <foreach collection="memberNos" item="memberNo" open="(" separator="," close=")">
            #{memberNo}
        </foreach>
        </script>
        """)
    int permanentlyDeleteWithdrawnMembers(@Param("memberNos") List<Long> memberNos);

    // 선택한 탈퇴 회원을 거주 상태로 복원하고 탈퇴일을 제거한다.
    @Update("""
        <script>
        UPDATE member
        SET mem_status = '거주',
            delete_at = NULL
        WHERE delete_at IS NOT NULL
          AND member_no IN
        <foreach collection="memberNos" item="memberNo" open="(" separator="," close=")">
            #{memberNo}
        </foreach>
        </script>
        """)
    int restoreMembers(@Param("memberNos") List<Long> memberNos);

    // 관리자 화면의 회원 삭제는 즉시 제거하지 않고 탈퇴 상태와 최초 탈퇴일을 기록한다.
    @Update("""
    UPDATE member
    SET login_id = CONCAT('unit_', mem_dong, '_', mem_ho),
        login_pwd = 'EMPTY',
        mem_name = '미등록',
        mem_phone = '미등록',
        role = 'RESIDENT',
        mem_status = '전출',
        delete_at = CURRENT_TIMESTAMP
    WHERE member_no = #{memberNo}
""")
    void delete(int memberNo);

    // 입주민 마이페이지
    @Select("SELECT m.*, " +
            "m.create_at AS mem_create_at, m.delete_at AS mem_delete_at " +
            "FROM member m WHERE login_id = #{loginId}")
    MemberDTO residentMypage(String loginId);

    // 로그인 회원과 vehicle_car.member_no를 조인해 해당 입주민의 차량만 조회한다.
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

    // 새 DB의 vehicle_car.member_no를 기준으로 로그인 입주민 차량의 최근 입출차 기록을 조회한다.
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
        LIMIT 5
        """)
    List<MemberDTO.ResidentCarLog> residentCarLogs(String loginId);

    // 입주민은 마이페이지에서 연락처와 새 비밀번호만 변경할 수 있다.
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

    // 입주민이 직접 탈퇴하면 전출 상태와 최초 탈퇴 시각을 기록한다.
    @Update("""
        UPDATE member
        SET mem_status = '전출',
            delete_at = COALESCE(delete_at, CURRENT_TIMESTAMP)
        WHERE login_id = #{loginId}
          AND UPPER(TRIM(role)) = 'RESIDENT'
        """)
    int residentDelete(String loginId);

    // 아이디 중복확인
    @Select("SELECT EXISTS (SELECT 1 FROM member WHERE login_id = #{loginId})" )
    boolean checkLoginId(String LoginId);

}

