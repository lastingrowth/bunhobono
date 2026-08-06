package api.robot_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RobotService {

    @Resource
    private RobotMapper robotMapper;

    // 전체 로봇 조회
    public List<RobotDTO> list() {
        return robotMapper.list();
    }

    // 로봇 상세 조회
    public RobotDTO detail(long robotNo) {
        return robotMapper.detail(robotNo);
    }

    // 같은 세트의 로봇 조회
    public List<RobotDTO> findBySetNo(int setNo) {
        return robotMapper.findBySetNo(setNo);
    }

    // 작업 가능한 로봇 세트 조회
    public Integer findAvailableSetNo() {
        return robotMapper.findAvailableSetNo();
    }

    // 로봇 세트 작업 시작
    @Transactional
    public int startSet(int setNo) {
        return robotMapper.updateSetStatus(
                setNo,
                "STANDBY",
                "WORKING"
        );
    }

    // 로봇 세트 작업 종료
    @Transactional
    public int finishSet(int setNo) {
        return robotMapper.updateSetStatus(
                setNo,
                "WORKING",
                "STANDBY"
        );
    }

    // 가상 로봇 상태 갱신
    @Transactional
    public int updateState(
            long robotNo,
            RobotDTO dto
    ) {
        dto.setRobotNo(robotNo);
        return robotMapper.updateState(dto);
    }
}