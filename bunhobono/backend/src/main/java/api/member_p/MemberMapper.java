package apt.member_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

}


