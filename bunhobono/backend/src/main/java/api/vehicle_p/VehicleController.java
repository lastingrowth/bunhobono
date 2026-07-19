package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    // 차량 등록 화면에서 선택 가능한 회원 검색
    // 기존 GET /api/vehicles는 건드리지 않고 /api/vehicles/search만 추가한다.
    // normal: 등록차량 2대 미만인 회원 조회
    // visit + RESIDENT: 유효한 방문차량이 없는 입주민 조회
    // visit + ADMIN: 관리자 회원 조회
    @GetMapping("/search")
    public List<VehicleDTO> search(
            @RequestParam String vehicleType,
            @RequestParam String role
    ) {
        return vehicleService.search(vehicleType, role);
    }

    // RESIDENT 본인 차량 목록 조회
    // 토큰의 loginId를 기준으로 본인 normal 차량 + 본인이 신청한 visit 차량만 조회된다.
    @GetMapping("/resident")
    public List<VehicleDTO> residentList(Authentication authentication) {
        return vehicleService.residentList(authentication.getName());
    }

    // ADMIN 차량 등록
    // URL은 기존 프론트 호환을 위해 /signUp 유지
    // 관리자가 등록하면 승인대기 없이 바로 APPROVED로 등록된다.
    @PostMapping("/signUp")
    public int adminRequest(@RequestBody VehicleDTO dto) {
        return vehicleService.adminRequest(dto);
    }

    // RESIDENT 방문차량 등록 신청
    // RESIDENT는 normal 직접 신청 불가, visit만 가능하다.
    // 토큰의 loginId로 신청자를 판단한다.
    @PostMapping("/resident/visit")
    public int residentVisitRequest(Authentication authentication,
                                    @RequestBody VehicleDTO dto) {
        return vehicleService.residentVisitRequest(authentication.getName(), dto);
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