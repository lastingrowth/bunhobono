package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
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

    // 등록 차량 목록 조회
    @GetMapping("")
    public List<VehicleDTO> list() {
        return vehicleService.listservice();
    }

    // ===== 입주민 대시보드 추가 시작 =====
    // JWT에 저장된 로그인 아이디로 본인이 등록한 차량만 조회
    @GetMapping("/resident")
    public List<VehicleDTO> residentList(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return vehicleService.listByLoginId(authentication.getName());
    }
    // ===== 입주민 대시보드 추가 끝 =====

    // 차량 등록
    @PostMapping("/signUp")
    public int signUp(@RequestBody VehicleDTO dto) {
        return vehicleService.signUp(dto);
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

    // 관리자 차량 상태 변경
    @PatchMapping("/{vehicleCarNo}/status")
    public int updateVehicleStatus(@PathVariable int vehicleCarNo,
                                   @RequestBody VehicleDTO dto) {
        dto.setVehicleCarNo(vehicleCarNo);
        return vehicleService.updateStatus(dto);
    }

}
