package api.reset_p;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResetController {

    private final ResetService resetService;

    public ResetController(ResetService resetService) {
        this.resetService = resetService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDemo() {
        resetService.resetDemo();

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "시연 데이터와 영상 상태가 초기화되었습니다."
                )
        );
    }
}