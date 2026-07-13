package api.vehicle_p;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDTO {


    private int vehicleCarNo;
    private String vehicleType,carNo,vehicleStatus;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    // 승인 처리자 이름
    private String approvedMemberName;
    //차량등록 승인시간
    private LocalDateTime approvedAt;

    // 승인 화면에서 전달하는 이용 기간 (일반 차량: 개월, 방문 차량: 시간)
    private Integer periodMonths;
    private Integer periodHours;


    //일련번호
    private int displayNo;
    // 승인 처리자 번호--외래키
    private Integer memberNo;

}
