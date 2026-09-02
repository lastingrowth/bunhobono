package api.openapi_p.aichat_p;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class AiKnowledgeServiceTest {
    @Test @DisplayName("UT-BE-AIKNOWLEDGE-001 클래스패스 안내 문서를 읽어 불변 목록으로 제공한다")
    void loadDocuments() { AiKnowledgeService service = new AiKnowledgeService(); service.loadDocuments(); assertThat(service.getDocuments()).isNotNull(); assertThatThrownBy(() -> service.getDocuments().add("변조")).isInstanceOf(UnsupportedOperationException.class); }
}
