package api.predictive_maintenance_p;

import api.a_security_config.AuthService;
import api.a_security_config.LoginDTO;
import api.a_security_config.VerificationService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PdmActionAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(
            PdmActionAuthorizationService.class
    );

    @Resource
    private AuthService authService;

    @Resource
    private VerificationService verificationService;

    @Value("${pdm.alert-phone:}")
    private String alertPhone;

    // 최초 저장된 예지보전 위험을 설정된 담당자 번호로 알린다.
    // 문자 발송 실패가 예측 결과 저장과 스케줄러 실행을 중단시키지 않게 처리한다.
    public void sendDangerAlert(
            String equipmentName,
            Map<String, Number> sensorValues
    ) {
        if (alertPhone == null || alertPhone.isBlank()) {
            log.warn("PDM danger SMS skipped: pdm.alert-phone is empty");
            return;
        }

        String sensorText = formatSensorValues(sensorValues);
        String message = """
                [BunhoBono 예지보전]
                %s에서 위험이 감지되었습니다.
                %s
                관리자 페이지에서 점검 후 조치해주세요.
                """.formatted(equipmentName, sensorText).trim();

        try {
            verificationService.sendPdmDangerSms(alertPhone, message);
        } catch (Exception exception) {
            log.error(
                    "Failed to send PDM danger SMS: equipment={}",
                    equipmentName,
                    exception
            );
        }
    }

    private String formatSensorValues(Map<String, Number> sensorValues) {
        if (sensorValues == null || sensorValues.isEmpty()) {
            return "센서값은 관리자 페이지에서 확인할 수 있습니다.";
        }

        return "센서값: " + sensorValues.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    // 로그인한 사용자의 회원번호를 조치 담당자 번호로 반환한다.
    public int requireMemberNo(String loginId) {
        LoginDTO member = authService.getUserInfo(loginId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
        if (member.getMemberNo() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "회원번호를 확인할 수 없습니다."
            );
        }

        return member.getMemberNo();
    }

    // 빈 조치 내용은 NULL로 저장하고 DB 컬럼 길이를 초과하지 않게 검사한다.
    public String normalizeActionNote(String actionNote) {
        if (actionNote == null || actionNote.isBlank()) {
            return null;
        }

        String normalized = actionNote.trim();
        if (normalized.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "조치 내용은 500자 이하로 입력해주세요."
            );
        }
        return normalized;
    }
}
