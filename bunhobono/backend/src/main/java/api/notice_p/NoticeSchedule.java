package api.notice_p;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoticeSchedule {

    @Resource
    NoticeService noticeService;

    @Scheduled(cron = "0 0 3 1 1 *", zone = "Asia/Seoul")
    public void aftAYearDelete() {
        int deletedCount = noticeService.aftAYearDelete();
        System.out.println("1년 지난 처리완료 알림 삭제 건수: " + deletedCount);
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void createNoticeFromCarLog() {
        int createdCount = noticeService.createNoticeFromCarLog();
        System.out.println("car_log 기준 생성된 알림 건수: " + createdCount);
    }
}
