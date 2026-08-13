package api.robot_task_p;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RobotTaskDTO {

    private Long taskNo;
    private Integer carLogNo;

    private Long pickupSpaceNo;
    private Long dropoffSpaceNo;

    private Integer setNo;
    private String taskType;
    private String taskPhase;
    private String taskStatus;
    private Integer priority;

    private OffsetDateTime requestedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime phaseUpdatedAt;
    private OffsetDateTime completedAt;

    // 현재 작업 단계의 예정 소요시간
    private Integer phaseDurationMs;

    private String failureReason;

    // robot_task_detail View 조회값
    private String pickupSpaceCode;
    private String dropoffSpaceCode;
    private String carNo;
    private String carKind;
    private String parkingCode;
    private String parkingName;
}
