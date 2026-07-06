package api.member_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 회원가입
    @Insert("insert into member(login_id, login_pwd, mem_dong, mem_ho, mem_name, mem_phone, role, mem_status) values" +
            "(#{loginId},#{loginPwd},#{memDong}, #{memHo},#{memName},#{memPhone},#{role},#{memStatus})")
    int signup (MemberDTO dto);

    // 로그인
    @Select("select * from member where (login_id, login_pwd) = (#{loginId}, #{loginPwd})")
    MemberDTO login (MemberDTO dto);

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

    @Update("UPDATE member SET role = #{role}, mem_name = #{memName}, mem_dong = #{memDong}, mem_ho = #{memHo}, mem_phone = #{memPhone}, login_id = #{loginId}, mem_status = #{memStatus} WHERE member_no = #{memberNo}")
    void update(MemberDTO dto);

    @Delete(" Delete FROM member WHERE member_no = #{memberNo}")
    void delete(int memberNo);


}


