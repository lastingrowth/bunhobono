package api.gate_pdm_p;

import api.predictive_maintenance_p.PredictiveMaintenanceClient;
import api.predictive_maintenance_p.PredictiveMaintenanceResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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

        latestPredictions.put(dto.getGateNo(), dto);
        addRecentPrediction(dto);
        saveAbnormalIfRequired(dto);
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

    private void saveAbnormalIfRequired(GatePdmDTO dto) {
        if ("주의".equals(dto.getRiskLevel())
                || "위험".equals(dto.getRiskLevel())) {
            gatePdmMapper.savePrediction(dto);
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
