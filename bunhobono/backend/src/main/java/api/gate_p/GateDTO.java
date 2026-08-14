package api.gate_p;

import lombok.Data;

@Data
public class GateDTO {

    private int gateNo;

    private Integer parkingNo;

    private String gateCode;
    private String gateName;
    private String gateType;
    private String gateArea;

    private String parkingName;
    private String parkingLocation;

    private int displayNo;

    // 0: 닫힘, 1: 열림
    private int gateStatus;
}
