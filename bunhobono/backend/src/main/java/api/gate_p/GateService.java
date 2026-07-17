package api.gate_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GateService {

    @Resource
    GateMapper gateMapper;

    // 게이트 목록
    public List<GateDTO> listservice(GateDTO dto) {
        return gateMapper.list(dto);
    }

    // 게이트 등록
    public int signUp(GateDTO dto) {
        return gateMapper.insert(dto);
    }

//    // 게이트 상세
//    public GateDTO getGateDetail(int gateNo) {
//        return gateMapper.detail(gateNo);
//    }

    // 게이트 삭제
    public int delete(int gateNo) {
        return gateMapper.delete(gateNo);
    }

    // 게이트 정보 수정
    public int updateGate(GateDTO dto) {
        return gateMapper.update(dto);
    }
}
