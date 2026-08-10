package api.openapi_p.aichat_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-chat")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    // 사용자의 질문을 받아 챗봇 답변 반환
    @PostMapping
    public AiChatResponseDTO chat(@RequestBody AiChatRequestDTO request) {
        return aiChatService.chat(request);
    }
}
