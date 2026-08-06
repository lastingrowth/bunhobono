package api.robot_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RobotDTO {

    private Long robotNo;
    private String robotCode;

    private Integer setNo;
    private String setPosition;

    private String robotStatus;
    private BigDecimal batteryLevel;
    private BigDecimal operatingHours;

    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime lastMaintenanceAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}