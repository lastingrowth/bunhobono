package api.faq_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FaqDTO {

    private int faqNo;

    private String category;
    private String question;
    private String answer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
