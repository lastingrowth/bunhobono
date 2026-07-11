package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    // 등록 차량 목록 조회
    public List<VehicleDTO> listservice() {
        return vehicleMapper.list();
    }

    // 차량 등록
    public int signUp(VehicleDTO dto) {
        return vehicleMapper.insert(dto);
    }

    // 차량 삭제
    public int delete(int vehicleCarNo) {
        return vehicleMapper.delete(vehicleCarNo);
    }

    // 차량 기본 정보 수정
    public int update(VehicleDTO dto) {
        return vehicleMapper.update(dto);
    }

    // 관리자 차량 상태 변경
    public int updateStatus(VehicleDTO dto) {
        return vehicleMapper.updateStatus(dto);
    }

}
