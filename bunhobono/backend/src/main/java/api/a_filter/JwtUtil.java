package api.a_filter;


import api.a_security_config.AuthService;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; //롬복꺼 갖다쓰면안됨
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final AuthService authService;

    public JwtUtil(@Value("${jwt.secret}") String secret, AuthService authService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.authService = authService;
    }

    public String createToken(String loginId, String role, String memStatus) {
        return Jwts.builder()
                .subject(loginId)
                .claim("loginId", loginId)
                .claim("role", role)
                .claim("memStatus", memStatus)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }


    public String getRole(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .build();

        return parser.parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public String getMemStatus(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .build();
        return parser.parseSignedClaims(token)
                .getPayload()
                .get("memStatus", String.class);
    }

    public String getLoginId(String token) {
        JwtParser parser = Jwts.parser().verifyWith(key).build();
        return parser.parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}
