package api.gate_p;

import lombok.Data;

@Data
public class GateDTO {
    private int gateNo, gateStatus;
    private String gateName, gateType, parkingName, parkingLocation;

    //외래키
    private int parkingNo;
    //일련번호
    private int displayNo;
}
