package api.scheduler_p;

import api.cameradata_p.CameraDataService;
import api.carlog_p.CarLogService;
import api.notice_p.NoticeService;
import api.vehicle_nt_p.VehicleNtService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    @Resource
    private CameraDataService cameraDataService;

    @Resource
    private CarLogService carLogService;

    @Resource
    private NoticeService noticeService;

    @Resource
    private VehicleNtService vehicleNtService;

    // 10분마다 방문차량 상태를 확인하고 입주민 차량 알림을 생성한다.
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void processVisitNotifications() {
        vehicleNtService.processVisitNotifications();
    }

    // 매일 자정: 처리 완료 후 3개월이 지난 방문차량 알림을 삭제한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void deleteOldCompletedVehicleNotifications() {
        vehicleNtService.deleteOldCompletedNotifications();
    }

    // 매일 자정: 촬영 후 3개월이 지난 카메라 데이터를 휴지통으로 이동한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveOldCameraDataToTrash() {
        cameraDataService.autoDelete();
    }

    // 매일 자정: 출차 후 3개월이 지난 입출차 기록을 휴지통으로 이동한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveOldCarLogsToTrash() {
        carLogService.moveOldCarLogsToTrash();
    }

    // 매일 자정: 처리 완료 후 1개월이 지난 장기주차 알림을 휴지통으로 이동한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveResolvedParkingNoticesToTrash() {
        noticeService.moveResolvedNoticesToTrash();
    }

    // 매일 자정: 하루 이상 주차 중인 미등록·만기 차량의 관리자 알림을 생성한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void createParkingNotices() {
        noticeService.createNoticesFromCarLog();
    }
}
