package api.camera_pdm_p;

import api.predictive_maintenance_p.PredictiveMaintenanceClient;
import api.predictive_maintenance_p.PredictiveMaintenanceResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CameraPdmService {

    @Resource
    private CameraPdmMapper cameraPdmMapper;

    @Resource
    private PredictiveMaintenanceClient predictiveMaintenanceClient;

    // 카메라 예지보전 결과 전체 조회
    public List<CameraPdmDTO> list() {
        return cameraPdmMapper.list();
    }

    // 카메라 예지보전 결과 상세 조회
    public CameraPdmDTO detail(long pdmNo) {
        return cameraPdmMapper.detail(pdmNo);
    }

    // 카메라 예지보전 결과 DB 저장
    public int savePrediction(CameraPdmDTO dto) {
        return cameraPdmMapper.savePrediction(dto);
    }

    // FastAPI에 카메라 예지보전을 요청한다.
    // FastAPI는 테스트 CSV에서 다음 센서 데이터 한 건을 가져와
    // XGBoost 모델로 정상·주의·위험 등급과 확률을 예측한다.
    public PredictiveMaintenanceResponseDTO analyzeAndSave() {
        return predictiveMaintenanceClient.predictNextCamera();
    }
}
