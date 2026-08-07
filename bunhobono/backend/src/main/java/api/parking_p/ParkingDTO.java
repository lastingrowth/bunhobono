package api.parking_p;

import lombok.Data;

@Data
public class ParkingDTO {

    private int parkingNo;
    private int parkingSpaces;
    private int displayNo;
    private int availableSpaces;

    private String parkingCode;
    private String parkingName;
    private String parkingType;
    private String parkingLocation;

    private boolean active;
}