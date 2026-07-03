package apt.vehicle_p;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDTO {

    private Integer vehicleCarNo, approvedNo;
    private String vehicleType, carNo, vehicleStatus;
    private LocalDateTime startDate, endDate, approvedAt;
}