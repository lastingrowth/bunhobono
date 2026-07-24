package api.reset_p;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;

@Service
public class ResetService {

    private final DataSource dataSource;
    private final RestClient fastApiClient;

    public ResetService(DataSource dataSource) {
        this.dataSource = dataSource;

        this.fastApiClient = RestClient.create(
                "http://127.0.0.1:8000"
        );
    }

    public void resetDemo() {
        resetDatabase();
        resetFastApi();
    }

    /**
     * demo_dummy_full.sql을 실행해 DB 데이터를 초기화한다.
     */
    private void resetDatabase() {
        ClassPathResource sqlFile =
                new ClassPathResource(
                        "sql/demo_dummy_full.sql"
                );

        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator();

        // SQL 실행 중 오류가 발생하면 즉시 중단
        populator.setContinueOnError(false);

        // SQL문의 기본 구분자는 세미콜론(;)
        populator.setSeparator(";");

        populator.addScript(sqlFile);
        populator.execute(dataSource);
    }

    /**
     * FastAPI의 영상 및 OCR 상태를 초기화한다.
     */
    private void resetFastApi() {
        fastApiClient.post()
                .uri("/demo/reset")
                .retrieve()
                .toBodilessEntity();
    }
}