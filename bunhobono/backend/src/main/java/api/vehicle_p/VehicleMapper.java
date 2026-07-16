package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    @Select("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no ASC) AS display_no,
            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.member_no,
            m.mem_dong AS mem_dong,
            m.mem_ho AS mem_ho,
            m.mem_name AS approved_member_name,
            vc.approved_at,
            cl.in_time AS in_time,
            cl.out_time AS out_time,
            CASE
                WHEN vc.vehicle_type = 'visit'
                     AND cl.in_time IS NOT NULL
                     AND vc.start_date IS NOT NULL
                     AND vc.end_date IS NOT NULL
                THEN cl.in_time + (vc.end_date - vc.start_date)
                ELSE vc.end_date
            END AS real_end_date
        FROM vehicle_car vc
        LEFT JOIN member m
            ON vc.member_no = m.member_no
        LEFT JOIN LATERAL (
            SELECT
                car_log.in_time,
                car_log.out_time
            FROM car_log
            WHERE car_log.vehicle_car_no = vc.vehicle_car_no
            ORDER BY car_log.in_time DESC
            LIMIT 1
        ) cl ON true
        ORDER BY vc.vehicle_car_no ASC
    """)
    List<VehicleDTO> list();


    @Select("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no ASC) AS display_no,
            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.member_no,
            m.mem_dong AS mem_dong,
            m.mem_ho AS mem_ho,
            m.mem_name AS approved_member_name,
            vc.approved_at,
            cl.in_time AS in_time,
            cl.out_time AS out_time,
            CASE
                WHEN vc.vehicle_type = 'visit'
                     AND cl.in_time IS NOT NULL
                     AND vc.start_date IS NOT NULL
                     AND vc.end_date IS NOT NULL
                THEN cl.in_time + (vc.end_date - vc.start_date)
                ELSE vc.end_date
            END AS real_end_date
        FROM vehicle_car vc
        JOIN member m
            ON vc.member_no = m.member_no
        LEFT JOIN LATERAL (
            SELECT
                car_log.in_time,
                car_log.out_time
            FROM car_log
            WHERE car_log.vehicle_car_no = vc.vehicle_car_no
            ORDER BY car_log.in_time DESC
            LIMIT 1
        ) cl ON true
        WHERE m.login_id = #{loginId}
        ORDER BY vc.vehicle_car_no ASC
    """)
    List<VehicleDTO> listByLoginId(String loginId);


    @Insert("""
        INSERT INTO vehicle_car (
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            member_no
        )
        VALUES (
            #{vehicleType},
            #{carNo},
            #{vehicleStatus},
            #{startDate},
            #{endDate},
            #{memberNo}
        )
    """)
    int insert(VehicleDTO dto);


    @Insert("""
        INSERT INTO vehicle_car (
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            member_no
        )
        VALUES (
            #{dto.vehicleType},
            #{dto.carNo},
            #{dto.vehicleStatus},
            #{dto.startDate},
            #{dto.endDate},
            (
                SELECT member_no
                FROM member
                WHERE login_id = #{loginId}
            )
        )
    """)
    int insertResidentVisit(@Param("loginId") String loginId,
                            @Param("dto") VehicleDTO dto);


    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car
        WHERE car_no = #{carNo}
          AND vehicle_status IN ('WAITING', 'APPROVED')
          AND (
                end_date IS NULL
                OR end_date > CURRENT_TIMESTAMP
          )
    """)
    int countActiveByCarNo(String carNo);


    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car vc
        JOIN member m
            ON vc.member_no = m.member_no
        WHERE m.login_id = #{loginId}
          AND vc.vehicle_type = 'visit'
          AND vc.vehicle_status IN ('WAITING', 'APPROVED')
          AND (
                vc.end_date IS NULL
                OR vc.end_date > CURRENT_TIMESTAMP
          )
    """)
    int countActiveVisitByLoginId(String loginId);


    @Update("""
        UPDATE vehicle_car
        SET vehicle_type = #{vehicleType},
            car_no = #{carNo},
            start_date = #{startDate},
            end_date = #{endDate}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int update(VehicleDTO dto);


    @Update("""
        UPDATE vehicle_car
        SET vehicle_status = #{vehicleStatus},
            member_no = COALESCE(#{memberNo}, member_no),
            start_date = #{startDate},
            end_date = #{endDate},
            approved_at = CASE
                WHEN #{vehicleStatus} = 'APPROVED'
                THEN CURRENT_TIMESTAMP
                ELSE approved_at
            END
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int updateStatus(VehicleDTO dto);


    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int delete(int vehicleCarNo);
}