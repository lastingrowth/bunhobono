package api.mem_notice_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MemNoticeService {

    @Resource
    private MemNoticeMapper memNoticeMapper;

    public List<MemNoticeDTO> notificationList(String loginId) {
        MemNoticeDTO dto = new MemNoticeDTO();
        dto.setLoginId(loginId);

        return memNoticeMapper.list(dto);
    }

    @Transactional
    public int markRead(String loginId, int memNoticeNo) {
        MemNoticeDTO dto = new MemNoticeDTO();
        dto.setLoginId(loginId);
        dto.setMemNoticeNo(memNoticeNo);

        return memNoticeMapper.markRead(dto);
    }

    @Transactional
    public int deleteNotification(String loginId, int memNoticeNo) {
        MemNoticeDTO dto = new MemNoticeDTO();
        dto.setLoginId(loginId);
        dto.setMemNoticeNo(memNoticeNo);

        int deletedCount = memNoticeMapper.delete(dto);

        if (deletedCount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "알림을 찾을 수 없습니다."
            );
        }

        return deletedCount;
    }

    // BillService 등 알림을 발생시키는 백엔드 기능에서 호출한다.
    @Transactional
    public int createNotification(MemNoticeDTO dto) {
        validateNotification(dto);
        return memNoticeMapper.insert(dto);
    }

    private void validateNotification(MemNoticeDTO dto) {
        if (dto == null
                || dto.getRecipientMemberNo() == null
                || dto.getReferenceTable() == null
                || dto.getReferenceTable().isBlank()
                || dto.getReferenceNo() == null
                || dto.getNoticeType() == null
                || dto.getNoticeType().isBlank()
                || dto.getTitle() == null
                || dto.getTitle().isBlank()
                || dto.getMessage() == null
                || dto.getMessage().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "입주민 알림 생성 정보가 올바르지 않습니다."
            );
        }
    }
}
