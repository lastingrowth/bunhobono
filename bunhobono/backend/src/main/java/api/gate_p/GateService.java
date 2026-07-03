package apt.gate_p;

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

    public GateDTO getGate(int gateNo) {
        return gateMapper.findById(gateNo);
    }


}
