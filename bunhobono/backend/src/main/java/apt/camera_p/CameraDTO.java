package apt.camera_p;

import lombok.Data;

import java.util.Date;

@Data
public class
CameraDTO {
    int cameraNo,parkingNo,gateNo;
    String cameraName, cameraType;
    Date installDate;
}
