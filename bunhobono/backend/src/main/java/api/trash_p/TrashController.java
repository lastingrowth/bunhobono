package api.trash_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/trash")
public class TrashController {

    @Resource
    TrashService trashService;

    // 휴지통 목록
    @GetMapping("")
    public List<TrashDTO> list(
            @RequestParam(required = false) String dataType) {

        return trashService.list(dataType);
    }

    // 휴지통 상세
    @GetMapping("/{trashNo}")
    public TrashDTO detail(@PathVariable long trashNo) {
        return trashService.detail(trashNo);
    }

    //휴지통 검색
    @GetMapping("/search")
    public List<TrashDTO> search(@RequestParam String carNo) {
        return trashService.searchByCarNo(carNo);
    }
}
