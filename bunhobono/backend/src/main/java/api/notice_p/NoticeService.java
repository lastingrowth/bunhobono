package api.notice_p;

import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    @Resource
    TrashService trashService;

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

    //매분실행 테스트용

    // 매일 자정 : 자동삭제 휴지통으로다가  스케쥴로 삭제 기능
    public void moveResolvedNoticesToTrash() {
        List<Integer> noticeNos =
                noticeMapper.findResolvedNoticeNosForTrash();
        int moveCount = 0;
        for (Integer noticeNo : noticeNos) {
            try {
                trashService.moveNotice(
                        noticeNo,
                        "SCHEDULED"
                );
                moveCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(
                "휴지통으로 이동된 알림 수: " + moveCount
        );
    }

    //차량검색
    public List<NoticeDTO> search(String carNo) {
        String keyword = normalizeCarNo(carNo);

        if (keyword.isEmpty()) {
            return noticeMapper.list();
        }

        return noticeMapper.list().stream()
                .filter(notice -> {
                    String registeredCarNo = normalizeCarNo(
                            notice.getRegisteredCarNo()
                    );
                    String capturedCarNo = normalizeCarNo(
                            notice.getCapturedCarNo()
                    );

                    return registeredCarNo.contains(keyword)
                            || capturedCarNo.contains(keyword);
                })
                .toList();
    }

    private String normalizeCarNo(String carNo) {
        return carNo == null
                ? ""
                : carNo.replaceAll("\\s+", "");
    }

    // 24시 통일  장기 주차/미등록 차량 알림 생성
    public void createNoticesFromCarLog() {
        noticeMapper.createNoticesFromCarLog();
    }
}
