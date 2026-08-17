package api.mem_purchase_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MemPurchaseDTO {

    private Integer memPurchaseNo;
    private Integer memberNo;
    private String snapshotLoginId;
    private String snapshotMemberName;
    private Integer snapshotDong;
    private Integer snapshotHo;
    private String purchaseType;
    private Integer purchaseQuantity;
    private BigDecimal purchaseAmount;
    private String purchaseStatus;
    private String paymentOrderId;
    private String paymentKey;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @Data
    public static class Payment {
        private String paymentKey;
        private String orderId;
        private BigDecimal amount;
        private BigDecimal totalAmount;
        private String method;
    }
}
