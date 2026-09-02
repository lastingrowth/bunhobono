package api.camera_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class CameraControllerTest {
 @Mock CameraService service; @InjectMocks CameraController controller;
 @Test @DisplayName("UT-BE-CAMERA-CTRL-001 카메라 목록을 조회한다") void list(){ CameraDTO d=new CameraDTO(); List<CameraDTO>x=List.of(d); when(service.listservice(d)).thenReturn(x); assertThat(controller.list(d)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-CTRL-002 카메라를 등록한다") void signup(){ CameraDTO d=new CameraDTO(); when(service.signUp(d)).thenReturn(1); assertThat(controller.signUp(d)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-CAMERA-CTRL-003 카메라를 삭제한다") void delete(){ when(service.delete(1)).thenReturn(1); assertThat(controller.deleteCamera(1)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-CAMERA-CTRL-004 경로 번호를 DTO에 설정해 카메라를 수정한다") void update(){ CameraDTO d=new CameraDTO(); when(service.update(d)).thenReturn(1); assertThat(controller.updateCamera(7,d)).isEqualTo(1); assertThat(d.getCameraNo()).isEqualTo(7); }
}
