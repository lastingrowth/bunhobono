package api.notice_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeDTO {
    private Integer displayNo;
    private Integer noticeNo;
    private Integer carLogNo;
    private String carNo;
    private LocalDateTime detectAt;
    private Integer stayDays;
    private String alertStat;

    private Integer handledByMemberNo;
    private String handledByMemberName;
    private LocalDateTime handledAt;

    private LocalDateTime inTime;
    private String parkingName;
}
