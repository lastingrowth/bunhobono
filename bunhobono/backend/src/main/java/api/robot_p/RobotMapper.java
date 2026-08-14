package api.robot_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RobotMapper {

    // 등록된 로봇을 세트와 위치 순서로 조회한다.
    @Select("""
        SELECT *
        FROM robot
        ORDER BY
            set_no,
            set_position
    """)
    List<RobotDTO> list();

    // 로봇 한 대의 현재 상태를 조회한다.
    @Select("""
        SELECT *
        FROM robot
        WHERE robot_no = #{robotNo}
    """)
    RobotDTO detail(long robotNo);

    // 특정 세트에 속한 두 로봇을 조회한다.
    @Select("""
        SELECT *
        FROM robot
        WHERE set_no = #{setNo}
        ORDER BY set_position
    """)
    List<RobotDTO> findBySetNo(int setNo);

    // 두 대 모두 작업 가능 상태인 로봇 세트를 찾는다.
    @Select("""
        SELECT robot.set_no
        FROM robot robot
        WHERE robot.robot_status IN (
                  'STANDBY',
                  'CHARGING'
              )
          AND robot.battery_level >= 30

          AND NOT EXISTS (
              SELECT 1
              FROM robot_task task
              WHERE task.set_no = robot.set_no
                AND task.task_status IN (
                    'WAITING',
                    'RUNNING'
                )
          )

        GROUP BY robot.set_no

        HAVING COUNT(*) = 2
           AND COUNT(
               DISTINCT robot.set_position
           ) = 2

        ORDER BY
            ABS(
                MOD(robot.set_no - 1, 2)
                - MOD(#{preferredSetNo} - 1, 2)
            )
            + ABS(
                (robot.set_no - 1) / 2
                - (#{preferredSetNo} - 1) / 2
            ),
            MIN(robot.battery_level) DESC NULLS LAST,
            robot.set_no

        LIMIT 1
    """)
    Integer findAvailableSetNo(
            @Param("preferredSetNo") int preferredSetNo
    );

    // 대기 또는 충전 중인 로봇 세트의 작업 시작
    @Update("""
        UPDATE robot
        SET robot_status = 'WORKING',
            updated_at = CURRENT_TIMESTAMP
        WHERE set_no = #{setNo}
          AND robot_status IN (
              'STANDBY',
              'CHARGING'
          )
          AND battery_level >= 30
    """)
    int startSet(@Param("setNo") int setNo);

    // 작업을 시작하거나 종료할 때 세트 상태를 변경한다.
    @Update("""
        UPDATE robot
        SET robot_status = #{robotStatus},
            updated_at = CURRENT_TIMESTAMP
        WHERE set_no = #{setNo}
          AND robot_status = #{currentStatus}
    """)
    int updateSetStatus(
            @Param("setNo") int setNo,
            @Param("currentStatus") String currentStatus,
            @Param("robotStatus") String robotStatus
    );

    // 로봇의 현재 상태를 반영한다.
    @Update("""
        UPDATE robot
        SET robot_status = COALESCE(
                #{robotStatus},
                robot_status
            ),
            battery_level = COALESCE(
                #{batteryLevel},
                battery_level
            ),
            operating_hours = COALESCE(
                #{operatingHours},
                operating_hours
            ),
            last_heartbeat_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE robot_no = #{robotNo}
    """)
    int updateState(RobotDTO dto);

    // 충전 시작 대상과 충전 중인 로봇 조회
    @Select("""
        SELECT *
        FROM robot
        WHERE (
                robot_status = 'STANDBY'
                AND battery_level < 70
              )
           OR robot_status IN (
                'CHARGING',
                'LOW_BATTERY'
              )
        ORDER BY
            set_no,
            set_position
    """)
    List<RobotDTO> findChargingRobots();

    // 작업이 없는 로봇의 충전 상태와 잔량 갱신
    @Update("""
        UPDATE robot
        SET robot_status = #{robotStatus},
            battery_level = #{batteryLevel},
            last_heartbeat_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE robot_no = #{robotNo}
          AND robot_status IN (
              'STANDBY',
              'CHARGING',
              'LOW_BATTERY'
          )
    """)
    int updateChargingState(RobotDTO dto);

    // 로봇 점검 완료 시각 갱신
    @Update("""
    UPDATE robot
    SET last_maintenance_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE robot_no = #{robotNo}
    """)
    int completeMaintenance(
            @Param("robotNo") long robotNo
    );

    // 주차로봇 등록
    @Insert("INSERT INTO robot " +
            "(robot_code, set_no, set_position, robot_status, battery_level, operating_hours) " +
            "VALUES " +
            "(#{robotCode}, #{setNo}, #{setPosition}, 'STANDBY', 100, 0)")
    int insert(RobotDTO dto);

    // 사용 이력이 없는 주차로봇 삭제
    @Delete("DELETE FROM robot " +
            "WHERE robot_no = #{robotNo} " +
            "AND robot_status <> 'WORKING' " +
            "AND NOT EXISTS " +
            "(SELECT 1 FROM robot_log WHERE robot_no = #{robotNo})")
    int delete(long robotNo);
}
