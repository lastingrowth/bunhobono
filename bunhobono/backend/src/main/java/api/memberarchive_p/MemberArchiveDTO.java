package api.memberarchive_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberArchiveDTO {

    // member_archive 테이블의 고유 번호
    private Long archiveNo;

    // archive로 옮겨지기 전 member 테이블의 member_no
    private Long originalMemberNo;

    // 전출 확정 당시 회원 정보
    private String loginId;
    private String memName;
    private String memPhone;
    private String role;
    private String memStatus;

    // 전출 확정 당시 동/호수
    private Integer memDong;
    private Integer memHo;

    // 기존 회원 가입일
    private LocalDateTime createAt;

    // 전출 처리일
    private LocalDateTime deleteAt;

    // archive에 보관된 시각
    private LocalDateTime archivedAt;
}