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


    //일련번호
    private int displayNo;
    // 승인 처리자 번호--외래키
    private Integer memberNo;

}
