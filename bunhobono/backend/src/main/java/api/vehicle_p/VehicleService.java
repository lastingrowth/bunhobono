package api.vehicle_p;

import api.vehicle_nt_p.VehicleNtService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    // 차량 알림과 자동 시간 처리는 별도 Service가 담당
    @Resource
    VehicleNtService vehicleNtService;

    // ADMIN 전체 차량 목록
    public List<VehicleDTO> listservice() {
        return vehicleMapper.list(null);
    }

    // 차량 등록 화면에서 선택 가능한 회원 검색
    public List<VehicleDTO> search(
            String vehicleType,
            String role
    ) {
        String type = vehicleType == null
                ? ""
                : vehicleType.trim().toLowerCase();

        String memberRole = role == null
                ? ""
                : role.trim().toUpperCase();

        return vehicleMapper.search(
                type,
                memberRole
        );
    }

    // RESIDENT 본인 차량 목록
    // JWT loginId를 기준으로 조회
    public List<VehicleDTO> residentList(String loginId) {
        return vehicleMapper.list(loginId);
    }

    // ADMIN 차량 등록
    // normal과 visit 모두 즉시 APPROVED 처리
    public int adminRequest(VehicleDTO dto) {
        normalizeCarNo(dto);
        normalizeVehicleType(dto);

        if (dto.getMemberNo() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "등록할 회원을 선택해주세요."
            );
        }

        validateDateRange(dto);

        // 같은 차량번호의 유효한 차량이 있으면 등록 불가
        if (
                vehicleMapper.countActiveByCarNo(
                        dto.getCarNo()
                ) > 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록되어 있거나 사용 중인 차량번호입니다."
            );
        }

        // 일반 등록차량은 회원 한 명당 최대 2대
        if (
                "normal".equals(dto.getVehicleType())
                        && vehicleMapper.countActiveNormalByMemberNo(
                        dto.getMemberNo()
                ) >= 2
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "선택한 회원은 등록차량을 최대 2대까지 등록할 수 있습니다."
            );
        }

        dto.setVehicleStatus("APPROVED");

        // ADMIN은 로그인 회원번호로 등록하지 않으므로
        // loginId 자리에 null을 전달
        return vehicleMapper.insert(null, dto);
    }

    // RESIDENT 방문차량 신청
    // 로그인한 회원에게 WAITING 상태로 등록
    public int residentVisitRequest(
            String loginId,
            VehicleDTO dto
    ) {
        normalizeCarNo(dto);

        dto.setVehicleType("visit");
        dto.setVehicleStatus("WAITING");

        // 방문 예정시간과 종료시간 검증
        validateResidentVisitDate(dto);

        // 유효한 방문차량 신청은 한 대만 가능
        if (
                vehicleMapper.countActiveVisitByLoginId(
                        loginId
                ) > 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        // 같은 차량번호가 이미 사용 중이면 신청 불가
        if (
                vehicleMapper.countActiveByCarNo(
                        dto.getCarNo()
                ) > 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        // Mapper가 loginId로 member_no를 찾아 INSERT
        return vehicleMapper.insert(
                loginId,
                dto
        );
    }

    // 차량 삭제
    public int delete(int vehicleCarNo) {
        return vehicleMapper.delete(vehicleCarNo);
    }

    // 차량 기본 정보 수정
    public int update(VehicleDTO dto) {
        normalizeCarNo(dto);
        validateDateRange(dto);

        return vehicleMapper.update(dto);
    }

    // 관리자의 승인 또는 직접 반려 처리
    // REJECTED는 vehicle_car 상태로 저장하지 않는다.
    @Transactional
    public int updateStatus(
            String adminLoginId,
            VehicleDTO dto
    ) {
        String status = dto.getVehicleStatus();

        // 승인대기 차량 승인
        if ("APPROVED".equalsIgnoreCase(status)) {
            return vehicleMapper.updateStatus(dto);
        }

        // 관리자 직접 반려
        if ("REJECTED".equalsIgnoreCase(status)) {
            String reason = dto.getRejectReason() == null
                    ? ""
                    : dto.getRejectReason().trim();

            if (reason.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST
                );
            }

            // 알림 저장과 WAITING 신청 삭제는
            // VehicleNtService가 담당
            return vehicleNtService.rejectRequest(
                    dto.getVehicleCarNo(),
                    adminLoginId,
                    reason
            );
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST
        );
    }

    // 차량번호의 모든 공백 제거
    private void normalizeCarNo(VehicleDTO dto) {
        String carNo = dto.getCarNo() == null
                ? ""
                : dto.getCarNo().replaceAll("\\s+", "");

        if (carNo.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "차량번호를 입력해주세요."
            );
        }

        dto.setCarNo(carNo);
    }

    // ADMIN 등록 차량 종류 정리
    private void normalizeVehicleType(VehicleDTO dto) {
        String type = dto.getVehicleType();

        if (type == null || type.isBlank()) {
            type = "normal";
        }

        type = type.trim().toLowerCase();

        if (
                !"normal".equals(type)
                        && !"visit".equals(type)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바르지 않은 차량 종류입니다."
            );
        }

        dto.setVehicleType(type);
    }

    // 시작시간과 종료시간 기본 검증
    private void validateDateRange(VehicleDTO dto) {
        if (
                dto.getStartDate() == null
                        || dto.getEndDate() == null
                        || !dto.getEndDate().isAfter(
                        dto.getStartDate()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "차량 등록 시작일과 종료일을 확인해주세요."
            );
        }
    }

    // RESIDENT 예상 방문시간 검증
    // 서버 현재시간으로부터 1시간 이후만 신청 가능
    private void validateResidentVisitDate(
            VehicleDTO dto
    ) {
        validateDateRange(dto);

        LocalDateTime minimumStart =
                LocalDateTime.now().plusHours(1);

        if (
                dto.getStartDate().isBefore(minimumStart)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
