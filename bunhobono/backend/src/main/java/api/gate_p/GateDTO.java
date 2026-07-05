package api.gate_p;

import lombok.Data;

@Data
public class GateDTO {
    private int gateNo, parkingNo;
    private String gateName, gateType, parkingName, parkingLocation;
}
