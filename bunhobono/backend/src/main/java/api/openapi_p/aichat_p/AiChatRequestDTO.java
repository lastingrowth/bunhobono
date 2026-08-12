package api.openapi_p.aichat_p;

import lombok.Data;

@Data
public class AiChatRequestDTO {

    // 사용자가 챗봇에 입력한 질문
    private String question;
}
