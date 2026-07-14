package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Resource
    VehicleService vehicleService;

    // 전체 차량 목록 조회
    // 관리자 화면에서는 전체 차량 목록으로 사용한다.
    @GetMapping("")
    public List<VehicleDTO> list() {
        return vehicleService.listservice();
    }

    // RESIDENT 본인 차량 목록 조회
    // 토큰의 loginId를 기준으로 본인 normal 차량 + 본인이 신청한 visit 차량만 조회된다.
    @GetMapping("/resident")
    public List<VehicleDTO> residentList(Authentication authentication) {
        return vehicleService.residentList(authentication.getName());
    }

    // ADMIN 차량 등록 신청
    // normal, visit 모두 가능하지만 등록 즉시 APPROVED가 아니라 WAITING으로 들어간다.
    @PostMapping("/signUp")
    public int signUp(@RequestBody VehicleDTO dto) {
        return vehicleService.adminSignUp(dto);
    }

    // RESIDENT 방문차량 등록 신청
    // RESIDENT는 normal 직접 신청 불가, visit만 가능하다.
    // 토큰의 loginId로 신청자를 판단한다.
    @PostMapping("/resident/visit")
    public int residentVisitSignUp(Authentication authentication,
                                   @RequestBody VehicleDTO dto) {
        return vehicleService.residentVisitSignUp(authentication.getName(), dto);
    }

    // 차량 삭제
    @DeleteMapping("/{vehicleCarNo}/delete")
    public int deleteVehicle(@PathVariable int vehicleCarNo) {
        return vehicleService.delete(vehicleCarNo);
    }

    // 차량 기본 정보 수정
    @PutMapping("/{vehicleCarNo}/edit")
    public int updateVehicle(@PathVariable int vehicleCarNo,
                             @RequestBody VehicleDTO dto) {
        dto.setVehicleCarNo(vehicleCarNo);
        return vehicleService.update(dto);
    }

    // 차량 승인 상태 변경
    // WAITING 차량을 APPROVED / UNKNOWN / EXPIRED 처리한다.
    @PatchMapping("/{vehicleCarNo}/status")
    public int updateVehicleStatus(@PathVariable int vehicleCarNo,
                                   @RequestBody VehicleDTO dto) {
        dto.setVehicleCarNo(vehicleCarNo);
        return vehicleService.updateStatus(dto);
    }
}