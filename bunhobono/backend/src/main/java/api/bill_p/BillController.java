package api.bill_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillController {

    @Resource
    private BillService billService;

    // 차량번호 뒤 4자리로 현재 주차 중인 비입주민 차량 목록 조회
    @GetMapping("/guest-cars")
    public List<BillDTO> guestCars(@RequestParam String lastFourDigits, @RequestParam Integer kioskNo) {
        return billService.findParkingBills(lastFourDigits, kioskNo);
    }

    // 차량번호로 현재 주차요금 조회
    @PostMapping("/calculate")
    public BillDTO calculate(@RequestBody BillDTO dto) {
        return billService.createOrRefreshBill(dto.getCarNo(), dto.getKioskNo());
    }

    // 토스페이먼츠 결제를 승인하고 정산을 완료 처리
    @PostMapping("/confirm")
    public BillDTO confirm(@RequestBody BillDTO dto) {
        return billService.confirmPayment(dto);
    }

    // 로그인한 입주민이 등록한 방문차량의 주차요금 정산서 조회
    @GetMapping("/resident/{billNo}")
    public BillDTO residentBill(Authentication authentication, @PathVariable int billNo) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return billService.findResidentBill(billNo, authentication.getName());
    }

    // 지난 기록으로 이동하지 않은 비입주민 정산 목록 조회
    @GetMapping("/admin")
    public List<BillDTO> adminList() {
        return billService.findAdminBillingList();
    }

    // 정산서 번호로 관리자 정산 상세정보 조회
    @GetMapping("/admin/{billNo}")
    public BillDTO adminDetail(@PathVariable int billNo) {
        return billService.findAdminBillingDetail(billNo);
    }

    // 미결제 정산서의 무료시간과 요금 규칙 수정 및 정산금액 재계산
    @PatchMapping("/admin/{billNo}")
    public BillDTO updateAdminBilling(@PathVariable int billNo, @RequestBody BillDTO dto) {
        return billService.updateAdminBilling(billNo, dto);
    }

    // 관리자가 결제 완료된 정산서를 지난 기록으로 직접 이동한다.
    @DeleteMapping("/admin/{billNo}/archive")
    public int archiveAdminBilling(@PathVariable int billNo) {
        return billService.moveAdminBillingToTrash(billNo);
    }
}
