package api.notice_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeDTO {
    private Integer displayNo;
    private Integer noticeNo;
    private Integer carLogNo;
    private String registeredCarNo;
    private String capturedCarNo;
    private String carKind;
    private LocalDateTime detectAt;
    private Integer stayDays;
    private String alertStat;

    private Integer handledByMemberNo;
    private String handledByMemberName;
    private LocalDateTime handledAt;

    private LocalDateTime inTime;
    private LocalDateTime outTime;
    private String parkingName;

    //스냅샷 칼럼
    private Integer snapshotCarLogNo;
    private String snapshotRegisteredCarNo;
    private String snapshotCapturedCarNo;
    private String snapshotCarKind;
    private String snapshotParkingName;
    private LocalDateTime snapshotInTime;
}
