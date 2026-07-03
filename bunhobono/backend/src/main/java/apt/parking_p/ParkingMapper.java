package apt.parking_p;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ParkingMapper {

    @Select("select * from parking order by parking_no")
    List<ParkingDTO> list(ParkingDTO dto);
}
