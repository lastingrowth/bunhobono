package api.cameradata_p;


import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CameraDataMapper {

    @Select("SELECT ROW_NUMBER() OVER (ORDER BY cd.camera_data_no DESC) AS display_no, " +
            "cd.camera_data_no, cd.camera_no, cd.vehicle_car_no, cd.car_no, cd.capture_time, " +
            "vc.vehicle_type, vc.vehicle_status, vc.start_date, vc.end_date " +
            "FROM camera_data cd LEFT JOIN vehicle_car vc ON cd.vehicle_car_no = vc.vehicle_car_no " +
            "ORDER BY cd.camera_data_no DESC")
    List<CameraDataDTO> list(CameraDataDTO dto);

    @Select("SELECT vehicle_car_no FROM vehicle_car WHERE car_no = #{carNo} ORDER BY vehicle_car_no DESC LIMIT 1")
    Integer findVehicleCarNo(String carNo);

    @Insert("INSERT INTO camera_data (camera_no, vehicle_car_no, car_no, capture_time, image_path, recognition_state, confidence_score) " +
            "VALUES (#{cameraNo}, #{vehicleCarNo}, #{carNo}, #{captureTime}, #{imagePath}, #{recognitionState}, #{confidenceScore})")
    @Options(useGeneratedKeys = true, keyProperty = "cameraDataNo")
    int insert(CameraDataDTO dto);

    @Select("SELECT cd.camera_data_no, cd.camera_no, cd.vehicle_car_no, cd.car_no, cd.capture_time, " +
            "cd.image_path, cd.recognition_state, cd.confidence_score, " +
            "vc.vehicle_type, vc.vehicle_status, vc.start_date, vc.end_date " +
            "FROM camera_data cd LEFT JOIN vehicle_car vc ON cd.vehicle_car_no = vc.vehicle_car_no " +
            "WHERE cd.camera_data_no = #{cameraDataNo}")
    CameraDataDTO detail(int cameraDataNo);

    @Select("SELECT cd.camera_data_no, cd.camera_no, cd.vehicle_car_no, cd.car_no, cd.capture_time, " +
            "cd.image_path, cd.recognition_state, cd.confidence_score, " +
            "vc.vehicle_type, vc.vehicle_status, vc.start_date, vc.end_date " +
            "FROM camera_data cd LEFT JOIN vehicle_car vc ON cd.vehicle_car_no = vc.vehicle_car_no " +
            "WHERE cd.car_no LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY cd.camera_data_no DESC")
    List<CameraDataDTO> searchByCarNo(String keyword);

    @Select("select * from camera_data where capture_time < NOW() - INTERVAL '3 months'")
    List<CameraDataDTO>deleteTarget();
    //1 minute 테스트용 쓰레기통행

    @Delete("delete from camera_data where camera_data_no = #{cameraDataNo}")
    int delete(int cameraDataNo);

}
