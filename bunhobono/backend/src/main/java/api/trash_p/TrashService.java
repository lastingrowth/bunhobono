package api.trash_p;

import api.cameradata_p.CameraDataMapper;
import api.carlog_p.CarLogMapper;
import api.notice_p.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrashService {

    private final TrashMapper trashMapper;
    private final CameraDataMapper cameraDataMapper;
    private final CarLogMapper carLogMapper;
    private final NoticeMapper noticeMapper;

    public List<TrashDTO> list(String dataType) {
        return trashMapper.list(dataType);
    }

    public TrashDTO detail(long trashNo) {
        return trashMapper.detail(trashNo);
    }


    // 휴지통 저장과 원본 삭제를 하나의 작업으로 처리
    // 중간에 실패하면 모든 DB 변경을 롤백
    // 카메라 데이터 휴지통 이동
    @Transactional      //트랜젝션으로 묶어줌
    public void moveCameraData(int cameraDataNo, String deleteType) {
        int saved = trashMapper.saveCameraData(cameraDataNo, deleteType);

        if (saved != 1) {
            throw new IllegalArgumentException("카메라 데이터를 찾을 수 없습니다.");
        }
        int deleted = cameraDataMapper.delete(cameraDataNo);
        if (deleted != 1) {
            throw new IllegalStateException("카메라 데이터 삭제에 실패했습니다.");
        }
    }

    // 카로그 휴지통 이동
    @Transactional
    public void moveCarLog(int carLogNo, String deleteType) {
        int saved = trashMapper.saveCarLog(carLogNo, deleteType);

        if (saved != 1) {
            throw new IllegalArgumentException("카로그를 찾을 수 없습니다.");
        }
        int deleted = carLogMapper.delete(carLogNo);
        if (deleted != 1) {
            throw new IllegalStateException("카로그 삭제에 실패했습니다.");
        }
    }

    // 알림 휴지통 이동
    @Transactional
    public void moveNotice(int noticeNo, String deleteType) {
        int saved = trashMapper.saveNotice(noticeNo, deleteType);

        if (saved != 1) {
            throw new IllegalArgumentException("알림을 찾을 수 없습니다.");
        }
        int deleted = noticeMapper.delete(noticeNo);
        if (deleted != 1) {
            throw new IllegalStateException("알림 삭제에 실패했습니다.");
        }
    }
}
