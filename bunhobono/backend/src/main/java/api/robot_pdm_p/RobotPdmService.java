package api.robot_pdm_p;

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
public class RobotPdmService {

    // CSV 시각을 한국 시간으로 변환하고 로봇 수와 메모리 이력 개수를 제한한다.
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int ROBOT_COUNT = 8;
    private static final int RECENT_HISTORY_LIMIT = 20;

    // robot_pdm 테이블 조회와 저장을 담당한다.
    @Resource
    private RobotPdmMapper robotPdmMapper;

    // FastAPI의 실제 로봇 예지보전 모델 API를 호출한다.
    @Resource
    private PredictiveMaintenanceClient predictiveMaintenanceClient;

    @Resource
    private PdmActionAuthorizationService pdmActionAuthorizationService;

    // 로봇별 가장 최근 예측 결과를 메모리에 보관한다.
    private final Map<Integer, RobotPdmDTO> latestPredictions =
            new ConcurrentHashMap<>();

    // 로봇별 최근 예측 결과를 최대 20건까지 메모리에 보관한다.
    private final Map<Integer, Deque<RobotPdmDTO>> recentPredictions =
            new ConcurrentHashMap<>();

    // DB에 저장된 로봇 예지보전 결과 전체를 조회한다.
    public List<RobotPdmDTO> list() {
        return robotPdmMapper.list();
    }

    // DB에 저장된 특정 예지보전 결과를 조회한다.
    public RobotPdmDTO detail(long pdmNo) {
        return robotPdmMapper.detail(pdmNo);
    }

    // 전달받은 예지보전 결과를 DB에 직접 저장한다.
    public int savePrediction(RobotPdmDTO dto) {
        return robotPdmMapper.savePrediction(dto);
    }

    // 테스트 CSV의 다음 행 한 건을 실제 모델로 수동 분석한다.
    public PredictiveMaintenanceResponseDTO analyzeOne() {
        PredictiveMaintenanceResponseDTO response =
                predictiveMaintenanceClient.predictNextRobot();
        processPrediction(response);
        return response;
    }

    // 로봇 1~8의 다음 CSV 행을 실제 모델로 한 번에 분석한다.
    public List<PredictiveMaintenanceResponseDTO> analyzeAll() {
        List<PredictiveMaintenanceResponseDTO> responses =
                predictiveMaintenanceClient.predictNextRobots();

        if (responses == null) {
            return List.of();
        }

        responses.forEach(this::processPrediction);
        return responses;
    }

    // FastAPI 응답을 변환한 뒤 최신값, 최근 이력, DB 저장 조건을 적용한다.
    private void processPrediction(PredictiveMaintenanceResponseDTO response) {
        RobotPdmDTO dto = convertToRobotPdmDTO(response);

        RobotPdmDTO latestDto = saveAbnormalIfRequired(dto);
        latestPredictions.put(dto.getRobotNo(), latestDto);
        addRecentPrediction(dto);
    }

    // 해당 로봇의 최신 이력을 앞에 추가하고 20건을 초과한 과거 이력은 제거한다.
    private void addRecentPrediction(RobotPdmDTO dto) {
        Deque<RobotPdmDTO> history = recentPredictions.computeIfAbsent(
                dto.getRobotNo(),
                robotNo -> new ConcurrentLinkedDeque<>()
        );

        history.addFirst(dto);

        while (history.size() > RECENT_HISTORY_LIMIT) {
            history.pollLast();
        }
    }

    // FastAPI 공통 응답을 robot_pdm 저장 및 조회용 DTO로 변환한다.
    private RobotPdmDTO convertToRobotPdmDTO(
            PredictiveMaintenanceResponseDTO response
    ) {
        RobotPdmDTO dto = new RobotPdmDTO();

        dto.setRobotNo(extractRobotNo(response.getEquipmentNo()));
        dto.setRiskScore(toBigDecimal(response.getRiskProbability()));
        dto.setRiskLevel(response.getRiskLevel());
        dto.setNormalProbability(getProbability(response, "정상"));
        dto.setWarningProbability(getProbability(response, "주의"));
        dto.setCriticalProbability(getProbability(response, "위험"));
        dto.setExpectedRiskLevel(response.getExpectedRiskLevel());
        dto.setPredictionCorrect(response.getPredictionCorrect());
        dto.setSourceRowIndex(response.getRowIndex());
        dto.setSensorValues(response.getSensorValues());
        dto.setActionStatus(
                Boolean.TRUE.equals(response.getActionRequired())
                        ? "ACTION_REQUIRED"
                        : "NOT_REQUIRED"
        );
        dto.setSensorCollectedAt(
                parseSensorCollectedAt(response.getSensorCollectedAt())
        );
        dto.setPredictedAt(toOffsetDateTime(response.getPredictedAt()));

        return dto;
    }

