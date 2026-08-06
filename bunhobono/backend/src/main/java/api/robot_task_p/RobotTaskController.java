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
    @GetMapping("")
    public List<RobotTaskDTO> list() {
        return robotTaskService.list();
    }

    // 로봇 작업 상세 조회
    @GetMapping("/{taskNo}")
    public RobotTaskDTO detail(
            @PathVariable long taskNo
    ) {
        return robotTaskService.detail(taskNo);
    }

    // 로봇 세트에 배정된 대기 작업 조회
    @GetMapping("/assigned/{setNo}")
    public RobotTaskDTO findAssignedTask(
            @PathVariable int setNo
    ) {
        return robotTaskService.findAssignedTask(
                setNo
        );
    }

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

    // 가상 로봇 작업 시작
    @PatchMapping("/{taskNo}/start")
    public int start(
            @PathVariable long taskNo
    ) {
        return robotTaskService.start(taskNo);
    }

    // 가상 로봇 작업 단계 갱신
    @PatchMapping("/{taskNo}/phase")
    public int updatePhase(
            @PathVariable long taskNo,
            @RequestBody RobotTaskDTO dto
    ) {
        return robotTaskService.updatePhase(
                taskNo,
                dto.getTaskPhase()
        );
    }

    // 가상 로봇 작업 완료
    @PatchMapping("/{taskNo}/complete")
    public int complete(
            @PathVariable long taskNo
    ) {
        return robotTaskService.complete(taskNo);
    }

    // 가상 로봇 작업 실패
    @PatchMapping("/{taskNo}/fail")
    public int fail(
            @PathVariable long taskNo,
            @RequestBody RobotTaskDTO dto
    ) {
        return robotTaskService.fail(
                taskNo,
                dto.getFailureReason()
        );
    }
}