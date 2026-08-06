package api.a_security_config;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    // ================================
    // 인증 유효시간과 SOLAPI 주소
    // ================================
    private static final int CODE_EXPIRE_MINUTES = 3;
    private static final String SOLAPI_SEND_URL =
            "https://api.solapi.com/messages/v4/send-many/detail";

    // ================================
    // 인증정보 임시 저장소와 외부 통신 객체
    // ================================
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, VerificationData> verificationStore = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final JavaMailSenderImpl mailSender;
    private final String mailUsername;
    private final String apiKey;
    private final String apiSecret;
    private final String senderNumber;

    // ================================
    // 이메일과 문자 발송 설정
    // ================================
    public VerificationService(
            @Value("${spring.mail.host:smtp.naver.com}") String mailHost,
            @Value("${spring.mail.port:465}") int mailPort,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}") boolean sslEnabled,
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.sender-number:}") String senderNumber
    ) {
        this.mailUsername = mailUsername.trim();
        this.mailSender = createMailSender(
                mailHost.trim(),
                mailPort,
                this.mailUsername,
                mailPassword,
                startTlsEnabled,
                sslEnabled
        );

        this.apiKey = apiKey.trim();
        this.apiSecret = apiSecret.trim();
        this.senderNumber = senderNumber.replaceAll("\\D", "");
    }

    // ================================
    // 임시로 저장할 인증 데이터
    // ================================
    private record VerificationData(
            String code,
            LocalDateTime expiresAt,
            boolean verified
    ) {}

    // ================================
    // 회원가입 전화번호 인증
    // ================================

    // 회원가입 전화번호로 인증번호를 보낸다.
    public void sendSignupPhoneCode(String phone) {
        sendCode(signupKey(phone), "PHONE", phone);
    }

    // 사용자가 입력한 회원가입 인증번호를 확인한다.
    public void verifySignupPhoneCode(String phone, String code) {
        verifyCode(signupKey(phone), code, false);
    }

    // 회원가입 직전에 인증 여부를 확인하고 사용한 인증정보를 삭제한다.
    public void consumeSignupPhoneVerification(String phone) {
        String key = signupKey(phone);
        VerificationData data = verificationStore.remove(key);

        if (data == null
                || !data.verified()
                || LocalDateTime.now().isAfter(data.expiresAt())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "전화번호 인증을 완료해 주세요."
            );
        }
    }

    // ================================
    // 아이디 찾기와 비밀번호 재설정
    // ================================

    // 아이디·비밀번호 찾기용 보안코드를 보낸다.
    public void sendRecoveryCode(
            String purpose,
            String loginId,
            String channel,
            String contact
    ) {
        sendCode(
                recoveryKey(purpose, loginId, channel, contact),
                channel,
                contact
        );
    }

    // 아이디 찾기 또는 비밀번호 재설정 보안코드를 확인한다.
    public void verifyRecoveryCode(
            String purpose,
            String loginId,
            String channel,
            String contact,
            String code
    ) {
        verifyCode(
                recoveryKey(purpose, loginId, channel, contact),
                code,
                true
        );

    }

    // ================================
    // 보안코드 생성과 확인
    // ================================

    // 6자리 보안코드를 만들어 문자 또는 이메일로 보낸다.
    private void sendCode(
            String key,
            String channel,
            String contact
    ) {
        LocalDateTime now = LocalDateTime.now();
        String code =
                String.valueOf(secureRandom.nextInt(900000) + 100000);

        if ("PHONE".equals(normalizeChannel(channel))) {
            sendSms(normalizePhone(contact), code);
        } else {
            sendEmail(normalizeEmail(contact), code);
        }

        verificationStore.put(
                key,
                new VerificationData(
                        code,
                        now.plusMinutes(CODE_EXPIRE_MINUTES),
                        false
                )
        );
    }

    // 입력한 보안코드가 맞고 만료되지 않았는지 확인한다.
    private void verifyCode(
            String key,
            String inputCode,
            boolean consume
    ) {
        VerificationData data = verificationStore.get(key);

        boolean invalid =
                data == null
                        || LocalDateTime.now().isAfter(data.expiresAt())
                        || inputCode == null
                        || !data.code().equals(inputCode.trim());

        if (invalid) {
            verificationStore.remove(key);
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "보안코드가 올바르지 않거나 만료되었습니다."
            );
        }

        if (consume) {
            verificationStore.remove(key);
            return;
        }

        verificationStore.put(
                key,
                new VerificationData(
                        data.code(),
                        data.expiresAt(),
                        true
                )
        );
    }

    // ================================
    // SOLAPI 문자 발송
    // ================================

    // SOLAPI로 보안코드 문자를 보낸다.
    private void sendSms(
            String phone,
            String code
    ) {
        String text =
                "[BunhoBono APT] 본인인증을 위해 인증번호["
                        + code
                        + "]를 입력해주세요.";

        String requestBody = """
                {"messages":[{"to":"%s","from":"%s","text":"%s"}],"showMessageList":true}
                """.formatted(phone, senderNumber, text);

        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(SOLAPI_SEND_URL)
                    )
                    .header(
                            "Authorization",
                            createAuthorizationHeader()
                    )
                    .header(
                            "Content-Type",
                            "application/json; charset=UTF-8"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    requestBody,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() / 100 != 2) {
                log.error(
                        "SOLAPI SMS request failed: status={}, body={}",
                        response.statusCode(),
                        response.body()
                );
                throw new IllegalStateException(
                        "SOLAPI returned HTTP " + response.statusCode()
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "보안코드를 발송하지 못했습니다."
            );

        } catch (Exception exception) {
            log.error("Failed to send SMS through SOLAPI", exception);
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "보안코드를 발송하지 못했습니다."
            );
        }
    }

    // ================================
    // SMTP 이메일 발송
    // ================================

    // SMTP 서버로 보안코드 이메일을 보낸다.
    private void sendEmail(
            String email,
            String code
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(mailUsername);
            helper.setTo(email);
            helper.setSubject(
                    "[BunhoBono APT] 계정 확인 보안코드"
            );

            helper.setText(
                    """
                    <div style="font-size:20px; line-height:1.7; color:#1f2937;">
                        보안코드는 <strong style="font-size:24px;">[%s]</strong>입니다.
                    </div>
                    """.formatted(code),
                    true
            );

            mailSender.send(message);

        } catch (Exception exception) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "이메일 보안코드를 발송하지 못했습니다."
            );
        }
    }

    // 설정 파일의 SMTP 정보로 이메일 발송기를 만든다.
    private JavaMailSenderImpl createMailSender(
            String host,
            int port,
            String username,
            String password,
            boolean startTlsEnabled,
            boolean sslEnabled
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put(
                "mail.smtp.starttls.enable",
                String.valueOf(startTlsEnabled)
        );
        properties.put(
                "mail.smtp.ssl.enable",
                String.valueOf(sslEnabled)
        );

        return sender;
    }

    // API Key와 Secret으로 SOLAPI 인증 헤더를 만든다.
    private String createAuthorizationHeader() throws Exception {
        String dateTime = Instant.now().toString();
        String salt =
                UUID.randomUUID().toString().replace("-", "");

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(
                new SecretKeySpec(
                        apiSecret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                )
        );

        String signature = HexFormat.of().formatHex(
                mac.doFinal(
                        (dateTime + salt).getBytes(StandardCharsets.UTF_8)
                )
        );

        return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
                .formatted(apiKey, dateTime, salt, signature);
    }

    // ================================
    // 인증정보를 구분할 저장 키
    // ================================

    private String signupKey(String phone) {
        return String.join(
                "|",
                "SIGNUP",
                "PHONE",
                normalizePhone(phone)
        );
    }

    private String recoveryKey(
            String purpose,
            String loginId,
            String channel,
            String contact
    ) {
        String normalizedChannel = normalizeChannel(channel);

        return String.join(
                "|",
                purpose,
                loginId,
                normalizedChannel,
                "PHONE".equals(normalizedChannel)
                        ? normalizePhone(contact)
                        : normalizeEmail(contact)
        );
    }

    // ================================
    // 입력값 정리
    // ================================

    private String normalizeChannel(String channel) {
        String normalized =
                channel == null
                        ? ""
                        : channel.trim().toUpperCase();

        if (!normalized.equals("PHONE")
                && !normalized.equals("EMAIL")) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "올바른 인증 방법을 선택해 주세요."
            );
        }

        return normalized;
    }

    // 전화번호에서 하이픈과 바깥 공백을 없앤다.
    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replace("-", "").trim();
    }

    // 이메일의 바깥 공백을 없애고 소문자로 바꾼다.
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    // ================================
    // HTTP 오류 응답 생성
    // ================================

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
