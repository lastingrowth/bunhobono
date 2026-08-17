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

    // FastAPI 게이트 모델 단건 수동 실행
    @PostMapping("/analyze")
    public PredictiveMaintenanceResponseDTO analyzeOne() {
        return gatePdmService.analyzeOne();
    }

    // 게이트 1~10의 최신 예측 결과 조회
    @GetMapping("/latest")
    public List<GatePdmDTO> latest() {
        return gatePdmService.getLatestPredictions();
    }

    // 특정 게이트의 최근 실시간 분석 결과 조회
    @GetMapping("/recent/{gateNo}")
    public List<GatePdmDTO> recent(
            @PathVariable int gateNo
    ) {
        return gatePdmService.getRecentPredictions(gateNo);
    }

    // FastAPI 게이트 1~10 모델 일괄 수동 실행
    @PostMapping("/analyze-all")
    public List<PredictiveMaintenanceResponseDTO> analyzeAll() {
        return gatePdmService.analyzeAll();
    }
}
