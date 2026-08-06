package api.member_p;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MemberDTO {

    // =====================================================
    // 1. 회원가입·조회·수정에 사용하는 기본 회원 정보를 전달한다.
    // =====================================================
    private Integer memberNo;
    private long displayNo;     // 회원 목록에 표시할 조회 순번이다.
    private String loginId;
    private String loginPwd;
    private Integer unitNo;        // 아파트 동/호수. 관리자는 null
    private String memName;
    private String memPhone;
    private String email;
    private String role;
    private String memStatus;
    private Integer dong;
    private Integer ho;
    private LocalDateTime memCreateAt;
    private LocalDateTime memDeleteAt;

    private Boolean archived;   // 전출 이력이 보관되었는지 표시한다.

    // =====================================================
    // [sms인증] 2. 전화번호 인증번호 발송과 확인 요청값을 전달한다.
    // =====================================================
    public record PhoneCodeRequest(
            String phone,
            String code
    ) {}

    // 아이디·비밀번호 찾기의 발송, 인증, 비밀번호 변경 요청값을 함께 전달한다.
    public record AccountRecoveryRequest(
            // 요청 목적: 아이디 찾기(FIND_ID) 또는 비밀번호 재설정(RESET_PASSWORD)
            String purpose,
            // 인증 방법: 문자(PHONE) 또는 이메일(EMAIL)
            String channel,
            // 보안코드를 받을 전화번호 또는 이메일 주소
            String contact,
            // 아이디 찾기에 사용하는 회원 이름
            String memName,
            // 아이디 찾기에 사용하는 동
            Integer dong,
            // 아이디 찾기에 사용하는 호수
            Integer ho,
            // 비밀번호 재설정 대상 아이디
            String loginId,
            // 사용자가 입력한 6자리 보안코드
            String code,
            // 인증 성공 후 저장할 새 비밀번호
            String newPassword
    ) {}

    // 보안문자 인증 후 조회된 아이디를 반환한다.
    public record AccountRecoveryResponse(
            String loginId
    ) {}

    // =====================================================
    // 3. 입주민 대시보드에 표시할 본인 차량 정보를 전달한다.
    // =====================================================
    @Data
    public static class ResidentVehicle {

        private Integer vehicleCarNo;
        private String vehicleType;
        private String carNo;
        private String vehicleStatus;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime approvedAt;
        private String parkingState;
        private String parkingName;
    }

    // =====================================================
    // 4. 입주민 차량의 입차·출차 기록을 전달한다.
    @Data
    // =====================================================
    public static class ResidentCarLog {

        private Integer carLogNo;
        private String carNo;
        private String parkingName;
        private String parkingState;
        private LocalDateTime inTime;
        private LocalDateTime outTime;
    }


    // =====================================================
    // 5. 회원 정보·본인 차량·입출차 기록을 대시보드 응답 하나로 묶는다.
    // =====================================================
    @Data
    public static class ResidentDashboard {

        private MemberDTO member;
        private List<ResidentVehicle> vehicles;
        private List<ResidentCarLog> recentCarLogs;


    }

    // =====================================================
    // 6. 비밀번호 변경 및 회원 탈퇴시, 입주민 본인 확인에 필요한 비밀번호와 보안문자 값을 전달한다.
    // =====================================================
    public record ResidentSecurityRequest(
            // 본인 확인에 사용하는 현재 비밀번호
            String currentPassword,
            // 비밀번호 변경 시 저장할 새 비밀번호
            String newPassword,
            // 서버에서 발급한 보안문자 이미지의 식별값
            String challengeId,
            // 사용자가 입력한 보안문자 정답
            String challengeAnswer
    ) {}

}
