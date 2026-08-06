package api.cameradata_p;

import api.carlog_p.CarLogService;
import api.carlog_p.CarLogDTO;
import api.gate_p.GateDTO;
import api.gate_p.GateService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import api.notice_p.NoticeService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import api.trash_p.TrashService;
import api.vehicle_p.VehicleService;


@Service
public class CameraDataService {


    @Resource
    CameraDataMapper cameraDataMapper;

    @Resource
    CarLogService carLogService;

    @Resource
    GateService gateService;

    @Resource
    TrashService trashService;

    @Resource
    NoticeService noticeService;

    @Resource
    VehicleService vehicleService;

    @Value("${file.camera-data}")
    private String uploadDir;

    // 카메라 기록 서버 페이징 조회
    public CameraDataDTO.PageResponse page(int page, int size, String keyword, Integer parkingNo) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String safeKeyword = keyword == null ? null : keyword.trim();

        List<CameraDataDTO> items = cameraDataMapper.page(safeKeyword, parkingNo, safeSize, offset);
        long totalCount = cameraDataMapper.countPage(safeKeyword, parkingNo);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / safeSize));

        return new CameraDataDTO.PageResponse(items, totalCount, safePage, safeSize, totalPages);
    }

    //ocr
    @Transactional
    public CameraDataDTO ocr(int cameraNo,
                             String carNo,
                             Double confidenceScore,
                             MultipartFile file,
                             MultipartFile cropFile) {
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

            // 7-1. 번호판 크롭 이미지가 함께 전달되면 별도 파일로 저장
            Path cropSavePath = null;

            if (cropFile != null && !cropFile.isEmpty()) {
                String cropOriginalFilename = cropFile.getOriginalFilename();
                String cropExt = ".jpg";

                if (cropOriginalFilename != null
                        && cropOriginalFilename.contains(".")) {
                    cropExt = cropOriginalFilename.substring(
                            cropOriginalFilename.lastIndexOf(".")
                    );
                }

                String cropSavedFilename =
                        timeText + "_" + okCarNo + "_crop" + cropExt;

                cropSavePath = Paths.get(uploadDir, cropSavedFilename);
                Files.copy(cropFile.getInputStream(), cropSavePath);
            }

            // 8. DB에 저장할 DTO 생성
            CameraDataDTO dto = new CameraDataDTO();
            dto.setCameraNo(cameraNo);
            String recognizedCarNo = carNo == null
                    ? ""
                    : carNo.trim().replaceAll("\\s+", "");
            dto.setOcrCarNo(recognizedCarNo);
            dto.setCarNo(recognizedCarNo);
            dto.setCaptureTime(Timestamp.valueOf(now));
            dto.setImagePath(savePath.toString());
            dto.setCropImagePath(
                    cropSavePath != null ? cropSavePath.toString() : null
            );
            boolean recognitionConfirmed = carNo != null
                    && !carNo.isBlank()
                    && !"미인식".equals(carNo)
                    && confidenceScore != null
                    && confidenceScore > 95.0;
            dto.setRecognitionState(recognitionConfirmed);
            dto.setConfidenceScore(confidenceScore);

            // vehicleNo는 나중에 차량 테이블 조회 후 넣으면 됨
            //dto.setVehicleCarNo(null);

            // OCR로 읽은 차량번호가 등록 차량 테이블에 있으면 vehicle_car_no를 찾아서 저장
            //
            Integer vehicleCarNo =
                    cameraDataMapper.findVehicleCarNo(recognizedCarNo);
            boolean autoCorrected = false;

            if (vehicleCarNo == null) {
                CameraDataDTO aliasVehicle =
                        cameraDataMapper.findApprovedVehicleByAlias(
                                recognizedCarNo
                        );

                if (aliasVehicle != null) {
                    vehicleCarNo = aliasVehicle.getVehicleCarNo();
                    dto.setCarNo(aliasVehicle.getCarNo());
                    autoCorrected = true;
                }
            }

            dto.setVehicleCarNo(vehicleCarNo);
            dto.setAutoCorrected(autoCorrected);

            // 9. camera_data 저장
            // 카메라 인식 기록이므로 등록/미등록 관계 없이 먼저 저장
            int insertCount = cameraDataMapper.insert(dto);

            boolean saved = insertCount == 1;
            boolean registered = vehicleCarNo != null;
            boolean gateOpened = false;
            Integer gateNo = null;

            if (saved && !recognitionConfirmed) {
                noticeService.createOcrReviewNotice(
                        dto.getCameraDataNo()
                );
            }

            if (saved) {
                // 데이터를 촬영한 카메라가 어느 게이트에 연결되어 있는지 확인
                GateDTO gate = gateService.findByCameraNo(cameraNo);

                // 카메라와 연결된 게이트가 없으면 입출차 처리를 진행하지 않는다
                if (gate == null) {
                    dto.setSaved(true);
                    dto.setRegistered(registered);
                    dto.setGateOpened(false);
                    dto.setGateNo(null);
                    return dto;
                }

                gateNo = gate.getGateNo();

                boolean isSiteGate =
                        "SITE".equalsIgnoreCase(gate.getGateArea());

                boolean isParkingGate =
                        "B1".equalsIgnoreCase(gate.getGateArea())
                                || "B2".equalsIgnoreCase(gate.getGateArea());

                boolean isEntryGate =
                        "In".equalsIgnoreCase(gate.getGateType());

                boolean isExitGate =
                        "Out".equalsIgnoreCase(gate.getGateType());

                // 내부 주차장 출차 차량의 미출차 기록 조회
                CarLogDTO parkedLog =
                        isParkingGate && isExitGate
                                ? carLogService.findCurrentlyParked(dto)
                                : null;

                // 출차 OCR 원본은 ocr_car_no에 유지하고, 화면과 카메라
                // 데이터에는 입차 때 관리자가 확정한 차량번호를 표시한다.
                if (parkedLog != null && parkedLog.getCarNo() != null) {
                    dto.setCarNo(parkedLog.getCarNo());
                    dto.setVehicleCarNo(parkedLog.getVehicleCarNo());
                    dto.setRecognitionState(true);
                    cameraDataMapper.applyMatchedCarNo(dto);
                }

                // B1·B2에서 입차기록 없이 출차를 시도한 경우
                if (isParkingGate
                        && isExitGate
                        && recognitionConfirmed
                        && parkedLog == null) {
                    noticeService.createExitWithoutEntryNotice(
                            dto.getCameraDataNo()
                    );
                }

                // 차량 유형·승인상태·방문기간을 포함하여 다시 조회
                CameraDataDTO processingData =
                        cameraDataMapper.detail(dto.getCameraDataNo());

                if (processingData == null) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT
                    );
                }

                registered = processingData.getVehicleCarNo() != null;

                int processed = 0;

                // B1·B2는 입출차 처리가 성공해야 게이트 개방
                if (isParkingGate) {
                    boolean canProcess =
                            (isEntryGate
                                    && registered
                                    && recognitionConfirmed)
                                    ||
                                    (isExitGate && parkedLog != null);

                    if (canProcess) {
                        processed =
                                carLogService.processCameraData(
                                        processingData
                                );
                    }
                }

                // 정문·후문은 승인 차량이면 car_log 없이 개방
                boolean canOpenGate =
                        (isSiteGate
                                && registered
                                && recognitionConfirmed)
                                || processed == 1;

                if (canOpenGate) {
                    if (gateService.open(gate.getGateNo()) != 1) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT
                        );
                    }

                    gateOpened = true;

                    cameraDataMapper.markGateOpened(
                            dto.getCameraDataNo()
                    );

                    gateService.scheduleClose(
                            gate.getGateNo()
                    );
                }

                // 미등록 차량이면 camera_data만 저장하고 멈춤
                // 이후 관리자 대시보드에서 확인 후 수동으로 게이트를 열도록 처리
            }

            dto.setSaved(saved);
            dto.setRegistered(registered);
            dto.setGateOpened(gateOpened);
            dto.setGateNo(gateNo);
            return dto;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("카메라 OCR 이미지 저장 실패", e);
        }
    }

    public CameraDataDTO getCameraData(int cameraDataNo) {
        return cameraDataMapper.detail(cameraDataNo);
    }

    @Transactional
    public CameraDataDTO editCarNo(
            int cameraDataNo,
            CameraDataDTO request) {
        CameraDataDTO current = cameraDataMapper.detail(cameraDataNo);

        if (current == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "카메라 데이터가 존재하지 않습니다"
            );
        }

        String carNo = request == null ? null : request.getCarNo();
        String normalizedCarNo = carNo == null
                ? ""
                : carNo.trim().replaceAll("\\s+", "");

        if (!normalizedCarNo.matches(
                "^(?:[가-힣]{2})?\\d{2,3}[가-힣]\\d{4}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "차량번호 형식이 올바르지 않습니다"
            );
        }


        Integer vehicleCarNo =
                cameraDataMapper.findVehicleCarNo(normalizedCarNo);

        boolean saveAlias = request != null
                && Boolean.TRUE.equals(request.getSaveAlias());
        String previousCarNo = current.getCarNo() == null
                ? ""
                : current.getCarNo().trim().replaceAll("\\s+", "");

        boolean aliasSaved = false;

        if (saveAlias && vehicleCarNo != null
                && !previousCarNo.isBlank()
                && !previousCarNo.equals(normalizedCarNo)) {
            Integer actualVehicleCarNo =
                    cameraDataMapper.findVehicleCarNo(previousCarNo);

            if (actualVehicleCarNo != null
                    && !actualVehicleCarNo.equals(vehicleCarNo)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "다른 등록 차량의 실제 번호는 별칭으로 사용할 수 없습니다"
                );
            }

            Integer aliasVehicleCarNo =
                    cameraDataMapper.findAliasVehicleCarNo(previousCarNo);

            if (aliasVehicleCarNo != null
                    && !aliasVehicleCarNo.equals(vehicleCarNo)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 다른 차량에 연결된 OCR 별칭입니다"
                );
            }

            cameraDataMapper.updateAlias(
                    vehicleCarNo,
                    previousCarNo
            );
            aliasSaved = true;
        }

        CameraDataDTO updateDto = new CameraDataDTO();
        updateDto.setCameraDataNo(cameraDataNo);
        updateDto.setCarNo(normalizedCarNo);
        updateDto.setVehicleCarNo(vehicleCarNo);

        if (cameraDataMapper.updateCarNo(updateDto) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "차량번호 수정에 실패했습니다"
            );
        }

        // 이미 게이트가 열려 입출차 로그가 생성 또는 완료된 경우에도
        // 동일한 camera_data를 참조하는 로그의 차량 정보를 함께 정정한다.
        carLogService.correctByCameraData(updateDto);

        CameraDataDTO updated = cameraDataMapper.detail(cameraDataNo);
        updated.setRegistered(vehicleCarNo != null);
        updated.setAliasSaved(aliasSaved);
        return updated;
    }

    // 긴급·작업 차량 방문등록과 게이트 개방
    @Transactional
    public int openGateByCameraData(
            String adminLoginId,
            int cameraDataNo
    ) {
        CameraDataDTO dto =
                cameraDataMapper.detail(cameraDataNo);

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "카메라 데이터가 존재하지 않습니다"
            );
        }

        GateDTO gate =
                gateService.findByCameraNo(dto.getCameraNo());

        if (gate == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "카메라와 연결된 게이트가 없습니다"
            );
        }

        // 긴급·작업 승인은 정문·후문 입구에서만 가능
        if (
                !"SITE".equalsIgnoreCase(gate.getGateArea())
                        || !"In".equalsIgnoreCase(
                        gate.getGateType()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST
            );
        }

        // 이미 등록된 차량에는 긴급·작업 등록을 적용하지 않음
        if (dto.getVehicleCarNo() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        String carNo = dto.getCarNo();

        if (carNo == null || carNo.isBlank()) {
            carNo = dto.getOcrCarNo();
        }

        carNo = carNo == null
                ? ""
                : carNo.replaceAll("\\s+", "");

        if (carNo.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST
            );
        }

        // 로그인 관리자의 24시간 방문차량으로 등록
        int registered =
                vehicleService.registerEmergencyVisit(
                        adminLoginId,
                        carNo
                );

        if (registered != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        Integer vehicleCarNo =
                cameraDataMapper.findVehicleCarNo(carNo);

        if (vehicleCarNo == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        // 촬영 데이터와 새 방문차량 연결
        CameraDataDTO matched = new CameraDataDTO();
        matched.setCameraDataNo(cameraDataNo);
        matched.setCarNo(carNo);
        matched.setVehicleCarNo(vehicleCarNo);

        cameraDataMapper.applyMatchedCarNo(matched);

        // 정문·후문은 car_log를 만들지 않고 게이트만 개방
        gateService.open(gate.getGateNo());
        cameraDataMapper.markGateOpened(cameraDataNo);
        gateService.scheduleClose(gate.getGateNo());

        return 1;
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

    //밤 12시실행요      자동쓰레기통행 삭제
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

    @Transactional
    public CameraDataDTO updateNote(
            int cameraDataNo,
            String camNote) {
        int updated = cameraDataMapper.updateNote(
                cameraDataNo,
                camNote
        );
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "카메라 데이터가 존재하지 않습니다."
            );
        }
        return cameraDataMapper.detail(cameraDataNo);
    }

    public Path getCameraCropImagePath(int cameraDataNo) {
        CameraDataDTO dto = cameraDataMapper.detail(cameraDataNo);
        if (dto == null ||
                dto.getCropImagePath() == null ||
                dto.getCropImagePath().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "번호판 크롭 이미지 경로가 없습니다."
            );
        }

        Path path = Paths.get(dto.getCropImagePath());

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "번호판 크롭 이미지 파일이 없습니다."
            );
        }

        return path;
    }
}