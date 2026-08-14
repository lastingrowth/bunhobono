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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GatePdmService {

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    @Resource
    private GatePdmMapper gatePdmMapper;

    @Resource
    private PredictiveMaintenanceClient predictiveMaintenanceClient;

    // 게이트 예지보전 결과 전체 조회
    public List<GatePdmDTO> list() {
        return gatePdmMapper.list();
    }

    // 게이트 예지보전 결과 상세 조회
    public GatePdmDTO detail(long pdmNo) {
        return gatePdmMapper.detail(pdmNo);
    }

    // 게이트 예지보전 결과 직접 저장
    public int savePrediction(GatePdmDTO dto) {
        return gatePdmMapper.savePrediction(dto);
    }

    // 수동 검증용: 예측하고 저장 조건을 적용한다.
    public GatePdmDTO analyzeAndSave() {
        return analyze();
    }

    // FastAPI 공통 응답을 게이트 PDM 저장 DTO로 변환한다.
    private GatePdmDTO convertToGatePdmDTO(
            PredictiveMaintenanceResponseDTO response
    ) {
        GatePdmDTO dto = new GatePdmDTO();

        // GATE-02 → gateNo 2
        dto.setGateNo(
                extractEquipmentNo(
                        response.getEquipmentNo()
                )
        );

        // 모델이 최종 선택한 등급의 확률
        dto.setRiskScore(
                toBigDecimal(
                        response.getRiskProbability()
                )
        );

        dto.setRiskLevel(
                response.getRiskLevel()
        );

        // 등급별 예측 확률
        dto.setNormalProbability(
                getProbability(
                        response,
                        "정상"
                )
        );

        dto.setWarningProbability(
                getProbability(
                        response,
                        "주의"
                )
        );

        dto.setCriticalProbability(
                getProbability(
                        response,
                        "위험"
                )
        );

        // 테스트 CSV 정답과 모델 예측 비교 결과
        dto.setExpectedRiskLevel(
                response.getExpectedRiskLevel()
        );

        dto.setPredictionCorrect(
                response.getPredictionCorrect()
        );

        // 테스트 CSV에 기록된 센서 수집 시각
        dto.setSensorCollectedAt(
                parseSensorCollectedAt(
                        response.getSensorCollectedAt()
                )
        );

        // FastAPI가 실제로 예측한 시각
        dto.setPredictedAt(
                toOffsetDateTime(
                        response.getPredictedAt()
                )
        );

        return dto;
    }

    // GATE-02처럼 문자열로 전달된 장비 코드에서 숫자를 추출한다.
    private int extractEquipmentNo(
            String equipmentNo
    ) {
        if (equipmentNo == null
                || equipmentNo.isBlank()) {
            throw new IllegalArgumentException(
                    "게이트 장비 코드가 없습니다."
            );
        }

        String numberText =
                equipmentNo.replaceAll(
                        "\\D",
                        ""
                );

        if (numberText.isBlank()) {
            throw new IllegalArgumentException(
                    "게이트 장비 코드에서 번호를 찾을 수 없습니다: "
                            + equipmentNo
            );
        }

        return Integer.parseInt(numberText);
    }

    // FastAPI의 Double 확률을 DB 저장용 BigDecimal로 변환한다.
    private BigDecimal toBigDecimal(
            Double value
    ) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }

    // 정상·주의·위험 중 요청한 등급의 확률을 가져온다.
    private BigDecimal getProbability(
            PredictiveMaintenanceResponseDTO response,
            String riskLevel
    ) {
        if (response.getProbabilities() == null) {
            return null;
        }

        return toBigDecimal(
                response
                        .getProbabilities()
                        .get(riskLevel)
        );
    }

    // CSV 형식의 센서 수집 시각을 한국 시간으로 변환한다.
    private OffsetDateTime parseSensorCollectedAt(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        LocalDateTime localDateTime =
                LocalDateTime.parse(
                        value.replace(
                                " ",
                                "T"
                        )
                );

        return localDateTime
                .atZone(KOREA_ZONE)
                .toOffsetDateTime();
    }

    // FastAPI 예측 시각을 한국 시간으로 변환한다.
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

    // FastAPI에서 게이트 센서 데이터 한 건을 가져와 모델로 분석한다.
    // 최신 상태를 메모리에 보관하고 상태에 따라 DB 저장 여부를 결정한다.
    public GatePdmDTO analyze() {
        PredictiveMaintenanceResponseDTO response =
                predictiveMaintenanceClient.predictNextGate();

        GatePdmDTO dto = convertToGatePdmDTO(response);

        // Vue 조회용 최신 상태 보관
        latestPredictions.put(dto.getGateNo(), dto);

        // 조건에 따라 DB 저장
        saveIfRequired(dto);

        return dto;
    }

    private void saveIfRequired(GatePdmDTO dto) {
        if ("주의".equals(dto.getRiskLevel())
                || "위험".equals(dto.getRiskLevel())) {
            gatePdmMapper.savePrediction(dto);
            return;
        }

        if (!"정상".equals(dto.getRiskLevel())) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(KOREA_ZONE);
        OffsetDateTime lastSavedAt =
                lastNormalSavedAt.get(dto.getGateNo());

        if (lastSavedAt == null
                || lastSavedAt.plusHours(1).isBefore(now)
                || lastSavedAt.plusHours(1).isEqual(now)) {
            gatePdmMapper.savePrediction(dto);
            lastNormalSavedAt.put(dto.getGateNo(), now);
        }
    }
    
    private final Map<Integer, GatePdmDTO> latestPredictions =
            new ConcurrentHashMap<>();

    private final Map<Integer, OffsetDateTime> lastNormalSavedAt =
            new ConcurrentHashMap<>();

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
}
