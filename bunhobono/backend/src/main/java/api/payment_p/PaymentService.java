package api.payment_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Resource
    PaymentMapper mapper;
}
