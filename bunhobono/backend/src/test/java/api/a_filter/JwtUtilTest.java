package api.a_filter;

import api.a_security_config.AuthService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        String testSecret =
                "test-secret-key-must-be-at-least-32-bytes-long";

        AuthService authService = mock(AuthService.class);

        jwtUtil = new JwtUtil(testSecret, authService);
    }

    @Test
    @DisplayName(
            "UT-BE-AFILTER-001 | 생성한 JWT에서 사용자 정보를 다시 조회한다"
    )
    void createToken_preservesUserInformation() {
        // Arrange
        String loginId = "admin01";
        String role = "ADMIN";
        String memStatus = "ACTIVE";

        // Act
        String token = jwtUtil.createToken(
                loginId,
                role,
                memStatus
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        loginId,
                        jwtUtil.getLoginId(token)
                ),
                () -> assertEquals(
                        role,
                        jwtUtil.getRole(token)
                ),
                () -> assertEquals(
                        memStatus,
                        jwtUtil.getMemStatus(token)
                )
        );
    }

    @Test
    @DisplayName(
            "UT-BE-AFILTER-002 | 다른 비밀키로 서명된 JWT를 거부한다"
    )
    void getClaims_rejectsTokenSignedWithDifferentSecret() {
        // Arrange
        JwtUtil anotherJwtUtil = new JwtUtil(
                "another-test-secret-key-must-be-at-least-32-bytes",
                mock(AuthService.class)
        );
        String token = anotherJwtUtil.createToken(
                "admin01",
                "ADMIN",
                "ACTIVE"
        );

        // Act & Assert
        assertThrows(
                JwtException.class,
                () -> jwtUtil.getLoginId(token)
        );
    }
}
