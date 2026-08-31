package api.camera_pdm_p;

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
public interface CameraPdmMapper {

    // 카메라 예지보전 결과 전체 조회
    @Select(
            "SELECT pdm_no, camera_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "predicted_at, action_status, action_note, action_by_member_no, " +
                    "(SELECT m.mem_name FROM member m WHERE m.member_no = camera_pdm.action_by_member_no) AS action_by_member_name, " +
                    "action_started_at, action_completed_at " +
                    "FROM camera_pdm " +
                    "ORDER BY predicted_at DESC, pdm_no DESC"
    )
    @Results(id = "cameraPdmResult", value = {
            @Result(column = "sensor_values", property = "sensorValues",
                    typeHandler = SensorValuesJsonTypeHandler.class)
    })
    List<CameraPdmDTO> list();

    // 카메라 예지보전 결과 상세 조회
    @Select(
            "SELECT pdm_no, camera_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "predicted_at, action_status, action_note, action_by_member_no, " +
                    "(SELECT m.mem_name FROM member m WHERE m.member_no = camera_pdm.action_by_member_no) AS action_by_member_name, " +
                    "action_started_at, action_completed_at " +
                    "FROM camera_pdm " +
                    "WHERE pdm_no = #{pdmNo}"
    )
    @ResultMap("cameraPdmResult")
    CameraPdmDTO detail(long pdmNo);

    // 카메라 예지보전 결과 저장
    @Insert(
            "INSERT INTO camera_pdm (" +
                    "camera_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "action_status" +
                    ") VALUES (" +
                    "#{cameraNo}, #{riskScore}, #{riskLevel}, " +
                    "#{normalProbability}, #{warningProbability}, " +
                    "#{criticalProbability}, #{expectedRiskLevel}, " +
                    "#{predictionCorrect}, #{sourceRowIndex}, " +
                    "#{sensorValues,typeHandler=api.predictive_maintenance_p.SensorValuesJsonTypeHandler}, #{sensorCollectedAt}, " +
                    "COALESCE(#{actionStatus}, 'NOT_REQUIRED')" +
                    ") ON CONFLICT DO NOTHING"
    )
    int savePrediction(CameraPdmDTO dto);

    // 카메라에 현재 조치가 필요한 위험 사건을 조회한다.
    @Select("""
        SELECT pdm_no, camera_no, risk_score, risk_level,
               normal_probability, warning_probability,
               critical_probability, expected_risk_level,
               prediction_correct, source_row_index, sensor_values, sensor_collected_at,
               predicted_at, action_status, action_note, action_by_member_no,
               (SELECT m.mem_name FROM member m WHERE m.member_no = camera_pdm.action_by_member_no) AS action_by_member_name,
               action_started_at, action_completed_at
        FROM camera_pdm
        WHERE camera_no = #{cameraNo}
          AND risk_level = '위험'
          AND action_status = 'ACTION_REQUIRED'
        ORDER BY pdm_no DESC
        LIMIT 1
    """)
    @ResultMap("cameraPdmResult")
    CameraPdmDTO findActiveAction(int cameraNo);

    // 활성 위험 사건을 조치 완료 상태로 변경한다.
    @Update("""
        UPDATE camera_pdm
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
