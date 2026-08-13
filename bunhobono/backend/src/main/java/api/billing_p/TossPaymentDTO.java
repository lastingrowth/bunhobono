package api.billing_p;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TossPaymentDTO {

    // 토스페이먼츠가 발급한 결제 고유키
    private String paymentKey;

    // 결제 요청 전에 정산서에 저장한 주문번호
    private String orderId;

    // 토스페이먼츠에 승인을 요청할 결제금액
    private BigDecimal amount;

    // 결제 승인 후 토스페이먼츠가 반환한 총 결제금액
    private BigDecimal totalAmount;

    // 결제 승인 후 토스페이먼츠가 반환할 결제수단
    private String method;
}
