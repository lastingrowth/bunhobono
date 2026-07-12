package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {
    //list
    @Select("SELECT ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no) AS display_no, " +
            "       vc.vehicle_car_no, " +
            "       vc.vehicle_type, " +
            "       vc.car_no, " +
            "       vc.vehicle_status, " +
            "       vc.start_date, " +
            "       vc.end_date, " +
            "       vc.member_no, " +
            "       m.mem_name AS approved_member_name, " +
            "       vc.approved_at " +
            "FROM vehicle_car vc " +
            "LEFT JOIN member m ON vc.member_no = m.member_no " +
            "ORDER BY vc.vehicle_car_no")
    List<VehicleDTO> list();

    //추가
    @Insert("INSERT INTO vehicle_car (vehicle_type, car_no, start_date, end_date) " +
            "VALUES (#{vehicleType}, #{carNo}, #{startDate}, #{endDate})")
    int insert(VehicleDTO dto);

    // 승인 대기·승인 상태이며 아직 만료되지 않은 같은 차량번호가 있는지 확인
    @Select("SELECT COUNT(*) FROM vehicle_car " +
            "WHERE car_no = #{carNo} " +
            "AND vehicle_status IN ('WAITING', 'APPROVED') " +
            "AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)")
    int countActiveByCarNo(String carNo);
    //삭제
    @Delete("DELETE FROM vehicle_car WHERE vehicle_car_no = #{vehicleCarNo}")
    int delete(int vehicleCarNo);
    //수정
    @Update("UPDATE vehicle_car " +
            "SET vehicle_type = #{vehicleType}, " +
            "    car_no = #{carNo}, " +
            "    start_date = #{startDate}, " +
            "    end_date = #{endDate} " +
            "WHERE vehicle_car_no = #{vehicleCarNo}")
    int update(VehicleDTO dto);

    // 관리자 차량 상태 변경
    @Update("UPDATE vehicle_car " +
            "SET vehicle_status = #{vehicleStatus}, " +
            "    member_no = COALESCE(#{memberNo}, member_no), " +
            "    start_date = #{startDate}, " +
            "    end_date = #{endDate}, " +
            "    approved_at = CASE WHEN #{vehicleStatus} = 'APPROVED' THEN CURRENT_TIMESTAMP ELSE approved_at END " +
            "WHERE vehicle_car_no = #{vehicleCarNo}")
    int updateStatus(VehicleDTO dto);
}
