package apt.a_security_config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("1234"));
        System.out.println(encoder.matches("1234", "$2a$10$82G8Glt74PLbFhY36ukoVuTTGuawbcFYEbPHo0ayztFIIJpQtmlji"));
        String hash1 = encoder.encode("1234");

        System.out.println(hash1); // 매번 다른 값

        System.out.println(encoder.matches("1234", "$2a$10$emgBloBpfGdgMI5o4iTi0OfDM65EOT0pSenIMjOTTTNBy3tVUArRi"));

        System.out.println(encoder.matches("1234", hash1)); // true

    }
}
