package api.member_p;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsAuthService {

    private static final int CODE_EXPIRE_MINUTES = 3;
    private static final int RESEND_SECONDS = 60;

    // SOLAPI에서 제공하는 문자 발송 REST API 주소.
    private static final String SOLAPI_SEND_URL =
            "https://api.solapi.com/messages/v4/send-many/detail";

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SmsAuthData> authStore = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;
    private final String apiSecret;
    private final String senderNumber;

    public SmsAuthService(
            @Value("${solapi.api-key:}") String apiKey,
            @Value("${solapi.api-secret:}") String apiSecret,
            @Value("${solapi.sender-number:}") String senderNumber
    ) {
        this.apiKey = apiKey.trim();
        this.apiSecret = apiSecret.trim();
        this.senderNumber = senderNumber.replaceAll("\\D", "");
    }

    // [sms인증] 인증번호와 인증 상태를 서버 메모리에 보관한다.
    private record SmsAuthData(
            String code,
            LocalDateTime expiresAt,
            LocalDateTime resendAt,
            boolean verified
    ) {
    }

    // [sms인증] 인증번호 6자리를 생성하고 문자로 발송한다.
    public void sendCode(String phone) {
        String normalizedPhone = normalizePhone(phone);
        LocalDateTime now = LocalDateTime.now();
        SmsAuthData savedData = authStore.get(normalizedPhone);

        // [sms인증] 1분 이내의 중복 발송을 차단한다.
        if (savedData != null && now.isBefore(savedData.resendAt())) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 후 다시 요청할 수 있습니다.");
        }

        String code = String.valueOf(secureRandom.nextInt(900000) + 100000);
        sendSms(normalizedPhone, code);
        authStore.put(normalizedPhone, new SmsAuthData(
                code,
                now.plusMinutes(CODE_EXPIRE_MINUTES),
                now.plusSeconds(RESEND_SECONDS),
                false
        ));
    }

    // [sms인증] 입력한 인증번호와 만료 시간을 확인한다.
    public void verifyCode(String phone, String inputCode) {
        String normalizedPhone = normalizePhone(phone);
        SmsAuthData savedData = authStore.get(normalizedPhone);

        // [sms인증] 발급된 인증번호가 없는 요청을 차단한다.
        if (savedData == null) {
            throw error(HttpStatus.BAD_REQUEST, "인증번호를 먼저 요청해 주세요.");
        }
        // [sms인증] 3분이 지난 인증번호를 만료 처리한다.
        if (LocalDateTime.now().isAfter(savedData.expiresAt())) {
            authStore.remove(normalizedPhone);
            throw error(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }
        // [sms인증] 입력값이 없거나 발급 번호와 다르면 인증을 거절한다.
        if (inputCode == null || !savedData.code().equals(inputCode.trim())) {
            throw error(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.");
        }

        authStore.put(normalizedPhone, new SmsAuthData(
                savedData.code(), savedData.expiresAt(), savedData.resendAt(), true
        ));
    }

    // [sms인증] 전화번호 인증 완료 여부를 반환한다.
    public boolean isVerified(String phone) {
        String normalizedPhone = normalizePhone(phone);
        SmsAuthData savedData = authStore.get(normalizedPhone);

        if (savedData == null || !savedData.verified()) {
            return false;
        }
        if (LocalDateTime.now().isAfter(savedData.expiresAt())) {
            authStore.remove(normalizedPhone);
            return false;
        }
        return true;
    }

    // [sms인증] 회원가입에 사용한 인증정보를 제거한다.
    public void consumeVerification(String phone) {
        authStore.remove(normalizePhone(phone));
    }

    // [sms인증] SOLAPI로 인증번호 문자를 발송한다.
    private void sendSms(String phone, String code) {
        String text = "[BunhoBono APT] 본인인증을 위해 인증번호[" + code + "]를 입력해주세요.";
        String requestBody = """
                {"messages":[{"to":"%s","from":"%s","text":"%s"}],"showMessageList":true}
                """.formatted(phone, senderNumber, text);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(SOLAPI_SEND_URL))
                    .header("Authorization", createAuthorizationHeader())
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            // [sms인증] SOLAPI가 발송 실패 상태를 반환하면 요청을 실패 처리한다.
            if (response.statusCode() / 100 != 2) {
                throw error(HttpStatus.BAD_GATEWAY, "인증번호 문자 발송에 실패했습니다.");
            }
        // [sms인증] 문자 발송 작업이 중단된 경우 스레드 상태를 복구한다.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw error(HttpStatus.BAD_GATEWAY, "인증번호 문자 발송이 중단되었습니다.");
        // [sms인증] 이미 구분된 발송 오류는 상태와 문구를 그대로 전달한다.
        } catch (ResponseStatusException exception) {
            throw exception;
        // [sms인증] 그 밖의 통신·서명 오류를 발송 실패로 처리한다.
        } catch (Exception exception) {
            throw error(HttpStatus.BAD_GATEWAY, "인증번호 문자를 발송하지 못했습니다.");
        }
    }

    // [sms인증] Mac과 API Secret으로 SOLAPI 요청 서명을 생성한다.
    private String createAuthorizationHeader() throws Exception {
        String dateTime = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(
                mac.doFinal((dateTime + salt).getBytes(StandardCharsets.UTF_8))
        );

        return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
                .formatted(apiKey, dateTime, salt, signature);
    }

    // [sms인증] 전화번호에서 하이픈을 제거하고 형식을 검사한다.
    private String normalizePhone(String phone) {
        String normalizedPhone = phone == null ? "" : phone.replaceAll("\\D", "");
        // [sms인증] 010으로 시작하는 11자리 번호가 아니면 요청을 차단한다.
        if (!normalizedPhone.matches("010\\d{8}")) {
            throw error(HttpStatus.BAD_REQUEST, "올바른 휴대전화 번호를 입력해 주세요.");
        }
        return normalizedPhone;
    }

    // [sms인증] 오류 상태와 안내 문구를 HTTP 예외로 변환한다.
    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
