package api.gate_pdm_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class GatePdmDTO {

    private Long pdmNo;
    private Integer gateNo;

    // 모델 예측 위험도
    private BigDecimal riskScore;
    private String riskLevel;

    // 등급별 예측 확률
    private BigDecimal normalProbability;
    private BigDecimal warningProbability;
    private BigDecimal criticalProbability;

    // 테스트 데이터 정답과 비교 결과
    private String expectedRiskLevel;
    private Boolean predictionCorrect;

    // 예측에 사용한 CSV 행 번호
    private Integer sourceRowIndex;

    // 모델 예측에 사용된 게이트 센서값
    private Map<String, Number> sensorValues;

    // 센서 수집 시각
    private OffsetDateTime sensorCollectedAt;

    // 예측에 사용한 모델 정보
    private OffsetDateTime predictedAt;

    // 위험 조치 상태와 관리자 조치 정보
    private String actionStatus;
    private String actionNote;
    private Integer actionByMemberNo;
    private String actionByMemberName;
    private OffsetDateTime actionStartedAt;
    private OffsetDateTime actionCompletedAt;
}
