package api.vehicle_nt_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleNtService {

    @Resource
    VehicleNtMapper vehicleNtMapper;

    // JWT loginId를 기준으로 입주민 본인의 알림 조회
    public List<VehicleNtDTO> notificationList(String loginId) {
        return vehicleNtMapper.list(loginId);
    }

    // 로그인한 입주민의 읽지 않은 알림을 모두 읽음 처리
    @Transactional
    public int markAllRead(String loginId) {
        return vehicleNtMapper.markAllRead(loginId);
    }

    // 관리자가 직접 입력한 사유로 방문차량 신청 반려
    // Mapper에서 알림 저장과 WAITING 신청 삭제를 한 번에 처리한다.
    @Transactional
    public int rejectRequest(
            int vehicleCarNo,
            String adminLoginId,
            String rejectReason
    ) {
        return vehicleNtMapper.rejectRequest(
                vehicleCarNo,
                adminLoginId,
                rejectReason
        );
    }

    // 10분마다 방문차량 신청과 주차시간을 확인한다.
    // 동일 알림의 중복 여부는 각 Mapper SQL에서 검사한다.
    @Transactional
    public void processVisitNotifications() {

        // 승인 처리시간이 지난 WAITING 신청
        vehicleNtMapper.rejectApprovalTimeoutRequests();

        // 승인 후 입차 가능시간이 지난 미입차 차량
        vehicleNtMapper.createNoEntryExpiredNotifications();

        // 실제 만기시간을 초과하여 주차 중인 차량
        vehicleNtMapper.createVisitOverdueNotifications();

        // 실제 만기시간을 초과한 뒤 출차한 차량
        vehicleNtMapper.createVisitOverdueExitNotifications();
    }

    // 매일 자정: 처리 완료 후 3개월이 지난 방문차량 알림 정리
    @Transactional
    public void deleteOldCompletedNotifications() {
        vehicleNtMapper.deleteOldCompletedNotifications();
    }
}
