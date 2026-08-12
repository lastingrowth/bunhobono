package api.billing_p;

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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    // 일일 최대요금 계산에 사용하는 24시간의 분 단위 값
    private static final int MINUTES_PER_DAY = 1440;

    @Resource
    private BillingMapper billingMapper;

    // 로컬 설정파일에 저장한 토스페이먼츠 시크릿 키
    @Value("${toss.secret-key}")
    private String tossSecretKey;

    // 토스페이먼츠 결제 승인 API 호출에 사용하는 HTTP 클라이언트
    private final RestClient tossRestClient =
            RestClient.builder()
                    .baseUrl("https://api.tosspayments.com")
                    .build();

    // 차량번호 뒤 4자리로 현재 주차 중인 차량 목록 조회
    public List<BillDTO> findParkingCars(String lastFourDigits) {
        String digits = lastFourDigits == null
                ? ""
                : lastFourDigits.trim();

        if (!digits.matches("^\\d{4}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "차량번호 뒤 4자리를 입력해주세요."
            );
        }

        List<BillDTO> parkingCars = billingMapper.findOpenCarLogsByLastFourDigits(digits);

        if (parkingCars.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "일치하는 주차 차량을 찾을 수 없습니다."
            );
        }
        return parkingCars;
    }

    // 차량번호로 현재 주차 기록을 찾아 정산서를 생성하거나 갱신
    @Transactional
    public BillDTO createOrRefreshBill(
            String carNo,
            Integer kioskNo
    ) {
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

        // 같은 입출차 기록으로 이미 생성된 정산서가 있는지 조회한다.
        BillDTO bill = billingMapper.findByCarLogNo(parkingLog.getCarLogNo());

        // 결제가 끝난 정산서는 금액을 다시 계산하지 않고 그대로 반환한다.
        if (bill != null && "PAID".equalsIgnoreCase(bill.getBillStatus())) {
            return bill;
        }

        if (bill == null) {
            // 최초 정산이면 현재 활성화된 요금 규칙을 적용한다.
            FeeRuleDTO feeRule = billingMapper.findActiveFeeRule();

            validateFeeRule(feeRule);

            bill = new BillDTO();
            bill.setCarLogNo(parkingLog.getCarLogNo());
            bill.setFeeRuleNo(feeRule.getFeeRuleNo());
            bill.setKioskNo(kioskNo);
            bill.setBillStatus("UNPAID");

            // 입차시각, 무료시간, 요금 규칙으로 과금시간과 금액을 계산한다.
            calculateBill(
                    bill,
                    parkingLog.getInTime(),
                    parkingLog.getFreeTime(),
                    feeRule.getUnitMinutes(),
                    feeRule.getUnitFee(),
                    feeRule.getDailyMaxFee()
            );

            // 계산한 정산서를 bill 테이블에 등록한다.
            if (billingMapper.insert(bill) != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "정산서를 생성하지 못했습니다."
                );
            }
        } else {
            // 기존 미결제 정산서는 처음 적용한 요금 규칙을 유지한다.
            bill.setKioskNo(kioskNo);

            // 시간이 지났을 수 있으므로 현재 시각을 기준으로 금액만 다시 계산한다.
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

        // INSERT 또는 UPDATE 결과가 반영된 최종 정산서를 다시 조회한다.
        return billingMapper.findByCarLogNo(
                parkingLog.getCarLogNo()
        );
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
