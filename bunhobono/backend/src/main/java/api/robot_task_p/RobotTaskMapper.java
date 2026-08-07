package api.robot_task_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RobotTaskMapper {

    // 전체 로봇 작업 조회
    @Select("""
        SELECT *
        FROM robot_task_detail
        ORDER BY
            requested_at DESC,
            task_no DESC
    """)
    List<RobotTaskDTO> list();

    // 로봇 작업 상세 조회
    @Select("""
        SELECT *
        FROM robot_task_detail
        WHERE task_no = #{taskNo}
    """)
    RobotTaskDTO detail(
            @Param("taskNo") long taskNo
    );

    // 같은 차량의 진행 중인 동일 작업 조회
    @Select("""
        SELECT *
        FROM robot_task_detail
        WHERE car_log_no = #{carLogNo}
          AND task_type = #{taskType}
          AND task_status IN (
              'WAITING',
              'RUNNING'
          )
        ORDER BY requested_at DESC
        LIMIT 1
    """)
    RobotTaskDTO findActiveTask(
            @Param("carLogNo") int carLogNo,
            @Param("taskType") String taskType
    );

    // 로봇 세트 배정을 기다리는 작업 조회
    @Select("""
        SELECT *
        FROM robot_task
        WHERE task_status = 'WAITING'
          AND set_no IS NULL
        ORDER BY
            priority DESC,
            requested_at,
            task_no
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    """)
    RobotTaskDTO findNextWaitingTask();

    // 가상 로봇이 수행할 배정 작업 조회
    @Select("""
        SELECT *
        FROM robot_task_detail
        WHERE set_no = #{setNo}
          AND task_status = 'WAITING'
        ORDER BY
            priority DESC,
            requested_at,
            task_no
        LIMIT 1
    """)
    RobotTaskDTO findAssignedTask(
            @Param("setNo") int setNo
    );

    // 작업 완료 중복 요청 방지를 위한 잠금 조회
    @Select("""
        SELECT *
        FROM robot_task
        WHERE task_no = #{taskNo}
        FOR UPDATE
    """)
    RobotTaskDTO findByTaskNoForUpdate(
            @Param("taskNo") long taskNo
    );

    // 로봇 작업 등록
    @Insert("""
        INSERT INTO robot_task (
            car_log_no,
            pickup_space_no,
            dropoff_space_no,
            set_no,
            task_type,
            task_phase,
            task_status,
            priority
        )
        VALUES (
            #{carLogNo},
            #{pickupSpaceNo},
            #{dropoffSpaceNo},
            #{setNo},
            #{taskType},
            'WAITING',
            'WAITING',
            COALESCE(#{priority}, 0)
        )
    """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "taskNo",
            keyColumn = "task_no"
    )
    int insert(RobotTaskDTO dto);

    // 작업에 로봇 세트 배정
    @Update("""
        UPDATE robot_task
        SET set_no = #{setNo}
        WHERE task_no = #{taskNo}
          AND task_status = 'WAITING'
          AND set_no IS NULL
    """)
    int assignSet(
            @Param("taskNo") long taskNo,
            @Param("setNo") int setNo
    );

    // 가상 로봇 작업 시작
    @Update("""
        UPDATE robot_task
        SET task_status = 'RUNNING',
            task_phase = 'MOVING_EMPTY',
            started_at = CURRENT_TIMESTAMP
        WHERE task_no = #{taskNo}
          AND task_status = 'WAITING'
          AND set_no IS NOT NULL
    """)
    int start(
            @Param("taskNo") long taskNo
    );

    // 가상 로봇 작업 단계 갱신
    @Update("""
        UPDATE robot_task
        SET task_phase = #{taskPhase}
        WHERE task_no = #{taskNo}
          AND task_status = 'RUNNING'
    """)
    int updatePhase(
            @Param("taskNo") long taskNo,
            @Param("taskPhase") String taskPhase
    );

    // 로봇 작업 완료
    @Update("""
        UPDATE robot_task
        SET task_status = 'COMPLETED',
            task_phase = 'COMPLETED',
            completed_at = CURRENT_TIMESTAMP
        WHERE task_no = #{taskNo}
          AND task_status = 'RUNNING'
    """)
    int complete(
            @Param("taskNo") long taskNo
    );

    // 로봇 작업 실패
    @Update("""
        UPDATE robot_task
        SET task_status = 'FAILED',
            task_phase = 'FAILED',
            failure_reason = #{failureReason},
            completed_at = CURRENT_TIMESTAMP
        WHERE task_no = #{taskNo}
          AND task_status IN (
              'WAITING',
              'RUNNING'
          )
    """)
    int fail(
            @Param("taskNo") long taskNo,
            @Param("failureReason") String failureReason
    );
}