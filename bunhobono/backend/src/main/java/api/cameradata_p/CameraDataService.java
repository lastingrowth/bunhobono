package api.cameradata_p;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CameraDataService {


    @Resource
    CameraDataMapper cameraDataMapper;

    public List<CameraDataDTO> listservice(CameraDataDTO dto) {
        return cameraDataMapper.list(dto);
    }

    public int signUp(CameraDataDTO dto) {
        return cameraDataMapper.insert(dto);
    }

    public CameraDataDTO getCameraData(int cameraDataNo) {
        return cameraDataMapper.detail(cameraDataNo);
    }

    public List<CameraDataDTO> searchByCarNo(String keyword) {
        return cameraDataMapper.searchByCarNo(keyword);
    }
    //  3개월 지난 데이터 삭제
    public void deleteOlderThanMonths(int months) {
        LocalDate cutoffDate = LocalDate.now().minusMonths(months);
        cameraDataMapper.deleteOlderThanDate(cutoffDate);
        }

    //  스케줄러 메서드 (서비스 안에 포함)
    @Scheduled(cron = "0 * * * * ?")
    public void cleanupScheduler() {
        System.out.println(" 스케줄러 실행됨: " + LocalDateTime.now());
        deleteOlderThanMonths(3);
    }
}

