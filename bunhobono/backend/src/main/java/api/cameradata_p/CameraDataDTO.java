package api.cameradata_p;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class
CameraDataDTO {
    private Integer vehicleCarNo;
    private int cameraDataNo, displayNo;
    private String carNo, imagePath;
    private String ocrCarNo;
    private Timestamp captureTime;
    private Boolean recognitionState;
    private Double confidenceScore;

    // 카메라 인식 화면에서 함께 보여줄 등록 차량 정보
    private String vehicleType;
    private String vehicleStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime gateOpenedAt;

    // 등록 차량 소유 회원의 세대 연결 정보
    // 값이 있으면 입주민 차량, NULL이면 관리실 등록 차량이다.
    private Integer apartmentUnitNo;

    //외래키
    private int cameraNo;

    // OCR 저장 처리 결과
    private Boolean saved;
    private Boolean registered;
    private Boolean gateOpened;
    private Boolean saveAlias;
    private Boolean aliasSaved;
    private Boolean autoCorrected;
    private Integer gateNo;

    //crop 이미지 저장
    private String cropImagePath;
    
    //메모
    private String camNote;
    public record PageResponse(
            List<CameraDataDTO> items,
            long totalCount,
            int page,
            int size,
            int totalPages
    ) {
    }}
