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
    private Integer freeTime;

    private LocalDateTime inTime;
    private LocalDateTime outTime;

    // vehicle_car 조인 정보
    private String carNo;
    private String vehicleType;      // normal, visit
    private String vehicleStatus;    // WAITING, APPROVED, EXPIRED, UNKNOWN

    // 입차 게이트 정보
    private String inGateName;

    // 출차 게이트 정보
    private String outGateName;

    // parking 조인 정보
    private Integer parkingNo;
    private String parkingName;

    // 화면 표시용 가공 상태
    private String parkingState;     // PARKING, OUT
    private String carKind;          // REGISTERED, VISIT, UNKNOWN

    // 검색 조건용
    private Integer gateNo;
    private String sort;             // latest, oldest
    private Integer dong;
    private Integer ho;

    // 화면출력용
    private String inTimeText, outTimeText, parkingStateText, carKindText,
            inGateText, outGateText, feeText;
    private Integer fee;
    // 주차 시간 계산
    private String parkingTimeText;
}