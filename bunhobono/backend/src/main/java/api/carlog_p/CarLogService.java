package api.carlog_p;
import api.trash_p.TrashService;

import api.cameradata_p.CameraDataDTO;
import api.gate_p.GateDTO;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CarLogService {

    @Resource
    private CarLogMapper carLogMapper;

    @Resource
    private TrashService trashService;

    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogMapper.list(dto);
    }

    // 등록차량은 vehicleCarNo, 미등록차량은 carNo로 현재 미출차 로그 확인
    public boolean isAlreadyParking(CameraDataDTO cameraData) {
        return carLogMapper.existsOpenLog(cameraData);
    }


    // camera_data 저장 직후 호출: 게이트 유형에 따라 입차 생성 또는 출차 처리
    public void processCameraData(CameraDataDTO cameraData, GateDTO gate) {
        if (cameraData.getCarNo() == null || cameraData.getCarNo().isBlank()) {
            return;
        }

        if ("In".equalsIgnoreCase(gate.getGateType())) {
            boolean alreadyParking = isAlreadyParking(cameraData);
            if (alreadyParking) { return; }
            carLogMapper.insertEntry(cameraData, gate.getGateNo());

        } else if ("Out".equalsIgnoreCase(gate.getGateType())) {
            carLogMapper.completeExit(cameraData, gate.getGateNo());
        }
    }

    // 테스트용: 매분 실행
    // @Scheduled(cron = "0 * * * * *")
    // 매일 자정: 출차 후 15일 지난 입출차 기록을 휴지통으로 이동
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveOldCarLogsToTrash() {
        List<Integer> carLogNos =
                carLogMapper.findOldCarLogNosForTrash();
        int moveCount = 0;
        for (Integer carLogNo : carLogNos) {
            try {
                trashService.moveCarLog(
                        carLogNo,
                        "SCHEDULED"
                );
                moveCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(
                "휴지통으로 이동된 입출차 기록 수: " + moveCount
        );
    }
}
