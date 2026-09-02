package api.predictive_maintenance_p;

import api.a_security_config.AuthService;
import api.a_security_config.LoginDTO;
import api.a_security_config.VerificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdmActionAuthorizationServiceTest {
    @Mock AuthService authService; @Mock VerificationService verificationService;
    @InjectMocks PdmActionAuthorizationService service;

    @Test @DisplayName("UT-BE-PDM-AUTH-001 알림 번호가 비어 있으면 문자를 보내지 않는다")
    void skipBlankPhone() { ReflectionTestUtils.setField(service, "alertPhone", " "); service.sendDangerAlert("로봇 1", Map.of()); verifyNoInteractions(verificationService); }

    @Test @DisplayName("UT-BE-PDM-AUTH-002 위험 센서값을 담당자 문자 내용에 포함한다")
    void sendAlert() { ReflectionTestUtils.setField(service, "alertPhone", "01012345678"); service.sendDangerAlert("로봇 1", Map.of("온도", 80)); verify(verificationService).sendPdmDangerSms(eq("01012345678"), contains("온도=80")); }

    @Test @DisplayName("UT-BE-PDM-AUTH-003 센서값이 없으면 안내문으로 위험 문자를 발송한다")
    void sendAlertWithoutSensors() { ReflectionTestUtils.setField(service, "alertPhone", "01012345678"); service.sendDangerAlert("게이트 1", null); verify(verificationService).sendPdmDangerSms(eq("01012345678"), contains("관리자 페이지에서 확인")); }

    @Test @DisplayName("UT-BE-PDM-AUTH-004 문자 발송 실패가 예측 처리를 중단시키지 않는다")
    void ignoreSmsFailure() { ReflectionTestUtils.setField(service, "alertPhone", "01012345678"); doThrow(new IllegalStateException()).when(verificationService).sendPdmDangerSms(anyString(),anyString()); assertThatCode(() -> service.sendDangerAlert("로봇 1", Map.of())).doesNotThrowAnyException(); }

    @Test @DisplayName("UT-BE-PDM-AUTH-005 로그인 회원번호를 반환한다")
    void memberNo() { LoginDTO dto = new LoginDTO(); dto.setMemberNo(7); when(authService.getUserInfo("user")).thenReturn(dto); assertThat(service.requireMemberNo("user")).isEqualTo(7); }

    @Test @DisplayName("UT-BE-PDM-AUTH-006 로그인 정보가 없으면 거부한다")
    void rejectMissingMember() { assertThatThrownBy(() -> service.requireMemberNo("x")).isInstanceOf(ResponseStatusException.class); }

    @Test @DisplayName("UT-BE-PDM-AUTH-007 로그인 회원번호가 없으면 거부한다")
    void rejectMissingMemberNo() { LoginDTO dto=new LoginDTO(); when(authService.getUserInfo("x")).thenReturn(dto); assertThatThrownBy(() -> service.requireMemberNo("x")).isInstanceOf(ResponseStatusException.class); }

    @Test @DisplayName("UT-BE-PDM-AUTH-008 조치 내용 앞뒤 공백을 제거한다") void normalizeNote() { assertThat(service.normalizeActionNote("  점검 완료  ")).isEqualTo("점검 완료"); }
    @Test @DisplayName("UT-BE-PDM-AUTH-009 빈 조치 내용은 null로 변환한다") void normalizeBlankNote() { assertThat(service.normalizeActionNote(null)).isNull(); assertThat(service.normalizeActionNote(" ")).isNull(); }
    @Test @DisplayName("UT-BE-PDM-AUTH-010 조치 내용은 500자를 허용하고 초과하면 거부한다") void normalizeNoteLength() { assertThat(service.normalizeActionNote("가".repeat(500))).hasSize(500); assertThatThrownBy(() -> service.normalizeActionNote("가".repeat(501))).isInstanceOf(ResponseStatusException.class); }
}
