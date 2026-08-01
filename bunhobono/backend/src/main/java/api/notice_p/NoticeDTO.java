package api.notice_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NoticeDTO {

    private Integer displayNo;
    private Integer noticeNo;
    private String noticeType;

    // 알림 원본
    private Integer carLogNo;
    private Integer cameraDataNo;

    // 차량 표시 정보
    private String registeredCarNo;
    private String capturedCarNo;
    private String carKind;

    // 알림 발생 및 초과 정보
    private LocalDateTime detectAt;
    private LocalDateTime dueAt;
    private Long overdueMinutes;

    // 기존 프론트 호환용 계산 필드
    private Integer stayDays;
    private LocalDateTime expectedOutTime;

    // 처리 상태
    private String alertStat;
    private Integer handledByMemberNo;
    private String handledByMemberName;
    private LocalDateTime handledAt;

    // 입출차 및 위치 정보
    private LocalDateTime inTime;
    private LocalDateTime outTime;
    private String parkingName;

    // OCR 확인용
    private String imagePath;
    private BigDecimal confidenceScore;

    // 원본 삭제 후에도 알림 내용을 유지하기 위한 스냅샷
    private Integer snapshotCarLogNo;
    private Integer snapshotCameraDataNo;
    private String snapshotRegisteredCarNo;
    private String snapshotCapturedCarNo;
    private String snapshotCarKind;
    private String snapshotParkingName;
    private LocalDateTime snapshotInTime;
    private String snapshotImagePath;
    private BigDecimal snapshotConfidenceScore;
}