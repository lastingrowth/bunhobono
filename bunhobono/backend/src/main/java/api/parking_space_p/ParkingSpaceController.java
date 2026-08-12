package api.parking_space_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/parking-spaces")
public class ParkingSpaceController {

    @Resource
    private ParkingSpaceService parkingSpaceService;

    @GetMapping("")
    public List<ParkingSpaceDTO> list(
            @RequestParam(defaultValue = "B1")
            String parkingCode
    ) {
        return parkingSpaceService.list(parkingCode);
    }

    //내 차량 조회
    @GetMapping("/my-vehicles")
    public List<ParkingSpaceDTO> myVehicleLocations(
            Authentication authentication
    ) {
        return parkingSpaceService.myVehicleLocations(
                authentication.getName()
        );
    }

}