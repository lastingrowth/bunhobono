package api.gate_p;

import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/gates")
public class GateController {

    public record ManualOpenRequest(int cameraDataNo) {
    }


    @Resource
    GateService gateService;

    // 관리자가 카메라 기록을 확인한 후 해당 입차 게이트를 수동으로 연다.
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{gateNo}/open")
    public ResponseEntity<String> open(@PathVariable int gateNo,
                                       @RequestBody ManualOpenRequest request) {
        gateService.manualOpen(gateNo, request.cameraDataNo());
        return ResponseEntity.ok("게이트가 열렸습니다.");
    }

    // 수동 조작은 입차 게이트에만 허용한다.
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{gateNo}/close")
    public ResponseEntity<String> close(@PathVariable int gateNo) {
        gateService.manualClose(gateNo);
        return ResponseEntity.ok("게이트가 닫혔습니다.");
    }

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
}
