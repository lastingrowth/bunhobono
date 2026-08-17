package api.predictive_maintenance_p;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;
import java.util.List;

@Service
public class PredictiveMaintenanceClient {

    private final RestClient restClient;

    public PredictiveMaintenanceClient(
            @Value("${fast-api.base-url}")
            String fastApiBaseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .build();
    }

    // 게이트 테스트 CSV의 다음 행을 예측한다.
    public PredictiveMaintenanceResponseDTO predictNextGate() {
        return restClient.post()
                .uri(
                        "/demo/predictive-maintenance/gate/next"
                )
                .retrieve()
                .body(
                        PredictiveMaintenanceResponseDTO.class
                );
    }

    // FastAPI에서 게이트 1~10의 다음 예측 결과를 한 묶음으로 받는다.
    public List<PredictiveMaintenanceResponseDTO> predictNextGates() {
        return restClient.post()
                .uri(
                        "/demo/predictive-maintenance/gate/next-all"
                )
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<PredictiveMaintenanceResponseDTO>
                                >() {}
                );
    }

    // 카메라 테스트 CSV의 다음 행을 예측한다.
    public PredictiveMaintenanceResponseDTO predictNextCamera() {
        return restClient.post()
                .uri(
                        "/demo/predictive-maintenance/camera/next"
                )
                .retrieve()
                .body(
                        PredictiveMaintenanceResponseDTO.class
                );
    }

    // 로봇 테스트 CSV의 다음 행을 예측한다.
    public PredictiveMaintenanceResponseDTO predictNextRobot() {
        return restClient.post()
                .uri(
                        "/demo/predictive-maintenance/robot/next"
                )
                .retrieve()
                .body(
                        PredictiveMaintenanceResponseDTO.class
                );
    }

    // FastAPI에서 카메라 1~12의 다음 예측 결과를 한 묶음으로 받는다.
    public List<PredictiveMaintenanceResponseDTO> predictNextCameras() {
        return restClient.post()
                .uri(
                        "/demo/predictive-maintenance/camera/next-all"
                )
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<PredictiveMaintenanceResponseDTO>
                                >() {}
                );
    }
}
