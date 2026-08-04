package api.a_security_config;

import lombok.Data;

@Data
public class LoginDTO {
    private long memberNo;
    private String loginId,loginPwd,role,memStatus;
}
