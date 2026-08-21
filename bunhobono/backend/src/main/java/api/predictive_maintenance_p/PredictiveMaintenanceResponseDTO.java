package api.predictive_maintenance_p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PredictiveMaintenanceResponseDTO {
    // 장비 종류: GATE, CAMERA, ROBOT
    @JsonProperty("equipment_type")
    private String equipmentType;

    // 테스트 CSV의 장비 식별값
    @JsonProperty("equipment_no")
    private String equipmentNo;

    // 테스트 CSV에서 사용한 행 번호
    @JsonProperty("row_index")
    private Integer rowIndex;

    // 모델의 최종 예측 등급: 정상, 주의, 위험
    @JsonProperty("risk_level")
    private String riskLevel;

    // 모델이 최종 선택한 등급의 확률
    @JsonProperty("risk_probability")
    private Double riskProbability;

    // 정상, 주의, 위험 등급별 확률
    private Map<String, Double> probabilities;

    // 테스트 CSV에 기록된 실제 정답
    @JsonProperty("expected_risk_level")
    private String expectedRiskLevel;

    // 모델 예측과 테스트 정답의 일치 여부
    @JsonProperty("prediction_correct")
    private Boolean predictionCorrect;

    // 모델 예측에 사용된 장비별 센서값
    @JsonProperty("sensor_values")
    private Map<String, Number> sensorValues;

    // 테스트 CSV 센서 데이터 수집 시각
    @JsonProperty("sensor_collected_at")
    private String sensorCollectedAt;

    // FastAPI에서 예측한 시각
    @JsonProperty("predicted_at")
    private LocalDateTime predictedAt;

    // 위험 행이 조치 대상인지 여부
    @JsonProperty("action_required")
    private Boolean actionRequired;

    // 관리자가 조치할 때까지 현재 CSV 행이 고정되었는지 여부
    private Boolean held;
}
