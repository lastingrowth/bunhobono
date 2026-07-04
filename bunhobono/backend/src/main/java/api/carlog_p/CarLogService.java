package api.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CarLogService {

    @Resource
    CarLogMapper carLogMapper;

    public List<CarLogDTO> list(CarLogDTO dto) {
        List<CarLogDTO> logs = carLogMapper.list(dto);

        for (CarLogDTO log : logs) {
            // 시간 출력값 처리
            log.setInTimeText(timeText(log.getInTime()));
            log.setOutTimeText(log.getOutTime() == null ? "주차중" : timeText(log.getOutTime()));

            // 주차 시간 계산 처리
            log.setParkingTimeText(parkingTimeText(log.getInTime(), log.getOutTime()));

            // 상태/차량종류 한글 출력 처리
            log.setParkingStateText(formatParkingState(log.getParkingState()));
            log.setCarKindText(formatCarKind(log.getCarKind()));

            // 게이트명 출력 처리
            log.setInGateText(formatGate(log.getInGateName()));
            log.setOutGateText(formatGate(log.getOutGateName()));

            // 요금 출력 처리
            log.setFeeText(formatFee(log.getFee()));
        }

        return logs;
    }

    // 입차/출차 시간을 화면 출력용 문자열로 변환
    private String timeText(LocalDateTime time) {
        if (time == null) {
            return "-";
        }

        return time.format(DateTimeFormatter.ofPattern("'['yy.MM.dd HH:mm:ss']'"));
    }

    // 총 주차 시간을 계산해서 화면 출력용 문자열로 변환
    private String parkingTimeText(LocalDateTime inTime, LocalDateTime outTime) {
        if (inTime == null) {
            return "-";
        }

        LocalDateTime endTime = outTime == null ? LocalDateTime.now() : outTime;
        Duration duration = Duration.between(inTime, endTime);

        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return days + "일 " + hours + "시간 " + minutes + "분";
        }

        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }

        return minutes + "분";
    }

    // 주차 상태 코드를 한글로 변환
    private String formatParkingState(String value) {
        if ("PARKING".equals(value)) {
            return "주차중";
        }

        if ("OUT".equals(value)) {
            return "출차완료";
        }

        return "-";
    }

    // 차량 종류 코드를 한글로 변환
    private String formatCarKind(String value) {
        if ("REGISTERED".equals(value)) {
            return "등록차량";
        }

        if ("VISIT".equals(value)) {
            return "방문차량";
        }

        if ("UNKNOWN".equals(value)) {
            return "미등록차량";
        }

        return "-";
    }

    // 게이트명을 입차/출차 표시용으로 변환
    private String formatGate(String value) {
        if (value == null) {
            return "-";
        }

        if (value.endsWith("-IN")) {
            return value.replace("-IN", " GATE");
        }

        if (value.endsWith("-OUT")) {
            return value.replace("-OUT", " GATE");
        }

        return value;
    }

    // 요금을 원 단위 문자열로 변환
    private String formatFee(Integer fee) {
        if (fee == null) {
            return "0원";
        }

        return String.format("%,d원", fee);
    }

    // 자동삭제 ( 매일 새벽 3시)
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldLogs() {
        carLogMapper.deleteOldLogs();
    }
}