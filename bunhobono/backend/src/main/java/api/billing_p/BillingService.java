package api.billing_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillingService {

    // 일일 최대요금 계산에 사용하는 24시간의 분 단위 값
    private static final int MINUTES_PER_DAY = 1440;

    @Resource
    private BillingMapper billingMapper;

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
        }

        // INSERT 또는 UPDATE 결과가 반영된 최종 정산서를 다시 조회한다.
        return billingMapper.findByCarLogNo(
                parkingLog.getCarLogNo()
        );
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
