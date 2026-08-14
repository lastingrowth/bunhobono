package api.scheduler_p;

import api.billing_p.BillingService;
import api.cameradata_p.CameraDataService;
import api.carlog_p.CarLogService;
import api.inquiry_p.InquiryService;
import api.notice_p.NoticeService;
import api.robot_task_p.RobotTaskService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import api.robot_p.RobotService;

@Component
public class Scheduler {

    @Resource
    private CameraDataService cameraDataService;

    @Resource
    private BillingService billingService;

    @Resource
    private CarLogService carLogService;

    @Resource
    private InquiryService inquiryService;

    @Resource
    private NoticeService noticeService;

    @Resource
    private RobotService robotService;

    @Resource
    private RobotTaskService robotTaskService;

    // 매일 자정: 촬영 후 3개월이 지난 카메라 데이터를 휴지통으로 이동한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveOldCameraDataToTrash() {
        cameraDataService.autoDelete();
    }

    // 매일 자정: 출차 후 3개월이 지난 정산서와 입출차 기록을 휴지통으로 이동한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveOldCarLogsToTrash() {
        billingService.moveOldPaidBillsToTrash();
        carLogService.moveOldCarLogsToTrash();
    }

    // 매일 자정 : 답변 완료 후 3개월이 지난 문의를 휴지통으로 이동한다
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveOldInquiriesToTrash() {
        inquiryService.moveOldInquiriesToTrash();
    }

    // 매일 자정: 처리 완료 후 3개월이 지난 장기주차 알림을 휴지통으로 이동한다.
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void moveResolvedParkingNoticesToTrash() {
        noticeService.moveResolvedNoticesToTrash();
    }

    // 매일 자정: 하루 이상 주차 중인 미등록·만기 차량의 관리자 알림을 생성한다 에서
    // 10분마다 방문차량 초과 및 미등록차량 24시간 초과 알림을 생성한다.
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void createParkingNotices() {
        noticeService.createNoticesFromCarLog();
    }

    // 대기 작업을 사용 가능한 로봇 세트에 배정한다.
    @Scheduled(
            fixedDelay = 2000,
            initialDelay = 3000
    )
    public void dispatchRobotTasks() { robotService.dispatchWaitingTasks(); }

    // 실행 중인 로봇 작업을 다음 단계로 진행한다.
    @Scheduled(
            fixedDelay = 500,
            initialDelay = 5000
    )
    public void processRunningRobotTasks() { robotService.processRunningTasks(); }

    // 충전 중인 로봇의 배터리를 갱신한다.
    @Scheduled(
            fixedDelay = 1000,
            initialDelay = 1000
    )
    public void chargeIdleRobots() { robotService.chargeIdleRobots(); }

    // 출차대기면에서 10분 동안 출차하지 않은 차량을 다시 입차 처리한다.
    @Scheduled(
            fixedDelay = 5000,
            initialDelay = 10000
    )
    public void reparkTimedOutExitWaitingVehicles() {
        robotTaskService.createTimedOutReparkTasks();
    }



}
