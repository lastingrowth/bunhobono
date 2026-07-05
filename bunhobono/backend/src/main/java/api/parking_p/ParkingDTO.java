package api.parking_p;

import lombok.Data;

@Data
public class ParkingDTO {
    private int parkingNo, displayNo, availableSpaces, parkingSpaces;
    private String parkingName,  parkingLocation;
}
