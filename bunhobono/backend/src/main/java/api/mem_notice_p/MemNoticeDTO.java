package api.mem_notice_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemNoticeDTO {

    // 로그인한 입주민의 알림 조회·수정·삭제 조건으로 사용한다.
    // mem_notice 테이블에는 저장하지 않는다.
    private String loginId;

    private Integer memNoticeNo;
    private Integer recipientMemberNo;
    private String referenceTable;
    private Integer referenceNo;
    private String noticeType;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
