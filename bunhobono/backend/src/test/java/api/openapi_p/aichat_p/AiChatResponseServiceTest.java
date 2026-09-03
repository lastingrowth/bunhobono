package api.openapi_p.aichat_p;

import api.faq_p.FaqService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class AiChatResponseServiceTest {
    @Mock FaqService faqService;
    @Mock AiKnowledgeService aiKnowledgeService;
    @InjectMocks AiChatService service;
    MockRestServiceServer server;
    @BeforeEach void setup() {
        var builder=RestClient.builder(); server=MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service,"restClient",builder.build());
        ReflectionTestUtils.setField(service,"baseUrl","http://test.invalid");
        ReflectionTestUtils.setField(service,"apiKey","unit-test-dummy");
        ReflectionTestUtils.setField(service,"model","test-model");
        when(faqService.list()).thenReturn(List.of());
        when(aiKnowledgeService.getDocuments()).thenReturn(List.of("테스트 안내"));
    }
    private AiChatResponseDTO chat() {var q=new AiChatRequestDTO(); q.setQuestion("  안내해 주세요  "); return service.chat(q);}

    @ParameterizedTest(name="{index}: {0}")
    @CsvSource({"ANSWER: 안내합니다,ANSWER,안내합니다", "안내합니다,ANSWER,안내합니다", "NEED_INQUIRY,NEED_INQUIRY,문의", "ANSWER:,NEED_INQUIRY,문의", "```need_inquiry```,NEED_INQUIRY,문의"})
    @DisplayName("UT-BE-AICHAT-005 | 모델 응답의 제어 문자열을 사용자 답변 및 문의 안내로 변환한다")
    void transformsResponse(String answer,String type,String fragment) {
        server.expect(requestTo("http://test.invalid/models/test-model:generateContent"))
          .andExpect(method(HttpMethod.POST)).andExpect(header("x-goog-api-key","unit-test-dummy"))
          .andExpect(content().string(org.hamcrest.Matchers.containsString("테스트 안내")))
          .andRespond(withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\""+answer+"\"}]}}]}",MediaType.APPLICATION_JSON));
        var result=chat();
        assertThat(result.getResponseType()).isEqualTo(type);
        assertThat(result.getAnswer()).contains(fragment);
        assertThat(result.isFallback()).isFalse(); server.verify();
    }
    static Stream<String> invalidResponses() { return Stream.of("", "{}", "{\"candidates\":[]}", "{\"candidates\":[42]}", "{\"candidates\":[{}]}", "{\"candidates\":[{\"content\":{}}]}", "{\"candidates\":[{\"content\":{\"parts\":[42]}}]}", "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\" \"}]}}]}"); }
    @ParameterizedTest(name="{index}: malformed model response") @MethodSource("invalidResponses")
    @DisplayName("UT-BE-AICHAT-006 | 누락·잘못된 모델 응답 형식은 이용불가 대체 응답으로 변환한다")
    void malformedResponses(String json) {
        server.expect(requestTo("http://test.invalid/models/test-model:generateContent")).andRespond(withSuccess(json,MediaType.APPLICATION_JSON));
        var result=chat(); assertThat(result.isFallback()).isTrue(); assertThat(result.getResponseType()).isEqualTo("AI_UNAVAILABLE"); server.verify();
    }
    @Test @DisplayName("UT-BE-AICHAT-007 | 모델 서버 503 장애 시 외부 오류 대신 이용불가 안내를 반환한다")
    void serverFailure() {
        server.expect(requestTo("http://test.invalid/models/test-model:generateContent")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(chat().getResponseType()).isEqualTo("AI_UNAVAILABLE"); server.verify();
    }
}
