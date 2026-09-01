package api.carlog_p;
import api.trash_p.TrashService; import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class CarLogControllerTest { @Mock CarLogService service; @Mock TrashService trash; @InjectMocks CarLogController controller;
 @Test @DisplayName("UT-BE-CARLOG-CTRL-001 입출차 목록을 조회한다") void list(){ CarLogDTO d=new CarLogDTO(); List<CarLogDTO>x=List.of(); when(service.list(d)).thenReturn(x); assertThat(controller.list(d)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CARLOG-CTRL-002 조건으로 입출차 목록을 검색한다") void search(){ CarLogDTO d=new CarLogDTO(); List<CarLogDTO>x=List.of(); when(service.list(d)).thenReturn(x); assertThat(controller.search(d)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CARLOG-CTRL-003 키오스크 정산 대상 주차차량을 조회한다") void parking(){ List<CarLogDTO>x=List.of(); when(service.findParkingCars("3456",1)).thenReturn(x); assertThat(controller.parkingCars("3456",1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CARLOG-CTRL-004 입출차 기록을 지난 기록으로 이동한다") void delete(){ assertThat(controller.delete(1)).isEqualTo(1); verify(trash).moveCarLog(1,"MANUAL"); } }
