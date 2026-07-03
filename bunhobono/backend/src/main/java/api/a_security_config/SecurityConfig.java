package apt.a_security_config;


import apt.a_filter.JwtUtil;
import apt.a_filter.Jwtfilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean//비번 암호화
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) {

        http.csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
<<<<<<< HEAD:bunhobono/backend/src/main/java/api/a_security_config/SecurityConfig.java
                                .requestMatchers("/login").permitAll()
                                .requestMatchers("/carlog/**", "/vehicle/**").permitAll()
=======
                                .requestMatchers("/login","/**").permitAll()
                                .requestMatchers("/api/cameras/**").hasRole("ADMIN")   // ADMIN만 접근
                                .requestMatchers("/api/gates/**").hasAnyRole("ADMIN","MANAGER") // ADMIN, MANAGER 접근
                                .requestMatchers("/api/parkings/**").authenticated()   // 로그인만 필요
>>>>>>> origin/KimGwangSu:bunhobono/backend/src/main/java/apt/a_security_config/SecurityConfig.java
                                .anyRequest().authenticated()
                );

        //JWT 필터 등록
        http.addFilterBefore(new Jwtfilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource(){

        CorsConfiguration configuration = new CorsConfiguration();
        //ip와 port 허용
        configuration.addAllowedOrigin("http://localhost:5173");
        //method 허용
        configuration.addAllowedMethod("*");
        //header 허용
        configuration.addAllowedHeader("*");

        //인증정보 : 쿠키, Authorization 허용
        configuration.setAllowCredentials(true);

        //URL 패턴과 매핑
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //매핑에 대한 위의 CORS 정책허용
        source.registerCorsConfiguration("/**",configuration);

        return source;
    }
}
