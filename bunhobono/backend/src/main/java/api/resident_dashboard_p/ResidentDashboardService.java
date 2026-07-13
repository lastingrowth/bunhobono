package api.resident_dashboard_p;

import api.member_p.MemberDTO;
import api.member_p.MemberService;
import api.parking_p.ParkingDTO;
import api.parking_p.ParkingService;
import api.vehicle_p.VehicleDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// 기존 회원·주차 서비스와 대시보드 Mapper의 결과를 화면용 DTO로 집계한다.
public class ResidentDashboardService {

    @Resource
    private MemberService memberService;

    @Resource
    private ParkingService parkingService;

    @Resource
    private ResidentDashboardMapper dashboardMapper;

    // 로그인 ID를 기준으로 본인 정보, 차량 수, 주차 가능 면수와 최근 기록을 계산한다.
    public ResidentDashboardDTO getDashboard(String loginId) {
        MemberDTO member = memberService.residentMypage(loginId);
        if (member == null) {
            throw new IllegalArgumentException("입주민 정보를 찾을 수 없습니다.");
        }

        List<VehicleDTO> vehicles = dashboardMapper.findVehicles(loginId);
        List<ParkingDTO> parkings = parkingService.listservice(new ParkingDTO());

        ResidentDashboardDTO dashboard = new ResidentDashboardDTO();
        dashboard.setMember(member);
        dashboard.setVehicles(vehicles);
        dashboard.setParkings(parkings);
        dashboard.setRecentCarLogs(dashboardMapper.findRecentCarLogs(loginId));
        dashboard.setNormalVehicleCount((int) vehicles.stream()
                .filter(vehicle -> "normal".equalsIgnoreCase(vehicle.getVehicleType()))
                .count());
        dashboard.setVisitVehicleCount((int) vehicles.stream()
                .filter(vehicle -> "visit".equalsIgnoreCase(vehicle.getVehicleType()))
                .count());
        dashboard.setTotalParkingSpaces(parkings.stream()
                .mapToInt(ParkingDTO::getParkingSpaces)
                .sum());
        dashboard.setAvailableParkingSpaces(parkings.stream()
                .mapToInt(ParkingDTO::getAvailableSpaces)
                .sum());
        return dashboard;
    }

    // 내 차량 화면에서 사용할 로그인 입주민 전용 목록이다.
    public List<VehicleDTO> getVehicles(String loginId) {
        return dashboardMapper.findVehicles(loginId);
    }

    @Transactional
    public int createVehicle(String loginId, VehicleDTO vehicle) {
        normalizeVehiclePeriod(vehicle);
        int vehicleCarNo = dashboardMapper.insertVehicle(vehicle);
        dashboardMapper.linkVehicle(loginId, vehicleCarNo);
        return vehicleCarNo;
    }

    public int updateVehicle(String loginId, int vehicleCarNo, VehicleDTO vehicle) {
        validateOwner(loginId, vehicleCarNo);
        normalizeVehiclePeriod(vehicle);
        vehicle.setVehicleCarNo(vehicleCarNo);
        return dashboardMapper.updateVehicle(vehicle);
    }

    @Transactional
    public int deleteVehicle(String loginId, int vehicleCarNo) {
        validateOwner(loginId, vehicleCarNo);
        int deleted = dashboardMapper.deleteVehicle(vehicleCarNo);
        dashboardMapper.unlinkVehicle(loginId, vehicleCarNo);
        return deleted;
    }

    private void validateOwner(String loginId, int vehicleCarNo) {
        if (!dashboardMapper.ownsVehicle(loginId, vehicleCarNo)) {
            throw new IllegalArgumentException("본인이 등록한 차량만 변경할 수 있습니다.");
        }
    }

    // 입주민 차량은 기간을 사용하지 않고 방문 차량만 유효한 시작·종료 기간을 허용한다.
    private void normalizeVehiclePeriod(VehicleDTO vehicle) {
        if ("visit".equalsIgnoreCase(vehicle.getVehicleType())) {
            if (vehicle.getStartDate() == null || vehicle.getEndDate() == null) {
                throw new IllegalArgumentException("방문 차량은 시작일과 종료일이 필요합니다.");
            }
            if (!vehicle.getEndDate().isAfter(vehicle.getStartDate())) {
                throw new IllegalArgumentException("방문 종료일은 시작일 이후여야 합니다.");
            }
            return;
        }

        vehicle.setStartDate(null);
        vehicle.setEndDate(null);
    }
}
