package api.camera_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CameraMapper {

    @Select("select * from Camera order by camera_no")
    List<CameraDTO> list(CameraDTO dto);

    @Insert("INSERT INTO camera (parking_no, gate_no, camera_name, camera_type, install_date) " +
            "VALUES (#{parkingNo}, #{gateNo}, #{cameraName}, #{cameraType}, #{installDate})")
    @Options(useGeneratedKeys = true, keyProperty = "cameraNo")
    int insert(CameraDTO dto);

    @Select("SELECT * FROM camera WHERE camera_no = #{cameraNo}")
    CameraDTO detail(int cameraNo);
}
