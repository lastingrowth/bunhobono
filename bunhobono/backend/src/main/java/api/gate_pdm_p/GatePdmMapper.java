package api.gate_pdm_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GatePdmMapper {

    // 게이트 예지보전 결과 전체 조회
    @Select(
            "SELECT pdm_no, gate_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, sensor_collected_at, " +
                    "predicted_at " +
                    "FROM gate_pdm " +
                    "ORDER BY predicted_at DESC, pdm_no DESC"
    )
    List<GatePdmDTO> list();

    // 게이트 예지보전 결과 상세 조회
    @Select(
            "SELECT pdm_no, gate_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, sensor_collected_at, " +
                    "predicted_at " +
                    "FROM gate_pdm " +
                    "WHERE pdm_no = #{pdmNo}"
    )
    GatePdmDTO detail(long pdmNo);

    // 게이트 예지보전 결과 저장
    @Insert(
            "INSERT INTO gate_pdm (" +
                    "gate_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, sensor_collected_at" +
                    ") VALUES (" +
                    "#{gateNo}, #{riskScore}, #{riskLevel}, " +
                    "#{normalProbability}, #{warningProbability}, " +
                    "#{criticalProbability}, #{expectedRiskLevel}, " +
                    "#{predictionCorrect}, #{sensorCollectedAt}" +
                    ")"
    )
    int savePrediction(GatePdmDTO dto);
}
