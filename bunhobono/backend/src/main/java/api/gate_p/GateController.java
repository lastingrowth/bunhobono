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

//    // 게이트 상세 조회
//    @GetMapping("/{gateNo}/detail")
//    public ResponseEntity<GateDTO> getGateDetail(@PathVariable int gateNo) {
//        return ResponseEntity.ok(gateService.getGateDetail(gateNo));
//    }

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

    // 게이트 상태 변경
    // 관리자 화면에서 수동으로 열기/닫기 버튼을 눌렀을 때 호출
    @PutMapping("/{gateNo}/status")
    public int updateGateStatus(@PathVariable int gateNo,
                                @RequestBody GateDTO dto) {
        dto.setGateNo(gateNo);
        return gateService.updateStatus(dto);
    }
}
