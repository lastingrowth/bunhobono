package apt.a_filter;


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

        String auth = request.getHeader("Authorization");

        System.out.println("JwtFilter : " + auth);

        if (auth != null && auth.startsWith("Bearer")) {

                String token = auth.substring(7);

                String loginId = jwtUtil.getLoginId(token);

                String role = jwtUtil.getRole(token);
                String memStatus = jwtUtil.getMemStatus(token);
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(role),
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
