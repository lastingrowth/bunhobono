package apt.cameradata_p;

import lombok.Data;

import java.nio.file.Path;
import java.sql.Timestamp;

@Data
public class
CameraDataDTO {
    int cameraDataNo, cameraNo, vehicleNo;
    String carNo;
    Timestamp captureTime;
    String imagePath;
    Boolean recognitionState;
    Double confidenceScore;
}
