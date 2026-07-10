package api.member_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 회원가입
    @Insert("insert into member(login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone, role, mem_status) values" +
            "(#{loginId},#{loginPwd},#{memDong}, #{memHo},#{memName},#{memPhone},#{role},#{memStatus})")
    int signup (MemberDTO dto);

    // 회원목록
    @Select("select * from member")
    List<MemberDTO> list();

    // 회원상세
    @Select("select * from member where member_no = #{memberNo}")
    MemberDTO detail(int memberNo);

    // 회원검색
    @Select("""
        select *
        from member
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
    @Select("select * from member where mem_name like concat('%', #{keyword}, '%')")
    List<MemberDTO> searchByName(String keyword);

    @Select("select * from member where role like concat('%', #{keyword}, '%')")
    List<MemberDTO> searchByRole(String keyword);

    @Select("select * from member where mem_dong = #{dong} and mem_ho = #{ho}")
    List<MemberDTO> searchByDongHo(@Param("dong") Integer dong, @Param("ho") Integer ho);

    @Update("UPDATE member SET role = #{role}, mem_name = #{memName}, mem_dong = #{memDong}, mem_ho = #{memHo}, mem_phone = #{memPhone}, login_id = #{loginId}, login_pwd = #{loginPwd}, mem_status = #{memStatus} WHERE member_no = #{memberNo}")
    void update(MemberDTO dto);

    @Delete(" Delete FROM member WHERE member_no = #{memberNo}")
    void delete(int memberNo);

    // 입주민 마이페이지
    @Select("SELECT * FROM member WHERE login_id = #{loginId}")
    MemberDTO residentMypage(String loginId);

    // 입주민이 마이페이지 직접 수정
    @Update("UPDATE member SET role = #{role}, mem_name = #{memName}, mem_dong = #{memDong}, mem_ho = #{memHo}, mem_phone = #{memPhone}, login_id = #{loginId}, login_pwd = #{loginPwd}, mem_status = #{memStatus} WHERE login_id = #{loginId}")
    void residentMypageEdit(MemberDTO dto);

    // 입주민이 직접 회원 탈퇴
    @Delete("DELETE FROM member WHERE login_id = #{loginId}")
    int residentDelete(String loginId);

    // 아이디 중복확인
    @Select("SELECT EXISTS (SELECT 1 FROM member WHERE login_id = #{loginId})" )
    boolean checkLoginId(String LoginId);


}


