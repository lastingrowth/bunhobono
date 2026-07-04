package api.a_filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;

public class Jwtfilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public Jwtfilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("🔥 JwtFilter ENTER");
        System.out.println("👉 URI: " + request.getRequestURI());
        System.out.println("👉 Method: " + request.getMethod());

        // 로그인 화면은 JWT 검사 제외
        String uri = request.getRequestURI();

        if (uri.startsWith("/api/login") || uri.startsWith("/api/joinus")) {
            System.out.println("👉 LOGIN 요청 감지 → JWT 검사 스킵");
            filterChain.doFilter(request,response);
            return;
        }

        String auth = request.getHeader("Authorization");

        System.out.println("JwtFilter : " + auth);

        System.out.println("👉 Authorization: " + auth);

        if (auth == null) {
            System.out.println("⚠️ No token");
        }

        if (auth != null && auth.startsWith("Bearer ")) {

                String token = auth.substring(7);

                String loginId = jwtUtil.getLoginId(token);
                String role = jwtUtil.getRole(token);
                String memStatus = jwtUtil.getMemStatus(token);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role),
                        new SimpleGrantedAuthority(memStatus)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                loginId,
                                null,
                                authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);


        }

        filterChain.doFilter(request, response);

    }
}
