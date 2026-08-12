package api.vehicle_p;

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

    // 로그인한 입주민 세대의 이번 달 방문차량 입차 횟수
    public int getMonthlyVisitUsedCount(String loginId) {
        return vehicleMapper
                .countMonthlyVisitEntriesByLoginId(loginId);
    }

    // 로그인한 입주민 세대의 이번 달 방문차량 등록 수
    public int getMonthlyRegisteredVisitCount(String loginId) {
        return vehicleMapper
                .countMonthlyRegisteredVisitsByLoginId(loginId);
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

    // 관리자가 승인한 일반 미등록 차량을 관리실 방문차량으로 등록
    public int registerAdminVisit(
            String adminLoginId,
            String carNo
    ) {
        VehicleDTO dto = new VehicleDTO();
        dto.setCarNo(carNo);

        normalizeCarNo(dto);

        if (vehicleMapper.countActiveByCarNo(dto.getCarNo()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록되어 있거나 사용 중인 차량 번호입니다."
            );
        }

        LocalDateTime startDate = LocalDateTime.now();

        dto.setVehicleType("visit");
        dto.setVehicleStatus("APPROVED");
        dto.setStartDate(startDate);
        dto.setEndDate(startDate.plusMinutes(30));

        int inserted = vehicleMapper.insert(
                adminLoginId,
                dto
        );

        if (inserted != 1 || dto.getVehicleCarNo() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "일반 방문차량 등록에 실패했습니다."
            );
        }
        return dto.getVehicleCarNo();
    }

    // 긴급·작업 차량 72시간 방문등록
    public int registerEmergencyVisit(
            String adminLoginId,
            String carNo
    ) {
        VehicleDTO dto = new VehicleDTO();
        dto.setCarNo(carNo);

        normalizeCarNo(dto);

        // 현재 사용 중인 동일 차량번호는 중복 등록하지 않음
        if (vehicleMapper.countActiveByCarNo(dto.getCarNo()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록되어 있거나 사용 중인 차량번호입니다."
            );
        }

        LocalDateTime startDate = LocalDateTime.now();

        dto.setVehicleType("visit");
        dto.setVehicleStatus("APPROVED");
        dto.setStartDate(startDate);
        dto.setEndDate(startDate.plusHours(72));

        int inserted = vehicleMapper.insert(
                adminLoginId,
                dto
        );

        if (inserted != 1 || dto.getVehicleCarNo() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "긴급·작업 차량 방문등록에 실패했습니다."
            );
        }

        return dto.getVehicleCarNo();
    }

    // RESIDENT 방문차량 신청
    // 로그인한 회원이 바로 등록 가능. 월 10대 제한, (입차 된 후에 차감, 입차전에는 목록에서 삭제도 가능)
    public int residentVisitRequest(
            String loginId,
            VehicleDTO dto
    ) {
        normalizeCarNo(dto);

        dto.setVehicleType("visit");
        dto.setVehicleStatus("APPROVED"); //바로 등록되게 함

        // 방문 예정시간과 종료시간 검증
        validateResidentVisitDate(dto);

        int registeredCount =
                vehicleMapper
                        .countMonthlyRegisteredVisitsByLoginId(
                                loginId
                        );

        if (registeredCount >= 10) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "이번 달 무료 방문차량 등록 10대를 모두 사용했습니다. 추가 등록을 위해 결제해 주세요."
            );
        }

        // 같은 차량번호가 이미 사용 중이면 신청 불가
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

    // 입주민과 같은 세대의 미입차 방문차량 등록 취소
    @Transactional
    public int cancelUnenteredVisit(
            String loginId,
            int vehicleCarNo
    ) {
        int result = vehicleMapper.cancelUnenteredVisit(
                loginId,
                vehicleCarNo
        );

        if (result == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 입차했거나 취소할 수 없는 방문차량입니다."
            );
        }
        return result;
    }

    // 입차 전 방문차량의 예정 시작·종료시간만 수정
    @Transactional
    public int updateUnenteredVisitTime(
            String loginId,
            int vehicleCarNo,
            VehicleDTO dto
    ) {
        validateResidentVisitDate(dto);

        int result = vehicleMapper.updateUnenteredVisitTime(
                loginId,
                vehicleCarNo,
                dto.getStartDate(),
                dto.getEndDate()
        );

        if (result == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 입차했거나 수정할 수 없는 방문차량입니다."
            );
        }

        return result;
    }

    // 로그인한 입주민 본인 일반차량의 만기일 연장
    @Transactional
    public int extendResidentNormalVehicle(
            String loginId,
            int vehicleCarNo,
            LocalDateTime endDate
    ) {
        if (endDate == null || !endDate.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "새 만기일을 현재 시간 이후로 선택해주세요."
            );
        }

        int result = vehicleMapper.extendResidentNormalVehicle(
                loginId,
                vehicleCarNo,
                endDate
        );

        if (result == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "본인 차량은 만기 전 14일 이내에서만 기간을 연장할 수 있습니다."
            );
        }

        return result;
    }

    // 차량 기본 정보 수정
    public int update(VehicleDTO dto) {
        normalizeCarNo(dto);
        validateDateRange(dto);

        return vehicleMapper.update(dto);
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