    // 주의 또는 위험 결과는 분석 직후 DB에 즉시 저장한다.
    private RobotPdmDTO saveAbnormalIfRequired(RobotPdmDTO dto) {
        if ("주의".equals(dto.getRiskLevel())
                || "위험".equals(dto.getRiskLevel())) {
            int inserted = robotPdmMapper.savePrediction(dto);
            if (inserted == 1
                    && "ACTION_REQUIRED".equals(dto.getActionStatus())) {
                pdmActionAuthorizationService.sendDangerAlert(
                        "주차로봇 ROBOT_%02d".formatted(dto.getRobotNo()),
                        dto.getSensorValues()
                );
            }
        }

        if ("위험".equals(dto.getRiskLevel())) {
            RobotPdmDTO active = robotPdmMapper.findActiveAction(
                    dto.getRobotNo()
            );
            if (active != null) {
                return active;
            }
        }
        return dto;
    }

    // 활성 위험을 완료 처리하고 FastAPI가 다음 CSV 행으로 진행하게 한다.
    public RobotPdmDTO completeAction(
            long pdmNo,
            String actionNote,
            String loginId
    ) {
        RobotPdmDTO target = robotPdmMapper.detail(pdmNo);
        validateActionTarget(target);

        int memberNo = pdmActionAuthorizationService
                .requireMemberNo(loginId);
        String normalizedNote = pdmActionAuthorizationService
                .normalizeActionNote(actionNote);

        predictiveMaintenanceClient.completeRobotAction(target.getRobotNo());

        if (robotPdmMapper.completeAction(
                pdmNo,
                memberNo,
                normalizedNote
        ) != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 완료되었거나 조치할 수 없는 예측 결과입니다."
            );
        }

        RobotPdmDTO completed = robotPdmMapper.detail(pdmNo);
        latestPredictions.put(completed.getRobotNo(), completed);
        return completed;
    }

    private void validateActionTarget(RobotPdmDTO target) {
        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "로봇 예지보전 결과가 없습니다."
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

    // 매시 정각에 호출되어 로봇별 최신 정상 결과를 한 건씩 저장한다.
    public void saveHourlyNormalPredictions() {
        latestPredictions.values()
                .stream()
                .filter(dto -> "정상".equals(dto.getRiskLevel()))
                .sorted(Comparator.comparing(RobotPdmDTO::getRobotNo))
                .forEach(robotPdmMapper::savePrediction);
    }

    // ROBOT_01 같은 CSV 장비 코드에서 DB의 robot_no를 추출한다.
    private int extractRobotNo(String equipmentNo) {
        if (equipmentNo == null || equipmentNo.isBlank()) {
            throw new IllegalArgumentException("로봇 장비 코드가 없습니다.");
        }

        String numberText = equipmentNo.replaceAll("\\D", "");

        if (numberText.isBlank()) {
            throw new IllegalArgumentException(
                    "로봇 번호를 찾을 수 없습니다: " + equipmentNo
            );
        }

        int robotNo = Integer.parseInt(numberText);

        if (robotNo < 1 || robotNo > ROBOT_COUNT) {
            throw new IllegalArgumentException(
                    "지원하지 않는 로봇 번호입니다: " + robotNo
            );
        }

        return robotNo;
    }

    // FastAPI 응답에서 요청한 위험 등급의 확률을 가져온다.
    private BigDecimal getProbability(
            PredictiveMaintenanceResponseDTO response,
            String riskLevel
    ) {
        if (response.getProbabilities() == null) {
            return null;
        }

        return toBigDecimal(response.getProbabilities().get(riskLevel));
    }

    // JSON의 실수 값을 DB NUMERIC 컬럼에 맞는 BigDecimal로 변환한다.
    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    // CSV의 센서 수집 시각 문자열을 한국 시간 OffsetDateTime으로 변환한다.
    private OffsetDateTime parseSensorCollectedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(value.replace(" ", "T"))
                .atZone(KOREA_ZONE)
                .toOffsetDateTime();
    }

    // FastAPI 모델 예측 시각을 한국 시간 OffsetDateTime으로 변환한다.
    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }

        return value.atZone(KOREA_ZONE).toOffsetDateTime();
    }

    // 로봇 1~8의 현재 최신 예측 결과를 로봇 번호순으로 반환한다.
    public List<RobotPdmDTO> getLatestPredictions() {
        return latestPredictions.values()
                .stream()
                .sorted(Comparator.comparing(RobotPdmDTO::getRobotNo))
                .toList();
    }

    // 특정 로봇의 최근 실시간 분석 결과를 최신순으로 반환한다.
    public List<RobotPdmDTO> getRecentPredictions(int robotNo) {
        if (robotNo < 1 || robotNo > ROBOT_COUNT) {
            throw new IllegalArgumentException(
                    "지원하지 않는 로봇 번호입니다: " + robotNo
            );
        }

        Deque<RobotPdmDTO> history = recentPredictions.get(robotNo);
        return history == null ? List.of() : List.copyOf(history);
    }
}
