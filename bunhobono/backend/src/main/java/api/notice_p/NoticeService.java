package api.notice_p;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    public List<NoticeDTO> list() {
        return noticeMapper.list();
    }

    public NoticeDTO detail(int noticeNo){
        return noticeMapper.list().stream()
                .filter(notice -> notice.getNoticeNo() == noticeNo)
                .findFirst()
                .orElse(null);
    }

    public int status(NoticeDTO dto) {
        return noticeMapper.status(dto);
    }

    // 매년 1월 1일 새벽 3시: 1년 지난 해결 알림 삭제
    @Scheduled(cron = "0 0 3 1 1 *", zone = "Asia/Seoul")
    public void deleteResolvedNoticesAfterOneYear() {
        noticeMapper.deleteResolvedNoticesAfterOneYear();
    }

    // 매시 정각: 장기 주차/미등록 차량 알림 생성
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void createNoticesFromCarLog() {
        noticeMapper.createNoticesFromCarLog();
    }
}
