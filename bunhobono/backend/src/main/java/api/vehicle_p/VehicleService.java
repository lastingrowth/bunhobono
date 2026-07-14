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
        return vehicleMapper.list();
    }

    // RESIDENT 본인 차량 목록 조회
    // JWT의 loginId로 member와 vehicle_car를 조인해서 본인 차량만 조회한다.
    public List<VehicleDTO> residentList(String loginId) {
        return vehicleMapper.listByLoginId(loginId);
    }

    // ADMIN 차량 등록 신청
    // ADMIN은 normal, visit 모두 신청 가능하다.
    // 등록 즉시 승인되지 않고 무조건 WAITING 상태로 들어간다.
    // startDate/endDate 계산은 프론트에서 처리해서 보낸다.
    public int adminRequest(VehicleDTO dto) {
        normalizeCarNo(dto);

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

        dto.setVehicleStatus("WAITING");

        return vehicleMapper.insert(dto);
    }

    // RESIDENT 방문차량 등록 신청
    // RESIDENT는 normal 직접 신청 불가, visit만 신청 가능하다.
    // member_no는 Mapper에서 loginId로 찾아서 INSERT한다.
    public int residentVisitRequest(String loginId, VehicleDTO dto) {
        normalizeCarNo(dto);

        // RESIDENT는 무조건 방문차량만 신청 가능
        dto.setVehicleType("visit");

        // 신청 상태는 무조건 WAITING
        dto.setVehicleStatus("WAITING");

        // 이미 유효한 방문차량이 있으면 추가 신청 불가
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

    // 차량 삭제
    public int delete(int vehicleCarNo) {
        return vehicleMapper.delete(vehicleCarNo);
    }

    // 차량 기본 정보 수정
    public int update(VehicleDTO dto) {
        normalizeCarNo(dto);
        return vehicleMapper.update(dto);
    }

    // 관리자 차량 상태 변경
    // WAITING 차량을 APPROVED / UNKNOWN / EXPIRED 처리한다.
    // startDate/endDate 계산은 프론트에서 처리해서 보낸다.
    public int updateStatus(VehicleDTO dto) {

        if ("EXPIRED".equalsIgnoreCase(dto.getVehicleStatus())) {
            dto.setEndDate(LocalDateTime.now());
        }

        else if ("UNKNOWN".equalsIgnoreCase(dto.getVehicleStatus())) {
            dto.setStartDate(null);
            dto.setEndDate(null);
        }

        return vehicleMapper.updateStatus(dto);
    }

    // 차량번호 공백 제거 및 빈 값 검증
    private void normalizeCarNo(VehicleDTO dto) {
        String carNo = dto.getCarNo() == null ? "" : dto.getCarNo().trim();

        if (carNo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        dto.setCarNo(carNo);
    }
}