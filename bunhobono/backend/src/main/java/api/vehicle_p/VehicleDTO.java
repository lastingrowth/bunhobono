package api.vehicle_p;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDTO {

    // vehicle_car 기본 컬럼
    private Integer vehicleCarNo, approvedNo;

    private String carNo, vehicleType, vehicleStatus;

    private LocalDateTime startDate, endDate, approvedAt;

    // 승인 처리 요청값
    private Integer periodMonths;
    private Integer periodHours;
}