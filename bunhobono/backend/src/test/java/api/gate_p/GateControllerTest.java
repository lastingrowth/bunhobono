package api.gate_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class GateControllerTest { @Mock GateService service; @InjectMocks GateController controller;
 @Test @DisplayName("UT-BE-GATE-CTRL-001 게이트 목록을 조회한다") void list(){ GateDTO d=new GateDTO(); List<GateDTO>x=List.of(); when(service.listservice(d)).thenReturn(x); assertThat(controller.list(d)).isSameAs(x); }
 @Test @DisplayName("UT-BE-GATE-CTRL-002 출차 게이트 번호를 조회한다") void exit(){ when(service.findExitGateNo(1,2)).thenReturn(3); assertThat(controller.exitGate(1,2)).isEqualTo(3); }
 @Test @DisplayName("UT-BE-GATE-CTRL-003 게이트를 등록한다") void signup(){ GateDTO d=new GateDTO(); when(service.signUp(d)).thenReturn(1); assertThat(controller.signUp(d)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-GATE-CTRL-004 게이트를 삭제한다") void delete(){ when(service.delete(1)).thenReturn(1); assertThat(controller.deleteGate(1)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-GATE-CTRL-005 경로 번호를 DTO에 설정해 게이트를 수정한다") void update(){ GateDTO d=new GateDTO(); when(service.updateGate(d)).thenReturn(1); assertThat(controller.updateGate(7,d)).isEqualTo(1); assertThat(d.getGateNo()).isEqualTo(7); } }
