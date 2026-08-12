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

    @Resource
    private AiKnowledgeService aiKnowledgeService;

    private final RestClient restClient;

    public AiChatService() {
        this.restClient = RestClient.create();
    }

    // 사용자의 질문을 받아 Gemini 답변을 반환
    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        String question = validateQuestion(request);

        // DB에 등록된 FAQ 목록 조회
        List<FaqDTO> faqList = faqService.list();

        List<String> knowledgeDocuments = aiKnowledgeService.getDocuments();

        // FAQ 목록과 사용자 질문을 Gemini용 문장으로 생성
        String prompt = createPrompt(
                question,
                faqList,
                knowledgeDocuments
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

            // Gemini 응답 JSON에서 실제 답변 추출 후 답변 가능 여부 분기
            String answer = extractAnswer(responseBody);

            return modelResponse(answer);

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

    // FAQ, 프로젝트 안내 문서와 사용자 질문을 하나의 문장으로 생성
    private String createPrompt(
            String question,
            List<FaqDTO> faqList,
            List<String> knowledgeDocuments
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                너는 아파트 주차관리 시스템의 전문 상담 챗봇이다.

                다음 규칙을 반드시 지킨다.
                1. FAQ에 정확한 답변이 있으면 FAQ를 우선 사용한다.
                2. FAQ에 없으면 프로젝트 안내 문서를 근거로 답변한다.
                3. 제공된 자료에 없는 사실을 추측하거나 만들어내지 않는다.
                4. 사용자별 실제 데이터 조회나 관리자 판단이 필요한 질문은 답을 추측하지 않는다.
                5. 다른 입주민의 개인정보, 차량번호, 연락처, 출입 기록은 제공하지 않는다.
                6. 비밀번호, 인증번호, API 키, JWT 같은 보안정보를 요청하거나 출력하지 않는다.
                7. 개발 코드나 DB 구조가 아니라 사용자가 이해하기 쉬운 한국어로 간결하게 답한다.

                답변 근거가 충분하면 반드시 다음 형식으로 반환한다.
                ANSWER: 답변 내용

                다음 중 하나에 해당하면 다른 설명 없이 NEED_INQUIRY만 반환한다.
                - FAQ와 프로젝트 안내 문서에 답변 근거가 없는 경우
                - 사용자별 실제 차량, 계정, 결제, 입출차 상태를 직접 조회해야 하는 경우
                - 관리자 판단이나 예외 처리가 필요한 경우
                - 개인정보 또는 민감정보 제공을 요구하는 경우

                ## FAQ

                """);

        if (faqList == null || faqList.isEmpty()) {
            prompt.append("등록된 FAQ가 없습니다.\n\n");
        } else {
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
        }

        prompt.append("## 프로젝트 안내 문서\n\n");

        if (knowledgeDocuments == null
                || knowledgeDocuments.isEmpty()) {
            prompt.append("등록된 프로젝트 안내 문서가 없습니다.\n\n");
        } else {
            for (String document : knowledgeDocuments) {
                prompt.append(document)
                        .append("\n\n");
            }
        }

        prompt.append("## 사용자 질문\n\n");
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

    // Gemini가 반환한 제어 문자열을 실제 사용자 응답으로 변환
    private AiChatResponseDTO modelResponse(String answer) {
        String normalizedAnswer = answer
                .replace("```", "")
                .trim();

        if ("NEED_INQUIRY".equalsIgnoreCase(normalizedAnswer)) {
            return response(
                    "안내 자료에서 정확한 답변을 찾지 못했습니다. "
                            + "1:1 문의 화면에서 문의를 등록해 주세요.",
                    false,
                    "NEED_INQUIRY"
            );
        }

        if (normalizedAnswer.regionMatches(
                true,
                0,
                "ANSWER:",
                0,
                "ANSWER:".length()
        )) {
            normalizedAnswer = normalizedAnswer
                    .substring("ANSWER:".length())
                    .trim();
        }

        if (normalizedAnswer.isBlank()) {
            return response(
                    "안내 자료에서 정확한 답변을 찾지 못했습니다. "
                            + "1:1 문의 화면에서 문의를 등록해 주세요.",
                    false,
                    "NEED_INQUIRY"
            );
        }

        return response(
                normalizedAnswer,
                false,
                "ANSWER"
        );
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
                        true,
                        "FAQ_FALLBACK"
                );
            }
        }

        return response(
                "현재 AI 상담 서비스에 연결할 수 없습니다. "
                        + "잠시 후 다시 시도하거나 1:1 문의를 이용해 주세요.",
                true,
                "AI_UNAVAILABLE"
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
            boolean fallback,
            String responseType
    ) {
        AiChatResponseDTO response =
                new AiChatResponseDTO();

        response.setAnswer(answer);
        response.setFallback(fallback);
        response.setResponseType(responseType);

        return response;
    }
}
