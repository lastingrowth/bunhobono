package api.bill_p;

import api.carlog_p.CarLogDTO;
import api.carlog_p.CarLogMapper;
import api.feerule_p.FeeRuleDTO;
import api.feerule_p.FeeRuleService;
import api.mem_notice_p.MemNoticeDTO;
import api.mem_notice_p.MemNoticeService;
import api.kiosk_p.KioskDTO;
import api.kiosk_p.KioskService;
import api.member_p.MemberDTO;
import api.member_p.MemberService;
import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BillService {

    // 일일 최대요금 계산에 사용하는 24시간의 분 단위 값
    private static final int MINUTES_PER_DAY = 1440;

    @Resource
    private BillMapper billMapper;

    @Resource
    private CarLogMapper carLogMapper;

    @Resource
    private FeeRuleService feeRuleService;

    @Resource
    private MemNoticeService memNoticeService;

    @Resource
    private MemberService memberService;

    @Resource
    private KioskService kioskService;

    @Resource
    private TrashService trashService;

    // 로컬 설정파일에 저장한 토스페이먼츠 시크릿 키
    @Value("${toss.secret-key}")
    private String tossSecretKey;

    // 토스페이먼츠 결제 승인 API 호출에 사용하는 HTTP 클라이언트
    private final RestClient tossRestClient =
            RestClient.builder()
                    .baseUrl("https://api.tosspayments.com")
                    .build();

    // 차량번호 뒤 4자리와 키오스크 위치로 현재 주차 중인 비입주민 정산서 조회
    public List<BillDTO> findParkingBills(String lastFourDigits, Integer kioskNo) {
        String digits = lastFourDigits == null ? "" : lastFourDigits.trim();

        if (digits.isBlank() || kioskNo == null || kioskNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        KioskDTO kiosk = kioskService.findByKioskNo(kioskNo);

        if (kiosk == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        BillDTO dto = new BillDTO();
        dto.setSnapshotCarNo(digits);

        boolean hasCurrentBill = false;
        Map<Integer, BillDTO> latest = new LinkedHashMap<>();

        for (BillDTO bill : billMapper.list(dto)) {
            if (bill.getCarLogNo() == null || bill.getOutTime() != null) {
                continue;
            }

            hasCurrentBill = true;

            if (bill.getParkingNo() == null || !bill.getParkingNo().equals(kiosk.getParkingNo())) {
                continue;
            }

            latest.putIfAbsent(bill.getSnapshotCarLogNo(), bill);
        }

        if (!hasCurrentBill) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (latest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return new ArrayList<>(latest.values());
    }

    // B2에 입차한 비입주민 차량의 미결제 정산서를 생성한다.
    public void createEntryBill(int carLogNo, String snapshotCarNo, LocalDateTime inTime) {
        // 입차시각에 적용 중인 요금 규칙을 조회한다.
        FeeRuleDTO feeRule = feeRuleService.findDefaultAt(inTime);

        BillDTO dto = new BillDTO();
        dto.setCarLogNo(carLogNo);
        dto.setSnapshotCarLogNo(carLogNo);
        dto.setSnapshotCarNo(snapshotCarNo);
        dto.setFeeRuleNo(feeRule.getFeeRuleNo());
        dto.setChargeMinutes(0);
        dto.setBillAmount(BigDecimal.ZERO);
        dto.setBillStatus("UNPAID");
        dto.setIssuedAt(inTime);

        if (billMapper.insert(dto) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    // 현재 주차 중인 정산서를 갱신하고 출차 유예시간이 지났으면 새 정산서를 발행한다.
    @Transactional
    public BillDTO createOrRefreshBill(String carNo, Integer kioskNo) {
        BillDTO dto = null;

        for (BillDTO bill : findParkingBills(carNo, kioskNo)) {
            if (carNo.equals(bill.getSnapshotCarNo())) {
                dto = bill;
                break;
            }
        }

        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 결제 후 출차 유예시간이 남아 있으면 기존 정산서를 반환한다.
        if ("PAID".equalsIgnoreCase(dto.getBillStatus())) {
            setExitAllowed(dto);

            if (dto.isExitAllowed()) {
                return dto;
            }

            // 출차 유예시간이 끝났으면 추가 미결제 정산서를 조회하거나 생성한다.
            dto = createAdditionalBill(dto.getCarLogNo());

            if (dto == null || !"UNPAID".equalsIgnoreCase(dto.getBillStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        dto.setKioskNo(kioskNo);
        calculateBill(dto);

        boolean isPaymentOrderCreated = false;

        // 무료 정산은 별도 결제 없이 완료한다.
        if (dto.getBillAmount().compareTo(BigDecimal.ZERO) == 0) {
            dto.setBillStatus("PAID");
            dto.setPaidAt(LocalDateTime.now());
        } else if (dto.getPaymentOrderId() == null || dto.getPaymentOrderId().isBlank()) {
            dto.setPaymentOrderId("BILL-" + dto.getBillNo() + "-" + UUID.randomUUID().toString().replace("-", ""));
            isPaymentOrderCreated = true;
        }

        if (billMapper.update(dto) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        // 새 결제 주문이 생성된 방문차량은 등록 입주민에게 정산 알림을 보낸다.
        if (isPaymentOrderCreated) {
            createVisitParkingFeeNotification(dto);
        }

        return billMapper.detail(dto);
    }

    // 입주민이 등록한 방문차량에 주차요금 발생 알림을 생성한다.
    private void createVisitParkingFeeNotification(BillDTO dto) {
        if (dto.getMemberNo() == null || !"VISIT".equalsIgnoreCase(dto.getCarKind())) {
            return;
        }

        String formattedAmount = NumberFormat.getNumberInstance(Locale.KOREA).format(dto.getBillAmount());

        MemNoticeDTO notice = new MemNoticeDTO();
        notice.setRecipientMemberNo(dto.getMemberNo());
        notice.setReferenceTable("bill");
        notice.setReferenceNo(dto.getBillNo());
        notice.setNoticeType("VISIT_PARKING_FEE_ISSUED");
        notice.setTitle("방문차량 주차요금 발생");
        notice.setMessage("등록하신 방문차량 " + dto.getSnapshotCarNo() + "에 주차요금 " + formattedAmount + "원이 부과되었습니다.");

        memNoticeService.createNotification(notice);
    }

    // 로그인한 입주민이 등록한 방문차량의 주차요금 정산서 조회
    @Transactional
    public BillDTO findResidentBill(int billNo, String loginId) {
        // 정산서 번호 필수값 확인
        if (billNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 로그인 정보 확인
        if (loginId == null || loginId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        BillDTO dto = new BillDTO();
        dto.setBillNo(billNo);
        dto = billMapper.detail(dto);

        if (dto == null || dto.getMemberNo() == null || !"VISIT".equalsIgnoreCase(dto.getCarKind())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        MemberDTO member = memberService.residentMypage(loginId);

        // 로그인한 입주민이 등록한 방문차량인지 확인
        if (member == null || !dto.getMemberNo().equals(member.getMemberNo())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 결제 완료된 정산서는 다시 계산하지 않는다.
        if ("PAID".equalsIgnoreCase(dto.getBillStatus())) {
            return dto;
        }

        calculateBill(dto);

        if (billMapper.update(dto) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return billMapper.detail(dto);
    }

    // 토스페이먼츠 결제를 승인하고 정산서를 결제완료 상태로 변경한다.
    @Transactional
    public BillDTO confirmPayment(BillDTO dto) {
        if (dto == null
                || dto.getPaymentKey() == null || dto.getPaymentKey().isBlank()
                || dto.getPaymentOrderId() == null || dto.getPaymentOrderId().isBlank()
                || dto.getBillAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 결제 요청 전에 저장한 주문번호로 정산서를 조회한다.
        BillDTO bill = billMapper.detail(dto);

        if (bill == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 같은 결제키로 이미 승인된 요청이면 저장된 결제 결과를 반환한다.
        if ("PAID".equalsIgnoreCase(bill.getBillStatus())) {
            if (dto.getPaymentKey().equals(bill.getPaymentKey())) {
                return bill;
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        // 프론트에서 전달한 금액과 백엔드에 저장된 정산금액을 비교한다.
        if (bill.getBillAmount().compareTo(dto.getBillAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String authorization = Base64.getEncoder().encodeToString(
                (tossSecretKey + ":").getBytes(StandardCharsets.UTF_8)
        );

        BillDTO approvedPayment;

        try {
            // 백엔드에 저장된 주문번호와 금액으로 토스페이먼츠 결제를 승인한다.
            approvedPayment = tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authorization)
                    .header("Idempotency-Key", bill.getPaymentOrderId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", dto.getPaymentKey(),
                            "orderId", bill.getPaymentOrderId(),
                            "amount", bill.getBillAmount()
                    ))
                    .retrieve()
                    .body(BillDTO.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }

        // 토스 승인 결과가 요청한 정산정보와 일치하는지 확인한다.
        if (approvedPayment == null
                || approvedPayment.getTotalAmount() == null
                || !dto.getPaymentKey().equals(approvedPayment.getPaymentKey())
                || !bill.getPaymentOrderId().equals(approvedPayment.getPaymentOrderId())
                || bill.getBillAmount().compareTo(approvedPayment.getTotalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }

        bill.setPaymentKey(approvedPayment.getPaymentKey());
        bill.setPaymentMethod(approvedPayment.getPaymentMethod());
        bill.setBillStatus("PAID");
        bill.setPaidAt(LocalDateTime.now());

        // 미결제 상태인 정산서만 결제완료로 변경한다.
        if (billMapper.update(bill) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return billMapper.detail(bill);
    }

    // 지난 기록으로 이동하지 않은 비입주민 정산 목록 조회
    @Transactional
    public List<BillDTO> findAdminBillingList() {
        List<BillDTO> list = billMapper.list(new BillDTO());

        for (BillDTO dto : list) {
            if ("UNPAID".equalsIgnoreCase(dto.getBillStatus()) && dto.getCarLogNo() != null) {
                BillDTO bill = findCurrentUnpaidBill(dto.getCarLogNo());

                if (bill != null && Objects.equals(bill.getBillNo(), dto.getBillNo())) {
                    dto.setChargeMinutes(bill.getChargeMinutes());
                    dto.setBillAmount(bill.getBillAmount());
                }
            }
            setExitAllowed(dto);
        }
        return list;
    }

    // 정산서 번호로 관리자 정산 상세정보 조회
    @Transactional
    public BillDTO findAdminBillingDetail(int billNo) {
        if (billNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        BillDTO dto = new BillDTO();
        dto.setBillNo(billNo);
        dto = billMapper.detail(dto);

        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // 현재 연결된 미결제 정산서는 현재시각 기준으로 다시 계산한다.
        if ("UNPAID".equalsIgnoreCase(dto.getBillStatus()) && dto.getCarLogNo() != null) {
            BillDTO bill = findCurrentUnpaidBill(dto.getCarLogNo());

            // 다른 정산서가 반환되는 비정상 상태에서는 기존 상세정보를 유지한다.
            if (bill != null && Objects.equals(bill.getBillNo(), dto.getBillNo())) {
                dto = bill;
            }
        }

        setExitAllowed(dto);

        return dto;
    }

    // 입출차 기록에 연결된 현재 미결제 정산금액을 계산하고 저장한다.
    @Transactional
    public BillDTO findCurrentUnpaidBill(int carLogNo) {
        if (carLogNo <= 0) {
            return null;
        }

        BillDTO dto = new BillDTO();
        dto.setCarLogNo(carLogNo);

        for (BillDTO bill : billMapper.list(dto)) {
            if (!"UNPAID".equalsIgnoreCase(bill.getBillStatus()) || bill.getInTime() == null) {
                continue;
            }

            calculateBill(bill);

            if (billMapper.update(bill) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }

            dto.setBillNo(bill.getBillNo());

            return billMapper.detail(dto);
        }

        return null;
    }

    // 미결제 정산서의 무료시간과 요금 규칙을 수정하고 정산금액을 다시 계산한다.
    @Transactional
    public BillDTO updateAdminBilling(int billNo, BillDTO dto) {
        if (billNo <= 0 || dto == null
                || dto.getFreeTime() == null || dto.getFreeTime() < 0
                || dto.getFeeRuleNo() == null || dto.getFeeRuleNo() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        BillDTO bill = findAdminBillingDetail(billNo);

        // 결제 완료된 정산서는 수정하지 않는다.
        if ("PAID".equalsIgnoreCase(bill.getBillStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        // 연결된 입출차 기록이 없으면 무료시간과 정산금액을 수정할 수 없다.
        if (bill.getCarLogNo() == null || bill.getInTime() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        FeeRuleDTO feeRule = feeRuleService.detail(dto.getFeeRuleNo());
        LocalDateTime now = LocalDateTime.now();

        // 현재 활성 상태인 요금 규칙만 정산서에 적용한다.
        if (feeRule.getEffectiveFrom() == null
                || feeRule.getEffectiveFrom().isAfter(now)
                || feeRule.getEffectiveTo() != null
                && !feeRule.getEffectiveTo().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (feeRule.getUnitMinutes() <= 0 || feeRule.getUnitFee() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        CarLogDTO carLog = new CarLogDTO();
        carLog.setCarLogNo(bill.getCarLogNo());
        carLog.setFreeTime(dto.getFreeTime());

        if (carLogMapper.updateFreeTime(carLog) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        // 새 무료시간과 선택한 요금 규칙으로 정산금액을 계산한다.
        bill.setFreeTime(dto.getFreeTime());
        bill.setFeeRuleNo(feeRule.getFeeRuleNo());
        bill.setUnitMinutes(feeRule.getUnitMinutes());
        bill.setUnitFee(feeRule.getUnitFee());
        bill.setDailyMaxFee(feeRule.getDailyMaxFee());

        calculateBill(bill);

        dto.setBillNo(billNo);
        dto.setFeeRuleNo(feeRule.getFeeRuleNo());
        dto.setChargeMinutes(bill.getChargeMinutes());
        dto.setBillAmount(bill.getBillAmount());
        dto.setBillStatus("UNPAID");

        if (billMapper.update(dto) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return findAdminBillingDetail(billNo);
    }

    // 같은 주차 건의 최종 정산서를 기준으로 출차 가능 여부 확인
    public boolean isExitAllowed(int carLogNo, LocalDateTime exitTime) {
        if (carLogNo <= 0 || exitTime == null) {
            return false;
        }

        BillDTO dto = new BillDTO();
        dto.setCarLogNo(carLogNo);

        for (BillDTO bill : billMapper.list(dto)) {
            return "PAID".equalsIgnoreCase(bill.getBillStatus())
                    && bill.getPaidAt() != null
                    && bill.getExitGraceMinutes() != null
                    && !exitTime.isAfter(bill.getPaidAt().plusMinutes(bill.getExitGraceMinutes()));
        }

        return false;
    }

    // 관리자가 결제 완료된 정산서를 지난 기록으로 직접 이동한다.
    public int moveAdminBillingToTrash(int billNo) {
        if (billNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        try {
            trashService.moveBill(billNo, "MANUAL");
            return 1;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    // 출차 유예시간이 지난 결제 완료 정산서의 추가 미결제 정산서를 생성한다.
    @Transactional
    public void createAdditionalBills() {
        BillDTO dto = new BillDTO();
        dto.setBillStatus("PAID");

        LocalDateTime now = LocalDateTime.now();
        Set<Integer> processedCarLogNos = new HashSet<>();

        for (BillDTO bill : billMapper.list(dto)) {
            if (bill.getCarLogNo() == null || !processedCarLogNos.add(bill.getCarLogNo())
                    || bill.getOutTime() != null || bill.getPaidAt() == null
                    || bill.getExitGraceMinutes() == null
                    || now.isBefore(bill.getPaidAt().plusMinutes(bill.getExitGraceMinutes()))) {
                continue;
            }

            createAdditionalBill(bill.getCarLogNo());
        }
    }

    // 결제 완료 후 3개월이 지난 정산서를 지난 기록으로 자동 이동한다.
    public void moveOldPaidBillsToTrash() {
        BillDTO dto = new BillDTO();
        dto.setBillStatus("PAID");

        LocalDateTime archiveBefore = LocalDateTime.now().minusMonths(3);

        for (BillDTO bill : billMapper.list(dto)) {
            // 결제시각이 없거나 아직 3개월이 지나지 않은 정산서는 제외한다.
            if (bill.getPaidAt() == null || !bill.getPaidAt().isBefore(archiveBefore)) {
                continue;
            }

            try {
                trashService.moveBill(bill.getBillNo(), "SCHEDULED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 동일 주차 건을 잠근 뒤 현재 미결제 정산서를 조회하거나 추가 정산서를 생성한다.
    private BillDTO createAdditionalBill(int carLogNo) {
        if (billMapper.lockCarLog(carLogNo) == null) {
            return null;
        }

        BillDTO dto = new BillDTO();
        dto.setCarLogNo(carLogNo);

        // 잠금 대기 중 다른 요청이 정산서를 생성했을 수 있으므로 최신 정산서를 다시 조회한다.
        for (BillDTO bill : billMapper.list(dto)) {
            if (bill.getOutTime() != null) {
                return null;
            }

            if ("UNPAID".equalsIgnoreCase(bill.getBillStatus())) {
                return bill;
            }

            if (!"PAID".equalsIgnoreCase(bill.getBillStatus())
                    || bill.getPaidAt() == null || bill.getExitGraceMinutes() == null) {
                return null;
            }

            LocalDateTime issuedAt = bill.getPaidAt().plusMinutes(bill.getExitGraceMinutes());

            if (LocalDateTime.now().isBefore(issuedAt)) {
                return bill;
            }

            FeeRuleDTO feeRule = feeRuleService.findDefaultAt(issuedAt);

            dto = new BillDTO();
            dto.setCarLogNo(bill.getCarLogNo());
            dto.setSnapshotCarLogNo(bill.getSnapshotCarLogNo());
            dto.setSnapshotCarNo(bill.getSnapshotCarNo());
            dto.setFeeRuleNo(feeRule.getFeeRuleNo());
            dto.setChargeMinutes(0);
            dto.setBillAmount(BigDecimal.ZERO);
            dto.setBillStatus("UNPAID");
            dto.setIssuedAt(issuedAt);

            if (billMapper.insert(dto) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }

            return billMapper.detail(dto);
        }

        return null;
    }

    // 결제시각과 요금 규칙의 출차 유예시간으로 현재 출차 가능 여부를 설정한다.
    private void setExitAllowed(BillDTO dto) {
        LocalDateTime exitAllowedUntil = dto.getPaidAt() == null
                ? null
                : dto.getPaidAt().plusMinutes(dto.getExitGraceMinutes());

        dto.setExitAllowedUntil(exitAllowedUntil);
        dto.setExitAllowed(
                dto.getOutTime() == null
                        && "PAID".equalsIgnoreCase(dto.getBillStatus())
                        && exitAllowedUntil != null
                        && !LocalDateTime.now().isAfter(exitAllowedUntil)
        );
    }

    // 현재 정산서의 과금 시작시각부터 정산금액을 계산한다.
    private void calculateBill(BillDTO dto) {
        LocalDateTime chargeStartedAt = dto.getIssuedAt() == null ? dto.getInTime() : dto.getIssuedAt();
        long parkingSeconds = Duration.between(chargeStartedAt, LocalDateTime.now()).getSeconds();
        long parkingMinutes = Math.max(0, (parkingSeconds + 59) / 60);

        boolean isFirstBill = dto.getIssuedAt() == null || !dto.getIssuedAt().isAfter(dto.getInTime());
        int freeTime = isFirstBill && dto.getFreeTime() != null ? Math.max(0, dto.getFreeTime()) : 0;
        long chargeMinutes = Math.max(0, parkingMinutes - freeTime);

        dto.setChargeMinutes(Math.toIntExact(chargeMinutes));
        dto.setBillAmount(calculateAmount(chargeMinutes, dto));
    }

    // 과금시간에 요금 부과 단위와 일일 최대요금을 적용한다.
    private BigDecimal calculateAmount(long chargeMinutes, BillDTO dto) {
        if (chargeMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        long fullDays = chargeMinutes / MINUTES_PER_DAY;
        long remainingMinutes = chargeMinutes % MINUTES_PER_DAY;
        long fullDayUnits = (MINUTES_PER_DAY + dto.getUnitMinutes() - 1L) / dto.getUnitMinutes();

        BigDecimal fullDayFee = dto.getUnitFee().multiply(BigDecimal.valueOf(fullDayUnits));

        if (dto.getDailyMaxFee() != null) {
            fullDayFee = fullDayFee.min(dto.getDailyMaxFee());
        }

        BigDecimal billAmount = fullDayFee.multiply(BigDecimal.valueOf(fullDays));

        if (remainingMinutes > 0) {
            long remainingUnits = (remainingMinutes + dto.getUnitMinutes() - 1L) / dto.getUnitMinutes();
            BigDecimal remainingFee = dto.getUnitFee().multiply(BigDecimal.valueOf(remainingUnits));

            if (dto.getDailyMaxFee() != null) {
                remainingFee = remainingFee.min(dto.getDailyMaxFee());
            }

            billAmount = billAmount.add(remainingFee);
        }

        return billAmount;
    }
}
