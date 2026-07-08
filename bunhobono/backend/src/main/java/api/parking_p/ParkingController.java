package api.parking_p;

import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/parkings")
public class ParkingController {


    @Resource
    ParkingService parkingService;

    @GetMapping("")
    public List<ParkingDTO> list(ParkingDTO dto){
        List<ParkingDTO> list = parkingService.listservice(dto);
        System.out.println("주차장정보 확인: " + list);
        System.out.println(parkingService);
        return list;
    }

    @PostMapping("/signUp")
    public int sighUp(@RequestBody ParkingDTO dto){
        return parkingService.signUp(dto);
    }

    @GetMapping("/{parkingNo}")
    public ParkingDTO getParking(@PathVariable int parkingNo) {
        return parkingService.getParking(parkingNo);
    }

    @DeleteMapping("/{parkingNo}/delete")
    public int deleteParking(@PathVariable int parkingNo) {
        return parkingService.delete(parkingNo);
    }

    @PutMapping("/{parkingNo}/edit")
    public int updateParking(@RequestBody ParkingDTO dto) {
        return parkingService.updateParking(dto);
    }


}
