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

    // OCR 차량번호로 현재 입출차 가능한 승인 차량 조회
    @Select("""
    SELECT vc.vehicle_car_no
    FROM vehicle_car vc
    WHERE vc.car_no = #{carNo}
      AND vc.vehicle_status = 'APPROVED'
      AND (
            -- 일반 등록차량은 기존 등록기간 적용
            (
                vc.vehicle_type = 'normal'
                AND (
                    vc.start_date IS NULL
                    OR vc.start_date <= CURRENT_TIMESTAMP
                )
                AND (
                    vc.end_date IS NULL
                    OR vc.end_date > CURRENT_TIMESTAMP
                )
            )

            OR

            -- 방문차량은 예상 방문시간 1시간 전부터
            -- 예상 방문시간 1시간 후까지 최초 입차 가능
            (
                vc.vehicle_type = 'visit'
                AND CURRENT_TIMESTAMP
                    BETWEEN vc.start_date - INTERVAL '1 hour'
                        AND vc.start_date + INTERVAL '1 hour'
            )

            OR

            -- 이미 입차하여 주차 중인 차량은
            -- 시간이 초과되어도 출차할 수 있도록 조회
            EXISTS (
                SELECT 1
                FROM car_log cl
                WHERE cl.vehicle_car_no = vc.vehicle_car_no
                  AND cl.out_time IS NULL
            )
          )
    ORDER BY vc.vehicle_car_no DESC
    LIMIT 1
""")
    Integer findVehicleCarNo(String carNo);


    // OCR 별칭 차량번호로 현재 입출차 가능한 승인 차량 조회
    @Select("""
    SELECT vc.vehicle_car_no,
           vc.car_no
    FROM vehicle_car vc
    WHERE vc.alias_car_no = #{aliasCarNo}
      AND vc.vehicle_status = 'APPROVED'
      AND (
            -- 일반 등록차량은 기존 등록기간 적용
            (
                vc.vehicle_type = 'normal'
                AND (
                    vc.start_date IS NULL
                    OR vc.start_date <= CURRENT_TIMESTAMP
                )
                AND (
                    vc.end_date IS NULL
                    OR vc.end_date > CURRENT_TIMESTAMP
                )
            )

            OR

            -- 방문차량은 예상 방문시간 앞뒤 1시간 동안 최초 입차 가능
            (
                vc.vehicle_type = 'visit'
                AND CURRENT_TIMESTAMP
                    BETWEEN vc.start_date - INTERVAL '1 hour'
                        AND vc.start_date + INTERVAL '1 hour'
            )

            OR

            -- 이미 입차한 차량은 등록시간이 지나도 출차 가능
            EXISTS (
                SELECT 1
                FROM car_log cl
                WHERE cl.vehicle_car_no = vc.vehicle_car_no
                  AND cl.out_time IS NULL
            )
          )
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
