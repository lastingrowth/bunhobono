package api.a_filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtfilterTest {

    private JwtUtil jwtUtil;
    private FilterChain filterChain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtUtil = mock(JwtUtil.class);
        filterChain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @CsvSource({
            "POST, /api/login",
            "POST, /api/joinus",
            "POST, /api/billing/pay",
            "POST, /api/robot-tasks/park-out"
    })
    @DisplayName(
            "UT-BE-AFILTER-003 | 공개 API 요청은 JWT 검사를 건너뛴다"
    )
    void publicRequest_skipsJwtParsing(
            String method,
            String uri
    ) throws Exception {
        // Arrange
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, uri);

        // Act
        new Jwtfilter(jwtUtil).doFilter(
                request,
                response,
                filterChain
        );

        // Assert
        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(jwtUtil);
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @Test
    @DisplayName(
            "UT-BE-AFILTER-004 | 정상 Bearer JWT로 인증 정보를 생성한다"
    )
    void bearerToken_createsAuthentication() throws Exception {
        // Arrange
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/members");
        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtUtil.getLoginId("valid-token"))
                .thenReturn("resident01");
        when(jwtUtil.getRole("valid-token"))
                .thenReturn("RESIDENT");
        when(jwtUtil.getMemStatus("valid-token"))
                .thenReturn("ACTIVE");

        // Act
        new Jwtfilter(jwtUtil).doFilter(
                request,
                response,
                filterChain
        );

        // Assert
        var authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        assertNotNull(authentication);
        assertEquals("resident01", authentication.getName());
        assertEquals(
                List.of("RESIDENT", "ACTIVE"),
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        );
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName(
            "UT-BE-AFILTER-005 | 토큰이 없어도 인증 없이 필터 체인을 계속한다"
    )
    void missingToken_continuesWithoutAuthentication() throws Exception {
        // Arrange
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/boards");

        // Act
        new Jwtfilter(jwtUtil).doFilter(
                request,
                response,
                filterChain
        );

        // Assert
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );
        verifyNoInteractions(jwtUtil);
        verify(filterChain).doFilter(any(), any());
    }
}
