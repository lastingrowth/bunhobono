package api.camera_p;

import lombok.Data;

import java.util.Date;

@Data
public class
CameraDTO {
    private int cameraNo, gateNo;
    private String cameraName, cameraType;
    private Date installDate;
    //일련번호
    private int displayNo;
}
