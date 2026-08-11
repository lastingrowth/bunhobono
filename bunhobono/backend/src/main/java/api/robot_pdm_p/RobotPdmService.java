package api.robot_pdm_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RobotPdmService {

    @Resource
    private RobotPdmMapper robotPdmMapper;

    // 예지보전 결과 전체 조회
    public List<RobotPdmDTO> list() {
        return robotPdmMapper.list();
    }

    // 예지보전 결과 상세 조회
    public RobotPdmDTO detail(long pdmNo) {
        return robotPdmMapper.detail(pdmNo);
    }

    // 예지보전 분석 결과 저장
    public int savePrediction(RobotPdmDTO dto) {
        return robotPdmMapper.savePrediction(dto);
    }
}