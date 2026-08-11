package api.robot_log_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RobotLogService {

    @Resource
    private RobotLogMapper robotLogMapper;

    // 로봇별·작업별 원시 상태값 조회
    public List<RobotLogDTO> list(
            Long robotNo,
            Long taskNo
    ) {
        return robotLogMapper.list(
                robotNo,
                taskNo
        );
    }

    // 로봇 원시 상태값 저장
    public int insert(RobotLogDTO dto) {
        if (dto.getSourceEventId() == null
                || dto.getSourceEventId().isBlank()) {
            dto.setSourceEventId(
                    UUID.randomUUID().toString()
            );
        }

        return robotLogMapper.insert(dto);
    }
}