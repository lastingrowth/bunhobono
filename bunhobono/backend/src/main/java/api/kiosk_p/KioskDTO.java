package api.kiosk_p;

import lombok.Data;

import java.time.LocalDate;

@Data
public class KioskDTO {
    private int displayNo;         // 화면에 표시하는 순번
    private int kioskNo;           // 실제 키오스크 PK
    private int parkingNo;         // 주차장 FK
    private String parkingName;    // 주차장 이름
    private String modelName;      // 키오스크 모델명
    private String kioskLocation;  // 설치 위치
    private LocalDate installDate; // 설치일
}
