package api.robot_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class RobotDTO {

    private Long robotNo;
    private String robotCode;

    private Integer setNo;
    private String setPosition;

    private String robotStatus;
    private BigDecimal batteryLevel;
    private BigDecimal operatingHours;

    private OffsetDateTime lastHeartbeatAt;
    private OffsetDateTime lastMaintenanceAt;

    // 최근 점검 후 경과일
    private Integer daysSinceMaintenance;


    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}