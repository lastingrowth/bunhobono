package api.parking_p;

import lombok.Data;

@Data
public class ParkingDTO {
    private int parkingNo;
    private String parkingName, parkingSpaces, parkingLocation;
}
