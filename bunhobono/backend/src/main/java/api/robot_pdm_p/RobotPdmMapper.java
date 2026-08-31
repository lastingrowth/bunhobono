package api.robot_pdm_p;

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
public interface RobotPdmMapper {

    // DB에 저장된 로봇 예지보전 결과를 최신순으로 조회한다.
    @Select(
            "SELECT pdm_no, robot_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "predicted_at, action_status, action_note, action_by_member_no, " +
                    "(SELECT m.mem_name FROM member m WHERE m.member_no = robot_pdm.action_by_member_no) AS action_by_member_name, " +
                    "action_started_at, action_completed_at " +
                    "FROM robot_pdm " +
                    "ORDER BY predicted_at DESC, pdm_no DESC"
    )
    @Results(id = "robotPdmResult", value = {
            @Result(column = "sensor_values", property = "sensorValues",
                    typeHandler = SensorValuesJsonTypeHandler.class)
    })
    List<RobotPdmDTO> list();

    // 예지보전 결과 고유번호로 저장된 결과 한 건을 조회한다.
    @Select(
            "SELECT pdm_no, robot_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "predicted_at, action_status, action_note, action_by_member_no, " +
                    "(SELECT m.mem_name FROM member m WHERE m.member_no = robot_pdm.action_by_member_no) AS action_by_member_name, " +
                    "action_started_at, action_completed_at " +
                    "FROM robot_pdm " +
                    "WHERE pdm_no = #{pdmNo}"
    )
    @ResultMap("robotPdmResult")
    RobotPdmDTO detail(long pdmNo);

    // FastAPI 모델의 예측 결과를 robot_pdm 테이블에 저장한다.
    @Insert(
            "INSERT INTO robot_pdm (" +
                    "robot_no, risk_score, risk_level, " +
                    "normal_probability, warning_probability, " +
                    "critical_probability, expected_risk_level, " +
                    "prediction_correct, source_row_index, sensor_values, sensor_collected_at, " +
                    "action_status" +
                    ") VALUES (" +
                    "#{robotNo}, #{riskScore}, #{riskLevel}, " +
                    "#{normalProbability}, #{warningProbability}, " +
                    "#{criticalProbability}, #{expectedRiskLevel}, " +
                    "#{predictionCorrect}, #{sourceRowIndex}, " +
                    "#{sensorValues,typeHandler=api.predictive_maintenance_p.SensorValuesJsonTypeHandler}, #{sensorCollectedAt}, " +
                    "COALESCE(#{actionStatus}, 'NOT_REQUIRED')" +
                    ") ON CONFLICT DO NOTHING"
    )
    int savePrediction(RobotPdmDTO dto);

    // 로봇에 현재 조치가 필요한 위험 사건을 조회한다.
    @Select("""
        SELECT pdm_no, robot_no, risk_score, risk_level,
               normal_probability, warning_probability,
               critical_probability, expected_risk_level,
               prediction_correct, source_row_index, sensor_values, sensor_collected_at,
               predicted_at, action_status, action_note, action_by_member_no,
               (SELECT m.mem_name FROM member m WHERE m.member_no = robot_pdm.action_by_member_no) AS action_by_member_name,
               action_started_at, action_completed_at
        FROM robot_pdm
        WHERE robot_no = #{robotNo}
          AND risk_level = '위험'
          AND action_status = 'ACTION_REQUIRED'
        ORDER BY pdm_no DESC
        LIMIT 1
    """)
    @ResultMap("robotPdmResult")
    RobotPdmDTO findActiveAction(int robotNo);

    // 활성 위험 사건을 조치 완료 상태로 변경한다.
    @Update("""
        UPDATE robot_pdm
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
