package api.vehicle_nt_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // 로그인한 입주민의 읽지 않은 차량 알림을 모두 읽음 처리
    // 요청 본문 없이 JWT의 loginId만 사용한다.
    @PatchMapping("/resident/read")
    public int markAllRead(
            Authentication authentication
    ) {
        return vehicleNtService.markAllRead(
                authentication.getName()
        );
    }
}