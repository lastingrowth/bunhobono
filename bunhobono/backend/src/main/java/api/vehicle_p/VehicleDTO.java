package api.vehicle_p;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDTO {

    private int vehicleCarNo, displayNo;
    private String vehicleType, carNo, vehicleStatus, approvedMemberName;
    private String memName, role;
    private LocalDateTime startDate, endDate, approvedAt, inTime, outTime, realEndDate;
    private Integer memberNo, memDong, memHo;
}