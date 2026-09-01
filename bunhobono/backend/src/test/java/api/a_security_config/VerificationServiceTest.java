package api.a_security_config;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationServiceTest {
    VerificationService service; HttpClient httpClient; JavaMailSenderImpl mailSender;

    @BeforeEach @SuppressWarnings({"rawtypes","unchecked"})
    void setUp() throws Exception {
        service=new VerificationService("smtp.test",465,"sender@test.com","pw",false,true,"key","secret","01012345678");
        httpClient=mock(HttpClient.class); HttpResponse response=mock(HttpResponse.class); when(response.statusCode()).thenReturn(200); when(httpClient.send(any(),any())).thenReturn(response); ReflectionTestUtils.setField(service,"httpClient",httpClient);
        mailSender=mock(JavaMailSenderImpl.class); MimeMessage message=new JavaMailSenderImpl().createMimeMessage(); when(mailSender.createMimeMessage()).thenReturn(message); ReflectionTestUtils.setField(service,"mailSender",mailSender);
    }

    @Test @DisplayName("UT-BE-ASECURITY-002 | 회원가입 전화번호로 인증번호를 발송하고 상태를 저장한다") void sendSignupPhoneCode() throws Exception { service.sendSignupPhoneCode("010-1111-2222"); assertThat(store()).hasSize(1); verify(httpClient).send(any(),any()); }
    @Test @DisplayName("UT-BE-ASECURITY-003 | 올바른 전화 인증번호를 인증 완료 상태로 변경한다") void verifySignupPhoneCode(){ issuePhone(); service.verifySignupPhoneCode("01011112222",issuedCode()); assertThat(store()).hasSize(1); }
    @Test @DisplayName("UT-BE-ASECURITY-004 | 인증 완료 전화번호를 회원가입 시 한 번만 소모한다") void consumeSignupPhoneVerification(){ issuePhone(); service.verifySignupPhoneCode("01011112222",issuedCode()); service.consumeSignupPhoneVerification("01011112222"); assertThat(store()).isEmpty(); assertBadRequest(() -> service.consumeSignupPhoneVerification("01011112222")); }
    @Test @DisplayName("UT-BE-ASECURITY-005 | 회원가입 이메일로 인증번호를 발송하고 상태를 저장한다") void sendSignupEmailCode(){ service.sendSignupEmailCode("resident@test.com"); assertThat(store()).hasSize(1); verify(mailSender).send(any(MimeMessage.class)); }
    @Test @DisplayName("UT-BE-ASECURITY-006 | 올바른 이메일 인증번호를 인증 완료 상태로 변경한다") void verifySignupEmailCode(){ issueEmail(); service.verifySignupEmailCode("resident@test.com",issuedCode()); assertThat(store()).hasSize(1); }
    @Test @DisplayName("UT-BE-ASECURITY-007 | 인증 완료 이메일을 회원가입 시 한 번만 소모한다") void consumeSignupEmailVerification(){ issueEmail(); service.verifySignupEmailCode("resident@test.com",issuedCode()); service.consumeSignupEmailVerification("resident@test.com"); assertThat(store()).isEmpty(); assertBadRequest(() -> service.consumeSignupEmailVerification("resident@test.com")); }
    @Test @DisplayName("UT-BE-ASECURITY-008 | 계정 복구 인증번호를 목적과 로그인 ID 기준으로 발송한다") void sendRecoveryCode(){ service.sendRecoveryCode("FIND_ID","user","PHONE","01011112222"); assertThat(store().keySet().iterator().next()).contains("FIND_ID").contains("user"); }
    @Test @DisplayName("UT-BE-ASECURITY-009 | 계정 복구 인증번호는 확인 즉시 소모한다") void verifyRecoveryCode(){ service.sendRecoveryCode("FIND_ID","user","PHONE","01011112222"); service.verifyRecoveryCode("FIND_ID","user","PHONE","01011112222",issuedCode()); assertThat(store()).isEmpty(); }
    @Test @DisplayName("UT-BE-ASECURITY-011 | 예지보전 위험 알림 문자를 정규화해 발송한다") void sendPdmDangerSms() throws Exception { service.sendPdmDangerSms("010-1111-2222"," 위험 발생 "); verify(httpClient).send(any(),any()); }
    @Test @DisplayName("UT-BE-ASECURITY-012 | 잘못된 인증번호를 거부하고 발급 상태를 제거한다") void rejectWrongCode(){ issuePhone(); assertBadRequest(() -> service.verifySignupPhoneCode("01011112222","000000")); assertThat(store()).isEmpty(); }
    @Test @DisplayName("UT-BE-ASECURITY-013 | 발급되지 않은 회원가입 인증정보 소모를 거부한다") void rejectMissingSignupVerification(){ assertBadRequest(() -> service.consumeSignupPhoneVerification("01011112222")); assertBadRequest(() -> service.consumeSignupEmailVerification("resident@test.com")); }
    @Test @DisplayName("UT-BE-ASECURITY-014 | 지원하지 않는 계정 복구 채널을 거부한다") void rejectUnsupportedChannel(){ assertBadRequest(() -> service.sendRecoveryCode("FIND_ID","user","KAKAO","contact")); verifyNoInteractions(httpClient,mailSender); }
    @Test @DisplayName("UT-BE-ASECURITY-015 | 발급되지 않은 계정 복구 인증번호를 거부한다") void rejectMissingRecoveryCode(){ assertBadRequest(() -> service.verifyRecoveryCode("FIND_ID","user","PHONE","01011112222","123456")); }
    @Test @DisplayName("UT-BE-ASECURITY-016 | 수신번호나 내용이 없는 위험 알림 문자를 거부한다") void rejectBlankDangerSms(){ assertBadRequest(() -> service.sendPdmDangerSms(" ","위험")); assertBadRequest(() -> service.sendPdmDangerSms("01011112222"," ")); verifyNoInteractions(httpClient); }

    void issuePhone(){ service.sendSignupPhoneCode("01011112222"); } void issueEmail(){ service.sendSignupEmailCode("resident@test.com"); }
    @SuppressWarnings("unchecked") Map<String,Object> store(){ return (Map<String,Object>)ReflectionTestUtils.getField(service,"verificationStore"); }
    String issuedCode(){ return (String)ReflectionTestUtils.invokeMethod(store().values().iterator().next(),"code"); }
    void assertBadRequest(Runnable action){ assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)); }
}
