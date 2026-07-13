package api.resident_dashboard_p;

import api.carlog_p.CarLogDTO;
import api.vehicle_p.VehicleDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
// resident_vehicle의 소유 관계를 기준으로 본인 차량과 최근 입출차 기록을 조회한다.
public interface ResidentDashboardMapper {

    // 차량 상세 정보는 기존 vehicle_car에서 가져오고 소유 관계만 resident_vehicle에서 확인한다.
    // 로그인한 입주민 차량의 최근 입출차 및 주차장 위치를 최대 5건 조회한다.
    @Select("""
        SELECT ROW_NUMBER() OVER (ORDER BY rv.created_at DESC) AS display_no,
               vc.vehicle_car_no,
               vc.vehicle_type,
               vc.car_no,
               vc.vehicle_status,
               vc.start_date,
               vc.end_date,
               vc.approved_at
        FROM resident_vehicle rv
        JOIN vehicle_car vc ON vc.vehicle_car_no = rv.vehicle_car_no
        WHERE rv.login_id = #{loginId}
        ORDER BY rv.created_at DESC
        """)
    List<VehicleDTO> findVehicles(String loginId);

    // 차량을 등록하고 PostgreSQL이 생성한 차량 번호를 반환한다.
    @Select("""
        INSERT INTO vehicle_car (vehicle_type, car_no, start_date, end_date)
        VALUES (#{vehicleType}, #{carNo}, #{startDate}, #{endDate})
        RETURNING vehicle_car_no
        """)
    int insertVehicle(VehicleDTO dto);

    // 등록 차량과 로그인한 입주민의 소유 관계를 독립 테이블에 저장한다.
    @Insert("""
        INSERT INTO resident_vehicle (login_id, vehicle_car_no)
        VALUES (#{loginId}, #{vehicleCarNo})
        ON CONFLICT (login_id, vehicle_car_no) DO NOTHING
        """)
    int linkVehicle(@Param("loginId") String loginId,
                    @Param("vehicleCarNo") int vehicleCarNo);

    // 수정·삭제 요청 차량이 로그인한 입주민 소유인지 확인한다.
    @Select("""
        SELECT EXISTS (
            SELECT 1 FROM resident_vehicle
            WHERE login_id = #{loginId}
              AND vehicle_car_no = #{vehicleCarNo}
        )
        """)
    boolean ownsVehicle(@Param("loginId") String loginId,
                        @Param("vehicleCarNo") int vehicleCarNo);

    @Update("""
        UPDATE vehicle_car
        SET vehicle_type = #{vehicleType},
            car_no = #{carNo},
            start_date = CASE
                WHEN #{vehicleType} = 'visit' THEN CAST(#{startDate} AS TIMESTAMP)
                ELSE start_date
            END,
            end_date = CASE
                WHEN #{vehicleType} = 'visit' THEN CAST(#{endDate} AS TIMESTAMP)
                ELSE CAST(NULL AS TIMESTAMP)
            END
        WHERE vehicle_car_no = #{vehicleCarNo}
        """)
    int updateVehicle(VehicleDTO dto);

    @Delete("DELETE FROM vehicle_car WHERE vehicle_car_no = #{vehicleCarNo}")
    int deleteVehicle(int vehicleCarNo);

    @Delete("DELETE FROM resident_vehicle WHERE login_id = #{loginId} AND vehicle_car_no = #{vehicleCarNo}")
    int unlinkVehicle(@Param("loginId") String loginId,
                      @Param("vehicleCarNo") int vehicleCarNo);

    @Select("""
        SELECT cl.car_log_no,
               vc.car_no,
               cl.vehicle_car_no,
               vc.vehicle_type,
               vc.vehicle_status,
               p.parking_no,
               p.parking_name,
               cl.in_gate_no,
               ig.gate_name AS in_gate_name,
               cl.in_time,
               cl.out_gate_no,
               og.gate_name AS out_gate_name,
               cl.out_time,
               CASE WHEN cl.out_time IS NULL THEN 'PARKING' ELSE 'OUT' END AS parking_state
        FROM resident_vehicle rv
        JOIN vehicle_car vc ON vc.vehicle_car_no = rv.vehicle_car_no
        JOIN car_log cl ON cl.vehicle_car_no = vc.vehicle_car_no
        LEFT JOIN gate ig ON ig.gate_no = cl.in_gate_no
        LEFT JOIN gate og ON og.gate_no = cl.out_gate_no
        LEFT JOIN parking p ON p.parking_no = ig.parking_no
        WHERE rv.login_id = #{loginId}
        ORDER BY cl.in_time DESC
        LIMIT 5
        """)
    List<CarLogDTO> findRecentCarLogs(String loginId);
}
