package api.member_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 회원가입
    // 가입 시 생성일은 비워 두고 관리자가 거주·근무 상태로 승인할 때 기록한다.
    @Insert("insert into member(login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone, role, mem_status, create_at) values" +
            "(#{loginId},#{loginPwd},#{memDong}, #{memHo},#{memName},#{memPhone},#{role},#{memStatus},NULL)")
    int signup (MemberDTO dto);

    // 회원목록
    @Select("SELECT m.*, m.create_at AS mem_create_at FROM member m")
    List<MemberDTO> list();

    // 회원상세
    @Select("SELECT m.*, m.create_at AS mem_create_at FROM member m WHERE member_no = #{memberNo}")
    MemberDTO detail(int memberNo);

    // 회원검색
    @Select("""
        select m.*, m.create_at AS mem_create_at
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
        """)
    List<MemberDTO> search(
            @Param("type") String type,
            @Param("keyword") String keyword,
            @Param("dong") Integer dong,
            @Param("ho") Integer ho
    );
    @Select("SELECT m.*, m.create_at AS mem_create_at FROM member m WHERE mem_name LIKE concat('%', #{keyword}, '%')")
    List<MemberDTO> searchByName(String keyword);

    @Select("SELECT m.*, m.create_at AS mem_create_at FROM member m WHERE role LIKE concat('%', #{keyword}, '%')")
    List<MemberDTO> searchByRole(String keyword);

    @Select("SELECT m.*, m.create_at AS mem_create_at FROM member m WHERE mem_dong = #{dong} AND mem_ho = #{ho}")
    List<MemberDTO> searchByDongHo(@Param("dong") Integer dong, @Param("ho") Integer ho);

    // 활성 상태로 최초 승인되는 시점에만 create_at을 저장하고 이후에는 보존한다.
    @Update("""
        UPDATE member
        SET role = #{role},
            mem_name = #{memName},
            mem_dong = #{memDong},
            mem_ho = #{memHo},
            mem_phone = #{memPhone},
            login_id = #{loginId},
            login_pwd = #{loginPwd},
            mem_status = #{memStatus},
            create_at = CASE
                WHEN create_at IS NULL AND #{memStatus} IN ('거주', '근무')
                THEN CURRENT_TIMESTAMP
                ELSE create_at
            END
        WHERE member_no = #{memberNo}
        """)
    void update(MemberDTO dto);

    @Delete("DELETE FROM member WHERE member_no = #{memberNo}")
    void delete(int memberNo);

    // 입주민 마이페이지
    @Select("SELECT m.*, m.create_at AS mem_create_at FROM member m WHERE login_id = #{loginId}")
    MemberDTO residentMypage(String loginId);

    // 입주민이 마이페이지 직접 수정
    // loginPwd가 전달된 경우에만 비밀번호 컬럼을 변경한다.
    @Update("""
        <script>
        UPDATE member
        SET role = #{role},
            mem_name = #{memName},
            mem_dong = #{memDong},
            mem_ho = #{memHo},
            mem_phone = #{memPhone},
            mem_status = #{memStatus}
            <if test="loginPwd != null and loginPwd != ''">
                , login_pwd = #{loginPwd}
            </if>
        WHERE login_id = #{loginId}
        </script>
        """)
    void residentMypageEdit(MemberDTO dto);

    // 입주민이 직접 회원 탈퇴
    @Delete("DELETE FROM member WHERE login_id = #{loginId}")
    int residentDelete(String loginId);

    // 아이디 중복확인
    @Select("SELECT EXISTS (SELECT 1 FROM member WHERE login_id = #{loginId})" )
    boolean checkLoginId(String LoginId);

}


