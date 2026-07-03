package apt.a_security_config;

import apt.a_filter.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class LoginController {

    @Resource
    AuthService authService;

    @Resource
    PasswordEncoder passwordEncoder;

    @Resource
    JwtUtil jwtUtil;



    @PostMapping("/login")
    public Object login(@RequestBody LoginDTO dto) {
        System.out.println("로그인 진입: loginId=" + dto.getLoginId());

        // DB에서 유저 정보 조회
        LoginDTO userInfo = authService.getUserInfo(dto.getLoginId());


        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        // BCrypt 검증
        if (!passwordEncoder.matches(dto.getLoginPwd(), userInfo.getLoginPwd())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
        // DB에서 가져온 role, grade를 토큰에 넣음
        String token = jwtUtil.createToken(
                userInfo.getLoginId(),
                userInfo.getRole(),
                userInfo.getMemStatus()
        );

        return Map.of("token", token, "userId", userInfo.getLoginId());
    }

}
