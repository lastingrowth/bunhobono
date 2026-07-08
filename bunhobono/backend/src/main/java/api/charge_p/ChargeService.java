package api.charge_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargeService {

    @Resource
    ChargeMapper mapper;

    public List<ChargeDTO> findAllCharges() {
        return mapper.list();
    }
}
