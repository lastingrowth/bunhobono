package api.member_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {

    // =====================================================
    // 회원가입 및 가입 가능한 세대 확인
    // =====================================================

    // 회원가입
    // 외부 회원가입은 PENDING으로 저장 / 가입일 = 실제 회원가입한 시각.
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

    // 회원가입 전에 입력한 아이디가 이미 사용 중인지 확인한다.
    @Select("SELECT EXISTS (SELECT 1 FROM member WHERE login_id = #{loginId})" )
    boolean checkLoginId(String loginId);

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

    // =====================================================
    // 관리자 회원 목록·상세·검색·수정·승인·삭제
    // =====================================================

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


    @Delete("""
        DELETE FROM vehicle_car
        WHERE member_no = #{memberNo}
        """)
    int deleteVehiclesByMemberNo(@Param("memberNo") int memberNo);

    // 기존 delete는 유지하되 반환형을 int로 변경 가능
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
    int delete(@Param("memberNo") int memberNo);


    // =====================================================
    // 입주민 마이페이지·차량·입출차 조회
    // =====================================================

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
        """)
    List<MemberDTO.ResidentCarLog> residentCarLogs(String loginId);

    // =====================================================
    // 입주민 정보 수정·비밀번호 확인·탈퇴
    // =====================================================

    // 입주민은 마이페이지에서 '연락처'와 '비밀번호'만 변경할 수 있다.
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

    // [pw변경시/회원탈퇴시] 현재 로그인한 입주민의 암호화된 비밀번호를 한번 더 조회.
    @Select("SELECT login_pwd " +
            "FROM member " +
            "WHERE login_id = #{loginId} " +
            "AND UPPER(TRIM(role)) = 'RESIDENT' " +
            "AND delete_at IS NULL")
    String findResPw(String loginId);

    // 현재 비밀번호가 확인된 입주민의 비밀번호를 변경함.
    @Update("UPDATE member " +
            "SET login_pwd = #{encodedPassword} " +
            "WHERE login_id = #{loginId} " +
            "AND UPPER(TRIM(role)) = 'RESIDENT' " +
            "AND delete_at IS NULL")
    int changeResidentPassword(
            @Param("loginId") String loginId,
            @Param("encodedPassword") String encodedPassword
    );

    // 입주민이 직접 회원탈퇴하면 stauts를 '전출'로 변경 '탈퇴 시각'을 기록함.
    @Update("UPDATE member " +
            "SET mem_status = '전출', " +
            "delete_at = COALESCE(delete_at, CURRENT_TIMESTAMP) " +
            "WHERE login_id = #{loginId} " +
            "AND UPPER(TRIM(role)) = 'RESIDENT' " +
            "AND delete_at IS NULL")
    int residentDelete(String loginId);

}

