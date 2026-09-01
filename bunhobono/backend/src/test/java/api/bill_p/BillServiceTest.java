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
