package api.carlog_p;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class CarLogDTO {

    // 목록 표시값
    private Integer displayNo;
    private String carNo;
    private String carKind;
    private String parkingState;

    // 입출차 기록
    private Integer carLogNo;
    private Integer vehicleCarNo;

    private Integer cameraDataNo;
    private Integer outCameraDataNo;

    private Integer inGateNo;
    private String inGateName;
    private LocalDateTime inTime;

    private Integer outGateNo;
    private String outGateName;
    private LocalDateTime outTime;

    private Integer freeTime;

    // 입차 당시 차량 정보
    private String snapshotCarNo;
    private String snapshotCarKind;

    // 차량 조회값
    private String vehicleType;
    private String vehicleStatus;

    // 주차장 조회값
    private Integer parkingNo;
    private String parkingCode;
    private String parkingName;

    // 현재 주차면 조회값
    private String spaceCode;
    private String spaceType;
    private OffsetDateTime spaceUpdatedAt;

    // 목록 검색 조건
    @JsonIgnore
    private Integer gateNo;

    @JsonIgnore
    private String lastFourDigits;

    @JsonIgnore
    private String sort;

    // 카메라 게이트 처리용
    @JsonIgnore
    private String gateType;

    @JsonIgnore
    private String gateArea;
}