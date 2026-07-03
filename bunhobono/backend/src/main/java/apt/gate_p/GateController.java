package apt.gate_p;

import apt.parking_p.ParkingDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/gate")
public class GateController {


    @Resource
    GateService gateService;

    @GetMapping("")
    public List<GateDTO> list(GateDTO dto){
        List<GateDTO> list = gateService.listservice(dto);
        System.out.println("게이트정보 확인: " + list);
        System.out.println(gateService);
        return list;
    }

    @PostMapping("/signUp")
    public int sighUp(@RequestBody GateDTO dto){
        return gateService.signUp(dto);
    }

    @GetMapping("/{gateNo}")
    public GateDTO getGate(@PathVariable int gateNo) {
        return gateService.getGate(gateNo);
    }
}
