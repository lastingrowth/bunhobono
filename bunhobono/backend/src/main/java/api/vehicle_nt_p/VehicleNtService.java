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

    // 로그인한 입주민의 읽지 않은 알림 전체 읽음 처리
    @Transactional
    public int markAllRead(String loginId) {
        return vehicleNtMapper.markAllRead(loginId);
    }

    // 관리자 방문차량 승인
    // 차량 승인과 승인 알림 저장을 Mapper의 한 SQL로 처리한다.
    @Transactional
    public int approveRequest(
            int vehicleCarNo,
            String adminLoginId
    ) {
        return vehicleNtMapper.approveRequest(
                vehicleCarNo,
                adminLoginId
        );
    }

    // 관리자가 입력한 사유로 방문차량 신청 반려
    // 신청 삭제와 반려 알림 저장을 Mapper의 한 SQL로 처리한다.
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

    // 로그인한 입주민 본인의 일반 알림 직접 삭제
    // VISIT_OVERDUE_EXIT는 Mapper 조건에 의해 삭제되지 않는다.
    @Transactional
    public int deleteNotification(
            String loginId,
            int vehicleNtNo
    ) {
        return vehicleNtMapper.deleteNotification(
                loginId,
                vehicleNtNo
        );
    }

    // 방문차량 신청과 주차시간을 확인하고 필요한 알림을 생성한다.
    // 실제 실행 주기는 api.scheduler_p.Scheduler에서 관리한다.
    @Transactional
    public void processVisitNotifications() {

        // 승인시간이 지난 WAITING 신청 자동 취소 및 알림
        vehicleNtMapper.rejectApprovalTimeoutRequests();

        // 승인 후 입차시간이 지난 미입차 차량 알림
        vehicleNtMapper.createNoEntryExpiredNotifications();

        // 실제 만기시간을 초과하여 주차 중인 차량 알림
        vehicleNtMapper.createVisitOverdueNotifications();

        // 실제 만기시간을 초과한 뒤 출차한 차량 최종 알림
        vehicleNtMapper.createVisitOverdueExitNotifications();
    }

    // 읽은 지 7일이 지난 일반 알림 자동삭제
    // VISIT_OVERDUE_EXIT는 자동삭제 대상에서 제외한다.
    @Transactional
    public void deleteOldCompletedNotifications() {
        vehicleNtMapper.deleteOldCompletedNotifications();
    }
}