package api.charge_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ChargeService {

    @Resource
    ChargeMapper mapper;
}
