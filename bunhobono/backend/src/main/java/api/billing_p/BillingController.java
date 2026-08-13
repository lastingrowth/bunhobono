package api.billing_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Resource
    private BillingService billingService;

    // 차량번호 뒤 4자리로 현재 주차 중인 차량 목록 조회
    @GetMapping("/cars")
    public List<BillDTO> cars(
            @RequestParam String lastFourDigits
    ) {
        return billingService.findParkingCars(lastFourDigits);
    }

    // 차량번호로 현재 주차요금 조회
    @PostMapping("/calculate")
    public BillDTO calculate(@RequestBody BillDTO dto) {
        return billingService.createOrRefreshBill(
                dto.getCarNo(),
                dto.getKioskNo()
        );
    }

    // 토스페이먼츠 결제를 승인하고 정산을 완료 처리
    @PostMapping("/confirm")
    public TossPaymentDTO confirm(@RequestBody TossPaymentDTO dto) {
        return billingService.confirmPayment(dto);
    }
}
