package api.cameradata_p;


import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CameraDataMapper {

    @Select("SELECT ROW_NUMBER() OVER (ORDER BY cd.camera_data_no DESC) AS display_no, " +
            "cd.camera_data_no, cd.camera_no, cd.vehicle_car_no, cd.car_no, cd.ocr_car_no, cd.capture_time, " +
            "cd.recognition_state, cd.confidence_score, " +
            "vc.vehicle_type, vc.vehicle_status, vc.start_date, vc.end_date " +
            "FROM camera_data cd LEFT JOIN vehicle_car vc ON cd.vehicle_car_no = vc.vehicle_car_no " +
            "ORDER BY cd.camera_data_no DESC")
    List<CameraDataDTO> list(CameraDataDTO dto);

    @Select("""
        SELECT vehicle_car_no
        FROM vehicle_car
        WHERE car_no = #{carNo}
          AND vehicle_status = 'APPROVED'
          AND (start_date IS NULL OR start_date <= CURRENT_TIMESTAMP)
          AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)
        ORDER BY vehicle_car_no DESC
        LIMIT 1
    """)
    Integer findVehicleCarNo(String carNo);

    @Select("""
        SELECT vc.vehicle_car_no, vc.car_no
        FROM vehicle_car vc
        WHERE vc.alias_car_no = #{aliasCarNo}
          AND vc.vehicle_status = 'APPROVED'
          AND (vc.start_date IS NULL OR vc.start_date <= CURRENT_TIMESTAMP)
          AND (vc.end_date IS NULL OR vc.end_date > CURRENT_TIMESTAMP)
        LIMIT 1
    """)
    CameraDataDTO findApprovedVehicleByAlias(String aliasCarNo);

    @Select("""
        SELECT vehicle_car_no
        FROM vehicle_car
        WHERE alias_car_no = #{aliasCarNo}
    """)
    Integer findAliasVehicleCarNo(String aliasCarNo);

    @Update("""
        UPDATE vehicle_car
        SET alias_car_no = #{aliasCarNo}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int updateAlias(
            @Param("vehicleCarNo") int vehicleCarNo,
            @Param("aliasCarNo") String aliasCarNo
    );

    @Insert("INSERT INTO camera_data (camera_no, vehicle_car_no, car_no, ocr_car_no, capture_time, image_path, crop_image_path, recognition_state, confidence_score) " +
            "VALUES (#{cameraNo}, #{vehicleCarNo}, #{carNo}, #{ocrCarNo}, #{captureTime}, #{imagePath}, #{cropImagePath}, #{recognitionState}, #{confidenceScore})")
    @Options(useGeneratedKeys = true, keyProperty = "cameraDataNo")
    int insert(CameraDataDTO dto);

    @Select("SELECT cd.camera_data_no, cd.camera_no, cd.vehicle_car_no, cd.car_no, cd.ocr_car_no, cd.capture_time, " +
            "cd.image_path, cd.crop_image_path, cd.recognition_state, cd.confidence_score, " +
            "vc.vehicle_type, vc.vehicle_status, vc.start_date, vc.end_date " +
            "FROM camera_data cd LEFT JOIN vehicle_car vc ON cd.vehicle_car_no = vc.vehicle_car_no " +
            "WHERE cd.camera_data_no = #{cameraDataNo}")
    CameraDataDTO detail(int cameraDataNo);

    @Update("""
        UPDATE camera_data
        SET car_no = #{carNo},
            vehicle_car_no = #{vehicleCarNo},
            recognition_state = TRUE
        WHERE camera_data_no = #{cameraDataNo}
    """)
    int updateCarNo(CameraDataDTO dto);

    @Update("""
        UPDATE camera_data
        SET car_no = #{carNo},
            vehicle_car_no = #{vehicleCarNo},
            recognition_state = TRUE
        WHERE camera_data_no = #{cameraDataNo}
    """)
    int applyMatchedCarNo(CameraDataDTO dto);

    @Select("SELECT cd.camera_data_no, cd.camera_no, cd.vehicle_car_no, cd.car_no, cd.ocr_car_no, cd.capture_time, " +
            "cd.image_path, cd.crop_image_path, cd.recognition_state, cd.confidence_score, " +
            "vc.vehicle_type, vc.vehicle_status, vc.start_date, vc.end_date " +
            "FROM camera_data cd LEFT JOIN vehicle_car vc ON cd.vehicle_car_no = vc.vehicle_car_no " +
            "WHERE cd.car_no LIKE CONCAT('%', #{keyword}, '%') " +
            "OR cd.ocr_car_no LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY cd.camera_data_no DESC")
    List<CameraDataDTO> searchByCarNo(String keyword);

    @Select("select * from camera_data where capture_time < NOW() - INTERVAL '3 months'")
    List<CameraDataDTO>deleteTarget();
    //1 minute 테스트용 쓰레기통행

    @Delete("delete from camera_data where camera_data_no = #{cameraDataNo}")
    int delete(int cameraDataNo);

}
