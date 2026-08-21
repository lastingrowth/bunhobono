package api.camera_pdm_p;

import api.predictive_maintenance_p.PredictiveMaintenanceClient;
import api.predictive_maintenance_p.PredictiveMaintenanceResponseDTO;
import api.predictive_maintenance_p.PdmActionAuthorizationService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class CameraPdmService {

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final int RECENT_HISTORY_LIMIT = 20;

    @Resource
    private CameraPdmMapper cameraPdmMapper;

    @Resource
    private PredictiveMaintenanceClient predictiveMaintenanceClient;

    @Resource
    private PdmActionAuthorizationService pdmActionAuthorizationService;

    // 카메라별 최신 예측 결과
    private final Map<Integer, CameraPdmDTO> latestPredictions =
            new ConcurrentHashMap<>();

    private final Map<Integer, Deque<CameraPdmDTO>> recentPredictions =
            new ConcurrentHashMap<>();

    // DB에 저장된 카메라 예지보전 결과 전체 조회
    public List<CameraPdmDTO> list() {
        return cameraPdmMapper.list();
    }

    // DB에 저장된 카메라 예지보전 결과 상세 조회
    public CameraPdmDTO detail(long pdmNo) {
        return cameraPdmMapper.detail(pdmNo);
    }

    // 카메라 예지보전 결과 직접 저장
    public int savePrediction(CameraPdmDTO dto) {
        return cameraPdmMapper.savePrediction(dto);
    }

    // 카메라 한 건 수동 분석
    public PredictiveMaintenanceResponseDTO analyzeOne() {
        PredictiveMaintenanceResponseDTO response =
                predictiveMaintenanceClient.predictNextCamera();

        processPrediction(response);

        return response;
    }

    // 카메라 1~12 일괄 분석
    public List<PredictiveMaintenanceResponseDTO> analyzeAll() {
        List<PredictiveMaintenanceResponseDTO> responses =
                predictiveMaintenanceClient.predictNextCameras();

        if (responses == null) {
            return List.of();
        }

        for (PredictiveMaintenanceResponseDTO response : responses) {
            processPrediction(response);
        }

        return responses;
    }

    // FastAPI 응답을 변환하고 최신 상태와 DB 저장 조건을 적용한다.
    private void processPrediction(
            PredictiveMaintenanceResponseDTO response
    ) {
        CameraPdmDTO dto = convertToCameraPdmDTO(response);

        CameraPdmDTO latestDto = saveAbnormalIfRequired(dto);
        latestPredictions.put(dto.getCameraNo(), latestDto);
        addRecentPrediction(dto);
    }

    private void addRecentPrediction(CameraPdmDTO dto) {
        Deque<CameraPdmDTO> history =
                recentPredictions.computeIfAbsent(
                        dto.getCameraNo(),
                        cameraNo -> new ConcurrentLinkedDeque<>()
                );

        history.addFirst(dto);

        while (history.size() > RECENT_HISTORY_LIMIT) {
            history.pollLast();
        }
    }

    // FastAPI 응답을 DB 저장용 DTO로 변환한다.
    private CameraPdmDTO convertToCameraPdmDTO(
            PredictiveMaintenanceResponseDTO response
    ) {
        CameraPdmDTO dto = new CameraPdmDTO();

        dto.setCameraNo(
                extractCameraNo(response.getEquipmentNo())
        );

        dto.setRiskScore(
                toBigDecimal(response.getRiskProbability())
        );

        dto.setRiskLevel(response.getRiskLevel());

        dto.setNormalProbability(
                getProbability(response, "정상")
        );

        dto.setWarningProbability(
                getProbability(response, "주의")
        );

        dto.setCriticalProbability(
                getProbability(response, "위험")
        );

        dto.setExpectedRiskLevel(
                response.getExpectedRiskLevel()
        );

        dto.setPredictionCorrect(
                response.getPredictionCorrect()
        );

        dto.setSourceRowIndex(response.getRowIndex());
        dto.setSensorValues(response.getSensorValues());
        dto.setActionStatus(
                Boolean.TRUE.equals(response.getActionRequired())
                        ? "ACTION_REQUIRED"
                        : "NOT_REQUIRED"
        );

        dto.setSensorCollectedAt(
                parseSensorCollectedAt(
                        response.getSensorCollectedAt()
                )
        );

        dto.setPredictedAt(
                toOffsetDateTime(response.getPredictedAt())
        );

        return dto;
    }

    // 주의·위험 결과는 분석 직후 즉시 저장한다.
    private CameraPdmDTO saveAbnormalIfRequired(CameraPdmDTO dto) {
        if ("주의".equals(dto.getRiskLevel())
                || "위험".equals(dto.getRiskLevel())) {
            int inserted = cameraPdmMapper.savePrediction(dto);
            if (inserted == 1
                    && "ACTION_REQUIRED".equals(dto.getActionStatus())) {
                pdmActionAuthorizationService.sendDangerAlert(
                        "카메라 ANT-%03d".formatted(dto.getCameraNo()),
                        dto.getSensorValues()
                );
            }
        }

        if ("위험".equals(dto.getRiskLevel())) {
            CameraPdmDTO active = cameraPdmMapper.findActiveAction(
                    dto.getCameraNo()
            );
            if (active != null) {
                return active;
            }
        }
        return dto;
    }

    // 활성 위험을 완료 처리하고 FastAPI가 다음 CSV 행으로 진행하게 한다.
    public CameraPdmDTO completeAction(
            long pdmNo,
            String actionNote,
            String loginId
    ) {
        CameraPdmDTO target = cameraPdmMapper.detail(pdmNo);
        validateActionTarget(target);

        int memberNo = pdmActionAuthorizationService
                .requireMemberNo(loginId);
        String normalizedNote = pdmActionAuthorizationService
                .normalizeActionNote(actionNote);

        predictiveMaintenanceClient.completeCameraAction(
                target.getCameraNo()
        );

        if (cameraPdmMapper.completeAction(
                pdmNo,
                memberNo,
                normalizedNote
        ) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 완료되었거나 조치할 수 없는 예측 결과입니다."
            );
        }

        CameraPdmDTO completed = cameraPdmMapper.detail(pdmNo);
        latestPredictions.put(completed.getCameraNo(), completed);
        return completed;
    }

    private void validateActionTarget(CameraPdmDTO target) {
        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "카메라 예지보전 결과가 없습니다."
            );
        }
        if (!"위험".equals(target.getRiskLevel())
                || !"ACTION_REQUIRED".equals(target.getActionStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 조치가 필요한 위험 결과가 아닙니다."
            );
        }
    }

    // 매시 정각에 카메라별 최신 정상 결과를 한 건씩 저장한다.
    public void saveHourlyNormalPredictions() {
        latestPredictions.values()
                .stream()
                .filter(dto -> "정상".equals(dto.getRiskLevel()))
                .sorted(Comparator.comparing(CameraPdmDTO::getCameraNo))
                .forEach(dto -> {
            cameraPdmMapper.savePrediction(dto);
                });
    }

    // ANT-001 같은 장비 코드에서 카메라 번호를 추출한다.
    private int extractCameraNo(String equipmentNo) {
        if (equipmentNo == null || equipmentNo.isBlank()) {
            throw new IllegalArgumentException(
                    "카메라 장비 코드가 없습니다."
            );
        }

        String numberText =
                equipmentNo.replaceAll("\\D", "");

        if (numberText.isBlank()) {
            throw new IllegalArgumentException(
                    "카메라 번호를 찾을 수 없습니다: "
                            + equipmentNo
            );
        }

        int cameraNo = Integer.parseInt(numberText);

        if (cameraNo < 1 || cameraNo > 12) {
            throw new IllegalArgumentException(
                    "지원하지 않는 카메라 번호입니다: "
                            + cameraNo
            );
        }

        return cameraNo;
    }

    private BigDecimal getProbability(
            PredictiveMaintenanceResponseDTO response,
            String riskLevel
    ) {
        if (response.getProbabilities() == null) {
            return null;
        }

        return toBigDecimal(
                response.getProbabilities().get(riskLevel)
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }

    private OffsetDateTime parseSensorCollectedAt(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime
                .parse(value.replace(" ", "T"))
                .atZone(KOREA_ZONE)
                .toOffsetDateTime();
    }

    private OffsetDateTime toOffsetDateTime(
            LocalDateTime value
    ) {
        if (value == null) {
            return null;
        }

        return value
                .atZone(KOREA_ZONE)
                .toOffsetDateTime();
    }

    // 카메라 1~12의 현재 최신 예측 결과
    public List<CameraPdmDTO> getLatestPredictions() {
        return latestPredictions.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                CameraPdmDTO::getCameraNo
                        )
                )
                .toList();
    }

    public List<CameraPdmDTO> getRecentPredictions(int cameraNo) {
        Deque<CameraPdmDTO> history =
                recentPredictions.get(cameraNo);

        if (history == null) {
            return List.of();
        }

        return List.copyOf(history);
    }
}
