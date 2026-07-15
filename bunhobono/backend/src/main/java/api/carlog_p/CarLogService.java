package api.carlog_p;

import api.cameradata_p.CameraDataDTO;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarLogService {

    @Resource
    private CarLogMapper carLogMapper;

    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogMapper.list(dto);
    }


    // camera_data 저장 직후 호출: 게이트 유형에 따라 입차 생성 또는 출차 처리
    public void processCameraData(CameraDataDTO cameraData) {
        if (cameraData.getCarNo() == null || cameraData.getCarNo().isBlank()) {
            return;
        }

        CarLogDTO gate = carLogMapper.findGateByCameraNo(cameraData.getCameraNo());
        if (gate == null) {
            throw new IllegalArgumentException("카메라에 연결된 게이트를 찾을 수 없습니다.");
        }

        if ("In".equalsIgnoreCase(gate.getGateType())) {
            boolean alreadyParking = carLogMapper.existsOpenLog(cameraData);
            if (alreadyParking) { return; }
            carLogMapper.insertEntry(cameraData, gate.getGateNo());

        } else if ("Out".equalsIgnoreCase(gate.getGateType())) {
            carLogMapper.completeExit(cameraData, gate.getGateNo());
        }
        // Both 게이트는 방향 정보가 없으므로 자동 입·출차 처리하지 않음
    }


    // 시연용: 매분 실행
// @Scheduled(cron = "0 * * * * *")
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldLogs() {
        carLogMapper.deleteOldLogs();
    }
}
