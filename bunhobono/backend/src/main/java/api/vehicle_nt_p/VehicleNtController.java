package api.vehicle_nt_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/vehicle-nt")
public class VehicleNtController {

    @Resource
    VehicleNtService vehicleNtService;

    // 로그인한 입주민 본인의 차량 알림 조회
    // JWT에 저장된 loginId를 사용한다.
    @GetMapping("/resident")
    public List<VehicleNtDTO> notificationList(
            Authentication authentication
    ) {
        return vehicleNtService.notificationList(
                authentication.getName()
        );
    }

    // 로그인한 입주민의 읽지 않은 알림 전체 읽음 처리
    @PatchMapping("/resident/read")
    public int markAllRead(
            Authentication authentication
    ) {
        return vehicleNtService.markAllRead(
                authentication.getName()
        );
    }

    // 로그인한 입주민 본인의 일반 알림 직접 삭제
    // VISIT_OVERDUE_EXIT 알림은 Mapper에서 삭제를 차단한다.
    @DeleteMapping("/resident/{vehicleNtNo}")
    public int deleteNotification(
            Authentication authentication,
            @PathVariable int vehicleNtNo
    ) {
        return vehicleNtService.deleteNotification(
                authentication.getName(),
                vehicleNtNo
        );
    }
}