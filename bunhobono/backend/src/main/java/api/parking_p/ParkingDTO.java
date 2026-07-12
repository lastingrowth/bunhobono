package api.parking_p;

import lombok.Data;

@Data
public class ParkingDTO {
    private int parkingNo, parkingSpaces;
    private String parkingName,  parkingLocation;

    //일련번호 vue용
    private int displayNo;
    //현재 주차 가능한 자리 수
    private int availableSpaces;
    //외래키없음
}
