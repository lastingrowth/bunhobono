package api.robot_pdm_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class RobotPdmDTO {

    private Long pdmNo;
    private Long robotLogNo;

    // 예지보전 위험도
    private BigDecimal riskScore;
    private String riskLevel;

    // 모델 예측 확률
    private BigDecimal normalProbability;
    private BigDecimal warningProbability;
    private BigDecimal criticalProbability;

    // 예측 결과
    private String predictionReason;
    private String modelVersion;
    private OffsetDateTime predictedAt;
}