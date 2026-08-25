package api.gate_pdm_p;

import api.predictive_maintenance_p.SensorValuesJsonTypeHandler;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GatePdmMapper {

    // 게이트 예지보전 결과 전체 조회
    @Select(
            "SELECT pdm_no, gate_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "predicted_at, action_status, action_note, action_by_member_no, " +
                    "action_started_at, action_completed_at " +
                    "FROM gate_pdm " +
                    "ORDER BY predicted_at DESC, pdm_no DESC"
    )
    @Results(id = "gatePdmResult", value = {
            @Result(column = "sensor_values", property = "sensorValues",
                    typeHandler = SensorValuesJsonTypeHandler.class)
    })
    List<GatePdmDTO> list();

    // 게이트 예지보전 결과 상세 조회
    @Select(
            "SELECT pdm_no, gate_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "predicted_at, action_status, action_note, action_by_member_no, " +
                    "action_started_at, action_completed_at " +
                    "FROM gate_pdm " +
                    "WHERE pdm_no = #{pdmNo}"
    )
    @ResultMap("gatePdmResult")
    GatePdmDTO detail(long pdmNo);

    // 게이트 예지보전 결과 저장
    @Insert(
            "INSERT INTO gate_pdm (" +
                    "gate_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "action_status" +
                    ") VALUES (" +
                    "#{gateNo}, #{riskScore}, #{riskLevel}, " +
                    "#{normalProbability}, #{warningProbability}, " +
                    "#{criticalProbability}, #{expectedRiskLevel}, " +
                    "#{predictionCorrect}, #{sourceRowIndex}, " +
                    "#{sensorValues,typeHandler=api.predictive_maintenance_p.SensorValuesJsonTypeHandler}, #{sensorCollectedAt}, " +
                    "COALESCE(#{actionStatus}, 'NOT_REQUIRED')" +
                    ") ON CONFLICT DO NOTHING"
    )
    int savePrediction(GatePdmDTO dto);

    // 게이트에 현재 조치가 필요한 위험 사건을 조회한다.
    @Select("""
        SELECT pdm_no, gate_no, risk_score, risk_level,
               normal_probability, warning_probability,
               critical_probability, expected_risk_level,
               prediction_correct, source_row_index, sensor_values, sensor_collected_at,
               predicted_at, action_status, action_note, action_by_member_no,
               action_started_at, action_completed_at
        FROM gate_pdm
        WHERE gate_no = #{gateNo}
          AND risk_level = '위험'
          AND action_status = 'ACTION_REQUIRED'
        ORDER BY pdm_no DESC
        LIMIT 1
    """)
    @ResultMap("gatePdmResult")
    GatePdmDTO findActiveAction(int gateNo);

    // 활성 위험 사건을 조치 완료 상태로 변경한다.
    @Update("""
        UPDATE gate_pdm
        SET action_status = 'COMPLETED',
            action_note = #{actionNote},
            action_by_member_no = #{memberNo},
            action_completed_at = CURRENT_TIMESTAMP
        WHERE pdm_no = #{pdmNo}
          AND action_status = 'ACTION_REQUIRED'
    """)
    int completeAction(
            @Param("pdmNo") long pdmNo,
            @Param("memberNo") int memberNo,
            @Param("actionNote") String actionNote
    );
}
