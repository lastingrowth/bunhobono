package api.openapi_p.aichat_p;

import lombok.Data;

@Data
public class AiChatResponseDTO {

    // 사용자에게 보여줄 챗봇 답변
    private String answer;

    // Gemini 호출 실패 후 대체 응답을 사용했는지 여부
    private boolean fallback;

    // ANSWER, NEED_INQUIRY, FAQ_FALLBACK, AI_UNAVAILABLE
    private String responseType;
}
