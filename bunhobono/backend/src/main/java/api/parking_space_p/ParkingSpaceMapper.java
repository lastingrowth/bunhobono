package api.parking_space_p;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ParkingSpaceMapper {

    // 주차장에 등록된 공간과 현재 차량 조회
    @Select("""
        SELECT
            space.space_no,
            space.parking_no,
            space.gate_no,
            space.car_log_no,
            space.space_code,
            space.space_type,
            space.active,
            space.created_at,
            space.updated_at,
            parking.parking_code,
            gate.gate_code,
            car_log.car_no,
            car_log.snapshot_car_kind AS car_kind,
            car_log.in_time
        FROM parking_space space

        JOIN parking
            ON space.parking_no = parking.parking_no

        LEFT JOIN gate
            ON space.gate_no = gate.gate_no

        LEFT JOIN car_log_detail car_log
            ON space.car_log_no = car_log.car_log_no

        WHERE parking.parking_code = #{parkingCode}
          AND parking.active = TRUE
          AND space.active = TRUE

        ORDER BY
            CASE
                WHEN gate.gate_code IS NULL THEN 999
                ELSE RIGHT(gate.gate_code, 1)::INT
            END,
            CASE space.space_type
                WHEN 'ENTRY_WAIT' THEN 1
                WHEN 'EXIT_WAIT' THEN 2
                ELSE 3
            END,
            space.space_code
    """)
    List<ParkingSpaceDTO> list(String parkingCode);

    // 비어 있고 작업에 배정되지 않은 대기면 조회
    @Select("""
        SELECT space.*
        FROM parking_space space
        WHERE space.gate_no = #{gateNo}
          AND space.space_type = #{spaceType}
          AND space.car_log_no IS NULL
          AND space.active = TRUE

          AND NOT EXISTS (
              SELECT 1
              FROM robot_task task
              WHERE task.dropoff_space_no = space.space_no
                AND task.task_status IN (
                    'WAITING',
                    'RUNNING'
                )
          )

        ORDER BY
            CASE
                WHEN space.space_type = 'EXIT_WAIT'
                    THEN SUBSTRING(
                        space.space_code
                        FROM '[0-9]+$'
                    )::INT
            END DESC,
            RANDOM()
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    """)
    ParkingSpaceDTO findEmptyWaitingSpace(
            @Param("gateNo") int gateNo,
            @Param("spaceType") String spaceType
    );

    // 비어 있고 작업에 배정되지 않은 주차면 조회
    @Select("""
        SELECT space.*
        FROM parking_space space

        JOIN gate entry_gate
            ON entry_gate.gate_no = #{entryGateNo}

        WHERE space.parking_no = #{parkingNo}
          AND space.space_type = 'PARKING'
          AND space.car_log_no IS NULL
          AND space.active = TRUE

          AND NOT EXISTS (
              SELECT 1
              FROM robot_task task
              WHERE task.dropoff_space_no = space.space_no
                AND task.task_status IN (
                    'WAITING',
                    'RUNNING'
                )
          )

        ORDER BY
            CASE
                WHEN RIGHT(entry_gate.gate_code, 1) = '1'
                    THEN MOD(
                        SUBSTRING(
                            space.space_code
                            FROM '[0-9]+$'
                        )::INT - 1,
                        20
                    )
                ELSE 19 - MOD(
                    SUBSTRING(
                        space.space_code
                        FROM '[0-9]+$'
                    )::INT - 1,
                    20
                )
            END,
            (
                SUBSTRING(
                    space.space_code
                    FROM '[0-9]+$'
                )::INT - 1
            ) / 20,
            space.space_code
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    """)
    ParkingSpaceDTO findEmptyParkingSpace(
            @Param("parkingNo") int parkingNo,
            @Param("entryGateNo") int entryGateNo
    );

    // 차량 입출차 기록으로 현재 위치 조회
    @Select("""
        SELECT
            space.*,
            parking.parking_code,
            gate.gate_code,
            car_log.car_no,
            car_log.snapshot_car_kind AS car_kind
        FROM parking_space space

        JOIN parking
            ON space.parking_no = parking.parking_no

        LEFT JOIN gate
            ON space.gate_no = gate.gate_no

        LEFT JOIN car_log_detail car_log
            ON space.car_log_no = car_log.car_log_no

        WHERE space.car_log_no = #{carLogNo}
          AND space.active = TRUE
    """)
    ParkingSpaceDTO findByCarLogNo(
            @Param("carLogNo") int carLogNo
    );

    // 출차대기면 도착 후 10분이 지난 미출차 차량 조회
    @Select("""
        SELECT space.car_log_no
        FROM parking_space space

        JOIN car_log log
            ON log.car_log_no = space.car_log_no

        WHERE space.space_type = 'EXIT_WAIT'
          AND space.car_log_no IS NOT NULL
          AND space.updated_at <= CURRENT_TIMESTAMP - INTERVAL '10 minutes'
          AND space.active = TRUE
          AND log.out_time IS NULL

          AND EXISTS (
              SELECT 1
              FROM robot_task park_out
              WHERE park_out.car_log_no = space.car_log_no
                AND park_out.task_type = 'PARK_OUT'
                AND park_out.task_status = 'COMPLETED'
                AND park_out.dropoff_space_no = space.space_no
          )

          AND NOT EXISTS (
              SELECT 1
              FROM robot_task park_in
              WHERE park_in.car_log_no = space.car_log_no
                AND park_in.task_type = 'PARK_IN'
                AND park_in.task_status IN ('WAITING', 'RUNNING')
          )

        ORDER BY space.updated_at
    """)
    List<Integer> findTimedOutExitWaitCarLogNos();

    // 빈 공간에 차량 입출차 기록 배정
    @Update("""
        UPDATE parking_space
        SET car_log_no = #{carLogNo},
            updated_at = CURRENT_TIMESTAMP
        WHERE space_no = #{spaceNo}
          AND car_log_no IS NULL
          AND active = TRUE
    """)
    int assignCarLog(
            @Param("spaceNo") long spaceNo,
            @Param("carLogNo") int carLogNo
    );

    // 차량이 떠난 공간을 빈자리로 변경
    @Update("""
        UPDATE parking_space
        SET car_log_no = NULL,
            updated_at = CURRENT_TIMESTAMP
        WHERE space_no = #{spaceNo}
          AND car_log_no = #{carLogNo}
          AND active = TRUE
    """)
    int releaseCarLog(
            @Param("spaceNo") long spaceNo,
            @Param("carLogNo") int carLogNo
    );

    //내 차량 조회
    @Select("""
        SELECT
            space.space_no,
            space.space_code,
            space.space_type,
            parking.parking_no,
            parking.parking_code,
            parking.parking_name,
            vehicle.vehicle_car_no,
            vehicle.car_no,
            log.car_log_no,
            log.in_time
        FROM parking_space space
        JOIN car_log log
            ON log.car_log_no = space.car_log_no
        JOIN vehicle_car vehicle
            ON vehicle.vehicle_car_no = log.vehicle_car_no
        JOIN member member
            ON member.member_no = vehicle.member_no
        JOIN parking parking
            ON parking.parking_no = space.parking_no
        WHERE member.login_id = #{loginId}
        AND log.out_time IS NULL
        AND space.active = TRUE
        ORDER BY log.in_time DESC
    """)
    List<ParkingSpaceDTO> myVehicleLocations(
            @Param("loginId") String loginId
    );
}
