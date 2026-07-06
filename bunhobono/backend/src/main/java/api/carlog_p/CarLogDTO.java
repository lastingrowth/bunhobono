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

    // 입차 게이트 조인 컬럼
    private String inGateName;

    // 출차 게이트 조인 컬럼
    private String outGateName;

    // parking 조인 컬럼
    private Integer parkingNo;
    private String parkingName;

    // 요금/결제 조인 컬럼
    private Integer fee;
    private String chargeStatus;
    private String paymentStatus;

    // 위반 조인 컬럼
    private Integer wrongCarNo;
    private String wrongReasonType;
    private String wrongDescription;

    // 검색 조건
    private Integer gateNo;
    private Integer parkingNoSearch;

    private String parkingState;
    private String carKind;
    private String carNoSearch;
    private String sort;
}