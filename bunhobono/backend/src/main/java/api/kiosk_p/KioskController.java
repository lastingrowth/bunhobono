package api.kiosk_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/kiosk")
public class KioskController {

    @Resource
    KioskService kioskService;

    // 키오스크 전체 목록 조회
    @GetMapping("")
    public List<KioskDTO> list() {
        return kioskService.list();
    }

    // 키오스크 삭제
    @DeleteMapping("/{kioskNo}/delete")
    public int delete(@PathVariable int kioskNo) {
        return kioskService.delete(kioskNo);
    }

    // 키오스크 등록
    @PostMapping("/signUp")
    public int signUp(@RequestBody KioskDTO kioskDTO) {
        return kioskService.signUp(kioskDTO);
    }

}
