package api.cameradata_p;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class
CameraDataDTO {
    private Integer vehicleCarNo;
    private int cameraDataNo, cameraNo, displayNo;
    private String carNo, imagePath;
    private Timestamp captureTime;
    private Boolean recognitionState;
    private Double confidenceScore;
}
