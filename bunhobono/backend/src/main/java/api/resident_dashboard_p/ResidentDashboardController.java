package api.resident_dashboard_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import api.vehicle_p.VehicleDTO;

import java.util.List;

@RestController
@RequestMapping("/api/resident/dashboard")
// 입주민 대시보드 조회만 담당하며 차량 CRUD는 기존 차량 화면에 위임한다.
public class ResidentDashboardController {

    @Resource
    private ResidentDashboardService dashboardService;

    // 인증된 로그인 ID를 사용하므로 다른 입주민 ID를 요청 변수로 받지 않는다.
    @GetMapping
    public ResidentDashboardDTO dashboard(Authentication authentication) {
        return dashboardService.getDashboard(loginId(authentication));
    }

    @GetMapping("/vehicles")
    public List<VehicleDTO> vehicles(Authentication authentication) {
        return dashboardService.getVehicles(loginId(authentication));
    }

    @PostMapping("/vehicles")
    public int createVehicle(Authentication authentication, @RequestBody VehicleDTO vehicle) {
        return dashboardService.createVehicle(loginId(authentication), vehicle);
    }

    @PutMapping("/vehicles/{vehicleCarNo}")
    public int updateVehicle(Authentication authentication,
                             @PathVariable int vehicleCarNo,
                             @RequestBody VehicleDTO vehicle) {
        return dashboardService.updateVehicle(loginId(authentication), vehicleCarNo, vehicle);
    }

    @DeleteMapping("/vehicles/{vehicleCarNo}")
    public int deleteVehicle(Authentication authentication, @PathVariable int vehicleCarNo) {
        return dashboardService.deleteVehicle(loginId(authentication), vehicleCarNo);
    }

    private String loginId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return authentication.getName();
    }
}
