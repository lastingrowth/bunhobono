package api.inquiry_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InquiryDTO {

    private int inquiryNo;
    private int memberNo;

    private Integer rootInquiryNo;

    private String category;
    private String title;
    private String content;
    private String status;

    private String answerContent;
    private Integer answeredBy;
    private LocalDateTime answeredAt;

    private LocalDateTime createdAt;
    private Integer trashNo;
}
