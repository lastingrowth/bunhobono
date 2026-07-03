package apt.parking_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/parking")
public class ParkingController {


    @Resource
    ParkingService parkingService;

    @GetMapping("/list")
    public List<ParkingDTO> list(ParkingDTO dto){
        List<ParkingDTO> list = parkingService.listservice(dto);
        System.out.println("조회된 데이터 확인: " + list);
        System.out.println(parkingService);
        return list;
    }


    @GetMapping("/main")
    public String list(){
        return "11";
    }
}
