package api.cameradata_p;

import lombok.Data;

import java.nio.file.Path;
import java.sql.Timestamp;

@Data
public class
CameraDataDTO {
    private int cameraDataNo, cameraNo, vehicleNo, displayNo;
    private String carNo, imagePath;
    private Timestamp captureTime;
    private Boolean recognitionState;
    private Double confidenceScore;
}
