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

    public boolean isAlreadyParking(CameraDataDTO cameraData) {
        return carLogMapper.existsOpenLog(cameraData);
    }

    // 게이트가 열린 뒤에만 입차 로그를 생성하거나 출차 로그를 완료한다.
    public int processCameraData(CameraDataDTO cameraData, GateDTO gate) {
        if ("In".equalsIgnoreCase(gate.getGateType())) {
            return carLogMapper.insertEntry(cameraData, gate.getGateNo());

        } else if ("Out".equalsIgnoreCase(gate.getGateType())) {
            return carLogMapper.completeExit(cameraData, gate.getGateNo());
        }
        return 0;
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
