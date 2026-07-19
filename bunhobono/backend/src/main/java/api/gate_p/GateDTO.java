package api.gate_p;

import lombok.Data;

@Data
public class GateDTO {
    private int gateNo;

    private String gateName, gateType, parkingName, parkingLocation;

    //외래키
    private int parkingNo;
    //일련번호
    private int displayNo;

    // 게이트 상태
    // 0 = 닫힘, 1 = 열림
    private int gateStatus;
}
