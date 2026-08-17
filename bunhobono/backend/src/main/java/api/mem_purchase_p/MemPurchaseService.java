package api.mem_purchase_p;

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
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class MemPurchaseService {

    private static final String VISIT_REGISTRATION_COUNT =
            "VISIT_REGISTRATION_COUNT";

    @Resource
    private MemPurchaseMapper memPurchaseMapper;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    private final RestClient tossRestClient =
            RestClient.builder()
                    .baseUrl("https://api.tosspayments.com")
                    .build();

    // 로그인한 입주민 세대의 이번 달 결제 완료 추가 횟수를 조회한다.
    public int getMonthlyPaidVisitQuantity(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        MemPurchaseDTO condition = new MemPurchaseDTO();
        condition.setSnapshotLoginId(loginId);

        return memPurchaseMapper
                .sumMonthlyPaidVisitQuantity(condition);
    }

    // 로그인한 입주민의 방문차량 추가 횟수 구매 주문을 생성한다.
    @Transactional
    public MemPurchaseDTO createOrder(
            String loginId,
            MemPurchaseDTO request
    ) {
        if (loginId == null || loginId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        Integer quantity = request == null
                ? null
                : request.getPurchaseQuantity();

        BigDecimal purchaseAmount =
                calculatePurchaseAmount(quantity);

        MemPurchaseDTO condition = new MemPurchaseDTO();
        condition.setSnapshotLoginId(loginId);

        MemPurchaseDTO member =
                memPurchaseMapper.findActiveResident(condition);

        if (member == null || member.getMemberNo() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "구매 가능한 입주민 정보를 찾을 수 없습니다."
            );
        }

        member.setPurchaseType(VISIT_REGISTRATION_COUNT);
        member.setPurchaseQuantity(quantity);
        member.setPurchaseAmount(purchaseAmount);
        member.setPurchaseStatus("UNPAID");
        member.setPaymentOrderId(
                "MEM-PURCHASE-"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
        );

        if (memPurchaseMapper.insert(member) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "구매 주문을 생성하지 못했습니다."
            );
        }

        return member;
    }

    // 토스페이먼츠 결제를 승인하고 구매 주문을 결제완료로 변경한다.
    @Transactional
    public MemPurchaseDTO.Payment confirmPayment(
            String loginId,
            MemPurchaseDTO.Payment payment
    ) {
        validatePayment(loginId, payment);

        MemPurchaseDTO condition = new MemPurchaseDTO();
        condition.setPaymentOrderId(payment.getOrderId());

        MemPurchaseDTO purchase =
                memPurchaseMapper.findByPaymentOrderId(condition);

        if (purchase == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "구매 주문정보를 찾을 수 없습니다."
            );
        }

        if (!loginId.equals(purchase.getSnapshotLoginId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인의 구매 주문만 결제할 수 있습니다."
            );
        }

        if ("PAID".equalsIgnoreCase(purchase.getPurchaseStatus())) {
            if (payment.getPaymentKey().equals(purchase.getPaymentKey())) {
                payment.setMethod(purchase.getPaymentMethod());
                payment.setTotalAmount(purchase.getPurchaseAmount());
                return payment;
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 결제가 완료된 구매 주문입니다."
            );
        }

        if (!"UNPAID".equalsIgnoreCase(purchase.getPurchaseStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "결제할 수 없는 구매 주문입니다."
            );
        }

        if (purchase.getPurchaseAmount().compareTo(payment.getAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "결제금액이 구매금액과 일치하지 않습니다."
            );
        }

        String authorization = Base64.getEncoder()
                .encodeToString(
                        (tossSecretKey + ":")
                                .getBytes(StandardCharsets.UTF_8)
                );

        MemPurchaseDTO.Payment approvedPayment;

        try {
            approvedPayment = tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Basic " + authorization
                    )
                    .header(
                            "Idempotency-Key",
                            purchase.getPaymentOrderId()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "paymentKey",
                                    payment.getPaymentKey(),
                                    "orderId",
                                    purchase.getPaymentOrderId(),
                                    "amount",
                                    purchase.getPurchaseAmount()
                            )
                    )
                    .retrieve()
                    .body(MemPurchaseDTO.Payment.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "토스페이먼츠 결제 승인이 실패했습니다."
            );
        }

        if (approvedPayment == null
                || approvedPayment.getTotalAmount() == null
                || !payment.getPaymentKey().equals(
                        approvedPayment.getPaymentKey()
                )
                || !purchase.getPaymentOrderId().equals(
                        approvedPayment.getOrderId()
                )
                || purchase.getPurchaseAmount().compareTo(
                        approvedPayment.getTotalAmount()
                ) != 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "토스페이먼츠 승인 결과가 구매정보와 일치하지 않습니다."
            );
        }

        purchase.setPaymentKey(approvedPayment.getPaymentKey());
        purchase.setPaymentMethod(approvedPayment.getMethod());

        if (memPurchaseMapper.markPaid(purchase) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "결제 결과를 저장하지 못했습니다."
            );
        }

        return approvedPayment;
    }

    private BigDecimal calculatePurchaseAmount(Integer quantity) {
        if (quantity == null) {
            throw invalidQuantity();
        }

        return switch (quantity) {
            case 1 -> BigDecimal.valueOf(5000);
            case 5 -> BigDecimal.valueOf(20000);
            case 10 -> BigDecimal.valueOf(43000);
            default -> throw invalidQuantity();
        };
    }

    private ResponseStatusException invalidQuantity() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "구매 횟수는 1회, 5회, 10회 중에서 선택해 주세요."
        );
    }

    private void validatePayment(
            String loginId,
            MemPurchaseDTO.Payment payment
    ) {
        if (loginId == null
                || loginId.isBlank()
                || payment == null
                || payment.getPaymentKey() == null
                || payment.getPaymentKey().isBlank()
                || payment.getOrderId() == null
                || payment.getOrderId().isBlank()
                || payment.getAmount() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "결제 승인정보를 확인해 주세요."
            );
        }
    }
}
