package api.camera_pdm_p;

import api.predictive_maintenance_p.PredictiveMaintenanceResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/camera-pdm")
public class CameraPdmController {

    @Resource
    private CameraPdmService cameraPdmService;

    // 카메라 예지보전 결과 전체 조회
    @GetMapping
    public List<CameraPdmDTO> list() {
        return cameraPdmService.list();
    }

    // 카메라 예지보전 결과 상세 조회
    @GetMapping("/{pdmNo}")
    public CameraPdmDTO detail(
            @PathVariable long pdmNo
    ) {
        return cameraPdmService.detail(pdmNo);
    }

    // 카메라 예지보전 결과 직접 저장
    @PostMapping
    public int savePrediction(
            @RequestBody CameraPdmDTO dto
    ) {
        return cameraPdmService.savePrediction(dto);
    }

    // FastAPI 카메라 모델 단건 수동 실행
    @PostMapping("/analyze")
    public PredictiveMaintenanceResponseDTO analyzeOne() {
        return cameraPdmService.analyzeOne();
    }

    // 카메라 1~12의 최신 예측 결과 조회
    @GetMapping("/latest")
    public List<CameraPdmDTO> latest() {
        return cameraPdmService.getLatestPredictions();
    }

    // FastAPI 카메라 1~12 모델 일괄 수동 실행
    @GetMapping("/recent/{cameraNo}")
    public List<CameraPdmDTO> recent(
            @PathVariable int cameraNo
    ) {
        return cameraPdmService.getRecentPredictions(cameraNo);
    }

    @PostMapping("/analyze-all")
    public List<PredictiveMaintenanceResponseDTO> analyzeAll() {
        return cameraPdmService.analyzeAll();
    }
}
