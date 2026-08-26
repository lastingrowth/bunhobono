package api.feerule_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-rules")
public class FeeRuleController {

    @Resource
    private FeeRuleService feeRuleService;

    // 전체 요금 규칙 목록 조회
    @GetMapping
    public List<FeeRuleDTO> list() {
        return feeRuleService.list();
    }

    // 새로운 요금 규칙 등록
    @PostMapping
    public int insert(@RequestBody FeeRuleDTO dto) {
        return feeRuleService.insert(dto);
    }

    // 예약 규칙을 수정하거나 활성 규칙의 새 버전 등록
    @PatchMapping("/{feeRuleNo}")
    public int update(@PathVariable int feeRuleNo, @RequestBody FeeRuleDTO dto) {
        return feeRuleService.update(feeRuleNo, dto);
    }
}
