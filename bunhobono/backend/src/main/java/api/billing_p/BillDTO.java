package api.billing_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillDTO {

    // bill 테이블
    private Integer billNo;
    private Integer carLogNo;
    private Integer feeRuleNo;
    private Integer kioskNo;

    private Integer chargeMinutes;
    private BigDecimal billAmount;
    private String billStatus;

    private String paymentOrderId;
    private String paymentKey;
    private String paymentMethod;

    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;

    // 정산 화면 표시용 입출차 정보
    private Integer cameraDataNo;
    private String carNo;
    private String carKind;
    private LocalDateTime inTime;
    private LocalDateTime outTime;
    private Integer freeTime;
    private Integer parkingNo;
    private String parkingCode;

    // 적용 요금 규칙
    private String ruleName;
    private Integer unitMinutes;
    private BigDecimal unitFee;
    private BigDecimal dailyMaxFee;

    // 관리자 정산 화면 표시용 출차 가능정보
    private boolean exitAllowed;
    private LocalDateTime exitAllowedUntil;
}
