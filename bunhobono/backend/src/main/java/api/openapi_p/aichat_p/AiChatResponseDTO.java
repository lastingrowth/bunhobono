package api.openapi_p.aichat_p;

import lombok.Data;

@Data
public class AiChatResponseDTO {

    // 사용자에게 보여줄 챗봇 답변
    private String answer;

    // Gemini 호출 실패로 대체 답변을 사용했는지 여부
    private boolean fallback;
}
