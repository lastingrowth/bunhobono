package api.camera_p;

import api.parking_p.ParkingDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CameraMapper {

    @Select("select * from Camera order by camera_no")
    List<CameraDTO> list(CameraDTO dto);

    @Insert("INSERT INTO camera (parking_no, gate_no, camera_name, camera_type, install_date) " +
            "VALUES (#{parkingNo}, #{gateNo}, #{cameraName}, #{cameraType}, #{installDate})")
    @Options(useGeneratedKeys = true, keyProperty = "cameraNo")
    int insert(CameraDTO dto);

    @Delete("DELETE FROM camera WHERE camera_no = #{cameraNo}")
    int delete(int cameraNo);

    @Update("UPDATE camera " +
            "SET parking_no = #{parkingNo}, " +
            "    gate_no = #{gateNo}, " +
            "    camera_name = #{cameraName}, " +
            "    camera_type = #{cameraType}, " +
            "    install_date = #{installDate} " +
            "WHERE camera_no = #{cameraNo}")
    int update(CameraDTO dto);
}


