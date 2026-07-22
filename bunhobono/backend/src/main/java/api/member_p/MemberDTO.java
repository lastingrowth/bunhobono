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
    private long memberNo;
    private long displayNo;     // 회원 목록에 표시할 조회 순번이다.
    private String loginId;
    private String loginPwd;
    private String memName;
    private String memPhone;
    private String role;
    private String memStatus;
    private int memDong;
    private int memHo;
    private LocalDateTime memCreateAt;
    private LocalDateTime memDeleteAt;

    private Boolean archived;   // 전출 이력이 보관되었는지 표시한다.

    // =====================================================
    // 2. 입주민 대시보드에 표시할 본인 차량 정보를 전달한다.
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
    // 3. 입주민 차량의 입차·출차 기록을 전달한다.
    // =====================================================
    @Data
    public static class ResidentCarLog {

        private Integer carLogNo;
        private String carNo;
        private String parkingName;
        private String parkingState;
        private LocalDateTime inTime;
        private LocalDateTime outTime;
    }

    // =====================================================
    // 4. 회원 정보·본인 차량·입출차 기록을 대시보드 응답 하나로 묶는다.
    // =====================================================
    @Data
    public static class ResidentDashboard {

        private MemberDTO member;
        private List<ResidentVehicle> vehicles;
        private List<ResidentCarLog> recentCarLogs;
    }

    // =====================================================
    // 5. 입주민 본인 확인에 필요한 비밀번호와 보안문자 값을 전달한다.
    // =====================================================
    public record ResidentSecurityRequest(
            String currentPassword,
            String newPassword,
            String challengeId,
            String challengeAnswer
    ) {}
}
