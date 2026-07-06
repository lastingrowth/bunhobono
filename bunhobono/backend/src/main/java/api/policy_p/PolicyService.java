package api.policy_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyService {

    @Resource
    PolicyMapper mapper;

    public List<PolicyDTO> list() {
        return mapper.list();
    }

    public PolicyDTO detail(int feePolicyNo) {
        return mapper.detail(feePolicyNo);
    }

    public int insert(PolicyDTO dto) {
        return mapper.insert(dto);
    }

    public int update(PolicyDTO dto) {
        return mapper.update(dto);
    }

    public int delete(int feePolicyNo) {
        return mapper.delete(feePolicyNo);
    }

}
