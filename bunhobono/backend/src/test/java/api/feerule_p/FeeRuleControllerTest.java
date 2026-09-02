package api.feerule_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class FeeRuleControllerTest { @Mock FeeRuleService service; @InjectMocks FeeRuleController controller;
 @Test @DisplayName("UT-BE-FEERULE-CTRL-001 요금규칙 목록을 조회한다") void list(){ List<FeeRuleDTO>x=List.of(); when(service.list()).thenReturn(x); assertThat(controller.list()).isSameAs(x); }
 @Test @DisplayName("UT-BE-FEERULE-CTRL-002 요금규칙을 등록한다") void insert(){ FeeRuleDTO d=new FeeRuleDTO(); when(service.insert(d)).thenReturn(1); assertThat(controller.insert(d)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-FEERULE-CTRL-003 경로 번호와 DTO로 요금규칙을 수정한다") void update(){ FeeRuleDTO d=new FeeRuleDTO(); when(service.update(1,d)).thenReturn(1); assertThat(controller.update(1,d)).isEqualTo(1); } }
