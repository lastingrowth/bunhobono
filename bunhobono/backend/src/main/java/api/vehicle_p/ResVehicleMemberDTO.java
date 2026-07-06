package api.vehicle_p;

import lombok.Data;

@Data
public class ResVehicleMemberDTO {

    // 로그인한 입주민 화면 표시용 정보
    private Integer memberNo;

    private String loginId;
    private String memName;

    private Integer memDong;
    private Integer memHo;
}