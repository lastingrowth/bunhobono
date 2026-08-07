package api.parking_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/parkings")
public class ParkingController {

    @Resource
    private ParkingService parkingService;

    @GetMapping("")
    public List<ParkingDTO> list() {
        return parkingService.list();
    }

    @PostMapping("/signUp")
    public int insert(@RequestBody ParkingDTO dto) {
        return parkingService.insert(dto);
    }

    @DeleteMapping("/{parkingNo}/delete")
    public int delete(@PathVariable int parkingNo) {
        return parkingService.delete(parkingNo);
    }

    @PutMapping("/{parkingNo}/edit")
    public int update(
            @PathVariable int parkingNo,
            @RequestBody ParkingDTO dto
    ) {
        dto.setParkingNo(parkingNo);
        return parkingService.update(dto);
    }
}