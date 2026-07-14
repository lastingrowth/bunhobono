package api.carlog_p;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CarLogDTO {

    // 목록 응답 순서
    private Integer displayNo;
    private Integer carLogNo;
    private String carNo;
    private String carKind;
    private String parkingState;

    private Integer vehicleCarNo;
    private String vehicleType;
    private String vehicleStatus;

    private Integer parkingNo;
    private String parkingName;

    private Integer cameraDataNo;
    private Integer inGateNo;
    private String inGateName;
    private LocalDateTime inTime;

    private Integer outGateNo;
    private String outGateName;
    private LocalDateTime outTime;

    // 목록 검색 조건
    @JsonIgnore
    private Integer gateNo;
    private String sort;

    // camera_data 자동 처리용 게이트 정보
    @JsonIgnore
    private String gateType;

    //스냅샷 칼럼
    private String snapshotCarNo;
}
