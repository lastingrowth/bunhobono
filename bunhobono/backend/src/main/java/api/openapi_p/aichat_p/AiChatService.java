package api.openapi_p.aichat_p;

import api.faq_p.FaqDTO;
import api.faq_p.FaqService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private static final Logger log =
            LoggerFactory.getLogger(AiChatService.class);

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.api-key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Resource
    private FaqService faqService;

    private final RestClient restClient;

    public AiChatService() {
        this.restClient = RestClient.create();
    }

    // 사용자의 질문을 받아 Gemini 답변을 반환
    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        String question = validateQuestion(request);

        // DB에 등록된 FAQ 목록 조회
        List<FaqDTO> faqList = faqService.list();

        // FAQ 목록과 사용자 질문을 Gemini용 문장으로 생성
        String prompt = createPrompt(
                question,
                faqList
        );

        // API 키가 없으면 Gemini를 호출하지 않고 대체 답변 반환
        if (apiKey == null || apiKey.isBlank()) {
            log.warn(
                    "Gemini API 키를 현재 Spring 프로세스에서 찾지 못했습니다."
            );

            return fallback(
                    question,
                    faqList
            );
        }

        try {
            // Gemini generateContent API의 요청 JSON 생성
            Map<String, Object> requestBody =
                    Map.of(
                            "contents",
                            List.of(
                                    Map.of(
                                            "parts",
                                            List.of(
                                                    Map.of(
                                                            "text",
                                                            prompt
                                                    )
                                            )
                                    )
                            )
                    );

            // Gemini API에 질문 전송
            Map<?, ?> responseBody = restClient
                    .post()
                    .uri(
                            baseUrl
                                    + "/models/"
                                    + model
                                    + ":generateContent"
                    )
                    .header(
                            "x-goog-api-key",
                            apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // Gemini 응답 JSON에서 실제 답변 추출
            String answer = extractAnswer(responseBody);

            return response(
                    answer,
                    false
            );

        } catch (Exception e) {
            log.error(
                    "Gemini API 호출에 실패했습니다.",
                    e
            );

            // 호출 제한, 네트워크 오류 등이 발생하면 대체 답변 반환
            return fallback(
                    question,
                    faqList
            );
        }
    }

    // 사용자 질문 입력값 검사
    private String validateQuestion(AiChatRequestDTO request) {
        if (request == null
                || request.getQuestion() == null
                || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문을 입력해 주세요."
            );
        }

        String question = request
                .getQuestion()
                .trim();

        if (question.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문은 500자 이하로 입력해 주세요."
            );
        }

        return question;
    }

    // FAQ 목록과 사용자 질문을 하나의 문장으로 생성
    private String createPrompt(
            String question,
            List<FaqDTO> faqList
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(
                "너는 아파트 주차관리 시스템의 입주민 안내 챗봇이다.\n"
        );
        prompt.append(
                "반드시 아래 FAQ 내용을 기준으로 간단하고 정확하게 답변한다.\n"
        );
        prompt.append(
                "FAQ에서 답을 찾을 수 없으면 추측하지 말고 1:1 문의를 이용하라고 안내한다.\n\n"
        );

        for (FaqDTO faq : faqList) {
            prompt.append("[FAQ]\n");
            prompt.append("분류: ")
                    .append(faq.getCategory())
                    .append("\n");
            prompt.append("질문: ")
                    .append(faq.getQuestion())
                    .append("\n");
            prompt.append("답변: ")
                    .append(faq.getAnswer())
                    .append("\n\n");
        }

        prompt.append("[사용자 질문]\n");
        prompt.append(question);

        return prompt.toString();
    }

    // Gemini 응답 JSON에서 답변 문장 추출
    private String extractAnswer(Map<?, ?> responseBody) {
        if (responseBody == null) {
            throw new IllegalStateException(
                    "Gemini 응답이 없습니다."
            );
        }

        Object candidatesObject =
                responseBody.get("candidates");

        if (!(candidatesObject instanceof List<?> candidates)
                || candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini 답변 후보가 없습니다."
            );
        }

        Object candidateObject =
                candidates.get(0);

        if (!(candidateObject instanceof Map<?, ?> candidate)) {
            throw new IllegalStateException(
                    "Gemini 답변 형식이 올바르지 않습니다."
            );
        }

        Object contentObject =
                candidate.get("content");

        if (!(contentObject instanceof Map<?, ?> content)) {
            throw new IllegalStateException(
                    "Gemini 답변 내용이 없습니다."
            );
        }

        Object partsObject =
                content.get("parts");

        if (!(partsObject instanceof List<?> parts)
                || parts.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini 답변 문장이 없습니다."
            );
        }

        Object partObject =
                parts.get(0);

        if (!(partObject instanceof Map<?, ?> part)) {
            throw new IllegalStateException(
                    "Gemini 답변 문장 형식이 올바르지 않습니다."
            );
        }

        Object textObject =
                part.get("text");

        if (!(textObject instanceof String answer)
                || answer.isBlank()) {
            throw new IllegalStateException(
                    "Gemini 답변이 비어 있습니다."
            );
        }

        return answer.trim();
    }

    // Gemini 호출 실패 시 FAQ 또는 안내 문구 반환
    private AiChatResponseDTO fallback(
            String question,
            List<FaqDTO> faqList
    ) {
        String normalizedQuestion =
                normalize(question);

        for (FaqDTO faq : faqList) {
            String normalizedFaqQuestion =
                    normalize(faq.getQuestion());

            if (normalizedQuestion.contains(normalizedFaqQuestion)
                    || normalizedFaqQuestion.contains(normalizedQuestion)) {
                return response(
                        faq.getAnswer(),
                        true
                );
            }
        }

        return response(
                "현재 AI 답변을 불러올 수 없습니다. "
                        + "자주하는 질문을 확인하거나 1:1 문의를 이용해 주세요.",
                true
        );
    }

    // 질문 비교를 위해 공백과 문장부호 제거
    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll(
                        "[\\s?!.,]",
                        ""
                )
                .toLowerCase();
    }

    // 최종 챗봇 응답 DTO 생성
    private AiChatResponseDTO response(
            String answer,
            boolean fallback
    ) {
        AiChatResponseDTO response =
                new AiChatResponseDTO();

        response.setAnswer(answer);
        response.setFallback(fallback);

        return response;
    }
}