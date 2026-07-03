package apt.a_security_config;

import lombok.Data;

@Data
public class LoginDTO {
    int memberNo;
    String loginId,loginPwd,role,memStatus;
}
