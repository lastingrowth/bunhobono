package apt.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/carlog")
public class CarLogController {

    @Resource
    CarLogService carLogService;

    @GetMapping("/list")
    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogService.list(dto);
    }
}