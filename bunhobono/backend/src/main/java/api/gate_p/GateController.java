package api.gate_p;

import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
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
        List<GateDTO> list = gateService.listservice(dto);
        System.out.println("게이트정보 확인: " + list);
        System.out.println(gateService);
        return list;
    }
    //생성
    @PostMapping("/signUp")
    public int sighUp(@RequestBody GateDTO dto){
        return gateService.signUp(dto);
    }

    // 게이트 상세 조회
    @GetMapping("/{gateNo}/detail")
    public ResponseEntity<GateDTO> getGateDetail(@PathVariable int gateNo) {
        return ResponseEntity.ok(gateService.getGateDetail(gateNo));
    }
}
