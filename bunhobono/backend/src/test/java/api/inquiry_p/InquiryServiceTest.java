package api.inquiry_p;

import api.a_security_config.AuthService;
import api.a_security_config.LoginDTO;
import api.mem_notice_p.MemNoticeDTO;
import api.mem_notice_p.MemNoticeService;
import api.trash_p.TrashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {
    @Mock InquiryMapper mapper;
    @Mock AuthService authService;
    @Mock TrashService trashService;
    @Mock MemNoticeService memNoticeService;
    @InjectMocks InquiryService service;
    LoginDTO resident;
    LoginDTO admin;

    @BeforeEach
    void setUp() {
        resident = user(10, "RESIDENT");
        admin = user(20, "ADMIN");
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-001 | 입주민 문의를 정리하고 작성자 번호로 등록한다")
    void insert_normalizesAndSetsMember() {
        when(authService.getUserInfo("resident01")).thenReturn(resident);
        InquiryDTO dto = inquiry(" PARKING ", " 제목 ", " 내용 ");
        when(mapper.insert(dto)).thenReturn(1);
        assertEquals(1, service.insert(dto, "resident01"));
        assertEquals(10, dto.getMemberNo());
        assertEquals("PARKING", dto.getCategory());
        assertEquals("제목", dto.getTitle());
        assertEquals("내용", dto.getContent());
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-002 | 입주민 회원번호로 본인 문의 목록을 조회한다")
    void listByMember_usesMemberNumber() {
        when(authService.getUserInfo("resident01")).thenReturn(resident);
        List<InquiryDTO> list = List.of(new InquiryDTO());
        when(mapper.listByMemberNo(10)).thenReturn(list);
        assertSame(list, service.listByMember("resident01"));
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-003 | 관리자 문의 상태를 정규화해 조회한다")
    void listByStatus_normalizesStatus() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        List<InquiryDTO> expected = List.of(new InquiryDTO());
        when(mapper.listByStatus("WAITING")).thenReturn(expected);
        assertSame(expected, service.listByStatus(" waiting ", "admin01"));
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-004 | 관리자 답변을 저장하고 입주민 알림을 생성한다")
    void answer_trimsAndCreatesNotification() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        InquiryDTO saved = inquiry("PARKING", "주차 문의", "내용");
        saved.setInquiryNo(1);
        saved.setMemberNo(10);
        saved.setStatus("WAITING");
        when(mapper.detail(1)).thenReturn(saved);
        when(mapper.answer(any())).thenReturn(1);
        InquiryDTO answer = new InquiryDTO();
        answer.setAnswerContent(" 답변 내용 ");
        assertEquals(1, service.answer(1, answer, "admin01"));
        assertEquals("답변 내용", answer.getAnswerContent());
        assertEquals(20, answer.getAnsweredBy());
        verify(memNoticeService).createNotification(any(MemNoticeDTO.class));
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-005 | 답변완료 문의에 재문의 흐름을 연결한다")
    void reInquiry_linksToRootInquiry() {
        when(authService.getUserInfo("resident01")).thenReturn(resident);
        InquiryDTO previous = inquiry("VISIT", "방문 문의", "기존");
        previous.setInquiryNo(1);
        previous.setStatus("ANSWERED");
        when(mapper.detailByMember(1, 10)).thenReturn(previous);
        when(mapper.countWaitingByRoot(1)).thenReturn(0);
        InquiryDTO request = new InquiryDTO();
        request.setContent(" 재문의 ");
        when(mapper.insert(request)).thenReturn(1);
        assertEquals(1, service.reInquiry(1, request, "resident01"));
        assertEquals(1, request.getRootInquiryNo());
        assertEquals("VISIT", request.getCategory());
        assertEquals("방문 문의", request.getTitle());
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-006 | 문의 흐름 전체를 하위 문의부터 휴지통으로 이동한다")
    void deleteByMember_movesEntireThread() {
        when(authService.getUserInfo("resident01")).thenReturn(resident);
        InquiryDTO inquiry = new InquiryDTO();
        inquiry.setInquiryNo(1);
        when(mapper.detailByMember(1, 10)).thenReturn(inquiry);
        when(mapper.findInquiryNosByRoot(1, 10)).thenReturn(List.of(3, 2, 1));
        assertEquals(3, service.deleteByMember(1, "resident01"));
        verify(trashService).moveInquiry(3, "MANUAL");
        verify(trashService).moveInquiry(1, "MANUAL");
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-007 | 오래된 문의 이동 중 실패해도 다음 건을 계속 처리한다")
    void moveOldInquiriesToTrash_continues() {
        when(mapper.findInquiryNosForTrash()).thenReturn(List.of(1, 2));
        org.mockito.Mockito.doThrow(new IllegalStateException())
                .when(trashService).moveInquiry(1, "SCHEDULED");
        service.moveOldInquiriesToTrash();
        verify(trashService).moveInquiry(2, "SCHEDULED");
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-008 | 권한이 없는 문의 접근을 거부한다")
    void insert_rejectsNonResident() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.insert(inquiry("ETC", "제목", "내용"), "admin01"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-009 | 관리자는 문의 상세를 조회한다")
    void detailByAdmin_returnsInquiry() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        InquiryDTO expected = new InquiryDTO();
        when(mapper.detail(4)).thenReturn(expected);
        assertSame(expected, service.detailByAdmin(4, "admin01"));
    }

    @Test
    @DisplayName("UT-BE-INQUIRY-010 | 관리자는 문의 흐름을 하위 항목부터 휴지통으로 이동한다")
    void deleteByAdmin_movesThread() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        InquiryDTO target = new InquiryDTO(); target.setInquiryNo(1); target.setMemberNo(10);
        when(mapper.detail(1)).thenReturn(target);
        when(mapper.findInquiryNosByRoot(1, 10)).thenReturn(List.of(2, 1));
        assertEquals(2, service.deleteByAdmin(1, "admin01"));
        verify(trashService).moveInquiry(2, "MANUAL");
        verify(trashService).moveInquiry(1, "MANUAL");
    }

    @Test @DisplayName("UT-BE-INQUIRY-011 | 입주민 회원번호로 본인 문의 상세를 조회한다")
    void detailByMember_returnsInquiry(){ when(authService.getUserInfo("resident01")).thenReturn(resident); InquiryDTO detail=new InquiryDTO(); when(mapper.detailByMember(1,10)).thenReturn(detail); assertSame(detail,service.detailByMember(1,"resident01")); }

    @Test @DisplayName("UT-BE-INQUIRY-012 | 본인 문의 상세가 없으면 404로 처리한다")
    void detailByMember_rejectsMissing(){ when(authService.getUserInfo("resident01")).thenReturn(resident); assertStatus(HttpStatus.NOT_FOUND,() -> service.detailByMember(9,"resident01")); }

    @Test @DisplayName("UT-BE-INQUIRY-013 | 문의 등록의 분류·제목·내용을 각각 검증한다")
    void insert_rejectsInvalidFields(){ when(authService.getUserInfo("resident01")).thenReturn(resident); assertStatus(HttpStatus.BAD_REQUEST,() -> service.insert(null,"resident01")); assertStatus(HttpStatus.BAD_REQUEST,() -> service.insert(inquiry("WRONG","제목","내용"),"resident01")); assertStatus(HttpStatus.BAD_REQUEST,() -> service.insert(inquiry("ETC"," ","내용"),"resident01")); assertStatus(HttpStatus.BAD_REQUEST,() -> service.insert(inquiry("ETC","제목"," "),"resident01")); verify(mapper,org.mockito.Mockito.never()).insert(any()); }

    @Test @DisplayName("UT-BE-INQUIRY-014 | 잘못된 관리자 문의상태 검색을 거부한다")
    void listByStatus_rejectsInvalidStatus(){ when(authService.getUserInfo("admin01")).thenReturn(admin); assertStatus(HttpStatus.BAD_REQUEST,() -> service.listByStatus("DONE","admin01")); }

    @Test @DisplayName("UT-BE-INQUIRY-015 | 관리자 문의 상세가 없으면 404로 처리한다")
    void detailByAdmin_rejectsMissing(){ when(authService.getUserInfo("admin01")).thenReturn(admin); assertStatus(HttpStatus.NOT_FOUND,() -> service.detailByAdmin(9,"admin01")); }

    @Test @DisplayName("UT-BE-INQUIRY-016 | 빈 답변·없는 문의·답변완료 문의를 구분해 거부한다")
    void answer_rejectsInvalidState(){ when(authService.getUserInfo("admin01")).thenReturn(admin); InquiryDTO blank=new InquiryDTO(); blank.setAnswerContent(" "); assertStatus(HttpStatus.BAD_REQUEST,() -> service.answer(1,blank,"admin01")); InquiryDTO answer=new InquiryDTO(); answer.setAnswerContent("답변"); assertStatus(HttpStatus.NOT_FOUND,() -> service.answer(1,answer,"admin01")); InquiryDTO answered=new InquiryDTO(); answered.setStatus("ANSWERED"); when(mapper.detail(2)).thenReturn(answered); assertStatus(HttpStatus.CONFLICT,() -> service.answer(2,answer,"admin01")); }

    @Test @DisplayName("UT-BE-INQUIRY-017 | 답변 DB 갱신 실패 시 알림을 생성하지 않는다")
    void answer_rejectsMapperConflict(){ when(authService.getUserInfo("admin01")).thenReturn(admin); InquiryDTO waiting=new InquiryDTO(); waiting.setStatus("WAITING"); when(mapper.detail(1)).thenReturn(waiting); InquiryDTO answer=new InquiryDTO(); answer.setAnswerContent("답변"); when(mapper.answer(answer)).thenReturn(0); assertStatus(HttpStatus.CONFLICT,() -> service.answer(1,answer,"admin01")); verifyNoInteractions(memNoticeService); }

    @Test @DisplayName("UT-BE-INQUIRY-018 | 재문의는 본인 답변완료 문의와 빈 내용 여부를 검증한다")
    void reInquiry_rejectsInvalidState(){ when(authService.getUserInfo("resident01")).thenReturn(resident); assertStatus(HttpStatus.NOT_FOUND,() -> service.reInquiry(1,new InquiryDTO(),"resident01")); InquiryDTO waiting=inquiry("ETC","제목","내용"); waiting.setStatus("WAITING"); when(mapper.detailByMember(2,10)).thenReturn(waiting); assertStatus(HttpStatus.CONFLICT,() -> service.reInquiry(2,new InquiryDTO(),"resident01")); InquiryDTO answered=inquiry("ETC","제목","내용"); answered.setInquiryNo(3); answered.setStatus("ANSWERED"); when(mapper.detailByMember(3,10)).thenReturn(answered); when(mapper.countWaitingByRoot(3)).thenReturn(1); assertStatus(HttpStatus.CONFLICT,() -> service.reInquiry(3,new InquiryDTO(),"resident01")); }

    @Test @DisplayName("UT-BE-INQUIRY-019 | 관리자 삭제 대상 문의가 없으면 404로 처리한다")
    void deleteByAdmin_rejectsMissing(){ when(authService.getUserInfo("admin01")).thenReturn(admin); assertStatus(HttpStatus.NOT_FOUND,() -> service.deleteByAdmin(9,"admin01")); }

    @Test @DisplayName("UT-BE-INQUIRY-020 | 입주민 삭제 대상 문의가 없으면 404로 처리한다")
    void deleteByMember_rejectsMissing(){ when(authService.getUserInfo("resident01")).thenReturn(resident); assertStatus(HttpStatus.NOT_FOUND,() -> service.deleteByMember(9,"resident01")); }

    @Test @DisplayName("UT-BE-INQUIRY-021 | 로그인 정보가 없으면 입주민·관리자 기능을 거부한다")
    void authorization_rejectsMissingLogin(){ when(authService.getUserInfo("missing")).thenReturn(null); assertStatus(HttpStatus.UNAUTHORIZED,() -> service.listByMember("missing")); assertStatus(HttpStatus.UNAUTHORIZED,() -> service.listByStatus("WAITING","missing")); }

    private void assertStatus(HttpStatus status,Runnable action){ ResponseStatusException e=assertThrows(ResponseStatusException.class,action::run); assertEquals(status,e.getStatusCode()); }

    private static LoginDTO user(int memberNo, String role) {
        LoginDTO dto = new LoginDTO();
        dto.setMemberNo(memberNo);
        dto.setRole(role);
        return dto;
    }

    private static InquiryDTO inquiry(String category, String title, String content) {
        InquiryDTO dto = new InquiryDTO();
        dto.setCategory(category);
        dto.setTitle(title);
        dto.setContent(content);
        return dto;
    }
}
