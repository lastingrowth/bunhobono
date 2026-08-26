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

    // 입출차 기록과 키오스크 위치에 맞는 활성 출차 게이트 번호 조회
    @GetMapping("/{carLogNo}/exit-gate")
    public int exitGate(@PathVariable int carLogNo, @RequestParam(required = false) Integer kioskNo) {
        return gateService.findExitGateNo(carLogNo, kioskNo);
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
