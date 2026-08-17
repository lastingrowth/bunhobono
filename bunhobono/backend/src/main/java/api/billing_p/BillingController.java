package api.billing_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Resource
    private BillingService billingService;

    // 출차 유형, 차량번호 뒤 4자리, 키오스크 번호로 현재 주차 차량 목록을 조회한다.
    @GetMapping("/cars")
    public List<BillDTO> cars(
            @RequestParam String lastFourDigits,
            @RequestParam String exitType,
            @RequestParam Integer kioskNo
    ) {
        return billingService.findParkingCars(lastFourDigits, exitType, kioskNo);
    }

    // 차량과 현재 키오스크에 맞는 활성 출차 게이트 번호를 조회한다.
    @GetMapping("/cars/{carLogNo}/exit-gate")
    public int exitGate(
            @PathVariable int carLogNo,
            @RequestParam(required = false) Integer kioskNo
    ) {
        return billingService.findExitGateNo(carLogNo, kioskNo);
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

    // 로그인한 입주민이 등록한 방문차량의 주차요금 고지서 조회
    @GetMapping("/resident/{billNo}")
    public BillDTO residentBill(
            Authentication authentication,
            @PathVariable int billNo
    ) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return billingService.findResidentBill(
                billNo,
                authentication.getName()
        );
    }

    // 현재 주차 중인 비입주민 차량의 관리자 정산 목록 조회
    @GetMapping("/admin")
    public List<BillDTO> adminList() {
        return billingService.findAdminBillingList();
    }

    // 등록된 요금 규칙 목록 조회
    @GetMapping("/admin/fee-rules")
    public List<FeeRuleDTO> feeRuleList() {
        return billingService.findFeeRuleList();
    }

    // 새로운 요금 규칙 등록
    @PostMapping("/admin/fee-rules")
    public FeeRuleDTO createFeeRule(
            @RequestBody FeeRuleDTO dto
    ) {
        return billingService.createFeeRule(dto);
    }

    // 요금 규칙의 적용 종료일시 수정
    @PatchMapping("/admin/fee-rules/{feeRuleNo}/effective-to")
    public FeeRuleDTO updateFeeRuleEffectiveTo(
            @PathVariable int feeRuleNo,
            @RequestBody FeeRuleDTO dto
    ) {
        return billingService.updateFeeRuleEffectiveTo(
                feeRuleNo,
                dto.getEffectiveTo()
        );
    }

    // 예약 상태의 요금 규칙 전체 수정
    @PatchMapping("/admin/fee-rules/{feeRuleNo}")
    public FeeRuleDTO updateScheduledFeeRule(
            @PathVariable int feeRuleNo,
            @RequestBody FeeRuleDTO dto
    ) {
        return billingService.updateScheduledFeeRule(
                feeRuleNo,
                dto
        );
    }

    // 입출차 기록 번호로 관리자 정산 상세정보 조회
    @GetMapping("/admin/{carLogNo}")
    public BillDTO adminDetail(
            @PathVariable int carLogNo
    ) {
        return billingService.findAdminBillingDetail(carLogNo);
    }

    // 미결제 정산의 무료시간 수정 및 정산금액 재계산
    @PatchMapping("/admin/{carLogNo}")
    public BillDTO updateAdminBilling(
            @PathVariable int carLogNo,
            @RequestBody BillDTO dto
    ) {
        return billingService.updateAdminBilling(
                carLogNo,
                dto.getFreeTime()
        );
    }

    // 관리자가 완료 정산서를 지난 기록으로 직접 이동한다.
    @DeleteMapping("/admin/{billNo}/archive")
    public int archiveAdminBilling(
            @PathVariable int billNo
    ) {
        billingService.moveAdminBillingToTrash(billNo);
        return 1;
    }
}
