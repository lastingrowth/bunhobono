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

    // FastAPI 카메라 모델 수동 실행
    @PostMapping("/analyze")
    public PredictiveMaintenanceResponseDTO analyze() {
        return cameraPdmService.analyzeAndSave();
    }
}
