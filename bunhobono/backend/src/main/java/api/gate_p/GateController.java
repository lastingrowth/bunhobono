package api.gate_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/gates")
public class GateController {

    @Resource
    GateService gateService;

    //목록
    @GetMapping("")
    public List<GateDTO> list(GateDTO dto){
        return gateService.listservice(dto);
    }

    //생성
    @PostMapping("/signUp")
    public int signUp(@RequestBody GateDTO dto){
        return gateService.signUp(dto);
    }

    @DeleteMapping("/{gateNo}/delete")
    public int deleteGate(@PathVariable int gateNo) {
        return gateService.delete(gateNo);
    }

    @PutMapping("/{gateNo}/edit")
    public int updateGate(@PathVariable int gateNo,
                          @RequestBody GateDTO dto) {
        dto.setGateNo(gateNo);
        return gateService.updateGate(dto);
    }
}
