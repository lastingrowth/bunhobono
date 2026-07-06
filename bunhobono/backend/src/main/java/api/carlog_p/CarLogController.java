package api.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/carlog")
public class CarLogController {

    @Resource
    CarLogService carLogService;

    // 차량 입출차 로그 목록 조회
    @GetMapping("/list")
    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogService.list(dto);
    }
}