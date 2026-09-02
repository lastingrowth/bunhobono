package api.notice_p;

import api.bill_p.BillDTO;
import api.bill_p.BillService;
import api.trash_p.TrashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {
    @Mock NoticeMapper mapper;
    @Mock BillService billService;
    @Mock TrashService trashService;
    @InjectMocks NoticeService service;

    @Test
    @DisplayName("UT-BE-NOTICE-001 | 알림 목록을 반환한다")
    void list_returnsMapperList() {
        List<NoticeDTO> expected = List.of(new NoticeDTO());
        when(mapper.list()).thenReturn(expected);
        assertSame(expected, service.list());
    }

    @Test
    @DisplayName("UT-BE-NOTICE-002 | 빈 차량번호 검색은 전체 목록을 반환한다")
    void search_blankReturnsList() {
        List<NoticeDTO> expected = List.of(new NoticeDTO());
        when(mapper.list()).thenReturn(expected);
        assertSame(expected, service.search("  "));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-003 | 차량번호의 공백을 제거해 검색한다")
    void search_normalizesCarNumber() {
        List<NoticeDTO> expected = List.of(new NoticeDTO());
        when(mapper.search("12가3456")).thenReturn(expected);
        assertSame(expected, service.search("12가 3456"));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-004 | 장기주차 알림 상세에 현재 정산금액을 합친다")
    void detail_enrichesOverstayWithBill() {
        NoticeDTO notice = new NoticeDTO();
        notice.setCarLogNo(10);
        notice.setNoticeType("VISIT_OVERDUE");
        when(mapper.detail(1)).thenReturn(notice);
        BillDTO bill = new BillDTO();
        bill.setBillNo(2);
        bill.setChargeMinutes(60);
        bill.setBillAmount(BigDecimal.valueOf(3000));
        when(billService.findCurrentUnpaidBill(10)).thenReturn(bill);
        NoticeDTO result = service.detail(1);
        assertEquals(2, result.getBillNo());
        assertEquals(60, result.getChargeMinutes());
        assertEquals(BigDecimal.valueOf(3000), result.getBillAmount());
    }

    @Test
    @DisplayName("UT-BE-NOTICE-005 | 일반 알림이나 미납 정산이 없는 상세는 그대로 반환한다")
    void detail_returnsWithoutBillWhenNotApplicable() {
        NoticeDTO notice = new NoticeDTO();
        notice.setNoticeType("OCR_REVIEW");
        when(mapper.detail(1)).thenReturn(notice);
        assertSame(notice, service.detail(1));
        verifyNoInteractions(billService);
    }

    @Test
    @DisplayName("UT-BE-NOTICE-006 | 없는 알림 상세 조회를 거부한다")
    void detail_rejectsMissingNotice() {
        when(mapper.detail(1)).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> service.detail(1));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-007 | OCR 검토 알림을 처리완료로 변경한다")
    void status_resolvesOcrReview() {
        when(mapper.findAdminMemberNoByLoginId("admin01")).thenReturn(7);
        NoticeDTO saved = new NoticeDTO();
        saved.setNoticeNo(1);
        saved.setNoticeType("OCR_REVIEW");
        when(mapper.detail(1)).thenReturn(saved);
        when(mapper.resolveOcrReview(1, 7)).thenReturn(1);
        NoticeDTO request = new NoticeDTO();
        request.setNoticeNo(1);
        request.setAlertStat("Resolved");
        assertEquals(1, service.status("admin01", request));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-008 | 장기주차 알림을 출차 후 처리완료로 변경한다")
    void status_resolvesOverstay() {
        NoticeDTO saved = notice(1, "VISIT_OVERDUE");
        when(mapper.findAdminMemberNoByLoginId("admin")).thenReturn(7);
        when(mapper.detail(1)).thenReturn(saved);
        when(mapper.resolveAfterExit(1, 7)).thenReturn(1);
        assertEquals(1, service.status("admin", resolvedRequest(1)));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-009 | 입차 없는 출차 알림은 원본 또는 스냅샷 촬영번호로 처리한다")
    void status_resolvesExitWithoutEntry() {
        NoticeDTO saved = notice(1, "EXIT_WITHOUT_ENTRY");
        saved.setSnapshotCameraDataNo(9);
        when(mapper.findAdminMemberNoByLoginId("admin")).thenReturn(7);
        when(mapper.detail(1)).thenReturn(saved);
        when(mapper.resolveExitWithoutEntry(9, 7)).thenReturn(1);
        assertEquals(1, service.status("admin", resolvedRequest(1)));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-010 | 알림 처리는 관리자·대상·요청상태·유형·갱신 실패를 거부한다")
    void status_rejectsInvalidCases() {
        NoticeDTO request = resolvedRequest(1);
        when(mapper.findAdminMemberNoByLoginId("admin")).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> service.status("admin", request));
        when(mapper.findAdminMemberNoByLoginId("admin")).thenReturn(7);
        when(mapper.detail(1)).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> service.status("admin", request));
        NoticeDTO saved = notice(1, "OCR_REVIEW");
        when(mapper.detail(1)).thenReturn(saved);
        request.setAlertStat("Open");
        assertThrows(ResponseStatusException.class, () -> service.status("admin", request));
        request.setAlertStat("Resolved");
        saved.setNoticeType("UNKNOWN");
        assertThrows(ResponseStatusException.class, () -> service.status("admin", request));
        saved.setNoticeType("OCR_REVIEW");
        when(mapper.resolveOcrReview(1, 7)).thenReturn(0);
        assertThrows(ResponseStatusException.class, () -> service.status("admin", request));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-011 | 촬영번호 없는 출차 알림 처리를 거부한다")
    void status_rejectsMissingCameraData() {
        when(mapper.findAdminMemberNoByLoginId("admin")).thenReturn(7);
        when(mapper.detail(1)).thenReturn(notice(1, "EXIT_WITHOUT_ENTRY"));
        assertThrows(ResponseStatusException.class, () -> service.status("admin", resolvedRequest(1)));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-012 | 차량기록에서 장기주차 알림을 생성한다")
    void createNoticesFromCarLog() {
        when(mapper.createNoticesFromCarLog()).thenReturn(1);
        assertEquals(1, service.createNoticesFromCarLog());
    }

    @Test @DisplayName("UT-BE-NOTICE-013 | OCR 검토 알림을 생성한다")
    void createOcrReviewNotice() {
        when(mapper.createOcrReviewNotice(2)).thenReturn(1);
        assertEquals(1, service.createOcrReviewNotice(2));
    }

    @Test @DisplayName("UT-BE-NOTICE-014 | 입차 없는 출차 알림을 생성한다")
    void createExitWithoutEntryNotice() {
        when(mapper.createExitWithoutEntryNotice(3)).thenReturn(1);
        assertEquals(1, service.createExitWithoutEntryNotice(3));
    }

    @Test
    @DisplayName("UT-BE-NOTICE-015 | 한 알림 이동 실패 후에도 다음 알림을 처리한다")
    void moveResolvedNoticesToTrash_continuesAfterFailure() {
        when(mapper.findResolvedNoticeNosForTrash()).thenReturn(List.of(1, 2));
        doThrow(new IllegalStateException()).when(trashService).moveNotice(1, "SCHEDULED");
        service.moveResolvedNoticesToTrash();
        verify(trashService).moveNotice(2, "SCHEDULED");
    }

    private NoticeDTO notice(int no, String type) {
        NoticeDTO dto = new NoticeDTO(); dto.setNoticeNo(no); dto.setNoticeType(type); return dto;
    }

    private NoticeDTO resolvedRequest(int no) {
        NoticeDTO dto = new NoticeDTO(); dto.setNoticeNo(no); dto.setAlertStat("Resolved"); return dto;
    }
}
