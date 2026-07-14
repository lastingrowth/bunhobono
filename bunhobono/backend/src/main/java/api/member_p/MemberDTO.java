package api.member_p;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MemberDTO {

    // 회원 가입·조회·수정에 사용하는 기본 회원 정보다.
    private long memberNo;
    private String loginId;
    private String loginPwd;
    private String memName;
    private String memPhone;
    private String role;
    private String memStatus;
    private int memDong;
    private int memHo;
    private LocalDate memCreateAt;
    private LocalDate memDeleteAt;

    // 입주민 대시보드에 표시할 본인 차량 정보를 전달한다.
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

    // 입주민 차량의 최근 입차·출차 기록을 전달한다.
    @Data
    public static class ResidentCarLog {

        private Integer carLogNo;
        private String carNo;
        private String parkingName;
        private String parkingState;
        private LocalDateTime inTime;
        private LocalDateTime outTime;
    }

    // 회원 정보·본인 차량·최근 입출차 기록을 대시보드 응답 하나로 묶는다.
    @Data
    public static class ResidentDashboard {

        private MemberDTO member;
        private List<ResidentVehicle> vehicles;
        private List<ResidentCarLog> recentCarLogs;
    }
}
