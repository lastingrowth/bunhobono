package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    // 차량 전체 목록 조회
    @Select("""
        SELECT
            vehicle_car_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> list();

    // 차량번호 검색
    @Select("""
        SELECT
            vehicle_car_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE car_no LIKE CONCAT('%', #{carNo}, '%')
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> search(String carNo);

    // 차량 상세 조회
    @Select("""
        SELECT
            vehicle_car_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleNo}
    """)
    VehicleDTO detail(Integer vehicleNo);

    // 차량 등록
    @Insert("""
        INSERT INTO vehicle_car (
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        )
        VALUES (
            #{vehicleType},
            #{carNo},
            #{vehicleStatus},
            #{startDate},
            #{endDate},
            #{approvedNo},
            #{approvedAt}
        )
    """)
    int insert(VehicleDTO dto);

    // 차량 정보 수정
    @Update("""
        UPDATE vehicle_car
        SET
            vehicle_type = #{vehicleType},
            car_no = #{carNo},
            vehicle_status = #{vehicleStatus},
            start_date = #{startDate},
            end_date = #{endDate},
            approved_no = #{approvedNo},
            approved_at = #{approvedAt}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int update(VehicleDTO dto);

    // 차량 삭제
    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleNo}
    """)
    int delete(Integer vehicleNo);

    // 승인 대기 차량 목록
    @Select("""
        SELECT
            vehicle_car_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE vehicle_status = 'WAITING'
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> approveList();

    // 승인 상태 변경
    @Update("""
        UPDATE vehicle_car
        SET
            vehicle_status = #{vehicleStatus},
            start_date = #{startDate},
            end_date = #{endDate},
            approved_no = #{approvedNo},
            approved_at = #{approvedAt}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int changeStatus(VehicleDTO dto);
}