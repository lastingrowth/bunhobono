package api.camera_pdm_p;

import api.predictive_maintenance_p.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CameraPdmServiceTest {
    @Mock CameraPdmMapper mapper; @Mock PredictiveMaintenanceClient client; @Mock PdmActionAuthorizationService auth;
    @InjectMocks CameraPdmService service;

    @Test @DisplayName("UT-BE-CAMERA-PDM-001 | 저장된 카메라 예측 목록을 반환한다") void list(){ CameraPdmDTO d=new CameraPdmDTO(); when(mapper.list()).thenReturn(List.of(d)); assertThat(service.list()).containsExactly(d); verify(mapper).list(); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-002 | 예측 번호로 상세 결과를 반환한다") void detail(){ CameraPdmDTO d=new CameraPdmDTO(); when(mapper.detail(3)).thenReturn(d); assertThat(service.detail(3)).isSameAs(d); verify(mapper).detail(3); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-003 | 직접 입력한 예측 결과를 저장한다") void savePrediction(){ CameraPdmDTO d=new CameraPdmDTO(); when(mapper.savePrediction(d)).thenReturn(1); assertThat(service.savePrediction(d)).isEqualTo(1); verify(mapper).savePrediction(d); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-004 | 단건 정상 분석 결과를 최신·최근 이력에 반영한다") void analyzeOne_normal(){ PredictiveMaintenanceResponseDTO r=response("ANT-002","정상",false); when(client.predictNextCamera()).thenReturn(r); assertThat(service.analyzeOne()).isSameAs(r); assertThat(service.getLatestPredictions()).extracting(CameraPdmDTO::getCameraNo).containsExactly(2); assertThat(service.getRecentPredictions(2)).hasSize(1); verify(mapper,never()).savePrediction(any()); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-005 | 주의 분석 결과를 즉시 저장한다") void analyzeOne_warning(){ PredictiveMaintenanceResponseDTO r=response("ANT-001","주의",false); when(client.predictNextCamera()).thenReturn(r); when(mapper.savePrediction(any())).thenReturn(1); service.analyzeOne(); verify(mapper).savePrediction(argThat(d -> d.getCameraNo()==1 && "주의".equals(d.getRiskLevel()))); verifyNoInteractions(auth); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-006 | 조치가 필요한 위험 결과를 저장하고 담당자에게 알린다") void analyzeOne_dangerAlert(){ PredictiveMaintenanceResponseDTO r=response("ANT-003","위험",true); when(client.predictNextCamera()).thenReturn(r); when(mapper.savePrediction(any())).thenReturn(1); service.analyzeOne(); verify(auth).sendDangerAlert(eq("카메라 ANT-003"),eq(r.getSensorValues())); verify(mapper).findActiveAction(3); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-007 | 기존 활성 위험이 있으면 최신 상태로 사용한다") void analyzeOne_usesActiveDanger(){ PredictiveMaintenanceResponseDTO r=response("ANT-004","위험",true); CameraPdmDTO active=new CameraPdmDTO(); active.setCameraNo(4); active.setRiskLevel("위험"); when(client.predictNextCamera()).thenReturn(r); when(mapper.savePrediction(any())).thenReturn(1); when(mapper.findActiveAction(4)).thenReturn(active); service.analyzeOne(); assertThat(service.getLatestPredictions()).containsExactly(active); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-008 | 일괄 분석 응답이 null이면 빈 목록을 반환한다") void analyzeAll_null(){ when(client.predictNextCameras()).thenReturn(null); assertThat(service.analyzeAll()).isEmpty(); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-009 | 일괄 분석 결과를 카메라 번호별로 모두 반영한다") void analyzeAll(){ List<PredictiveMaintenanceResponseDTO> responses=List.of(response("ANT-002","정상",false),response("ANT-001","정상",false)); when(client.predictNextCameras()).thenReturn(responses); assertThat(service.analyzeAll()).isSameAs(responses); assertThat(service.getLatestPredictions()).extracting(CameraPdmDTO::getCameraNo).containsExactly(1,2); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-010 | 장비 코드가 없거나 지원 범위를 벗어나면 분석을 거부한다") void analyzeOne_invalidEquipment(){ when(client.predictNextCamera()).thenReturn(response(null,"정상",false),response("ANT-013","정상",false)); assertThatThrownBy(service::analyzeOne).isInstanceOf(IllegalArgumentException.class); assertThatThrownBy(service::analyzeOne).isInstanceOf(IllegalArgumentException.class); verifyNoInteractions(mapper); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-011 | 활성 위험 조치를 완료하고 최신 상태를 갱신한다") void completeAction(){ CameraPdmDTO target=danger(5); CameraPdmDTO completed=new CameraPdmDTO(); completed.setCameraNo(5); completed.setActionStatus("COMPLETED"); when(mapper.detail(10)).thenReturn(target,completed); when(auth.requireMemberNo("admin")).thenReturn(7); when(auth.normalizeActionNote(" 점검 ")).thenReturn("점검"); when(mapper.completeAction(10,7,"점검")).thenReturn(1); assertThat(service.completeAction(10," 점검 ","admin")).isSameAs(completed); verify(client).completeCameraAction(5); assertThat(service.getLatestPredictions()).containsExactly(completed); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-012 | 없는 예측과 조치 대상이 아닌 예측의 완료를 거부한다") void completeAction_invalidTarget(){ assertStatus(HttpStatus.NOT_FOUND,() -> service.completeAction(1,"x","admin")); CameraPdmDTO normal=new CameraPdmDTO(); normal.setRiskLevel("정상"); normal.setActionStatus("NOT_REQUIRED"); when(mapper.detail(2)).thenReturn(normal); assertStatus(HttpStatus.CONFLICT,() -> service.completeAction(2,"x","admin")); verifyNoInteractions(client,auth); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-013 | 조치 DB 갱신 실패를 충돌로 처리한다") void completeAction_updateConflict(){ CameraPdmDTO target=danger(1); when(mapper.detail(1)).thenReturn(target); when(auth.requireMemberNo("admin")).thenReturn(7); when(auth.normalizeActionNote("x")).thenReturn("x"); when(mapper.completeAction(1,7,"x")).thenReturn(0); assertStatus(HttpStatus.CONFLICT,() -> service.completeAction(1,"x","admin")); verify(client).completeCameraAction(1); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-014 | 시간별 저장은 최신 정상 결과만 카메라 번호순으로 저장한다") void saveHourlyNormalPredictions(){ when(client.predictNextCameras()).thenReturn(List.of(response("ANT-002","정상",false),response("ANT-001","주의",false),response("ANT-003","정상",false))); when(mapper.savePrediction(any())).thenReturn(1); service.analyzeAll(); clearInvocations(mapper); service.saveHourlyNormalPredictions(); InOrder order=inOrder(mapper); order.verify(mapper).savePrediction(argThat(d -> d.getCameraNo()==2)); order.verify(mapper).savePrediction(argThat(d -> d.getCameraNo()==3)); order.verifyNoMoreInteractions(); }
    @Test @DisplayName("UT-BE-CAMERA-PDM-015 | 최근 예측 이력은 카메라별 최신 20건으로 제한한다") void recentHistoryLimit(){ for(int i=0;i<21;i++){ when(client.predictNextCamera()).thenReturn(response("ANT-001","정상",false)); service.analyzeOne(); } assertThat(service.getRecentPredictions(1)).hasSize(20); assertThat(service.getRecentPredictions(12)).isEmpty(); }

    private PredictiveMaintenanceResponseDTO response(String no,String risk,boolean action){ PredictiveMaintenanceResponseDTO r=new PredictiveMaintenanceResponseDTO(); r.setEquipmentNo(no); r.setRiskLevel(risk); r.setRiskProbability(.8); r.setProbabilities(Map.of("정상",.8,"주의",.15,"위험",.05)); r.setSensorValues(Map.of("온도",30)); r.setActionRequired(action); r.setSensorCollectedAt("2026-08-31 10:00:00"); r.setPredictedAt(LocalDateTime.of(2026,8,31,10,1)); return r; }
    private CameraPdmDTO danger(int no){ CameraPdmDTO d=new CameraPdmDTO(); d.setCameraNo(no); d.setRiskLevel("위험"); d.setActionStatus("ACTION_REQUIRED"); return d; }
    private void assertStatus(HttpStatus status,Runnable action){ ResponseStatusException e=catchThrowableOfType(action::run,ResponseStatusException.class); assertThat(e).isNotNull(); assertThat(e.getStatusCode()).isEqualTo(status); }
}
