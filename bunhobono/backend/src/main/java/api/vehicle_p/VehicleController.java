package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Resource
    VehicleService vehicleService;

    // 차량 전체 목록
    @GetMapping
    public List<VehicleDTO> list() {
        return vehicleService.list();
    }

    // 차량번호 검색
    @GetMapping("/search")
    public List<VehicleDTO> search(@RequestParam String carNo) {
        return vehicleService.search(carNo);
    }

    // 차량 상세
    @GetMapping("/{vehicleNo}")
    public VehicleDTO detail(@PathVariable Integer vehicleNo) {
        return vehicleService.detail(vehicleNo);
    }

    // 차량 등록
    @PostMapping("/insert")
    public int insert(@RequestBody VehicleDTO dto) {
        return vehicleService.insert(dto);
    }

    // 차량 수정
    @PutMapping("/{vehicleNo}/edit")
    public int update(
            @PathVariable Integer vehicleNo,
            @RequestBody VehicleDTO dto
    ) {
        dto.setVehicleCarNo(vehicleNo);
        return vehicleService.update(dto);
    }

    // 차량 삭제
    @DeleteMapping("/{vehicleNo}/delete")
    public int delete(@PathVariable Integer vehicleNo) {
        return vehicleService.delete(vehicleNo);
    }

    // 승인 대기 목록
    @GetMapping("/approve")
    public List<VehicleDTO> approveList() {
        return vehicleService.approveList();
    }

    // 승인 대기 상세
    @GetMapping("/approve/{vehicleNo}")
    public VehicleDTO approveDetail(@PathVariable Integer vehicleNo) {
        return vehicleService.detail(vehicleNo);
    }

    // 승인 상태 변경
    @PutMapping("/approve/{vehicleNo}/status")
    public int changeStatus(
            @PathVariable Integer vehicleNo,
            @RequestBody VehicleDTO dto
    ) {
        dto.setVehicleCarNo(vehicleNo);
        return vehicleService.changeStatus(dto);
    }
}