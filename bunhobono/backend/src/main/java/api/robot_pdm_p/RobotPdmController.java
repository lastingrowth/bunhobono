package api.robot_pdm_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/robot-pdm")
public class RobotPdmController {

    @Resource
    private RobotPdmService robotPdmService;

    // 예지보전 결과 전체 조회
    @GetMapping
    public List<RobotPdmDTO> list() {
        return robotPdmService.list();
    }

    // 예지보전 결과 상세 조회
    @GetMapping("/{pdmNo}")
    public RobotPdmDTO detail(
            @PathVariable long pdmNo
    ) {
        return robotPdmService.detail(pdmNo);
    }

    // 예지보전 분석 결과 저장
    @PostMapping
    public int savePrediction(
            @RequestBody RobotPdmDTO dto
    ) {
        return robotPdmService.savePrediction(dto);
    }
}