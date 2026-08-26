package api.robot_pdm_p;

import api.predictive_maintenance_p.PredictiveMaintenanceResponseDTO;
import api.predictive_maintenance_p.PdmActionRequestDTO;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/robot-pdm")
public class RobotPdmController {

    // 로봇 예지보전 조회, 분석, 저장 로직을 담당한다.
    @Resource
    private RobotPdmService robotPdmService;

    // DB에 저장된 로봇 예지보전 결과 전체 조회
    @GetMapping
    public List<RobotPdmDTO> list() {
        return robotPdmService.list();
    }

    // DB에 저장된 로봇 예지보전 결과 상세 조회
    @GetMapping("/{pdmNo}")
    public RobotPdmDTO detail(@PathVariable long pdmNo) {
        return robotPdmService.detail(pdmNo);
    }

    // 로봇 예지보전 결과 직접 저장
    @PostMapping
    public int savePrediction(@RequestBody RobotPdmDTO dto) {
        return robotPdmService.savePrediction(dto);
    }

    // FastAPI 로봇 모델 단건 수동 실행
    @PostMapping("/analyze")
    public PredictiveMaintenanceResponseDTO analyzeOne() {
        return robotPdmService.analyzeOne();
    }

    // 로봇 1~8의 현재 최신 예측 결과 조회
    @GetMapping("/latest")
    public List<RobotPdmDTO> latest() {
        return robotPdmService.getLatestPredictions();
    }

    // 특정 로봇의 최근 실시간 예측 결과 조회
    @GetMapping("/recent/{robotNo}")
    public List<RobotPdmDTO> recent(@PathVariable int robotNo) {
        return robotPdmService.getRecentPredictions(robotNo);
    }

    // FastAPI 로봇 1~8 모델 일괄 수동 실행
    @PostMapping("/analyze-all")
    public List<PredictiveMaintenanceResponseDTO> analyzeAll() {
        return robotPdmService.analyzeAll();
    }

    // 관리자가 위험 조치를 완료하고 FastAPI의 CSV 행 고정을 해제한다.
    @PatchMapping("/{pdmNo}/complete-action")
    public RobotPdmDTO completeAction(
            @PathVariable long pdmNo,
            @RequestBody(required = false) PdmActionRequestDTO request,
            Authentication authentication
    ) {
        String actionNote = request == null ? null : request.getActionNote();
        String loginId = authentication == null
                ? null
                : authentication.getName();

        return robotPdmService.completeAction(
                pdmNo,
                actionNote,
                loginId
        );
    }
}
