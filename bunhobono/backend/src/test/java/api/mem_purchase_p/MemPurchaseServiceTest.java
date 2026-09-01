package api.mem_purchase_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemPurchaseServiceTest {
    @Mock MemPurchaseMapper mapper;
    @InjectMocks MemPurchaseService service;

    @Test
    @DisplayName("UT-BE-MEMPURCHASE-001 | 이번 달 결제 완료 추가시간을 조회한다")
    void getMonthlyPaidVisitQuantity_returnsSum() {
        when(mapper.sumMonthlyPaidVisitMinutes(any())).thenReturn(600);
        assertEquals(600, service.getMonthlyPaidVisitQuantity("resident01"));
    }

    @Test
    @DisplayName("UT-BE-MEMPURCHASE-002 | 선택한 방문시간의 구매 주문을 생성한다")
    void createOrder_calculatesAmountAndInsertsOrder() {
        MemPurchaseDTO request = new MemPurchaseDTO();
        request.setPurchaseQuantity(120);
        MemPurchaseDTO member = new MemPurchaseDTO();
        member.setMemberNo(1);
        member.setSnapshotLoginId("resident01");
        when(mapper.findActiveResident(any())).thenReturn(member);
        when(mapper.insert(member)).thenReturn(1);

        MemPurchaseDTO result = service.createOrder("resident01", request);
        assertSame(member, result);
        assertEquals(BigDecimal.valueOf(5000), result.getPurchaseAmount());
        assertEquals("UNPAID", result.getPurchaseStatus());
        assertEquals("VISIT_PARKING_MINUTES", result.getPurchaseType());
    }

    @Test
    @DisplayName("UT-BE-MEMPURCHASE-003 | 지원하지 않는 구매시간을 거부한다")
    void createOrder_rejectsInvalidQuantity() {
        MemPurchaseDTO request = new MemPurchaseDTO();
        request.setPurchaseQuantity(30);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createOrder("resident01", request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("UT-BE-MEMPURCHASE-004 | 같은 결제키의 완료 주문은 중복 승인하지 않는다")
    void confirmPayment_returnsIdempotentPaidResult() {
        MemPurchaseDTO.Payment payment = new MemPurchaseDTO.Payment();
        payment.setPaymentKey("key");
        payment.setOrderId("order");
        payment.setAmount(BigDecimal.valueOf(5000));
        MemPurchaseDTO purchase = new MemPurchaseDTO();
        purchase.setSnapshotLoginId("resident01");
        purchase.setPurchaseStatus("PAID");
        purchase.setPaymentKey("key");
        purchase.setPaymentMethod("카드");
        purchase.setPurchaseAmount(BigDecimal.valueOf(5000));
        when(mapper.findByPaymentOrderId(any())).thenReturn(purchase);

        MemPurchaseDTO.Payment result = service.confirmPayment("resident01", payment);
        assertSame(payment, result);
        assertEquals("카드", result.getMethod());
        assertEquals(BigDecimal.valueOf(5000), result.getTotalAmount());
        verify(mapper, never()).markPaid(any());
    }

    @Test @DisplayName("UT-BE-MEMPURCHASE-005 | 추가시간 조회는 로그인 ID를 필수로 요구한다")
    void getMonthlyPaidVisitQuantity_rejectsBlankLogin(){ assertStatus(HttpStatus.UNAUTHORIZED,() -> service.getMonthlyPaidVisitQuantity(" ")); verifyNoInteractions(mapper); }

    @Test @DisplayName("UT-BE-MEMPURCHASE-006 | 구매 주문은 로그인과 활성 입주민을 요구한다")
    void createOrder_rejectsInvalidMember(){ MemPurchaseDTO request=new MemPurchaseDTO(); request.setPurchaseQuantity(120); assertStatus(HttpStatus.UNAUTHORIZED,() -> service.createOrder(" ",request)); when(mapper.findActiveResident(any())).thenReturn(null); assertStatus(HttpStatus.NOT_FOUND,() -> service.createOrder("resident01",request)); }

    @Test @DisplayName("UT-BE-MEMPURCHASE-007 | 구매 주문 저장 실패를 충돌로 처리한다")
    void createOrder_rejectsInsertFailure(){ MemPurchaseDTO request=new MemPurchaseDTO(); request.setPurchaseQuantity(600); MemPurchaseDTO member=new MemPurchaseDTO(); member.setMemberNo(1); when(mapper.findActiveResident(any())).thenReturn(member); when(mapper.insert(member)).thenReturn(0); assertStatus(HttpStatus.CONFLICT,() -> service.createOrder("resident01",request)); assertEquals(BigDecimal.valueOf(24000),member.getPurchaseAmount()); }

    @Test @DisplayName("UT-BE-MEMPURCHASE-008 | 결제 승인 필수정보를 검증한다")
    void confirmPayment_rejectsInvalidInput(){ assertStatus(HttpStatus.BAD_REQUEST,() -> service.confirmPayment("resident01",null)); assertStatus(HttpStatus.BAD_REQUEST,() -> service.confirmPayment(" ",payment("key","order",5000))); verifyNoInteractions(mapper); }

    @Test @DisplayName("UT-BE-MEMPURCHASE-009 | 존재하지 않거나 다른 입주민의 구매 주문을 거부한다")
    void confirmPayment_rejectsWrongOrderOwner(){ MemPurchaseDTO.Payment p=payment("key","order",5000); assertStatus(HttpStatus.NOT_FOUND,() -> service.confirmPayment("resident01",p)); MemPurchaseDTO purchase=new MemPurchaseDTO(); purchase.setSnapshotLoginId("other"); when(mapper.findByPaymentOrderId(any())).thenReturn(purchase); assertStatus(HttpStatus.FORBIDDEN,() -> service.confirmPayment("resident01",p)); }

    @Test @DisplayName("UT-BE-MEMPURCHASE-010 | 완료 주문의 다른 결제키와 결제불가 상태를 충돌로 처리한다")
    void confirmPayment_rejectsInvalidStatus(){ MemPurchaseDTO.Payment p=payment("new","order",5000); MemPurchaseDTO paid=purchase("resident01","PAID",5000); paid.setPaymentKey("old"); when(mapper.findByPaymentOrderId(any())).thenReturn(paid); assertStatus(HttpStatus.CONFLICT,() -> service.confirmPayment("resident01",p)); MemPurchaseDTO cancelled=purchase("resident01","CANCELLED",5000); when(mapper.findByPaymentOrderId(any())).thenReturn(cancelled); assertStatus(HttpStatus.CONFLICT,() -> service.confirmPayment("resident01",p)); }

    @Test @DisplayName("UT-BE-MEMPURCHASE-011 | 요청 결제금액과 구매금액 불일치를 거부한다")
    void confirmPayment_rejectsAmountMismatch(){ MemPurchaseDTO.Payment p=payment("key","order",4000); when(mapper.findByPaymentOrderId(any())).thenReturn(purchase("resident01","UNPAID",5000)); assertStatus(HttpStatus.BAD_REQUEST,() -> service.confirmPayment("resident01",p)); verify(mapper,never()).markPaid(any()); }

    private MemPurchaseDTO.Payment payment(String key,String order,long amount){ MemPurchaseDTO.Payment p=new MemPurchaseDTO.Payment(); p.setPaymentKey(key); p.setOrderId(order); p.setAmount(BigDecimal.valueOf(amount)); return p; }
    private MemPurchaseDTO purchase(String login,String status,long amount){ MemPurchaseDTO p=new MemPurchaseDTO(); p.setSnapshotLoginId(login); p.setPurchaseStatus(status); p.setPurchaseAmount(BigDecimal.valueOf(amount)); return p; }
    private void assertStatus(HttpStatus status,Runnable action){ ResponseStatusException e=assertThrows(ResponseStatusException.class,action::run); assertEquals(status,e.getStatusCode()); }
}
