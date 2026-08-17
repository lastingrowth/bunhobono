package api.mem_purchase_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mem-purchase")
public class MemPurchaseController {

    @Resource
    private MemPurchaseService memPurchaseService;

    @PostMapping
    public MemPurchaseDTO createOrder(
            Authentication authentication,
            @RequestBody MemPurchaseDTO dto
    ) {
        return memPurchaseService.createOrder(
                getLoginId(authentication),
                dto
        );
    }

    @PostMapping("/confirm")
    public MemPurchaseDTO.Payment confirmPayment(
            Authentication authentication,
            @RequestBody MemPurchaseDTO.Payment payment
    ) {
        return memPurchaseService.confirmPayment(
                getLoginId(authentication),
                payment
        );
    }

    private String getLoginId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return authentication.getName();
    }
}
