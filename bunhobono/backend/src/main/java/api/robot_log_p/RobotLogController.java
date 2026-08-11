package api.robot_log_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/robot-logs")
public class RobotLogController {

    @Resource
    private RobotLogService robotLogService;

    // 로봇별·작업별 원시 상태값 조회
    @GetMapping
    public List<RobotLogDTO> list(
            @RequestParam(required = false) Long robotNo,
            @RequestParam(required = false) Long taskNo
    ) {
        return robotLogService.list(
                robotNo,
                taskNo
        );
    }

}
