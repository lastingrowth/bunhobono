package api.gate_pdm_p;

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
public class GatePdmService {

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final int RECENT_HISTORY_LIMIT = 20;

    @Resource
    private GatePdmMapper gatePdmMapper;

    @Resource
    private PredictiveMaintenanceClient predictiveMaintenanceClient;

    @Resource
    private PdmActionAuthorizationService pdmActionAuthorizationService;

    private final Map<Integer, GatePdmDTO> latestPredictions =
            new ConcurrentHashMap<>();

    private final Map<Integer, Deque<GatePdmDTO>> recentPredictions =
            new ConcurrentHashMap<>();

    public List<GatePdmDTO> list() {
        return gatePdmMapper.list();
    }

    public GatePdmDTO detail(long pdmNo) {
        return gatePdmMapper.detail(pdmNo);
    }

    public int savePrediction(GatePdmDTO dto) {
        return gatePdmMapper.savePrediction(dto);
    }

    public PredictiveMaintenanceResponseDTO analyzeOne() {
        PredictiveMaintenanceResponseDTO response =
                predictiveMaintenanceClient.predictNextGate();

        processPrediction(response);

        return response;
    }

    public List<PredictiveMaintenanceResponseDTO> analyzeAll() {
        List<PredictiveMaintenanceResponseDTO> responses =
                predictiveMaintenanceClient.predictNextGates();

        if (responses == null) {
            return List.of();
        }

        for (PredictiveMaintenanceResponseDTO response : responses) {
            processPrediction(response);
        }

        return responses;
    }

    private void processPrediction(
            PredictiveMaintenanceResponseDTO response
    ) {
        GatePdmDTO dto = convertToGatePdmDTO(response);

        GatePdmDTO latestDto = saveAbnormalIfRequired(dto);
        latestPredictions.put(dto.getGateNo(), latestDto);
        addRecentPrediction(dto);
    }

    private void addRecentPrediction(GatePdmDTO dto) {
        Deque<GatePdmDTO> history =
                recentPredictions.computeIfAbsent(
                        dto.getGateNo(),
                        gateNo -> new ConcurrentLinkedDeque<>()
                );

        history.addFirst(dto);

        while (history.size() > RECENT_HISTORY_LIMIT) {
            history.pollLast();
        }
    }

    private GatePdmDTO convertToGatePdmDTO(
            PredictiveMaintenanceResponseDTO response
    ) {
        GatePdmDTO dto = new GatePdmDTO();

        dto.setGateNo(
                extractGateNo(response.getEquipmentNo())
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

    private GatePdmDTO saveAbnormalIfRequired(GatePdmDTO dto) {
        if ("주의".equals(dto.getRiskLevel())
                || "위험".equals(dto.getRiskLevel())) {
            int inserted = gatePdmMapper.savePrediction(dto);
            if (inserted == 1
                    && "ACTION_REQUIRED".equals(dto.getActionStatus())) {
                pdmActionAuthorizationService.sendDangerAlert(
                        "게이트 GATE-%02d".formatted(dto.getGateNo()),
                        dto.getSensorValues()
                );
            }
        }

        if ("위험".equals(dto.getRiskLevel())) {
            GatePdmDTO active = gatePdmMapper.findActiveAction(
                    dto.getGateNo()
            );
            if (active != null) {
                return active;
            }
        }
        return dto;
    }

    // 활성 위험을 완료 처리하고 FastAPI가 다음 CSV 행으로 진행하게 한다.
    public GatePdmDTO completeAction(
            long pdmNo,
            String actionNote,
            String loginId
    ) {
        GatePdmDTO target = gatePdmMapper.detail(pdmNo);
        validateActionTarget(target);

        int memberNo = pdmActionAuthorizationService
                .requireMemberNo(loginId);
        String normalizedNote = pdmActionAuthorizationService
                .normalizeActionNote(actionNote);

        predictiveMaintenanceClient.completeGateAction(target.getGateNo());

        if (gatePdmMapper.completeAction(
                pdmNo,
                memberNo,
                normalizedNote
        ) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 완료되었거나 조치할 수 없는 예측 결과입니다."
            );
        }

        GatePdmDTO completed = gatePdmMapper.detail(pdmNo);
        latestPredictions.put(completed.getGateNo(), completed);
        return completed;
    }

    private void validateActionTarget(GatePdmDTO target) {
        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "게이트 예지보전 결과가 없습니다."
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

    public void saveHourlyNormalPredictions() {
        latestPredictions.values()
                .stream()
                .filter(dto -> "정상".equals(dto.getRiskLevel()))
                .sorted(Comparator.comparing(GatePdmDTO::getGateNo))
                .forEach(gatePdmMapper::savePrediction);
    }

    private int extractGateNo(String equipmentNo) {
        if (equipmentNo == null || equipmentNo.isBlank()) {
            throw new IllegalArgumentException(
                    "게이트 장비 코드가 없습니다."
            );
        }

        String numberText = equipmentNo.replaceAll("\\D", "");

        if (numberText.isBlank()) {
            throw new IllegalArgumentException(
                    "게이트 번호를 찾을 수 없습니다: "
                            + equipmentNo
            );
        }

        int gateNo = Integer.parseInt(numberText);

        if (gateNo < 1 || gateNo > 12) {
            throw new IllegalArgumentException(
                    "지원하지 않는 게이트 번호입니다: "
                            + gateNo
            );
        }

        return gateNo;
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

    public List<GatePdmDTO> getLatestPredictions() {
        return latestPredictions.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                GatePdmDTO::getGateNo
                        )
                )
                .toList();
    }

    public List<GatePdmDTO> getRecentPredictions(int gateNo) {
        Deque<GatePdmDTO> history =
                recentPredictions.get(gateNo);

        if (history == null) {
            return List.of();
        }

        return List.copyOf(history);
    }
}
