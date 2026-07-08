package api.cameradata_p;


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CameraDataMapper {

    @Select("select * from Camera_data order by camera_data_no")
    List<CameraDataDTO> list(CameraDataDTO dto);

    @Select("select vehicle_no from vehicle_car where car_no = #{carNo} limit 1")
    Integer findVehicleNo(String carNo);

    @Insert("INSERT INTO camera_data (camera_no, vehicle_no, car_no, capture_time, image_path, recognition_state, confidence_score) " +
                "VALUES (#{cameraNo}, #{vehicleNo}, #{carNo}, #{captureTime}, #{imagePath}, #{recognitionState}, #{confidenceScore})")
//    @Options(useGeneratedKeys = true, keyProperty = "cameraDataNo")
    int insert(CameraDataDTO dto);

    @Select("SELECT * FROM camera_data WHERE camera_data_no = #{cameraDataNo}")
    CameraDataDTO detail(int cameraDataNo);
}

