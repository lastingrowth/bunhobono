package api.faq_p;

import api.a_security_config.AuthService;
import api.a_security_config.LoginDTO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {
    @Mock FaqMapper faqMapper;
    @Mock AuthService authService;
    @InjectMocks FaqService faqService;
    LoginDTO admin;

    @BeforeEach
    void setUp() {
        admin = new LoginDTO();
        admin.setRole("ADMIN");
    }

    @Test
    @DisplayName("UT-BE-FAQ-001 | 관리자가 입력을 정리해 FAQ를 등록한다")
    void insert_validatesAdminAndNormalizesInput() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        FaqDTO dto = faq(" PARKING ", " 질문 ", " 답변 ");
        when(faqMapper.insert(dto)).thenReturn(1);

        assertEquals(1, faqService.insert(dto, "admin01"));
        assertEquals("PARKING", dto.getCategory());
        assertEquals("질문", dto.getQuestion());
        assertEquals("답변", dto.getAnswer());
    }

    @Test
    @DisplayName("UT-BE-FAQ-002 | 비관리자의 FAQ 변경을 거부한다")
    void insert_rejectsNonAdmin() {
        LoginDTO resident = new LoginDTO();
        resident.setRole("RESIDENT");
        when(authService.getUserInfo("resident01")).thenReturn(resident);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> faqService.insert(faq("PARKING", "질문", "답변"), "resident01")
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(faqMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("UT-BE-FAQ-003 | FAQ 목록을 Mapper에서 조회한다")
    void list_returnsMapperResult() {
        List<FaqDTO> expected = List.of(new FaqDTO());
        when(faqMapper.list()).thenReturn(expected);
        assertSame(expected, faqService.list());
    }

    @Test
    @DisplayName("UT-BE-FAQ-004 | FAQ 수정 대상이 없으면 404를 반환한다")
    void update_rejectsMissingRow() {
        when(authService.getUserInfo("admin01")).thenReturn(admin);
        FaqDTO dto = faq("ETC", "질문", "답변");
        when(faqMapper.update(dto)).thenReturn(0);
        ResponseStatusException update = assertThrows(
                ResponseStatusException.class,
                () -> faqService.update(1, dto, "admin01")
        );
        assertEquals(HttpStatus.NOT_FOUND, update.getStatusCode());
    }

    @Test @DisplayName("UT-BE-FAQ-005 | 관리자가 FAQ 번호와 정리된 내용을 수정한다")
    void update_success(){ when(authService.getUserInfo("admin01")).thenReturn(admin); FaqDTO dto=faq(" VISIT "," 질문 "," 답변 "); when(faqMapper.update(dto)).thenReturn(1); assertEquals(1,faqService.update(7,dto,"admin01")); assertEquals(7,dto.getFaqNo()); assertEquals("VISIT",dto.getCategory()); verify(faqMapper).update(dto); }

    @Test @DisplayName("UT-BE-FAQ-006 | 관리자가 FAQ를 삭제한다")
    void delete_success(){ when(authService.getUserInfo("admin01")).thenReturn(admin); when(faqMapper.delete(2)).thenReturn(1); assertEquals(1,faqService.delete(2,"admin01")); verify(faqMapper).delete(2); }

    @Test @DisplayName("UT-BE-FAQ-007 | FAQ 삭제 대상이 없으면 404를 반환한다")
    void delete_rejectsMissingRow(){ when(authService.getUserInfo("admin01")).thenReturn(admin); when(faqMapper.delete(2)).thenReturn(0); ResponseStatusException e=assertThrows(ResponseStatusException.class,() -> faqService.delete(2,"admin01")); assertEquals(HttpStatus.NOT_FOUND,e.getStatusCode()); }

    @Test @DisplayName("UT-BE-FAQ-008 | 로그인 정보가 없으면 관리자 기능을 거부한다")
    void admin_rejectsMissingLogin(){ when(authService.getUserInfo("missing")).thenReturn(null); ResponseStatusException e=assertThrows(ResponseStatusException.class,() -> faqService.insert(faq("ETC","질문","답변"),"missing")); assertEquals(HttpStatus.UNAUTHORIZED,e.getStatusCode()); verify(faqMapper,never()).insert(org.mockito.ArgumentMatchers.any()); }

    @Test @DisplayName("UT-BE-FAQ-009 | 잘못된 분류·질문·답변 입력을 각각 거부한다")
    void validateFaq_rejectsInvalidFields(){ when(authService.getUserInfo("admin01")).thenReturn(admin); assertBadRequest(() -> faqService.insert(null,"admin01")); assertBadRequest(() -> faqService.insert(faq("WRONG","질문","답변"),"admin01")); assertBadRequest(() -> faqService.insert(faq("ETC"," ","답변"),"admin01")); assertBadRequest(() -> faqService.insert(faq("ETC","질문"," "),"admin01")); }

    private void assertBadRequest(Runnable action){ ResponseStatusException e=assertThrows(ResponseStatusException.class,action::run); assertEquals(HttpStatus.BAD_REQUEST,e.getStatusCode()); }

    private static FaqDTO faq(String category, String question, String answer) {
        FaqDTO dto = new FaqDTO();
        dto.setCategory(category);
        dto.setQuestion(question);
        dto.setAnswer(answer);
        return dto;
    }
}
