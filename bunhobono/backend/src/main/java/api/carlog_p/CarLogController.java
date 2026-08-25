package api.carlog_p;

import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carlog")
public class CarLogController {

    @Resource
    private CarLogService carLogService;

    @Resource
    private TrashService trashService;

    @GetMapping({""})
    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogService.list(dto);
    }

    @GetMapping("/search")
    public List<CarLogDTO> search(CarLogDTO dto) {
        return carLogService.list(dto);
    }

    @GetMapping("/parking-cars")
    public List<CarLogDTO> parkingCars(@RequestParam String lastFourDigits, @RequestParam Integer kioskNo) {
        return carLogService.findParkingCars(lastFourDigits, kioskNo);
    }

    @DeleteMapping("/{carLogNo}/delete")
    public int delete(@PathVariable int carLogNo) {
        trashService.moveCarLog(carLogNo, "MANUAL");
        return 1;
    }
}
