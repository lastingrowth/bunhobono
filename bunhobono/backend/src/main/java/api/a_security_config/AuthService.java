package api.a_security_config;



import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;



@Service
public class AuthService {

    @Resource
    AuthMapper authMapper;

    public LoginDTO getUserInfo(String loginId) {
        return authMapper.findByLoginId(loginId);
    }

}
