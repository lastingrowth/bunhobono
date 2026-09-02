package api.camera_pdm_p;
import api.predictive_maintenance_p.*; import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.security.core.Authentication; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class CameraPdmControllerTest {
 @Mock CameraPdmService service; @Mock Authentication auth; @InjectMocks CameraPdmController controller;
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-001 예측 목록을 조회한다") void list(){ List<CameraPdmDTO>x=List.of(); when(service.list()).thenReturn(x); assertThat(controller.list()).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-002 예측 상세를 조회한다") void detail(){ CameraPdmDTO x=new CameraPdmDTO(); when(service.detail(1)).thenReturn(x); assertThat(controller.detail(1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-003 예측을 직접 저장한다") void save(){ CameraPdmDTO d=new CameraPdmDTO(); when(service.savePrediction(d)).thenReturn(1); assertThat(controller.savePrediction(d)).isEqualTo(1); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-004 단건 분석을 실행한다") void analyzeOne(){ PredictiveMaintenanceResponseDTO x=new PredictiveMaintenanceResponseDTO(); when(service.analyzeOne()).thenReturn(x); assertThat(controller.analyzeOne()).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-005 최신 예측을 조회한다") void latest(){ List<CameraPdmDTO>x=List.of(); when(service.getLatestPredictions()).thenReturn(x); assertThat(controller.latest()).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-006 카메라 최근 예측을 조회한다") void recent(){ List<CameraPdmDTO>x=List.of(); when(service.getRecentPredictions(1)).thenReturn(x); assertThat(controller.recent(1)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-007 전체 카메라 분석을 실행한다") void analyzeAll(){ List<PredictiveMaintenanceResponseDTO>x=List.of(); when(service.analyzeAll()).thenReturn(x); assertThat(controller.analyzeAll()).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-008 로그인 정보와 조치내용으로 위험 조치를 완료한다") void complete(){ PdmActionRequestDTO r=new PdmActionRequestDTO(); r.setActionNote("점검"); when(auth.getName()).thenReturn("admin"); CameraPdmDTO x=new CameraPdmDTO(); when(service.completeAction(1,"점검","admin")).thenReturn(x); assertThat(controller.completeAction(1,r,auth)).isSameAs(x); }
 @Test @DisplayName("UT-BE-CAMERA-PDM-CTRL-009 요청과 인증이 없어도 null 값으로 서비스에 전달한다") void completeNulls(){ controller.completeAction(1,null,null); verify(service).completeAction(1,null,null); }
}
