package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
        String carNo = dto.getCarNo() == null ? "" : dto.getCarNo().trim();
        if (carNo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차량번호를 입력해주세요.");
        }

        dto.setCarNo(carNo);
        if (vehicleMapper.countActiveByCarNo(carNo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록 또는 승인 대기 중인 차량번호입니다.");
        }
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
        if ("APPROVED".equalsIgnoreCase(dto.getVehicleStatus())) {
            LocalDateTime startDate = LocalDateTime.now();
            dto.setStartDate(startDate);

            if ("normal".equalsIgnoreCase(dto.getVehicleType())) {
                int periodMonths = dto.getPeriodMonths() == null ? 3 : dto.getPeriodMonths();
                dto.setEndDate(startDate.plusMonths(periodMonths));
            } else if ("visit".equalsIgnoreCase(dto.getVehicleType())) {
                int periodHours = dto.getPeriodHours() == null ? 1 : dto.getPeriodHours();
                dto.setEndDate(startDate.plusHours(periodHours));
            }
        } else if ("EXPIRED".equalsIgnoreCase(dto.getVehicleStatus())) {
            dto.setEndDate(LocalDateTime.now());
        } else if ("UNKNOWN".equalsIgnoreCase(dto.getVehicleStatus())) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }
        return vehicleMapper.updateStatus(dto);
    }

}
