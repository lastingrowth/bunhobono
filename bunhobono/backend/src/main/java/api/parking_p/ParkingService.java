package apt.parking_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {

    @Resource
    ParkingMapper parkingMapper;

    public List<ParkingDTO> listservice(ParkingDTO dto){
        return parkingMapper.list(dto);
    }
}
