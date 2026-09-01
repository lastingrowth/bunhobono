package api.faq_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.security.core.Authentication; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class FaqControllerTest { @Mock FaqService service; @Mock Authentication auth; @InjectMocks FaqController controller;
 @Test @DisplayName("UT-BE-FAQ-CTRL-001 FAQ 목록을 조회한다") void list(){ List<FaqDTO>x=List.of(); when(service.list()).thenReturn(x); assertThat(controller.list()).isSameAs(x); }
 @Test @DisplayName("UT-BE-FAQ-CTRL-002 로그인 관리자가 FAQ를 등록한다") void insert(){ FaqDTO d=new FaqDTO(); when(auth.getName()).thenReturn("admin"); when(service.insert(d,"admin")).thenReturn(1); assertThat(controller.insert(d,auth)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-FAQ-CTRL-003 로그인 관리자가 FAQ를 수정한다") void update(){ FaqDTO d=new FaqDTO(); when(auth.getName()).thenReturn("admin"); when(service.update(1,d,"admin")).thenReturn(1); assertThat(controller.update(1,d,auth)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-FAQ-CTRL-004 로그인 관리자가 FAQ를 삭제한다") void delete(){ when(auth.getName()).thenReturn("admin"); when(service.delete(1,"admin")).thenReturn(1); assertThat(controller.delete(1,auth)).isEqualTo(1); } }
