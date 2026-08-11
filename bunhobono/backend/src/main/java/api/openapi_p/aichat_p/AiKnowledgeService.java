package api.openapi_p.aichat_p;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiKnowledgeService {

    private final List<String> documents = new ArrayList<>();

    @PostConstruct
    public void loadDocuments() {
        documents.clear();

        try {
            PathMatchingResourcePatternResolver resolver =
                    new PathMatchingResourcePatternResolver();

            Resource[] resources =
                    resolver.getResources(
                            "classpath*:ai-knowledge/*.md"
                    );

            for (Resource resource : resources) {
                try (InputStream inputStream =
                             resource.getInputStream()) {

                    String content =
                            new String(
                                    inputStream.readAllBytes(),
                                    StandardCharsets.UTF_8
                            );

                    documents.add(
                            "[문서: "
                                    + resource.getFilename()
                                    + "]\n"
                                    + content
                    );
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI 안내 문서를 불러오지 못했습니다.",
                    e
            );
        }
    }

    public List<String> getDocuments() {
        return List.copyOf(documents);
    }
}