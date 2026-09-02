package api.bill_p;

import api.carlog_p.CarLogMapper;
import api.feerule_p.FeeRuleDTO;
import api.feerule_p.FeeRuleService;
import api.kiosk_p.KioskDTO;
import api.kiosk_p.KioskService;
import api.mem_notice_p.MemNoticeService;
import api.member_p.MemberDTO;
import api.member_p.MemberService;
import api.trash_p.TrashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillMapper billMapper;

    @Mock
    private CarLogMapper carLogMapper;

    @Mock
    private FeeRuleService feeRuleService;

    @Mock
    private MemNoticeService memNoticeService;

    @Mock
    private MemberService memberService;

    @Mock
    private KioskService kioskService;

    @Mock
    private TrashService trashService;

    @InjectMocks
    private BillService billService;

    @ParameterizedTest(name = "[{index}] digits={0}, kioskNo={1}")
    @MethodSource("invalidParkingBillSearchInputs")
    @DisplayName(
            "UT-BE-BILL-001 | 잘못된 주차 정산서 검색조건을 거부한다"
    )
    void findParkingBills_rejectsInvalidInput(
            String lastFourDigits,
            Integer kioskNo
    ) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> billService.findParkingBills(
                        lastFourDigits,
                        kioskNo
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-002 | 같은 주차장의 최신 주차 정산서만 반환한다"
    )
    void findParkingBills_filtersParkingAndDuplicateSnapshots() {
        KioskDTO kiosk = new KioskDTO();
        kiosk.setParkingNo(10);
        when(kioskService.findByKioskNo(1)).thenReturn(kiosk);

        BillDTO first = parkingBill(1, 101, 10, null);
        BillDTO duplicate = parkingBill(2, 101, 10, null);
        BillDTO otherParking = parkingBill(3, 102, 20, null);
        BillDTO exited = parkingBill(
                4,
                103,
                10,
                LocalDateTime.now()
        );
        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(
                        first,
                        duplicate,
                        otherParking,
                        exited
                ));

        List<BillDTO> result =
                billService.findParkingBills(" 1234 ", 1);

        assertEquals(List.of(first), result);
        verify(billMapper).list(argThat(condition ->
                "1234".equals(condition.getSnapshotCarNo())
        ));
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-003 | 현재 정산서가 다른 주차장에만 있으면 충돌로 처리한다"
    )
    void findParkingBills_rejectsBillFromDifferentParking() {
        KioskDTO kiosk = new KioskDTO();
        kiosk.setParkingNo(10);
        when(kioskService.findByKioskNo(1)).thenReturn(kiosk);
        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(parkingBill(1, 101, 20, null)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> billService.findParkingBills("1234", 1)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-004 | 입차 시점의 기본 요금 규칙으로 미결제 정산서를 생성한다"
    )
    void createEntryBill_createsUnpaidZeroAmountBill() {
        LocalDateTime inTime = LocalDateTime.of(
                2026,
                8,
                31,
                10,
                0
        );
        FeeRuleDTO feeRule = new FeeRuleDTO();
        feeRule.setFeeRuleNo(7);
        when(feeRuleService.findDefaultAt(inTime)).thenReturn(feeRule);
        when(billMapper.insert(any(BillDTO.class))).thenReturn(1);

        billService.createEntryBill(101, "123가1234", inTime);

        ArgumentCaptor<BillDTO> captor =
                ArgumentCaptor.forClass(BillDTO.class);
        verify(billMapper).insert(captor.capture());

        BillDTO inserted = captor.getValue();
        assertEquals(101, inserted.getCarLogNo());
        assertEquals(101, inserted.getSnapshotCarLogNo());
        assertEquals("123가1234", inserted.getSnapshotCarNo());
        assertEquals(7, inserted.getFeeRuleNo());
        assertEquals(0, inserted.getChargeMinutes());
        assertEquals(BigDecimal.ZERO, inserted.getBillAmount());
        assertEquals("UNPAID", inserted.getBillStatus());
        assertEquals(inTime, inserted.getIssuedAt());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-005 | 입차 정산서 저장 실패를 충돌로 처리한다"
    )
    void createEntryBill_rejectsFailedInsert() {
        FeeRuleDTO feeRule = new FeeRuleDTO();
        feeRule.setFeeRuleNo(7);
        when(feeRuleService.findDefaultAt(any())).thenReturn(feeRule);
        when(billMapper.insert(any(BillDTO.class))).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> billService.createEntryBill(
                        101,
                        "123가1234",
                        LocalDateTime.now()
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-006 | 결제 후 출차 유예시간 안이면 기존 정산서를 반환한다"
    )
    void createOrRefreshBill_returnsPaidBillWithinGracePeriod() {
        KioskDTO kiosk = new KioskDTO();
        kiosk.setParkingNo(10);
        when(kioskService.findByKioskNo(1)).thenReturn(kiosk);

        BillDTO paidBill = parkingBill(1, 101, 10, null);
        paidBill.setSnapshotCarNo("123가1234");
        paidBill.setBillStatus("PAID");
        paidBill.setPaidAt(LocalDateTime.now());
        paidBill.setExitGraceMinutes(10);
        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(paidBill));

        BillDTO result =
                billService.createOrRefreshBill("123가1234", 1);

        assertSame(paidBill, result);
        assertTrue(result.isExitAllowed());
        verify(billMapper, never()).update(any());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-007 | 로그인 입주민 소유의 결제 완료 방문차량 정산서를 반환한다"
    )
    void findResidentBill_returnsOwnedPaidVisitBill() {
        BillDTO bill = new BillDTO();
        bill.setBillNo(1);
        bill.setMemberNo(10);
        bill.setCarKind("VISIT");
        bill.setBillStatus("PAID");
        when(billMapper.detail(any(BillDTO.class))).thenReturn(bill);

        MemberDTO member = new MemberDTO();
        member.setMemberNo(10);
        when(memberService.residentMypage("resident01"))
                .thenReturn(member);

        BillDTO result =
                billService.findResidentBill(1, "resident01");

        assertSame(bill, result);
        verify(billMapper, never()).update(any());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-008 | 같은 결제키로 완료된 결제 재요청은 저장 결과를 반환한다"
    )
    void confirmPayment_returnsIdempotentPaidResult() {
        BillDTO request = paymentRequest("payment-key", "ORDER-1", "5000");
        BillDTO saved = paymentRequest("payment-key", "ORDER-1", "5000");
        saved.setBillStatus("PAID");
        when(billMapper.detail(request)).thenReturn(saved);

        BillDTO result = billService.confirmPayment(request);

        assertSame(saved, result);
        verify(billMapper, never()).update(any());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-009 | 요청 금액과 저장 금액이 다르면 결제를 거부한다"
    )
    void confirmPayment_rejectsAmountMismatch() {
        BillDTO request = paymentRequest("payment-key", "ORDER-1", "5000");
        BillDTO saved = paymentRequest(null, "ORDER-1", "6000");
        saved.setBillStatus("UNPAID");
        when(billMapper.detail(request)).thenReturn(saved);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> billService.confirmPayment(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(billMapper, never()).update(any());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-010 | 관리자 목록에서 결제 완료 정산서의 출차 가능 시간을 계산한다"
    )
    void findAdminBillingList_setsExitAllowedState() {
        BillDTO paid = new BillDTO();
        paid.setBillStatus("PAID");
        paid.setPaidAt(LocalDateTime.now());
        paid.setExitGraceMinutes(10);
        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(paid));

        List<BillDTO> result = billService.findAdminBillingList();

        assertTrue(result.get(0).isExitAllowed());
        assertEquals(
                paid.getPaidAt().plusMinutes(10),
                result.get(0).getExitAllowedUntil()
        );
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-011 | 관리자 정산 상세가 없으면 찾을 수 없음으로 처리한다"
    )
    void findAdminBillingDetail_rejectsMissingBill() {
        when(billMapper.detail(any(BillDTO.class))).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> billService.findAdminBillingDetail(1)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-012 | 현재 미결제 정산서를 계산하고 저장한 뒤 다시 조회한다"
    )
    void findCurrentUnpaidBill_calculatesAndUpdatesBill() {
        BillDTO unpaid = new BillDTO();
        unpaid.setBillNo(1);
        unpaid.setBillStatus("UNPAID");
        unpaid.setInTime(LocalDateTime.now().plusMinutes(10));
        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(unpaid));
        when(billMapper.update(unpaid)).thenReturn(1);

        BillDTO refreshed = new BillDTO();
        refreshed.setBillNo(1);
        when(billMapper.detail(any(BillDTO.class)))
                .thenReturn(refreshed);

        BillDTO result = billService.findCurrentUnpaidBill(101);

        assertSame(refreshed, result);
        assertEquals(0, unpaid.getChargeMinutes());
        assertEquals(BigDecimal.ZERO, unpaid.getBillAmount());
        verify(billMapper).update(unpaid);
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-013 | 잘못된 관리자 정산 수정 요청을 거부한다"
    )
    void updateAdminBilling_rejectsInvalidInput() {
        BillDTO request = new BillDTO();
        request.setFreeTime(-1);
        request.setFeeRuleNo(1);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> billService.updateAdminBilling(1, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-014 | 결제 완료 후 유예시간 안에서만 출차를 허용한다"
    )
    void isExitAllowed_checksPaidAtAndGracePeriod() {
        LocalDateTime paidAt = LocalDateTime.of(
                2026,
                8,
                31,
                10,
                0
        );
        BillDTO bill = new BillDTO();
        bill.setBillStatus("PAID");
        bill.setPaidAt(paidAt);
        bill.setExitGraceMinutes(10);
        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(bill));

        assertTrue(billService.isExitAllowed(
                101,
                paidAt.plusMinutes(10)
        ));
        assertFalse(billService.isExitAllowed(
                101,
                paidAt.plusMinutes(10).plusNanos(1)
        ));
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-015 | 관리자 수동 이동을 휴지통 서비스에 위임한다"
    )
    void moveAdminBillingToTrash_delegatesManualMove() {
        int result = billService.moveAdminBillingToTrash(1);

        assertEquals(1, result);
        verify(trashService).moveBill(1, "MANUAL");
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-016 | 출차 유예시간이 지난 결제 건에 추가 정산서를 생성한다"
    )
    void createAdditionalBills_createsBillAfterGracePeriod() {
        BillDTO paid = new BillDTO();
        paid.setCarLogNo(101);
        paid.setSnapshotCarLogNo(101);
        paid.setSnapshotCarNo("123가1234");
        paid.setBillStatus("PAID");
        paid.setPaidAt(LocalDateTime.now().minusMinutes(20));
        paid.setExitGraceMinutes(10);

        when(billMapper.list(argThat(condition ->
                condition != null
                        && "PAID".equals(condition.getBillStatus())
        ))).thenReturn(List.of(paid));
        when(billMapper.list(argThat(condition ->
                condition != null
                        && Integer.valueOf(101).equals(condition.getCarLogNo())
        ))).thenReturn(List.of(paid));
        when(billMapper.lockCarLog(101)).thenReturn(101);

        FeeRuleDTO feeRule = new FeeRuleDTO();
        feeRule.setFeeRuleNo(7);
        when(feeRuleService.findDefaultAt(any())).thenReturn(feeRule);
        when(billMapper.insert(any(BillDTO.class))).thenReturn(1);

        billService.createAdditionalBills();

        verify(billMapper).insert(argThat(inserted ->
                Integer.valueOf(101).equals(inserted.getCarLogNo())
                        && "UNPAID".equals(inserted.getBillStatus())
                        && BigDecimal.ZERO.equals(inserted.getBillAmount())
        ));
    }

    @Test
    @DisplayName(
            "UT-BE-BILL-017 | 3개월이 지난 결제 정산서만 지난 기록으로 이동한다"
    )
    void moveOldPaidBillsToTrash_movesOnlyExpiredBills() {
        BillDTO oldBill = new BillDTO();
        oldBill.setBillNo(1);
        oldBill.setPaidAt(LocalDateTime.now().minusMonths(4));

        BillDTO recentBill = new BillDTO();
        recentBill.setBillNo(2);
        recentBill.setPaidAt(LocalDateTime.now().minusMonths(1));

        when(billMapper.list(any(BillDTO.class)))
                .thenReturn(List.of(oldBill, recentBill));

        billService.moveOldPaidBillsToTrash();

        verify(trashService).moveBill(1, "SCHEDULED");
        verify(trashService, never()).moveBill(2, "SCHEDULED");
    }

    @Test @DisplayName("UT-BE-BILL-018 | 존재하지 않는 키오스크의 정산서 검색을 거부한다")
    void findParkingBills_rejectsMissingKiosk() { assertStatus(HttpStatus.NOT_FOUND, () -> billService.findParkingBills("1234", 9)); verify(kioskService).findByKioskNo(9); verifyNoMoreInteractions(billMapper); }

    @Test @DisplayName("UT-BE-BILL-019 | 현재 주차 정산서가 없으면 찾을 수 없음으로 처리한다")
    void findParkingBills_rejectsMissingCurrentBill() { KioskDTO k=new KioskDTO(); k.setParkingNo(1); when(kioskService.findByKioskNo(1)).thenReturn(k); when(billMapper.list(any())).thenReturn(List.of()); assertStatus(HttpStatus.NOT_FOUND, () -> billService.findParkingBills("1234",1)); }

    @Test @DisplayName("UT-BE-BILL-020 | 무료 미결제 정산서를 계산해 결제완료로 저장한다")
    void createOrRefreshBill_completesFreeBill() { KioskDTO k=new KioskDTO(); k.setParkingNo(1); when(kioskService.findByKioskNo(1)).thenReturn(k); BillDTO b=calculableBill(); b.setParkingNo(1); b.setSnapshotCarNo("12가1234"); b.setSnapshotCarLogNo(10); when(billMapper.list(any())).thenReturn(List.of(b)); when(billMapper.update(b)).thenReturn(1); when(billMapper.detail(b)).thenReturn(b); assertSame(b,billService.createOrRefreshBill("12가1234",1)); assertEquals("PAID",b.getBillStatus()); verify(billMapper).update(b); }

    @Test @DisplayName("UT-BE-BILL-021 | 입주민 정산서 요청의 번호와 로그인 ID를 검증한다")
    void findResidentBill_rejectsInvalidIdentity() { assertStatus(HttpStatus.BAD_REQUEST, () -> billService.findResidentBill(0,"user")); assertStatus(HttpStatus.UNAUTHORIZED, () -> billService.findResidentBill(1," ")); }

    @Test @DisplayName("UT-BE-BILL-022 | 입주민 소유의 미결제 정산서를 다시 계산해 저장한다")
    void findResidentBill_refreshesUnpaidBill() { BillDTO b=calculableBill(); b.setMemberNo(3); b.setCarKind("VISIT"); MemberDTO m=new MemberDTO(); m.setMemberNo(3); when(billMapper.detail(any())).thenReturn(b); when(memberService.residentMypage("user")).thenReturn(m); when(billMapper.update(b)).thenReturn(1); assertSame(b,billService.findResidentBill(1,"user")); verify(billMapper).update(b); }

    @Test @DisplayName("UT-BE-BILL-023 | 필수 결제 승인정보가 없으면 요청을 거부한다")
    void confirmPayment_rejectsIncompleteRequest() { assertStatus(HttpStatus.BAD_REQUEST, () -> billService.confirmPayment(null)); assertStatus(HttpStatus.BAD_REQUEST, () -> billService.confirmPayment(new BillDTO())); verifyNoInteractions(billMapper); }

    @Test @DisplayName("UT-BE-BILL-024 | 저장된 결제 주문이 없으면 찾을 수 없음으로 처리한다")
    void confirmPayment_rejectsMissingOrder() { BillDTO request=paymentRequest("key","order","1000"); when(billMapper.detail(request)).thenReturn(null); assertStatus(HttpStatus.NOT_FOUND, () -> billService.confirmPayment(request)); }

    @Test @DisplayName("UT-BE-BILL-025 | 결제된 주문에 다른 결제키가 들어오면 충돌로 처리한다")
    void confirmPayment_rejectsDifferentKeyForPaidBill() { BillDTO request=paymentRequest("new","order","1000"); BillDTO saved=paymentRequest("old","order","1000"); saved.setBillStatus("PAID"); when(billMapper.detail(request)).thenReturn(saved); assertStatus(HttpStatus.CONFLICT, () -> billService.confirmPayment(request)); }

    @Test @DisplayName("UT-BE-BILL-026 | 관리자 정산 상세 번호가 0 이하면 거부한다")
    void findAdminBillingDetail_rejectsInvalidNumber() { assertStatus(HttpStatus.BAD_REQUEST, () -> billService.findAdminBillingDetail(0)); verifyNoInteractions(billMapper); }

    @Test @DisplayName("UT-BE-BILL-027 | 잘못된 입출차 번호와 미결제 정산서 부재는 null을 반환한다")
    void findCurrentUnpaidBill_returnsNull() { assertNull(billService.findCurrentUnpaidBill(0)); when(billMapper.list(any())).thenReturn(List.of()); assertNull(billService.findCurrentUnpaidBill(10)); }

    @Test @DisplayName("UT-BE-BILL-028 | 관리자 변경 요금규칙과 무료시간으로 미결제 정산서를 갱신한다")
    void updateAdminBilling_updatesCalculatedValues() { BillDTO saved=calculableBill(); saved.setBillNo(1); saved.setCarLogNo(10); when(billMapper.detail(any())).thenReturn(saved); when(billMapper.list(any())).thenReturn(List.of()); FeeRuleDTO rule=new FeeRuleDTO(); rule.setFeeRuleNo(2); rule.setEffectiveFrom(LocalDateTime.now().minusDays(1)); rule.setUnitMinutes(10); rule.setUnitFee(BigDecimal.valueOf(1000)); when(feeRuleService.detail(2)).thenReturn(rule); when(carLogMapper.updateFreeTime(any())).thenReturn(1); when(billMapper.update(any())).thenReturn(1); BillDTO request=new BillDTO(); request.setFreeTime(30); request.setFeeRuleNo(2); assertSame(saved,billService.updateAdminBilling(1,request)); verify(carLogMapper).updateFreeTime(argThat(c -> c.getCarLogNo()==10 && c.getFreeTime()==30)); assertEquals("UNPAID",request.getBillStatus()); }

    @Test @DisplayName("UT-BE-BILL-029 | 출차 허용 확인의 필수값과 미결제 상태를 거부한다")
    void isExitAllowed_rejectsInvalidOrUnpaid() { assertFalse(billService.isExitAllowed(0,LocalDateTime.now())); assertFalse(billService.isExitAllowed(1,null)); BillDTO unpaid=new BillDTO(); unpaid.setBillStatus("UNPAID"); when(billMapper.list(any())).thenReturn(List.of(unpaid)); assertFalse(billService.isExitAllowed(1,LocalDateTime.now())); }

    @Test @DisplayName("UT-BE-BILL-030 | 잘못된 번호와 미완료 정산서의 지난 기록 이동을 거부한다")
    void moveAdminBillingToTrash_rejectsInvalidMove() { assertStatus(HttpStatus.BAD_REQUEST, () -> billService.moveAdminBillingToTrash(0)); doThrow(new IllegalArgumentException()).when(trashService).moveBill(2,"MANUAL"); assertStatus(HttpStatus.CONFLICT, () -> billService.moveAdminBillingToTrash(2)); }

    private BillDTO calculableBill() { BillDTO b=new BillDTO(); b.setBillNo(1); b.setCarLogNo(10); b.setBillStatus("UNPAID"); b.setInTime(LocalDateTime.now().plusMinutes(1)); b.setFreeTime(0); b.setUnitMinutes(10); b.setUnitFee(BigDecimal.valueOf(1000)); return b; }
    private void assertStatus(HttpStatus status, Runnable action) { ResponseStatusException e=assertThrows(ResponseStatusException.class,action::run); assertEquals(status,e.getStatusCode()); }

    private static Stream<Arguments> invalidParkingBillSearchInputs() {
        return Stream.of(
                Arguments.of(null, 1),
                Arguments.of("   ", 1),
                Arguments.of("1234", null),
                Arguments.of("1234", 0),
                Arguments.of("1234", -1)
        );
    }

    private static BillDTO parkingBill(
            int billNo,
            int snapshotCarLogNo,
            int parkingNo,
            LocalDateTime outTime
    ) {
        BillDTO bill = new BillDTO();
        bill.setBillNo(billNo);
        bill.setCarLogNo(snapshotCarLogNo);
        bill.setSnapshotCarLogNo(snapshotCarLogNo);
        bill.setParkingNo(parkingNo);
        bill.setSnapshotCarNo("123가1234");
        bill.setBillStatus("UNPAID");
        bill.setOutTime(outTime);
        return bill;
    }

    private static BillDTO paymentRequest(
            String paymentKey,
            String orderId,
            String amount
    ) {
        BillDTO dto = new BillDTO();
        dto.setPaymentKey(paymentKey);
        dto.setPaymentOrderId(orderId);
        dto.setBillAmount(new BigDecimal(amount));
        return dto;
    }
}
