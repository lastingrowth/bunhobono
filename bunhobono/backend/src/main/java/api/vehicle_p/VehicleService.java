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

    // ADMIN 전체 차량 목록 조회
    public List<VehicleDTO> listservice() {
        return vehicleMapper.list(null);
    }

    // 차량 등록 화면에서 선택 가능한 회원 검색
    public List<VehicleDTO> search(String vehicleType, String role) {
        String type = vehicleType == null ? "" : vehicleType.trim().toLowerCase();
        String memberRole = role == null ? "" : role.trim().toUpperCase();

        return vehicleMapper.search(type, memberRole);
    }

    // RESIDENT 본인 차량 목록 조회
    public List<VehicleDTO> residentList(String loginId) {
        return vehicleMapper.list(loginId);
    }

    // ADMIN 차량 등록
    // 관리자가 등록하면 승인대기 없이 바로 APPROVED 처리한다.
    public int adminRequest(VehicleDTO dto) {
        normalizeCarNo(dto);

        // 차량은 반드시 특정 회원에게 등록되어야 한다.
        if (dto.getMemberNo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (dto.getVehicleType() == null || dto.getVehicleType().isBlank()) {
            dto.setVehicleType("normal");
        }

        if (!"normal".equalsIgnoreCase(dto.getVehicleType())
                && !"visit".equalsIgnoreCase(dto.getVehicleType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (vehicleMapper.countActiveByCarNo(dto.getCarNo()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        // 등록차량은 회원 1명당 2대까지만 가능
        if ("normal".equalsIgnoreCase(dto.getVehicleType())
                && vehicleMapper.countActiveNormalByMemberNo(dto.getMemberNo()) >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        dto.setVehicleStatus("APPROVED");

        return vehicleMapper.insert(dto);
    }

    // RESIDENT 방문차량 등록 신청
    public int residentVisitRequest(String loginId, VehicleDTO dto) {
        normalizeCarNo(dto);

        dto.setVehicleType("visit");
        dto.setVehicleStatus("WAITING");

        if (vehicleMapper.countActiveVisitByLoginId(loginId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (vehicleMapper.countActiveByCarNo(dto.getCarNo()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return vehicleMapper.insertResidentVisit(loginId, dto);
    }

    public int delete(int vehicleCarNo) {
        return vehicleMapper.delete(vehicleCarNo);
    }

    public int update(VehicleDTO dto) {
        normalizeCarNo(dto);
        return vehicleMapper.update(dto);
    }

    public int updateStatus(VehicleDTO dto) {
        if ("EXPIRED".equalsIgnoreCase(dto.getVehicleStatus())) {
            dto.setEndDate(LocalDateTime.now());
        } else if ("UNKNOWN".equalsIgnoreCase(dto.getVehicleStatus())) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }

        return vehicleMapper.updateStatus(dto);
    }

    private void normalizeCarNo(VehicleDTO dto) {
        String carNo = dto.getCarNo() == null ? "" : dto.getCarNo().trim();

        if (carNo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        dto.setCarNo(carNo);
    }
}