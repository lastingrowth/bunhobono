package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
