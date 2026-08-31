package api.robot_pdm_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class RobotPdmDTO {

    // 예지보전 결과 내부 고유번호와 분석 대상 로봇 번호
    private Long pdmNo;
    private Integer robotNo;

    // 모델이 최종 선택한 위험 등급의 확률과 위험 등급
    private BigDecimal riskScore;
    private String riskLevel;

    // 정상·주의·위험 등급별 예측 확률
    private BigDecimal normalProbability;
    private BigDecimal warningProbability;
    private BigDecimal criticalProbability;

    // 테스트 CSV의 실제 정답과 모델 예측의 일치 여부
    private String expectedRiskLevel;
    private Boolean predictionCorrect;

    // 예측에 사용한 CSV 행 번호
    private Integer sourceRowIndex;

    // 모델 예측에 사용된 로봇 센서값
    private Map<String, Number> sensorValues;

    // 센서 데이터 수집 시각과 FastAPI 모델 예측 시각
    private OffsetDateTime sensorCollectedAt;
    private OffsetDateTime predictedAt;

    // 위험 조치 상태와 관리자 조치 정보
    private String actionStatus;
    private String actionNote;
    private Integer actionByMemberNo;
    private String actionByMemberName;
    private OffsetDateTime actionStartedAt;
    private OffsetDateTime actionCompletedAt;
}
