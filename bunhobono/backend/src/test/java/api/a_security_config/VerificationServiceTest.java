package api.a_security_config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationServiceTest {

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
                "smtp.test.local",
                465,
                "test@example.com",
                "test-password",
                false,
                true,
                "test-api-key",
                "test-api-secret",
                "010-1234-5678"
        );
    }

    @ParameterizedTest(name = "[{index}] {0} 인증정보 없음")
    @CsvSource({
            "PHONE, 010-1234-5678",
            "EMAIL, resident@example.com"
    })
    @DisplayName(
            "UT-BE-ASECURITY-002 | 완료되지 않은 회원가입 인증정보 사용을 거부한다"
    )
    void consumeSignupVerification_rejectsMissingVerification(
            String channel,
            String contact
    ) {
        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> consumeSignupVerification(channel, contact)
        );

        // Assert
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @ParameterizedTest(name = "[{index}] phone={0}, message={1}")
    @MethodSource("invalidDangerSmsInputs")
    @DisplayName(
            "UT-BE-ASECURITY-003 | 수신번호나 내용이 없는 위험 알림 문자를 거부한다"
    )
    void sendPdmDangerSms_rejectsBlankInput(
            String phone,
            String message
    ) {
        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> verificationService.sendPdmDangerSms(
                        phone,
                        message
                )
        );

        // Assert
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @ParameterizedTest(name = "[{index}] channel={0}")
    @CsvSource({
            "SMS",
            "KAKAO"
    })
    @DisplayName(
            "UT-BE-ASECURITY-004 | 지원하지 않는 인증 채널을 거부한다"
    )
    void sendRecoveryCode_rejectsUnsupportedChannel(
            String channel
    ) {
        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> verificationService.sendRecoveryCode(
                        "FIND_PASSWORD",
                        "resident01",
                        channel,
                        "010-1234-5678"
                )
        );

        // Assert
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    @DisplayName(
            "UT-BE-ASECURITY-005 | 발급되지 않은 복구 보안코드를 거부한다"
    )
    void verifyRecoveryCode_rejectsCodeThatWasNotIssued() {
        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> verificationService.verifyRecoveryCode(
                        "FIND_PASSWORD",
                        "resident01",
                        "PHONE",
                        "010-1234-5678",
                        "123456"
                )
        );

        // Assert
        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    private void consumeSignupVerification(
            String channel,
            String contact
    ) {
        if ("PHONE".equals(channel)) {
            verificationService.consumeSignupPhoneVerification(contact);
            return;
        }

        verificationService.consumeSignupEmailVerification(contact);
    }

    private static Stream<Arguments> invalidDangerSmsInputs() {
        return Stream.of(
                Arguments.of(null, "위험이 감지되었습니다."),
                Arguments.of("   ", "위험이 감지되었습니다."),
                Arguments.of("010-1234-5678", null),
                Arguments.of("010-1234-5678", "   ")
        );
    }
}
