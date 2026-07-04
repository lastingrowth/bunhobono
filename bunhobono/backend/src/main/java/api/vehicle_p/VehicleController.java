package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/vehicle")
public class VehicleController {

    @Resource
    VehicleService vehicleService;

    @GetMapping("list")
    public List<VehicleDTO> list() {
        return vehicleService.list();
    }
}