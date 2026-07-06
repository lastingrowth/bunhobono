package api.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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