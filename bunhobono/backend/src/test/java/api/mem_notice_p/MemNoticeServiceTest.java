package api.mem_notice_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemNoticeServiceTest {
    @Test
    @DisplayName("UT-BE-MEMNOTICE-001 | 로그인 회원 조건으로 알림 목록을 조회한다")
    void notificationList_buildsCondition() {
        MemNoticeMapper mapper = mock(MemNoticeMapper.class);
        MemNoticeService service = service(mapper);
        List<MemNoticeDTO> expected = List.of(new MemNoticeDTO());
        when(mapper.list(argThat(dto -> "resident01".equals(dto.getLoginId()))))
                .thenReturn(expected);
        assertSame(expected, service.notificationList("resident01"));
    }

    @Test @DisplayName("UT-BE-MEMNOTICE-005 | 로그인 ID와 알림 번호로 읽음 처리한다")
    void markRead_buildsCondition(){ MemNoticeMapper mapper=mock(MemNoticeMapper.class); MemNoticeService service=service(mapper); when(mapper.markRead(argThat(d -> "resident01".equals(d.getLoginId()) && d.getMemNoticeNo()==1))).thenReturn(1); assertEquals(1,service.markRead("resident01",1)); }

    @Test @DisplayName("UT-BE-MEMNOTICE-006 | 본인 알림을 삭제하고 처리 건수를 반환한다")
    void deleteNotification_success(){ MemNoticeMapper mapper=mock(MemNoticeMapper.class); MemNoticeService service=service(mapper); when(mapper.delete(argThat(d -> "resident01".equals(d.getLoginId()) && d.getMemNoticeNo()==2))).thenReturn(1); assertEquals(1,service.deleteNotification("resident01",2)); }

    @Test @DisplayName("UT-BE-MEMNOTICE-007 | 알림 생성 필수값을 각각 검증한다")
    void createNotification_rejectsEachMissingField(){ MemNoticeService service=service(mock(MemNoticeMapper.class)); assertThrows(IllegalArgumentException.class,() -> service.createNotification(null)); MemNoticeDTO missingTitle=validNotice(); missingTitle.setTitle(" "); assertThrows(IllegalArgumentException.class,() -> service.createNotification(missingTitle)); MemNoticeDTO missingMessage=validNotice(); missingMessage.setMessage(null); assertThrows(IllegalArgumentException.class,() -> service.createNotification(missingMessage)); }

    @Test
    @DisplayName("UT-BE-MEMNOTICE-002 | 없는 알림 삭제를 404로 처리한다")
    void deleteNotification_rejectsMissingNotice() {
        MemNoticeMapper mapper = mock(MemNoticeMapper.class);
        MemNoticeService service = service(mapper);
        when(mapper.delete(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteNotification("resident01", 1));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("UT-BE-MEMNOTICE-003 | 필수값을 갖춘 입주민 알림을 생성한다")
    void createNotification_insertsValidNotice() {
        MemNoticeMapper mapper = mock(MemNoticeMapper.class);
        MemNoticeService service = service(mapper);
        MemNoticeDTO dto = validNotice();
        when(mapper.insert(dto)).thenReturn(1);
        assertEquals(1, service.createNotification(dto));
    }

    @Test
    @DisplayName("UT-BE-MEMNOTICE-004 | 필수값이 없는 알림 생성을 거부한다")
    void createNotification_rejectsInvalidNotice() {
        MemNoticeService service = service(mock(MemNoticeMapper.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.createNotification(new MemNoticeDTO()));
    }

    private static MemNoticeService service(MemNoticeMapper mapper) {
        MemNoticeService service = new MemNoticeService();
        ReflectionTestUtils.setField(service, "memNoticeMapper", mapper);
        return service;
    }

    private static MemNoticeDTO validNotice() {
        MemNoticeDTO dto = new MemNoticeDTO();
        dto.setRecipientMemberNo(1);
        dto.setReferenceTable("inquiry");
        dto.setReferenceNo(2);
        dto.setNoticeType("INQUIRY_ANSWERED");
        dto.setTitle("답변 등록");
        dto.setMessage("답변이 등록되었습니다.");
        return dto;
    }
}
