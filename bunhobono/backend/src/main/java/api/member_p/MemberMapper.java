package api.member_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper {
    // 회원가입
    @Insert("insert into member(mem_login_id, mem_login_pwd, mem_dong, mem_ho, mem_name, mem_phone, mem_role, mem_status) values" +
            "(#{memLoginId},#{memLoginPwd},#{memDong}, #{memHo},#{memName},#{memPhone},#{memRole},#{memStatus})")
    int signup (MemberDTO dto);

    // 로그인
    @Select("select * from member where (mem_login_id, mem_login_pwd) = (#{memLoginId}, #{memLoginPwd})")
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
                and mem_role like concat('%', #{keyword}, '%')
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

    @Select("select * from member where mem_role like concat('%', #{keyword}, '%')")
    List<MemberDTO> searchByRole(String keyword);

    @Select("select * from member where mem_dong = #{dong} and mem_ho = #{ho}")
    List<MemberDTO> searchByDongHo(@Param("dong") Integer dong, @Param("ho") Integer ho);

}


