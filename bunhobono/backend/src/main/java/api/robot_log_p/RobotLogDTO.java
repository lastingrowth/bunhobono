package api.robot_log_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class RobotLogDTO {

    private Long robotLogNo;
    private String sourceEventId;

    private Long robotNo;
    private Long taskNo;

    // 로봇과 작업 현재 상태
    private String robotStatus;
    private String taskPhase;
    private String payloadState;

    // 주행 구동부 원시 상태값
    private BigDecimal driveMotorTemperatureC;
    private BigDecimal driveMotorCurrentA;
    private BigDecimal driveVibrationMmS;

    // 배터리 원시 상태값
    private BigDecimal batteryVoltageV;
    private BigDecimal batteryTemperatureC;
    private BigDecimal batteryLevel;

    // 안전 상태
    private Boolean obstacleDetected;
    private Boolean safetyStop;
    private String alarmCode;

    // 가상 로봇 측정 시각
    private OffsetDateTime sampledAt;

    // DB 저장 시각
    private OffsetDateTime createdAt;

    // 조회 화면 표시값
    private String robotCode;
    private Integer setNo;
    private String setPosition;
    private String taskType;
}