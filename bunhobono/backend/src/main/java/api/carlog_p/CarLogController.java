package api.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/carlog")
public class CarLogController {

    @Resource
    private CarLogService carLogService;

    @GetMapping({""})
    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogService.list(dto);
    }

    @GetMapping("/search")
    public List<CarLogDTO> search(CarLogDTO dto) {
        return carLogService.list(dto);
    }

    @DeleteMapping("/{carLogNo}/delete")
    public int delete(@PathVariable int carLogNo) {
        return carLogService.delete(carLogNo);
    }
}
