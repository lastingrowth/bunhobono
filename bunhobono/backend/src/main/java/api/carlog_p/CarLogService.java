package api.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarLogService {

    @Resource
    CarLogMapper carLogMapper;

    // 차량 입출차 로그 목록 조회
    public List<CarLogDTO> list(CarLogDTO dto) {
        List<CarLogDTO> logs = carLogMapper.list(dto);

        for (CarLogDTO log : logs) {
            setLogState(log);
        }

        return logs;
    }

    // 조회된 로그의 상태 코드 세팅
    private void setLogState(CarLogDTO log) {
        if (log.getOutTime() == null) {
            log.setParkingState("PARKING");
        } else {
            log.setParkingState("OUT");
        }

        if (log.getVehicleCarNo() == null) {
            log.setCarKind("UNKNOWN");
            return;
        }

        if ("visit".equals(log.getVehicleType())) {
            log.setCarKind("VISIT");
            return;
        }

        log.setCarKind("REGISTERED");
    }

    // 오래된 차량 로그 자동삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldLogs() {
        carLogMapper.deleteOldLogs();
    }
}