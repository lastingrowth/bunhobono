package api.cameradata_p;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class
CameraDataDTO {
    private Integer vehicleCarNo;
    private int cameraDataNo, displayNo;
    private String carNo, imagePath;
    private Timestamp captureTime;
    private Boolean recognitionState;
    private Double confidenceScore;

    // 카메라 인식 화면에서 함께 보여줄 등록 차량 정보
    private String vehicleType;
    private String vehicleStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    //외래키
    private int cameraNo;
}
