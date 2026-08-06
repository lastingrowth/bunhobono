package api.parking_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {

    @Resource
    private ParkingMapper parkingMapper;

    public List<ParkingDTO> list() {
        return parkingMapper.list();
    }

    public int insert(ParkingDTO dto) {
        return parkingMapper.insert(dto);
    }

    public int delete(int parkingNo) {
        return parkingMapper.delete(parkingNo);
    }

    public int update(ParkingDTO dto) {
        return parkingMapper.updateParking(dto);
    }
}