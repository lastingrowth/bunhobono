package api.gate_pdm_p;

import api.predictive_maintenance_p.PredictiveMaintenanceResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/gate-pdm")
public class GatePdmController {

    @Resource
    private GatePdmService gatePdmService;

    // 게이트 예지보전 결과 전체 조회
    @GetMapping
    public List<GatePdmDTO> list() {
        return gatePdmService.list();
    }

    // 게이트 예지보전 결과 상세 조회
    @GetMapping("/{pdmNo}")
    public GatePdmDTO detail(
            @PathVariable long pdmNo
    ) {
        return gatePdmService.detail(pdmNo);
    }

    // 게이트 예지보전 결과 직접 저장
    @PostMapping
    public int savePrediction(
            @RequestBody GatePdmDTO dto
    ) {
        return gatePdmService.savePrediction(dto);
    }

    // FastAPI 게이트 모델 수동 실행
    @PostMapping("/analyze")
    public GatePdmDTO analyze() {
        return gatePdmService.analyzeAndSave();
    }

    // 게이트별 최신 예지보전 상태 조회
    @GetMapping("/current")
    public List<GatePdmDTO> current() {
        return gatePdmService.getLatestPredictions();
    }
}
