package api.a_security_config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    @DisplayName(
            "UT-BE-ASECURITY-001 | 로그인 ID에 해당하는 회원 정보를 Mapper에서 조회한다"
    )
    void getUserInfo_returnsMapperResult() {
        // Arrange
        AuthMapper authMapper = mock(AuthMapper.class);
        AuthService authService = new AuthService();
        authService.authMapper = authMapper;

        LoginDTO expected = new LoginDTO();
        expected.setMemberNo(1);
        expected.setLoginId("resident01");
        expected.setRole("RESIDENT");
        expected.setMemStatus("ACTIVE");

        when(authMapper.findByLoginId("resident01"))
                .thenReturn(expected);

        // Act
        LoginDTO actual = authService.getUserInfo("resident01");

        // Assert
        assertSame(expected, actual);
        verify(authMapper).findByLoginId("resident01");
    }
}
