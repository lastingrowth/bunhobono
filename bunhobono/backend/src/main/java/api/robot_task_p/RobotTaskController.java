package api.robot_task_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/robot-tasks")
public class RobotTaskController {

    @Resource
    private RobotTaskService robotTaskService;

    // 전체 로봇 작업 조회
    @GetMapping
    public List<RobotTaskDTO> list() {
        return robotTaskService.list(); }

    // 로봇 작업 상세 조회
    @GetMapping("/{taskNo}")
    public RobotTaskDTO detail(
            @PathVariable long taskNo
    ) { return robotTaskService.detail(taskNo); }

    // 입주민 출차 작업 신청
    @PostMapping("/park-out")
    public RobotTaskDTO createParkOutTask(
            @RequestParam int carLogNo,
            @RequestParam int exitGateNo
    ) {
        return robotTaskService.createParkOutTask(
                carLogNo,
                exitGateNo
        );
    }

    // 출차대기 차량 다시 입차
    @PostMapping("/repark")
    public RobotTaskDTO createReparkTask(
            @RequestParam int carLogNo
    ) {
        return robotTaskService.createReparkTask(carLogNo);
    }

}
