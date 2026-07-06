package api.parking_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ParkingMapper {

    @Select("SELECT ROW_NUMBER() OVER (ORDER BY p.parking_no) AS display_no, " +
            "       p.parking_no, " +
            "       p.parking_name, " +
            "       p.parking_spaces, " +
            "       p.parking_location, " +
            "       p.parking_spaces - COUNT(cl.car_log_no) AS available_spaces " +
            "FROM parking p " +
            "LEFT JOIN gate g ON p.parking_no = g.parking_no " +
            "LEFT JOIN car_log cl ON g.gate_no = cl.in_gate_no AND cl.out_time IS NULL " +
            "GROUP BY p.parking_no, p.parking_name, p.parking_spaces, p.parking_location " +
            "ORDER BY p.parking_no")
    List<ParkingDTO> list(ParkingDTO dto);


    @Insert("INSERT INTO parking (parking_name, parking_spaces, parking_location) " +
            "VALUES (#{parkingName}, #{parkingSpaces}, #{parkingLocation})")
    int insert(ParkingDTO dto);

    @Select("SELECT * FROM parking WHERE parking_no = #{parkingNo}")
    ParkingDTO detail(int parkingNo);

    @Delete("DELETE FROM parking WHERE parking_no = #{parkingNo}")
    int delete(int parkingNo);

    @Update("UPDATE parking " +
            "SET parking_name = #{parkingName}, " +
            "    parking_spaces = #{parkingSpaces}, " +
            "    parking_location = #{parkingLocation} " +
            "WHERE parking_no = #{parkingNo}")
    int updateParking(ParkingDTO dto);
}
