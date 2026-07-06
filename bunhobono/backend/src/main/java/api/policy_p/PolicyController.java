package api.policy_p;

import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Delete;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-policies")
public class PolicyController {

    @Resource
    PolicyService service;

    @GetMapping
    public List<PolicyDTO> getPolicies(){
        return service.list();
    }

    @GetMapping("/{feePolicyNo}")
    public PolicyDTO getPolicy(@PathVariable int feePolicyNo) {
        return service.detail(feePolicyNo);
    }

    @PostMapping
    public PolicyDTO createPoilcy(@RequestBody PolicyDTO dto) {
        service.insert(dto);
        return dto;
    }

    @PutMapping("{feePolicyNo}")
    public void updatePolicy(@PathVariable int feePolicyNo, @RequestBody PolicyDTO dto) {
        dto.setFeePolicyNo(feePolicyNo);
        service.update(dto);
    }

    @DeleteMapping("/{feePolicyNo}")
    public void deletePolicy(@PathVariable int feePolicyNo) {
        service.delete(feePolicyNo);
    }
}
