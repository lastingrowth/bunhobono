package api.parking_space_p;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ParkingSpaceDTO {

    private Long spaceNo;
    private Integer parkingNo;
    private Integer gateNo;
    private Integer carLogNo;

    private String spaceCode;
    private String spaceType;

    private boolean active;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 주차면 목록 조회 정보
    private String parkingCode;
    private String gateCode;
    private String carNo;
    private String carKind;
}