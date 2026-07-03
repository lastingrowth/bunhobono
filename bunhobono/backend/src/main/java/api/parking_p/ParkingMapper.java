package apt.parking_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ParkingMapper {

    @Select("select * from parking order by parking_no")
    List<ParkingDTO> list(ParkingDTO dto);

    @Insert("INSERT INTO parking_area (parking_name, parking_spaces, parking_location) " +
                "VALUES (#{parkingName}, #{parkingSpaces}, #{parkingLocation})")
    int insert(ParkingDTO dto);

    @Select("SELECT * FROM parking WHERE parking_no = #{parkingNo}")
    ParkingDTO detail(int parkingNo);

}
