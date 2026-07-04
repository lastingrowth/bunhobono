package api.parking_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingService {

    @Resource
    ParkingMapper parkingMapper;

    //조회
    public List<ParkingDTO> listservice(ParkingDTO dto) {
        return parkingMapper.list(dto);
    }

    //주차장등록
    public int signUp(ParkingDTO dto) {
        return parkingMapper.insert(dto);
    }

    public ParkingDTO getParking(int parkingNo) {
        return parkingMapper.detail(parkingNo);
    }
}
