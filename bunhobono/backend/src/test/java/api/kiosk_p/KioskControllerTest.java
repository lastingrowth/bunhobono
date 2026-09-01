package api.kiosk_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class KioskControllerTest { @Mock KioskService service; @InjectMocks KioskController controller;
 @Test @DisplayName("UT-BE-KIOSK-CTRL-001 키오스크 목록을 조회한다") void list(){ List<KioskDTO>x=List.of(); when(service.list()).thenReturn(x); assertThat(controller.list()).isSameAs(x); }
 @Test @DisplayName("UT-BE-KIOSK-CTRL-002 키오스크를 삭제한다") void delete(){ when(service.delete(1)).thenReturn(1); assertThat(controller.delete(1)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-KIOSK-CTRL-003 키오스크를 등록한다") void signup(){ KioskDTO d=new KioskDTO(); when(service.signUp(d)).thenReturn(1); assertThat(controller.signUp(d)).isEqualTo(1); } }
