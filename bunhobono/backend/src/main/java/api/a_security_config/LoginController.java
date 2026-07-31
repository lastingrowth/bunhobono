package api.a_security_config;

import api.a_filter.JwtUtil;
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



    @PostMapping("/api/login")
    public Object login(@RequestBody LoginDTO dto) {
        System.out.println("로그인 진입: loginId=" + dto.getLoginId());

        System.out.println("DTO ID = " + dto.getLoginId());
        System.out.println("DTO PWD = " + dto.getLoginPwd());

        // DB에서 유저 정보 조회
        LoginDTO userInfo = authService.getUserInfo(dto.getLoginId());

        // 회원 존재 여부
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        System.out.println("DB ROLE = " + userInfo.getRole());

        // BCrypt 검증
        if (!passwordEncoder.matches(dto.getLoginPwd(), userInfo.getLoginPwd())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        // 계정 상태 확인
        if (!"ACTIVE".equalsIgnoreCase(userInfo.getMemStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "현재 로그인할 수 없는 계정입니다."));
        }

        // DB에서 가져온 role, grade를 토큰에 넣음
        String token = jwtUtil.createToken(
                userInfo.getLoginId(),
                userInfo.getRole(),
                userInfo.getMemStatus()
        );

        System.out.println("LOGIN SUCCESS");
        System.out.println("userId = " + userInfo.getLoginId());
        System.out.println("role = " + userInfo.getRole());
        System.out.println("memStatus = " + userInfo.getMemStatus());
        System.out.println("token = " + token);

        return Map.of("token", token, "userId", userInfo.getLoginId());
    }

}
