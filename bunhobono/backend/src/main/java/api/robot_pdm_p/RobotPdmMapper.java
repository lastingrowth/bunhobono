package api.robot_pdm_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RobotPdmMapper {

    // 예지보전 결과 전체 조회
    @Select("""
        SELECT
            pdm_no,
            robot_log_no,
            risk_score,
            risk_level,
            normal_probability,
            warning_probability,
            critical_probability,
            prediction_reason,
            model_version,
            predicted_at
        FROM robot_pdm
        ORDER BY
            predicted_at DESC,
            pdm_no DESC
    """)
    List<RobotPdmDTO> list();

    // 예지보전 결과 상세 조회
    @Select("""
        SELECT
            pdm_no,
            robot_log_no,
            risk_score,
            risk_level,
            normal_probability,
            warning_probability,
            critical_probability,
            prediction_reason,
            model_version,
            predicted_at
        FROM robot_pdm
        WHERE pdm_no = #{pdmNo}
    """)
    RobotPdmDTO detail(long pdmNo);

    // 로봇 원시 로그의 예지보전 결과 저장
    @Insert("""
        INSERT INTO robot_pdm (
            robot_log_no,
            risk_score,
            risk_level,
            normal_probability,
            warning_probability,
            critical_probability,
            prediction_reason,
            model_version
        )
        VALUES (
            #{robotLogNo},
            #{riskScore},
            #{riskLevel},
            #{normalProbability},
            #{warningProbability},
            #{criticalProbability},
            #{predictionReason},
            #{modelVersion}
        )
        ON CONFLICT (robot_log_no)
        DO UPDATE SET
            risk_score = EXCLUDED.risk_score,
            risk_level = EXCLUDED.risk_level,
            normal_probability =
                EXCLUDED.normal_probability,
            warning_probability =
                EXCLUDED.warning_probability,
            critical_probability =
                EXCLUDED.critical_probability,
            prediction_reason =
                EXCLUDED.prediction_reason,
            model_version =
                EXCLUDED.model_version,
            predicted_at = CURRENT_TIMESTAMP
    """)
    int savePrediction(RobotPdmDTO dto);
}