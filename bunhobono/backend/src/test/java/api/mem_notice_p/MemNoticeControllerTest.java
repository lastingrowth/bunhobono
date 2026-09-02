package api.mem_notice_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.security.core.Authentication; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class MemNoticeControllerTest { @Mock MemNoticeService service; @Mock Authentication auth; @InjectMocks MemNoticeController controller; @BeforeEach void login(){ when(auth.getName()).thenReturn("user"); }
 @Test @DisplayName("UT-BE-MEMNOTICE-CTRL-001 입주민 알림 목록을 조회한다") void list(){ List<MemNoticeDTO>x=List.of(); when(service.notificationList("user")).thenReturn(x); assertThat(controller.notificationList(auth)).isSameAs(x); }
 @Test @DisplayName("UT-BE-MEMNOTICE-CTRL-002 입주민 알림을 읽음 처리한다") void read(){ when(service.markRead("user",1)).thenReturn(1); assertThat(controller.markRead(auth,1)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-MEMNOTICE-CTRL-003 입주민 알림을 삭제한다") void delete(){ when(service.deleteNotification("user",1)).thenReturn(1); assertThat(controller.deleteNotification(auth,1)).isEqualTo(1); } }
