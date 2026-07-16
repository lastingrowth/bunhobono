package api.cameradata_p;
import api.gate_p.GateService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import api.trash_p.TrashService;

@Service
public class CameraDataService {


    @Resource
    CameraDataMapper cameraDataMapper;

    @Resource
    GateService gateService;

    @Resource
    TrashService trashService;

    @Value("${file.camera-data}")
    private String uploadDir;

    //list
    public List<CameraDataDTO> listservice(CameraDataDTO dto) {
        return cameraDataMapper.list(dto);
    }

    //ocr
    @Transactional
    public int ocr(int cameraNo,
                   String carNo,
                   Double confidenceScore,
                   MultipartFile file) {
        try{
            //1. 저장 폴더 없으면 생성
            Files.createDirectories(Paths.get(uploadDir));

            //2. 현재 시간 생성
            LocalDateTime now = LocalDateTime.now();

            String timeText = now.format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
            );

            // 3. 원본 파일 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String ext = "";

            if (originalFilename != null && originalFilename.contains(".")){
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 4. 파일명에 위험한 문자 제거
            String okCarNo = carNo.replaceAll("[^가-힣a-zA-Z0-9]", "");

            // 5. 저장할 파일명 생성
            String savedFilename = timeText + "_" + okCarNo + ext;

            // 6. 실제 저장 경로
            Path savePath = Paths.get(uploadDir, savedFilename);

            // 7. 원본 이미지 파일 저장
            Files.copy(file.getInputStream(), savePath);

            // 8. DB에 저장할 DTO 생성
            CameraDataDTO dto = new CameraDataDTO();
            dto.setCameraNo(cameraNo);
            dto.setCarNo(carNo);
            dto.setCaptureTime(Timestamp.valueOf(now));
            dto.setImagePath(savePath.toString());
            dto.setRecognitionState(carNo != null && !carNo.isBlank());
            dto.setConfidenceScore(confidenceScore);

            // vehicleNo는 나중에 차량 테이블 조회 후 넣으면 됨
            //dto.setVehicleCarNo(null);

            // OCR로 읽은 차량번호가 등록 차량 테이블에 있으면 vehicle_car_no를 찾아서 저장
            //
            Integer vehicleCarNo = cameraDataMapper.findVehicleCarNo(carNo);
            dto.setVehicleCarNo(vehicleCarNo);

            // 9. 촬영 데이터는 항상 저장하고, 승인 및 중복 검사는 GateService에서 처리한다.
            int insertCount = cameraDataMapper.insert(dto);
            if (insertCount == 1) {
                CameraDataDTO savedData = cameraDataMapper.detail(dto.getCameraDataNo());
                gateService.processAutomaticPassage(savedData);
            }
            return insertCount;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("카메라 OCR 이미지 저장 실패", e);
        }
    }

    public CameraDataDTO getCameraData(int cameraDataNo) {
        return cameraDataMapper.detail(cameraDataNo);
    }

    public List<CameraDataDTO> searchByCarNo(String keyword) {
        return cameraDataMapper.searchByCarNo(keyword);
    }

    public int deleteData() {
        List<CameraDataDTO> deleteList =
                cameraDataMapper.deleteTarget();

        int deleteCount = 0;

        for (CameraDataDTO dto : deleteList) {
            try {
                trashService.moveCameraData(
                        dto.getCameraDataNo(),
                        "SCHEDULED"
                );
                deleteCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deleteCount;
    }
    //매분실행 테스트용
   //@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")

    //밤 12시실행요      자동쓰레기통행 삭제
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void autoDelete() {
        System.out.println("스케쥴러 삭제 실행");

        int count = deleteData();

        System.out.println("휴지통으로 이동된 카메라 데이터 수  : " + count);
    }

    public Path getCameraImagePath(int cameraDataNo) {
        CameraDataDTO dto = cameraDataMapper.detail(cameraDataNo);

        if (dto == null ||
                dto.getImagePath() == null ||
                dto.getImagePath().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이미지 경로가 없습니다."
            );
        }

        Path path = Paths.get(dto.getImagePath());

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "이미지 파일이 없습니다."
            );
        }
        return path;
    }
}
