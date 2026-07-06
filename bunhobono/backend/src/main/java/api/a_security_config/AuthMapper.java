package api.a_security_config;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface                                                                                                                                                       AuthMapper {

    @Select("SELECT member_no, login_id, login_pwd, role, mem_status FROM member WHERE login_id = #{loginId}")
    LoginDTO findByLoginId(String loginId);

}