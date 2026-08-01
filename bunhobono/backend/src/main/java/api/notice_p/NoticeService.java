package api.notice_p;

import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    @Resource
    private TrashService trashService;

    // 관리자 알림 전체 조회
    public List<NoticeDTO> list() {
        return noticeMapper.list();
    }

    // 관리자 알림 상세 조회
    public NoticeDTO detail(int noticeNo) {
        NoticeDTO notice = noticeMapper.detail(noticeNo);

        if (notice == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return notice;
    }

    // 차량번호 검색
    public List<NoticeDTO> search(String carNo) {
        String keyword = normalizeCarNo(carNo);

        return keyword.isEmpty()
                ? noticeMapper.list()
                : noticeMapper.search(keyword);
    }

    // 미처리 알림을 처리완료로 변경
    @Transactional
    public int status(
            String adminLoginId,
            NoticeDTO dto
    ) {
        Integer adminMemberNo =
                noticeMapper.findAdminMemberNoByLoginId(
                        adminLoginId
                );

        if (adminMemberNo == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN
            );
        }

        NoticeDTO notice =
                noticeMapper.detail(dto.getNoticeNo());

        if (notice == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND
            );
        }

        if (!"Resolved".equalsIgnoreCase(
                dto.getAlertStat()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST
            );
        }

        int updated = switch (notice.getNoticeType()) {
            case "VISIT_OVERDUE", "UNKNOWN_OVERSTAY" ->
                    noticeMapper.resolveAfterExit(
                            notice.getNoticeNo(),
                            adminMemberNo
                    );

            case "OCR_REVIEW" ->
                    noticeMapper.resolveOcrReview(
                            notice.getNoticeNo(),
                            adminMemberNo
                    );

            case "EXIT_WITHOUT_ENTRY" ->
                    noticeMapper.resolveExitWithoutEntry(
                            cameraDataNo(notice),
                            adminMemberNo
                    );

            default ->
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST
                    );
        };

        return requireUpdated(updated);
    }

    // 방문차량 및 미등록차량 장기주차 알림 생성
    public int createNoticesFromCarLog() {
        return noticeMapper.createNoticesFromCarLog();
    }

    // OCR 확인 필요 알림 생성
    public int createOcrReviewNotice(int cameraDataNo) {
        return noticeMapper.createOcrReviewNotice(
                cameraDataNo
        );
    }

    // 입차기록 없는 출차 시도 알림 생성
    public int createExitWithoutEntryNotice(
            int cameraDataNo
    ) {
        return noticeMapper.createExitWithoutEntryNotice(
                cameraDataNo
        );
    }

    // 처리완료 후 3개월이 지난 알림을 휴지통으로 이동
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
                "휴지통으로 이동된 알림 수: "
                        + moveCount
        );
    }

    // 원본 촬영번호가 없으면 스냅샷 촬영번호 사용
    private int cameraDataNo(NoticeDTO notice) {
        Integer cameraDataNo =
                notice.getCameraDataNo() != null
                        ? notice.getCameraDataNo()
                        : notice.getSnapshotCameraDataNo();

        if (cameraDataNo == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        return cameraDataNo;
    }

    // Mapper의 UPDATE 결과 검증
    private int requireUpdated(int updated) {
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        return updated;
    }

    private String normalizeCarNo(String carNo) {
        return carNo == null
                ? ""
                : carNo.replaceAll("\\s+", "");
    }
}