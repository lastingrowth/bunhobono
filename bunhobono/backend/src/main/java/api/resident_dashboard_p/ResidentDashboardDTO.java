package api.resident_dashboard_p;

import api.carlog_p.CarLogDTO;
import api.member_p.MemberDTO;
import api.parking_p.ParkingDTO;
import api.vehicle_p.VehicleDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
// 입주민 대시보드 화면에 필요한 회원, 차량, 주차 현황을 한 번에 전달하는 응답 DTO
public class ResidentDashboardDTO {
    private MemberDTO member;
    private int normalVehicleCount;
    private int visitVehicleCount;
    private int totalParkingSpaces;
    private int availableParkingSpaces;
    private List<VehicleDTO> vehicles = new ArrayList<>();
    private List<ParkingDTO> parkings = new ArrayList<>();
    private List<CarLogDTO> recentCarLogs = new ArrayList<>();
}
