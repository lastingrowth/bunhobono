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

    // 관리자 로봇 점검 완료
    @PatchMapping("/{robotNo}/maintenance")
    public int completeMaintenance(
            @PathVariable long robotNo
    ) {
        return robotService.completeMaintenance(robotNo);
    }

    // 주차로봇 등록
    @PostMapping("/signUp")
    public int signUp(
            @RequestBody RobotDTO dto
    ) {
        return robotService.signUp(dto);
    }

    // 주차로봇 삭제
    @DeleteMapping("/{robotNo}/delete")
    public int delete(
            @PathVariable long robotNo
    ) {
        return robotService.delete(robotNo);
    }
}
