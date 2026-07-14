package api.trash_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrashDTO {
    private Long trashNo;           // 휴지통 고유 번호
    private String dataType;        // CAMERA_DATA, CAR_LOG, NOTICE
    private Integer originalNo;     // 원본 테이블의 PK 번호
    private String dataJson;        // 삭제 전 원본 행 전체 JSON
    private String deleteType;      // MANUAL, SCHEDULED
    private LocalDateTime deletedAt; // 휴지통 이동 시각
    private LocalDateTime purgeAt;   // 영구 삭제 예정 시
}
