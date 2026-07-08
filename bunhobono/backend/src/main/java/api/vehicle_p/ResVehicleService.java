package api.vehicle_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResVehicleService {

    @Resource
    VehicleMapper vehicleMapper;

    // 현재 로그인한 입주민 정보 조회
    public ResVehicleMemberDTO myInfo() {
        String loginId = getLoginId();

        return vehicleMapper.findLoginMemberInfo(loginId);
    }

    // 현재 로그인한 입주민의 차량 목록 조회
    public List<VehicleDTO> list() {
        Integer registeredNo = getLoginMemberNo();

        return vehicleMapper.resList(registeredNo);
    }

    // 현재 로그인한 입주민의 차량 상세 조회
    public VehicleDTO detail(Integer vehicleNo) {
        Integer registeredNo = getLoginMemberNo();

        return vehicleMapper.resDetail(registeredNo, vehicleNo);
    }

    // 현재 로그인한 입주민 명의로 차량 등록 신청
    public int insert(VehicleDTO dto) {
        Integer registeredNo = getLoginMemberNo();

        setWaitingRequest(dto, registeredNo);

        return vehicleMapper.resInsert(dto);
    }

    // 현재 로그인한 입주민 본인 차량만 수정 신청
    public int update(Integer vehicleNo, VehicleDTO dto) {
        Integer registeredNo = getLoginMemberNo();

        dto.setVehicleCarNo(vehicleNo);
        setWaitingRequest(dto, registeredNo);

        return vehicleMapper.resUpdate(dto);
    }

    // 현재 로그인한 입주민 본인 차량만 삭제
    public int delete(Integer vehicleNo) {
        Integer registeredNo = getLoginMemberNo();

        return vehicleMapper.resDelete(registeredNo, vehicleNo);
    }

    // 차량 등록/수정 신청 상태 세팅
    private void setWaitingRequest(VehicleDTO dto, Integer registeredNo) {
        dto.setRegisteredNo(registeredNo);
        dto.setVehicleStatus("WAITING");
        dto.setApprovedNo(null);
        dto.setApprovedAt(null);
        dto.setStartDate(null);
        dto.setEndDate(null);
    }

    // 현재 로그인한 사용자의 member_no 조회
    private Integer getLoginMemberNo() {
        String loginId = getLoginId();

        return vehicleMapper.findMemberNoByLoginId(loginId);
    }

    // JWT 인증 정보에서 loginId 조회
    private String getLoginId() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
}