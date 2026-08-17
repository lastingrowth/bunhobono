package api.billing_p;

import api.mem_notice_p.MemNoticeDTO;
import api.mem_notice_p.MemNoticeService;
import api.kiosk_p.KioskDTO;
import api.kiosk_p.KioskService;
import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    // 일일 최대요금 계산에 사용하는 24시간의 분 단위 값
    private static final int MINUTES_PER_DAY = 1440;

    // 비입주민 차량의 결제 완료 후 출차 허용시간
    private static final int EXIT_ALLOWED_MINUTES = 30;

    @Resource
    private BillingMapper billingMapper;

    @Resource
    private MemNoticeService memNoticeService;

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

    // 출차 유형, 차량번호 뒤 4자리, 키오스크 번호로 현재 주차 차량 목록을 조회한다.
    public List<BillDTO> findParkingCars(
            String lastFourDigits,
            String exitType,
            Integer kioskNo
    ) {
        String digits = lastFourDigits == null
                ? ""
                : lastFourDigits.trim();

        if (!digits.matches("^\\d{4}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "차량번호 뒤 4자리를 입력해주세요."
            );
        }

        // 화면에서 전달된 출차 유형을 대문자로 통일해 검증한다.
        String normalizedExitType = exitType == null
                ? ""
                : exitType.trim().toUpperCase();

        if (
                !"RESIDENT".equals(normalizedExitType)
                        && !"NON_RESIDENT".equals(normalizedExitType)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 출차 유형을 선택해주세요."
            );
        }

        // 키오스크 위치와 차량의 주차 층을 비교하려면 유효한 키오스크 번호가 필요하다.
        if (kioskNo == null || kioskNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "키오스크 정보를 확인할 수 없습니다."
            );
        }

        // 전달받은 키오스크 번호로 실제 설치된 키오스크와 주차장 정보를 조회한다.
        KioskDTO kiosk = kioskService.findByKioskNo(kioskNo);

        if (kiosk == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "키오스크 정보를 확인할 수 없습니다."
            );
        }

        List<BillDTO> parkingCars =
                billingMapper.findOpenCarLogsByLastFourDigits(digits, normalizedExitType);

        if (parkingCars.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "출차 유형과 차량번호가 일치하는 주차 차량을 찾을 수 없습니다."
            );
        }
        // 현재 키오스크와 같은 층에 주차된 차량만 목록에 표시한다.
        List<BillDTO> sameFloorCars = parkingCars.stream()
                .filter(car -> car.getParkingNo() == kiosk.getParkingNo())
                .toList();

        // 차량은 존재하지만 다른 층에 있으면 해당 층의 키오스크를 안내한다.
        if (sameFloorCars.isEmpty()) {
            String parkingCode = parkingCars.get(0).getParkingCode();

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "해당 차량은 "
                            + parkingCode
                            + " 주차장에 있습니다. "
                            + parkingCode
                            + " 키오스크를 이용해주세요."
            );
        }

        return sameFloorCars;
    }

    // B2에 입차한 비입주민 차량의 미결제 정산서를 생성한다.
    public void createEntryBill(
            int carLogNo,
            LocalDateTime inTime
    ) {
        // 입차시각에 적용 중인 요금 규칙을 조회한다.
        FeeRuleDTO feeRule =
                billingMapper.findFeeRuleByInTime(inTime);

        validateFeeRule(feeRule);

        BillDTO bill = new BillDTO();
        bill.setCarLogNo(carLogNo);
        bill.setFeeRuleNo(feeRule.getFeeRuleNo());
        bill.setChargeMinutes(0);
        bill.setBillAmount(BigDecimal.ZERO);
        bill.setBillStatus("UNPAID");

        if (billingMapper.insert(bill) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "입차 정산서를 생성하지 못했습니다."
            );
        }
    }

    // 차량번호로 현재 주차 기록을 찾아 기존 정산서를 갱신한다.
    @Transactional
    public BillDTO createOrRefreshBill(
            String carNo,
            Integer kioskNo
    ) {
        // 유효한 키오스크 번호인지 확인한다.
        if (kioskNo == null || kioskNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "키오스크 정보를 확인할 수 없습니다."
            );
        }

        // 키오스크가 설치된 주차장 정보를 조회한다.
        KioskDTO kiosk = kioskService.findByKioskNo(kioskNo);

        if (kiosk == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "키오스크 정보를 확인할 수 없습니다."
            );
        }

        // 사용자가 입력한 차량번호의 공백을 제거하고 형식을 확인한다.
        String normalizedCarNo = normalizeCarNo(carNo);

        // 아직 출차하지 않은 현재 주차 기록을 조회한다.
        BillDTO parkingLog =
                billingMapper.findOpenCarLogByCarNo(normalizedCarNo);

        if (parkingLog == null
                || parkingLog.getCarLogNo() == null
                || parkingLog.getInTime() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "현재 주차 중인 차량을 찾을 수 없습니다."
            );
        }

        // 차량과 키오스크의 주차 층이 다르면 해당 층의 키오스크를 안내한다.
        if (!parkingLog.getParkingNo().equals(kiosk.getParkingNo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "해당 차량은 "
                            + parkingLog.getParkingCode()
                            + " 주차장에 있습니다. "
                            + parkingLog.getParkingCode()
                            + " 키오스크를 이용해주세요."
            );
        }

        // 같은 입출차 기록으로 이미 생성된 정산서가 있는지 조회한다.
        BillDTO bill = billingMapper.findByCarLogNo(parkingLog.getCarLogNo());

        // 결제가 끝난 정산서는 금액을 다시 계산하지 않고 그대로 반환한다.
        if (bill != null && "PAID".equalsIgnoreCase(bill.getBillStatus())) {
            return bill;
        }

        // 비입주민 차량은 입차할 때 정산서가 생성되어 있어야 한다.
        if (bill == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "입차 시 생성된 정산서를 찾을 수 없습니다."
            );
        }

        // 기존 미결제 정산서는 입차할 때 적용한 요금 규칙을 유지한다.
        bill.setKioskNo(kioskNo);

        // 입차시각부터 현재까지 지난 시간을 반영해 정산금액을 다시 계산한다.
        calculateBill(
                bill,
                parkingLog.getInTime(),
                parkingLog.getFreeTime(),
                bill.getUnitMinutes(),
                bill.getUnitFee(),
                bill.getDailyMaxFee()
        );

        if (billingMapper.updateUnpaidAmount(bill) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "정산금액을 갱신하지 못했습니다."
            );
        }

        // 계산 결과가 0원이면 토스 결제 없이 정산 완료로 처리한다.
        if (bill.getBillAmount().compareTo(BigDecimal.ZERO) == 0) {
            if (
                    billingMapper.markZeroAmountPaid(
                            bill.getBillNo()
                    ) != 1
            ) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "무료 정산을 완료하지 못했습니다."
                );
            }
        } else if (bill.getPaymentOrderId() == null || bill.getPaymentOrderId().isBlank()) {
            // 유료 정산서에 토스페이먼츠 결제 주문번호를 최초 한 번만 생성
            String paymentOrderId = "BILL-"
                                    + bill.getBillNo()
                                    + "-"
                                    + UUID.randomUUID().toString().replace("-", "");

            bill.setPaymentOrderId(paymentOrderId);

            // 생성한 결제 주문번호와 결제를 진행한 키오스크를 정산서에 저장
            if (billingMapper.updatePaymentOrder(bill) != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "결제 주문번호를 저장하지 못했습니다."
                );
            }
        }

        // UPDATE 결과가 반영된 최종 정산서를 다시 조회한다.
        return billingMapper.findByCarLogNo(
                parkingLog.getCarLogNo()
        );
    }

    // 등록 방문차량의 주차요금 고지 알림 생성
    private void createVisitParkingFeeNotification(BillDTO bill) {
        BillDTO visitRegistrant =
                billingMapper.findVisitRegistrantByCarLogNo(
                        bill.getCarLogNo()
                );

        // 일반차량 또는 입주민이 등록하지 않은 차량은 알림 대상이 아니다.
        if (visitRegistrant == null) {
            return;
        }

        String formattedAmount =
                NumberFormat.getNumberInstance(Locale.KOREA)
                        .format(bill.getBillAmount());

        MemNoticeDTO notice = new MemNoticeDTO();
        notice.setRecipientMemberNo(
                visitRegistrant.getMemberNo()
        );
        notice.setReferenceTable("bill");
        notice.setReferenceNo(bill.getBillNo());
        notice.setNoticeType(
                "VISIT_PARKING_FEE_ISSUED"
        );
        notice.setTitle(
                "방문차량 주차요금 발생"
        );
        notice.setMessage(
                "등록하신 방문차량 "
                        + visitRegistrant.getCarNo()
                        + "에 주차요금 "
                        + formattedAmount
                        + "원이 부과되었습니다."
        );

        memNoticeService.createNotification(notice);
    }

    // 로그인한 입주민이 등록한 방문차량의 주차요금 고지서 조회
    public BillDTO findResidentBill(int billNo, String loginId) {
        if (billNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "고지서 번호를 확인해 주세요."
            );
        }

        BillDTO condition = new BillDTO();
        condition.setBillNo(billNo);
        condition.setLoginId(loginId);

        BillDTO bill = billingMapper.findResidentBill(condition);

        if (bill == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "결제할 수 있는 고지서를 찾을 수 없습니다."
            );
        }

        return bill;
    }

    // 토스페이먼츠 결제를 승인하고 정산서를 결제완료 상태로 변경한다
    @Transactional
    public TossPaymentDTO confirmPayment(TossPaymentDTO dto) {
        if(dto == null
                || dto.getPaymentKey() == null
                || dto.getPaymentKey().isBlank()
                || dto.getOrderId() == null
                || dto.getOrderId().isBlank()
                || dto.getAmount() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "결제 승인정보를 확인해주세요."
            );
        }

        // 결제 요청 전에 저장한 주문번호로 원본 정산서를 조회한다.
        BillDTO bill = billingMapper.findByPaymentOrderId(dto.getOrderId());

        if (bill == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "결제 주문정보를 찾을 수 없습니다."
            );
        }

        // 이미 같은 결제키로 승인된 요청이면 기존 결제 결과를 반환한다.
        if("PAID".equalsIgnoreCase(bill.getBillStatus())) {
            if (dto.getPaymentKey().equals(bill.getPaymentKey())) {
                dto.setMethod(bill.getPaymentMethod());
                return dto;
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 결제가 완료된 정산서입니다."
            );
        }

        // 주소로 전달된 금액과 백엔드에 저장된 정산금액이 같은지 검증한다.
        if (bill.getBillAmount().compareTo(dto.getAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "결제금액이 정산금액과 일치하지 않습니다."
            );
        }

        String authorization = Base64.getEncoder()
                                .encodeToString(
                                        (tossSecretKey + ":")
                                                .getBytes(StandardCharsets.UTF_8)
                                );

        TossPaymentDTO approvedPayment;

        try {
            // 프론트엔드 값이 아닌 백엔드 정산금액으로 토스 결제 승인을 요청한다.
            approvedPayment =
                    tossRestClient.post()
                            .uri("/v1/payments/confirm")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Basic " + authorization
                            )
                            .header(
                                    "Idempotency-Key",
                                    bill.getPaymentOrderId()
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(
                                    Map.of(
                                            "paymentKey",
                                            dto.getPaymentKey(),
                                            "orderId",
                                            bill.getPaymentOrderId(),
                                            "amount",
                                            bill.getBillAmount()
                                    )
                            )
                            .retrieve()
                            .body(TossPaymentDTO.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "토스페이먼츠 결제 승인이 실패했습니다."
            );
        }

        if (approvedPayment == null
                || approvedPayment.getTotalAmount() == null
                || !dto.getPaymentKey().equals(approvedPayment.getPaymentKey())
                || !bill.getPaymentOrderId().equals(approvedPayment.getOrderId())
                || bill.getBillAmount().compareTo(approvedPayment.getTotalAmount()) != 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "토스페이먼츠 승인 결과가 정산정보와 일치하지 않습니다."
            );
        }

        // 승인된 결제정보를 정산서에 저장하고 결제완료로 변경한다.
        bill.setPaymentKey(
                approvedPayment.getPaymentKey()
        );
        bill.setPaymentMethod(
                approvedPayment.getMethod()
        );

        if (billingMapper.markPaid(bill) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "결제 결과를 저장하지 못했습니다."
            );
        }

        return approvedPayment;
    }

    // 등록된 요금 규칙 목록 조회
    public List<FeeRuleDTO> findFeeRuleList() {
        return billingMapper.findFeeRuleList();
    }

    // 새로운 요금 규칙 등록
    @Transactional
    public FeeRuleDTO createFeeRule(FeeRuleDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "요금 규칙 정보를 입력해 주세요."
            );
        }

        String ruleName =
                dto.getRuleName() == null
                        ? ""
                        : dto.getRuleName().trim();

        if (ruleName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "요금 규칙명을 입력해 주세요."
            );
        }

        if (dto.getUnitMinutes() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "과금 단위는 1분 이상이어야 합니다."
            );
        }

        if (dto.getUnitFee() == null
                || dto.getUnitFee().compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "단위요금은 0원 이상이어야 합니다."
            );
        }

        if (dto.getDailyMaxFee() != null
                && dto.getDailyMaxFee().compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "일일 최대요금은 0원 이상이어야 합니다."
            );
        }

        if (dto.getEffectiveFrom() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "적용 시작일시를 입력해 주세요."
            );
        }

        if (dto.getEffectiveTo() != null
                && !dto.getEffectiveTo().isAfter(dto.getEffectiveFrom())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "적용 종료일시는 시작일시보다 뒤여야 합니다."
            );
        }

        dto.setRuleName(ruleName);

        try {
            if (billingMapper.insertFeeRule(dto) != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "요금 규칙을 등록하지 못했습니다."
                );
            }
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록된 요금 규칙명입니다."
            );
        }

        return dto;
    }

    // 요금 규칙의 적용 종료일시 수정
    @Transactional
    public FeeRuleDTO updateFeeRuleEffectiveTo(
            int feeRuleNo,
            LocalDateTime effectiveTo
    ) {
        if (feeRuleNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "요금 규칙 번호를 확인해 주세요."
            );
        }

        FeeRuleDTO feeRule =
                billingMapper.findFeeRuleByNo(feeRuleNo);

        if (feeRule == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "요금 규칙을 찾을 수 없습니다."
            );
        }

        if (effectiveTo != null
                && !effectiveTo.isAfter(feeRule.getEffectiveFrom())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "적용 종료일시는 시작일시보다 뒤여야 합니다."
            );
        }

        feeRule.setEffectiveTo(effectiveTo);

        if (billingMapper.updateFeeRuleEffectiveTo(feeRule) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "요금 규칙의 적용 종료일시를 수정하지 못했습니다."
            );
        }

        return feeRule;
    }

    // 예약 상태의 요금 규칙 전체 수정
    @Transactional
    public FeeRuleDTO updateScheduledFeeRule(
            int feeRuleNo,
            FeeRuleDTO dto
    ) {
        if (feeRuleNo <= 0 || dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "요금 규칙 정보를 확인해 주세요."
            );
        }

        FeeRuleDTO feeRule =
                billingMapper.findFeeRuleByNo(feeRuleNo);

        if (feeRule == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "요금 규칙을 찾을 수 없습니다."
            );
        }

        if (!feeRule.getEffectiveFrom().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "예약 상태의 요금 규칙만 전체 수정할 수 있습니다."
            );
        }

        String ruleName =
                dto.getRuleName() == null
                        ? ""
                        : dto.getRuleName().trim();

        if (ruleName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "요금 규칙명을 입력해 주세요."
            );
        }

        if (dto.getUnitMinutes() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "과금 단위는 1분 이상이어야 합니다."
            );
        }

        if (dto.getUnitFee() == null
                || dto.getUnitFee().compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "단위요금은 0원 이상이어야 합니다."
            );
        }

        if (dto.getDailyMaxFee() != null
                && dto.getDailyMaxFee().compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "일일 최대요금은 0원 이상이어야 합니다."
            );
        }

        if (dto.getEffectiveFrom() == null
                || !dto.getEffectiveFrom().isAfter(LocalDateTime.now())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "예약 규칙의 적용 시작일시는 현재 시각보다 뒤여야 합니다."
            );
        }

        if (dto.getEffectiveTo() != null
                && !dto.getEffectiveTo().isAfter(dto.getEffectiveFrom())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "적용 종료일시는 시작일시보다 뒤여야 합니다."
            );
        }

        dto.setFeeRuleNo(feeRuleNo);
        dto.setRuleName(ruleName);

        try {
            if (billingMapper.updateScheduledFeeRule(dto) != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "예약 요금 규칙을 수정하지 못했습니다."
                );
            }
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록된 요금 규칙명입니다."
            );
        }

        return billingMapper.findFeeRuleByNo(feeRuleNo);
    }

    // 현재 주차 중인 비입주민 차량의 정산 목록과 출차 가능 여부를 조회한다.
    public List<BillDTO> findAdminBillingList() {
        List<BillDTO> billingList =
                billingMapper.findAdminBillingList();

        for (BillDTO billing : billingList) {
            setExitAllowed(billing);
        }

        return billingList;
    }

    // 입출차 기록 번호로 관리자 정산 상세정보를 조회한다.
    public BillDTO findAdminBillingDetail(int carLogNo) {
        if (carLogNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "입출차 기록 번호를 확인해 주세요."
            );
        }

        BillDTO billing =
                billingMapper.findAdminBillingDetail(carLogNo);

        if (billing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "정산 상세정보를 찾을 수 없습니다."
            );
        }

        setExitAllowed(billing);

        return billing;
    }

    // 미정산·미결제 차량의 무료시간을 수정하고 필요한 경우 정산금액을 다시 계산한다.
    @Transactional
    public BillDTO updateAdminBilling(
            int carLogNo,
            Integer freeTime
    ) {
        if (carLogNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "입출차 기록 번호를 확인해 주세요."
            );
        }

        if (freeTime == null || freeTime < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "무료시간은 0분 이상이어야 합니다."
            );
        }

        // 차량·정산서·적용 요금 규칙을 함께 조회한다.
        BillDTO billing =
                billingMapper.findAdminBillingDetail(carLogNo);

        if (billing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "정산 상세정보를 찾을 수 없습니다."
            );
        }

        // 정산이 완료된 차량의 무료시간은 변경하지 않는다.
        if ("PAID".equalsIgnoreCase(billing.getBillStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "정산완료 차량은 수정할 수 없습니다."
            );
        }

        BillDTO updateDto = new BillDTO();
        updateDto.setCarLogNo(carLogNo);
        updateDto.setFreeTime(freeTime);

        // 정산완료 전 입출차 기록의 무료시간을 변경한다.
        if (billingMapper.updateAdminFreeTime(updateDto) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "무료시간을 수정하지 못했습니다."
            );
        }

        // 정산서가 아직 없으면 무료시간만 저장하고 상세정보를 다시 조회한다.
        if (billing.getBillNo() == null) {
            return findAdminBillingDetail(carLogNo);
        }

        // 기존 정산서에 저장된 요금 규칙으로 다시 계산할 수 있는지 확인한다.
        if (billing.getInTime() == null
                || billing.getUnitMinutes() == null
                || billing.getUnitMinutes() <= 0
                || billing.getUnitFee() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "정산금액을 다시 계산할 수 없습니다."
            );
        }

        // 새 무료시간과 기존 요금 규칙을 사용해 과금시간과 금액을 다시 계산한다.
        calculateBill(
                billing,
                billing.getInTime(),
                freeTime,
                billing.getUnitMinutes(),
                billing.getUnitFee(),
                billing.getDailyMaxFee()
        );

        if (billingMapper.updateUnpaidAmount(billing) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "정산금액을 갱신하지 못했습니다."
            );
        }

        // 재계산 결과가 0원이면 별도 결제 없이 정산완료로 처리한다.
        if (billing.getBillAmount().compareTo(BigDecimal.ZERO) == 0
                && billingMapper.markZeroAmountPaid(billing.getBillNo()) != 1
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "무료 정산을 완료하지 못했습니다."
            );
        }

        return findAdminBillingDetail(carLogNo);
    }

    // 관리자가 출차 완료된 정산서를 지난 기록으로 직접 이동한다.
    public void moveAdminBillingToTrash(int billNo) {
        if (billNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "정산서 번호를 확인해 주세요."
            );
        }

        try {
            trashService.moveBill(billNo,"MANUAL");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    e.getMessage()
            );
        }
    }

    // 출차 완료 후 3개월이 지난 완료 정산서를 지난 기록으로 자동 이동한다.
    public void moveOldPaidBillsToTrash() {
        List<Integer> billNos =
                billingMapper.findOldPaidBillNosForTrash();

        for (Integer billNo : billNos) {
            try {
                trashService.moveBill(billNo, "SCHEDULED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 결제상태·출차여부·결제 완료시각을 기준으로 출차 가능정보를 설정한다.
    private void setExitAllowed(BillDTO billing) {
        LocalDateTime paidAt = billing.getPaidAt();

        LocalDateTime exitAllowedUntil =
                paidAt == null
                        ? null
                        : paidAt.plusMinutes(EXIT_ALLOWED_MINUTES);

        billing.setExitAllowedUntil(exitAllowedUntil);

        billing.setExitAllowed(
                billing.getOutTime() == null
                        && "PAID".equalsIgnoreCase(billing.getBillStatus())
                        && exitAllowedUntil != null
                        && !LocalDateTime.now().isAfter(exitAllowedUntil)
        );
    }

    // 키오스크 위치에 맞는 활성 출차 게이트 번호를 조회한다.
    public int findExitGateNo(
            int carLogNo,
            Integer kioskNo
    ) {
        if (carLogNo <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "입출차 기록 번호를 확인해 주세요."
            );
        }

        Integer exitGateNo;

        // B1 키오스크 1은 왼쪽 출차 게이트를 사용한다.
        if (Integer.valueOf(1).equals(kioskNo)) {
            exitGateNo =
                    billingMapper
                            .findExitGateNoByCarLogNoAndGateCode(
                                    carLogNo,
                                    "B1-OUT-1"
                            );

            // B1 키오스크 2는 오른쪽 출차 게이트를 사용한다.
        } else if (Integer.valueOf(2).equals(kioskNo)) {
            exitGateNo =
                    billingMapper
                            .findExitGateNoByCarLogNoAndGateCode(
                                    carLogNo,
                                    "B1-OUT-2"
                            );

            // 키오스크 3, 4와 기존 호출은 원래 출구 조회 방식을 유지한다.
        } else {
            exitGateNo =
                    billingMapper.findExitGateNoByCarLogNo(
                            carLogNo
                    );
        }

        if (exitGateNo == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이용할 수 있는 출차 게이트를 찾을 수 없습니다."
            );
        }

        return exitGateNo;
    }

    // 입차시각부터 현재까지의 주차시간에서 무료시간을 제외하고 요금을 계산
    private void calculateBill(
            BillDTO bill,
            LocalDateTime inTime,
            Integer freeTime,
            int unitMinutes,
            BigDecimal unitFee,
            BigDecimal dailyMaxFee
    ) {
        // 입차시각부터 현재까지의 주차시간을 초 단위로 계산한다.
        long parkingSeconds =
                Duration.between(
                        inTime,
                        LocalDateTime.now()
                ).getSeconds();

        // 사용한 시간이 일부 분이라도 있으면 1분으로 올림한다.
        long parkingMinutes =
                Math.max(0, (parkingSeconds + 59) / 60);

        // car_log.free_time이 NULL이면 무료시간을 0분으로 처리한다.
        int appliedFreeTime =
                freeTime == null ? 0 : Math.max(0, freeTime);

        // 전체 주차시간에서 차량별 무료시간을 제외한다.
        long chargeMinutes =
                Math.max(0, parkingMinutes - appliedFreeTime);

        // 과금시간에 요금 단위와 일일 최대요금을 적용한다.
        BigDecimal billAmount =
                calculateAmount(
                        chargeMinutes,
                        unitMinutes,
                        unitFee,
                        dailyMaxFee
                );

        bill.setChargeMinutes(Math.toIntExact(chargeMinutes));
        bill.setBillAmount(billAmount);
    }

    // 과금시간을 24시간 단위로 나누어 일일 최대요금을 적용
    private BigDecimal calculateAmount(
            long chargeMinutes,
            int unitMinutes,
            BigDecimal unitFee,
            BigDecimal dailyMaxFee
    ) {
        if (chargeMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        // 과금시간에서 완전히 지난 24시간과 나머지 시간을 분리한다.
        long fullDays = chargeMinutes / MINUTES_PER_DAY;

        long remainingMinutes = chargeMinutes % MINUTES_PER_DAY;

        // 24시간의 단위요금을 계산한다.
        BigDecimal fullDayFee =
                calculateUnitFee(
                        MINUTES_PER_DAY,
                        unitMinutes,
                        unitFee
                );

        // 계산금액이 일일 최대요금보다 크면 최대요금을 적용한다.
        if (dailyMaxFee != null) {
            fullDayFee = fullDayFee.min(dailyMaxFee);
        }

        BigDecimal billAmount =
                fullDayFee.multiply(BigDecimal.valueOf(fullDays));

        // 24시간으로 나누고 남은 시간의 요금을 계산한다.
        if (remainingMinutes > 0) {
            BigDecimal remainingFee =
                    calculateUnitFee(
                            remainingMinutes,
                            unitMinutes,
                            unitFee
                    );

            if (dailyMaxFee != null) {
                remainingFee = remainingFee.min(dailyMaxFee);
            }

            billAmount = billAmount.add(remainingFee);
        }
        return billAmount;
    }

    // 과금시간을 요금 부과 단위로 올림하여 금액 계산
    private BigDecimal calculateUnitFee(
            long minutes,
            int unitMinutes,
            BigDecimal unitFee
    ) {
        BigDecimal chargedUnits =
                BigDecimal.valueOf(minutes)
                        .divide(
                                BigDecimal.valueOf(unitMinutes),
                                0,
                                RoundingMode.CEILING
                        );

        return chargedUnits.multiply(unitFee);
    }

    // 요금 계산에 사용할 활성 요금 규칙의 필수값 확인
    private void validateFeeRule(FeeRuleDTO feeRule) {
        if (feeRule == null
                || feeRule.getFeeRuleNo() == null
                || feeRule.getUnitMinutes() <= 0
                || feeRule.getUnitFee() == null
                || feeRule.getUnitFee()
                .compareTo(BigDecimal.ZERO) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "사용 가능한 요금 규칙이 없습니다."
            );
        }
    }

    // 차량번호의 공백을 제거하고 형식을 확인
    private String normalizeCarNo(String carNo) {
        String normalizedCarNo =
                carNo == null
                        ? ""
                        : carNo.trim()
                        .replaceAll("\\s+", "");

        if (!normalizedCarNo.matches("^(?:[가-힣]{2})?\\d{2,3}[가-힣]\\d{4}$")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "차량번호 형식이 올바르지 않습니다."
            );
        }
        return normalizedCarNo;
    }
}
