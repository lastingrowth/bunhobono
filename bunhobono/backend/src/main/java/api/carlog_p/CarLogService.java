package api.carlog_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CarLogService {

    @Resource
    CarLogMapper carLogMapper;

    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogMapper.list(dto);
    }
}