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
    // JWT의 loginId로 member_no를 찾고, 해당 member_no 차량만 조회한다.
    public List<VehicleDTO> residentList(String loginId) {
        Integer memberNo = vehicleMapper.findMemberNoByLoginId(loginId);

        if (memberNo == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다.");
        }

        return vehicleMapper.listByMemberNo(memberNo);
    }

    // 기존 코드 호환용 메서드
    // 기존 Controller에서 signUp()을 호출하던 구조를 유지하기 위해 남긴다.
    // 실제로는 ADMIN 등록 신청으로 처리한다.
    public int signUp(VehicleDTO dto) {
        return adminSignUp(dto);
    }

    // ADMIN 차량 등록 신청
    // ADMIN은 normal, visit 모두 신청 가능하다.
    // 단, 등록 즉시 승인되지 않고 무조건 WAITING 상태로 들어간다.
    public int adminSignUp(VehicleDTO dto) {
        normalizeCarNo(dto);

        if (dto.getVehicleType() == null || dto.getVehicleType().isBlank()) {
            dto.setVehicleType("normal");
        }

        if (!"normal".equalsIgnoreCase(dto.getVehicleType())
                && !"visit".equalsIgnoreCase(dto.getVehicleType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차량 종류는 normal 또는 visit만 가능합니다.");
        }

        if (vehicleMapper.countActiveByCarNo(dto.getCarNo()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록 또는 승인 대기 중인 차량번호입니다.");
        }

        // ADMIN이 등록해도 바로 승인하지 않고 승인대기로 보낸다.
        dto.setVehicleStatus("WAITING");

        // ADMIN 공적 방문차량은 특정 입주민 memberNo 없이 등록될 수 있다.
        // normal도 신청 단계에서는 승인 전이므로 start/end를 꼭 계산하지 않는다.
        return vehicleMapper.insertRequest(dto);
    }

    // RESIDENT 방문차량 등록 신청
    // RESIDENT는 normal 직접 신청 불가, visit만 신청 가능하다.
    public int residentVisitSignUp(String loginId, VehicleDTO dto) {
        Integer memberNo = vehicleMapper.findMemberNoByLoginId(loginId);

        if (memberNo == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다.");
        }

        normalizeCarNo(dto);

        // RESIDENT는 무조건 방문차량만 신청 가능
        dto.setVehicleType("visit");

        // 이미 유효한 방문차량이 있으면 추가 신청 불가
        if (vehicleMapper.countActiveVisitByMemberNo(memberNo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "방문차량은 1대만 신청할 수 있습니다.");
        }

        if (vehicleMapper.countActiveByCarNo(dto.getCarNo()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록 또는 승인 대기 중인 차량번호입니다.");
        }

        if (dto.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방문 시작 날짜와 시간이 필요합니다.");
        }

        Integer periodHours = dto.getPeriodHours();

        if (periodHours == null ||
                !(periodHours == 2 ||
                        periodHours == 4 ||
                        periodHours == 6 ||
                        periodHours == 8 ||
                        periodHours == 12)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방문 시간은 2, 4, 6, 8, 12시간만 가능합니다.");
        }

        // 신청자는 로그인한 입주민
        dto.setMemberNo(memberNo);

        // 신청 상태는 무조건 WAITING
        dto.setVehicleStatus("WAITING");

        // 입주민이 선택한 시작 날짜 + 선택 시간
        dto.setEndDate(dto.getStartDate().plusHours(periodHours));

        return vehicleMapper.insertRequest(dto);
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
    public int updateStatus(VehicleDTO dto) {

        if ("APPROVED".equalsIgnoreCase(dto.getVehicleStatus())) {

            if ("normal".equalsIgnoreCase(dto.getVehicleType())) {
                LocalDateTime startDate = LocalDateTime.now();
                dto.setStartDate(startDate);

                int periodMonths = dto.getPeriodMonths() == null ? 3 : dto.getPeriodMonths();
                dto.setEndDate(startDate.plusMonths(periodMonths));
            }

            else if ("visit".equalsIgnoreCase(dto.getVehicleType())) {
                // 방문차량은 승인 시간 기준으로 startDate/endDate를 덮어쓰지 않는다.
                // 신청 시 저장된 startDate/endDate를 "신청한 방문 시간 길이"로 사용한다.
                // 실제 만기일은 car_log.in_time 기준으로 CarLogMapper에서 계산한다.
                dto.setStartDate(null);
                dto.setEndDate(null);
            }
        }

        else if ("EXPIRED".equalsIgnoreCase(dto.getVehicleStatus())) {
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차량번호를 입력해주세요.");
        }

        dto.setCarNo(carNo);
    }
}