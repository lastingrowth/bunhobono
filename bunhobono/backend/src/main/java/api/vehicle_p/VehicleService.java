package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    // 차량 전체 목록 조회
    public List<VehicleDTO> list() {
        return vehicleMapper.list();
    }


    // 차량번호 검색
    public List<VehicleDTO> search(String carNo) {
        return vehicleMapper.search(carNo);
    }


    // 차량 상세 조회
    public VehicleDTO detail(Integer vehicleNo) {
        return vehicleMapper.detail(vehicleNo);
    }


    // 차량 등록
    public int insert(VehicleDTO dto) {
        if (dto.getVehicleStatus() == null || dto.getVehicleStatus().isEmpty()) {
            dto.setVehicleStatus("WAITING");
        }
        if (dto.getVehicleType() == null || dto.getVehicleType().isEmpty()) {
            dto.setVehicleType("normal");
        }
        return vehicleMapper.insert(dto);
    }


    // 차량 정보 수정
    public int update(VehicleDTO dto) {
        return vehicleMapper.update(dto);
    }


    // 차량 삭제
    public int delete(Integer vehicleNo) {
        return vehicleMapper.delete(vehicleNo);
    }


    // 승인 대기 차량 목록
    public List<VehicleDTO> approveList() {
        return vehicleMapper.approveList();
    }


    // 승인 상태 변경
    public int changeStatus(VehicleDTO dto) {
        if ("APPROVED".equals(dto.getVehicleStatus())) {
            VehicleDTO origin = vehicleMapper.detail(dto.getVehicleCarNo());

            if (dto.getVehicleType() == null && origin != null) {
                dto.setVehicleType(origin.getVehicleType());
            }
            if (dto.getApprovedAt() == null && origin != null) {
                dto.setApprovedAt(origin.getApprovedAt());
            }
            if (dto.getApprovedAt() == null) {
                dto.setApprovedAt(LocalDateTime.now());
            }
            setApprovalPeriod(dto);
        }
        return vehicleMapper.changeStatus(dto);
    }


    // 승인 시 승인 기준 시간으로 시작일과 만기일 계산
    private void setApprovalPeriod(VehicleDTO dto) {
        LocalDateTime startDate = dto.getApprovedAt();

        dto.setStartDate(startDate);

        if ("visit".equals(dto.getVehicleType())) {
            int hours = dto.getPeriodHours() == null ? 4 : dto.getPeriodHours();
            dto.setEndDate(startDate.plusHours(hours));
            return;
        }
        int months = dto.getPeriodMonths() == null ? 3 : dto.getPeriodMonths();
        dto.setEndDate(startDate.plusMonths(months));
    }
}