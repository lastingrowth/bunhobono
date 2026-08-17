package api.camera_pdm_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CameraPdmMapper {

    // 카메라 예지보전 결과 전체 조회
    @Select(
            "SELECT pdm_no, camera_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, sensor_collected_at, " +
                    "predicted_at " +
                    "FROM camera_pdm " +
                    "ORDER BY predicted_at DESC, pdm_no DESC"
    )
    List<CameraPdmDTO> list();

    // 카메라 예지보전 결과 상세 조회
    @Select(
            "SELECT pdm_no, camera_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, sensor_collected_at, " +
                    "predicted_at " +
                    "FROM camera_pdm " +
                    "WHERE pdm_no = #{pdmNo}"
    )
    CameraPdmDTO detail(long pdmNo);

    // 카메라 예지보전 결과 저장
    @Insert(
            "INSERT INTO camera_pdm (" +
                    "camera_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, sensor_collected_at" +
                    ") VALUES (" +
                    "#{cameraNo}, #{riskScore}, #{riskLevel}, " +
                    "#{normalProbability}, #{warningProbability}, " +
                    "#{criticalProbability}, #{expectedRiskLevel}, " +
                    "#{predictionCorrect}, #{sensorCollectedAt}" +
                    ")"
    )
    int savePrediction(CameraPdmDTO dto);
}
