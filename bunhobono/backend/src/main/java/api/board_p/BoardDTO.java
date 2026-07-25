package api.board_p;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class BoardDTO {

    // 공지 내용
    private Integer boardNo;
    private Integer listNo;
    private String title;
    private String content;

    // 이미지
    @JsonIgnore
    private String imagePath;
    private String imageName;
    private String imageType;
    private Boolean hasImage;

    @JsonIgnore
    private Boolean removeImage;

    // 게시 정보
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    private String periodStatus;
    private Boolean active;

    // 작성 정보
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
