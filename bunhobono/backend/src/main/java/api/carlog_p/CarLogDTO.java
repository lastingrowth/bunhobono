package api.carlog_p;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CarLogDTO {

    // car_log 기본 컬럼
    private Integer carLogNo;
    private Integer vehicleCarNo;
    private Integer inGateNo;
    private Integer outGateNo;
    private LocalDateTime inTime;
    private LocalDateTime outTime;

    // vehicle_car 조인 컬럼
    private String carNo;
    private String vehicleType;
    private String vehicleStatus;

    // gate 조인 컬럼
    private String inGateName;
    private String outGateName;

    // parking 조인 컬럼
    private Integer parkingNo;
    private String parkingName;

    // 검색 조건
    private Integer gateNo;
    private String parkingState;
    private String carKind;
    private String sort;
}