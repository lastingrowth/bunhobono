package api.camera_p;

import lombok.Data;

import java.util.Date;

/**
 * 카메라 정보 전달에 사용하는 DTO이다.
 *
 * <p>카메라 기본정보와 목록 화면에 표시할 차단기 및 주차장 정보를
 * Controller, Service, Mapper 계층 사이에서 전달한다.</p>
 */
@Data
public class
CameraDTO {
    /** 카메라 고유번호와 연결된 차단기 고유번호 */
    private int cameraNo, gateNo;

    /** 카메라명과 카메라 유형 */
    private String cameraName, cameraType;

    /** 카메라 설치일 */
    private Date installDate;
    /** 목록 화면에 표시할 순번 */
    private int displayNo;

    /** 카메라가 설치된 주차장명과 차단기명 */
    private String parkingName,gateName;

    /** 카메라 사용 여부 */
    private boolean active;
}
