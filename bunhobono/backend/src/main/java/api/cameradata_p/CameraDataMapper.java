package api.cameradata_p;


import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CameraDataMapper {

    @Select("SELECT ROW_NUMBER() OVER (ORDER BY camera_data_no) AS display_no,camera_data_no, car_no, capture_time FROM camera_data ")
    List<CameraDataDTO> list(CameraDataDTO dto);

    @Select("select vehicle_no from vehicle_car where car_no = #{carNo} limit 1")
    Integer findVehicleNo(String carNo);

    @Insert("INSERT INTO camera_data (camera_no, vehicle_no, car_no, capture_time, image_path, recognition_state, confidence_score) " +
                "VALUES (#{cameraNo}, #{vehicleNo}, #{carNo}, #{captureTime}, #{imagePath}, #{recognitionState}, #{confidenceScore})")
//    @Options(useGeneratedKeys = true, keyProperty = "cameraDataNo")
    int insert(CameraDataDTO dto);

    @Select("SELECT * FROM camera_data WHERE camera_data_no = #{cameraDataNo}")
    CameraDataDTO detail(int cameraDataNo);

    @Select("select * from camera_data where capture_time < NOW() - INTERVAL '3 months'")
    List<CameraDataDTO>deleteTarget();

    @Delete("delete from camera_data where camera_data_no = #{cameraDataNo}")
    int delete(int cameraDataNo);
}

