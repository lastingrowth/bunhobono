package api.a_filter;


import io.jsonwebtoken.Claims;
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
    private final long expirationMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMillis = expirationSeconds * 1000L;
    }

    public String createToken(String loginId, String role, String memStatus) {
        return Jwts.builder()
                .subject(loginId)
                .claim("loginId", loginId)
                .claim("role", role)
                .claim("memStatus", memStatus)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }


    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getMemStatus(String token) {
        return parseClaims(token).get("memStatus", String.class);
    }

    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .build();
        return parser.parseSignedClaims(token).getPayload();
    }

}
