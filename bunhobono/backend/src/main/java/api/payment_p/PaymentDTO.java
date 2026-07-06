package api.payment_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDTO {

    int paymentNo, chargeNo, paymentAmount;
    String paymentMethod, paymentStatus, transactionNo;
    LocalDateTime paidAt;
}
