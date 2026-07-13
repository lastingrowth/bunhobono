package api.camera_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CameraMapper {

    @Select("SELECT ROW_NUMBER() OVER (ORDER BY camera_no) AS display_no, " +
            "camera_no, gate_no, camera_name, camera_type, install_date " +
            "FROM camera ORDER BY camera_no")
    List<CameraDTO> list(CameraDTO dto);

    @Insert("INSERT INTO camera (gate_no, camera_name, camera_type, install_date) " +
            "VALUES (#{gateNo}, #{cameraName}, #{cameraType}, #{installDate})")
    @Options(useGeneratedKeys = true, keyProperty = "cameraNo")
    int insert(CameraDTO dto);

    @Delete("DELETE FROM camera WHERE camera_no = #{cameraNo}")
    int delete(int cameraNo);

    @Update("UPDATE camera " +
            "SET gate_no = #{gateNo}, " +
            "    camera_name = #{cameraName}, " +
            "    camera_type = #{cameraType}, " +
            "    install_date = #{installDate} " +
            "WHERE camera_no = #{cameraNo}")
    int update(CameraDTO dto);
}


