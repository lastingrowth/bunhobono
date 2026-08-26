package api.bill_p;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class BillDTO {

    // bill 테이블
    private Integer billNo;
    private Integer carLogNo;

    private Integer snapshotCarLogNo;
    private String snapshotCarNo;

    private Integer feeRuleNo;
    private Integer kioskNo;

    private Integer chargeMinutes;

    // 토스 요청의 amount를 기존 정산 금액 필드로 받는다.
    @JsonAlias("amount")
    private BigDecimal billAmount;

    private String billStatus;

    // 토스 요청·응답의 orderId를 기존 결제 주문번호 필드로 받는다.
    @JsonAlias("orderId")
    private String paymentOrderId;

    // 토스페이먼츠가 발급한 결제 고유키
    private String paymentKey;

    // 토스 응답의 method를 기존 결제수단 필드로 받는다.
    @JsonAlias("method")
    private String paymentMethod;

    // 토스페이먼츠 승인 응답의 결제금액 검증에 사용한다.
    private BigDecimal totalAmount;

    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;

    // 정산 화면 표시용 입출차 정보
    private Integer memberNo;
    private Integer cameraDataNo;
    private String carNo;
    private String carKind;
    private LocalDateTime inTime;
    private LocalDateTime outTime;
    private Integer freeTime;
    private Integer parkingNo;
    private String parkingCode;
    private String spaceCode;
    private String spaceType;
    private OffsetDateTime spaceUpdatedAt;

    // 적용 요금 규칙
    private String ruleName;
    private Integer unitMinutes;
    private BigDecimal unitFee;
    private BigDecimal dailyMaxFee;
    private Integer exitGraceMinutes;

    // 관리자 정산 화면 표시용 출차 가능정보
    private boolean exitAllowed;
    private LocalDateTime exitAllowedUntil;
}
