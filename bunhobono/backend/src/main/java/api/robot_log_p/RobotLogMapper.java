package api.robot_log_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RobotLogMapper {

    // 로봇 원시 상태값 저장
    @Insert("""
        INSERT INTO robot_log (
            source_event_id,
            robot_no,
            task_no,
            robot_status,
            task_phase,
            payload_state,
            drive_motor_temperature_c,
            drive_motor_current_a,
            drive_vibration_mm_s,
            battery_voltage_v,
            battery_temperature_c,
            battery_level,
            obstacle_detected,
            safety_stop,
            alarm_code,
            sampled_at
        )
        VALUES (
            CAST(#{sourceEventId} AS UUID),
            #{robotNo},
            #{taskNo},
            #{robotStatus},
            #{taskPhase},
            #{payloadState},
            #{driveMotorTemperatureC},
            #{driveMotorCurrentA},
            #{driveVibrationMmS},
            #{batteryVoltageV},
            #{batteryTemperatureC},
            #{batteryLevel},
            #{obstacleDetected},
            #{safetyStop},
            #{alarmCode},
            #{sampledAt}
        )
        ON CONFLICT (source_event_id)
        DO NOTHING
    """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "robotLogNo",
            keyColumn = "robot_log_no"
    )
    int insert(RobotLogDTO dto);

    // 로봇별·작업별 원시 상태값 조회
    @Select("""
        SELECT
            log.*,
            robot.robot_code,
            robot.set_no,
            robot.set_position,
            task.task_type
        FROM robot_log log

        JOIN robot
            ON log.robot_no = robot.robot_no

        LEFT JOIN robot_task task
            ON log.task_no = task.task_no

        WHERE (
            CAST(#{robotNo} AS BIGINT) IS NULL
            OR log.robot_no = CAST(#{robotNo} AS BIGINT)
        )
        AND (
            CAST(#{taskNo} AS BIGINT) IS NULL
            OR log.task_no = CAST(#{taskNo} AS BIGINT)
        )

        ORDER BY
            log.sampled_at DESC,
            log.robot_log_no DESC

        LIMIT 500
    """)
    List<RobotLogDTO> list(
            @Param("robotNo") Long robotNo,
            @Param("taskNo") Long taskNo
    );
}