package api.notice_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    NoticeMapper noticeMapper;

    public List<NoticeDTO> listservice(NoticeDTO dto) {
        return noticeMapper.list(dto);
    }

    public NoticeDTO detail(int noticeNo) {
        return noticeMapper.detail(noticeNo);
    }

    public void status(NoticeDTO dto) {
        noticeMapper.status(dto);
    }

    public int aftAYearDelete() {
        return noticeMapper.aftAYearDelete();
    }

    public int createNoticeFromCarLog() {
        return noticeMapper.createNoticeFromCarLog();
    }
}
