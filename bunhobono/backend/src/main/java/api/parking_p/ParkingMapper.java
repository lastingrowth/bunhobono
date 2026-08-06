package api.parking_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ParkingMapper {

    // 주차장별 현재 주차 가능 대수를 조회한다.
    @Select("""
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY parking.parking_no
            ) AS display_no,
            parking.parking_no,
            parking.parking_code,
            parking.parking_name,
            parking.parking_type,
            parking.parking_spaces,
            parking.parking_location,
            parking.active,

            CASE
                WHEN parking.parking_type = 'ROBOT' THEN (
                    SELECT COUNT(*)::INT
                    FROM parking_space space
                    WHERE space.parking_no = parking.parking_no
                      AND space.space_type = 'PARKING'
                      AND space.car_log_no IS NULL
                      AND space.active = TRUE

                      -- 진행 중인 입차 작업의 도착 주차면은 제외
                      AND NOT EXISTS (
                          SELECT 1
                          FROM robot_task task
                          WHERE task.dropoff_space_no = space.space_no
                            AND task.task_status IN (
                                'WAITING',
                                'RUNNING'
                            )
                      )
                )

                WHEN parking.parking_type = 'GENERAL' THEN
                    GREATEST(
                        parking.parking_spaces - (
                            SELECT COUNT(*)::INT
                            FROM car_log car_log
                            JOIN gate gate
                                ON car_log.in_gate_no = gate.gate_no
                            WHERE gate.parking_no = parking.parking_no
                              AND car_log.out_time IS NULL
                        ),
                        0
                    )

                ELSE parking.parking_spaces
            END AS available_spaces

        FROM parking parking
        WHERE parking.active = TRUE
        ORDER BY parking.parking_no
    """)
    List<ParkingDTO> list();

    // 새로운 주차장을 등록한다.
    @Insert("""
        INSERT INTO parking (
            parking_code,
            parking_name,
            parking_type,
            parking_spaces,
            parking_location,
            active
        )
        VALUES (
            #{parkingCode},
            #{parkingName},
            #{parkingType},
            #{parkingSpaces},
            #{parkingLocation},
            TRUE
        )
    """)
    int insert(ParkingDTO dto);

    // 주차장을 비활성화한다.
    @Update("""
        UPDATE parking
        SET active = FALSE
        WHERE parking_no = #{parkingNo}
          AND active = TRUE
    """)
    int delete(int parkingNo);

    // 주차장 정보를 수정한다.
    @Update("""
        UPDATE parking
        SET parking_code = #{parkingCode},
            parking_name = #{parkingName},
            parking_type = #{parkingType},
            parking_spaces = #{parkingSpaces},
            parking_location = #{parkingLocation}
        WHERE parking_no = #{parkingNo}
          AND active = TRUE
    """)
    int updateParking(ParkingDTO dto);
}