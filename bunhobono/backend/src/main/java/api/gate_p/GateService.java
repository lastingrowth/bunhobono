package api.gate_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GateService {


    @Resource
    GateMapper gateMapper;

    public List<GateDTO> listservice(GateDTO dto){
        return gateMapper.list(dto);
    }

    //게이트 등록
    public int signUp(GateDTO dto) {
        return gateMapper.insert(dto);
    }

//    //상세
//    public GateDTO getGateDetail(int gateNo) {
//        return gateMapper.detail(gateNo);
//    }

    //Delete
    public int delete(int gateNo) {
        return gateMapper.delete(gateNo);
    }
    //수정
    public int updateGate(GateDTO dto) {
        return gateMapper.update(dto);
    }
}
