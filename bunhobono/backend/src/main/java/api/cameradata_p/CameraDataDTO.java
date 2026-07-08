package api.cameradata_p;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class
CameraDataDTO {
    Integer  vehicleNo;
    int cameraDataNo, cameraNo;
    String carNo;
    Timestamp captureTime;
    String imagePath;
    Boolean recognitionState;
    Double confidenceScore;
}
