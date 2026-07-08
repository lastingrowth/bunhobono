package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/res/vehicles")
public class ResVehicleController {

    @Resource
    ResVehicleService resVehicleService;

    @GetMapping("/me")
    public ResVehicleMemberDTO myInfo() {
        return resVehicleService.myInfo();
    }

    // 입주민 본인 차량 목록
    @GetMapping
    public List<VehicleDTO> list() {
        return resVehicleService.list();
    }

    // 입주민 본인 차량 상세
    @GetMapping("/{vehicleNo}")
    public VehicleDTO detail(@PathVariable Integer vehicleNo) {
        return resVehicleService.detail(vehicleNo);
    }

    // 입주민 차량 등록 신청
    @PostMapping("/insert")
    public int insert(@RequestBody VehicleDTO dto) {
        return resVehicleService.insert(dto);
    }

    // 입주민 본인 차량 수정 신청
    @PutMapping("/{vehicleNo}/edit")
    public int update(
            @PathVariable Integer vehicleNo,
            @RequestBody VehicleDTO dto
    ) {
        return resVehicleService.update(vehicleNo, dto);
    }

    // 입주민 본인 차량 삭제
    @DeleteMapping("/{vehicleNo}/delete")
    public int delete(@PathVariable Integer vehicleNo) {
        return resVehicleService.delete(vehicleNo);
    }
}