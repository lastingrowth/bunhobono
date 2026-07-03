package apt.vehicle_p;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface VehicleMapper {

    @Select(
            " SELECT * FROM vehicle_car " +
            " ORDER BY vehicle_car_no" )
    List<VehicleDTO> list();
}