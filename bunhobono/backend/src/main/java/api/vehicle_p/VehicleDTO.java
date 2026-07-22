package api.vehicle_p;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDTO {

    private int vehicleCarNo, displayNo;
    private String vehicleType, carNo, vehicleStatus, approvedMemberName;
    private String memName, role;

    // 관리자 반려 요청 시 vehicle_nt에 저장할 반려 사유
    private String rejectReason;

    private LocalDateTime startDate, endDate, approvedAt, inTime, outTime, realEndDate;
    private Integer memberNo, memDong, memHo;

    private String expiryType;
    private Long remainingMinutes;

}