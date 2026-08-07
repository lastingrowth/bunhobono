package api.robot_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/robots")
public class RobotController {

    @Resource
    private RobotService robotService;

    // 전체 로봇 조회
    @GetMapping("")
    public List<RobotDTO> list() {
        return robotService.list();
    }

    // 로봇 상세 조회
    @GetMapping("/{robotNo}")
    public RobotDTO detail(
            @PathVariable long robotNo
    ) {
        return robotService.detail(robotNo);
    }

    // 가상 로봇 상태 갱신
    @PutMapping("/{robotNo}/state")
    public int updateState(
            @PathVariable long robotNo,
            @RequestBody RobotDTO dto
    ) {
        return robotService.updateState(
                robotNo,
                dto
        );
    }
}