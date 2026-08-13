package api.robot_p;

import api.parking_space_p.ParkingSpaceService;
import api.robot_log_p.RobotLogDTO;
import api.robot_log_p.RobotLogService;
import api.robot_task_p.RobotTaskDTO;
import api.robot_task_p.RobotTaskMapper;
import api.robot_task_p.RobotTaskService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RobotService {

    // 동시에 이동 가능한 로봇 세트 수
    private static final int MAX_MOVING_SET_COUNT = 2;

    // 1초마다 증가할 배터리 잔량
    private static final BigDecimal CHARGE_AMOUNT =
            BigDecimal.valueOf(0.5);

    // 충전 시작 기준
    private static final BigDecimal CHARGE_START_LEVEL =
            BigDecimal.valueOf(70);

    // 작업 제외 기준
    private static final BigDecimal LOW_BATTERY_LEVEL =
            BigDecimal.valueOf(30);

    // 작업 복귀 기준
    private static final BigDecimal RECOVERY_BATTERY_LEVEL =
            BigDecimal.valueOf(50);

    // 충전 완료 기준
    private static final BigDecimal FULL_BATTERY_LEVEL =
            BigDecimal.valueOf(100);

    @Resource
    private RobotMapper robotMapper;

    @Resource
    private RobotTaskMapper robotTaskMapper;

    @Resource
    private RobotTaskService robotTaskService;

    @Resource
    private ParkingSpaceService parkingSpaceService;

    @Resource
    private RobotLogService robotLogService;

    // 전체 로봇 조회
    public List<RobotDTO> list() {
        List<RobotDTO> robots =
                robotMapper.list();

        robots.forEach(
                this::setDaysSinceMaintenance
        );

        return robots;
    }

    // 로봇 상세 조회
    public RobotDTO detail(long robotNo) {
        RobotDTO robot =
                robotMapper.detail(robotNo);

        if (robot != null) {
            setDaysSinceMaintenance(robot);
        }

        return robot;
    }

    // 같은 세트의 로봇 조회
    public List<RobotDTO> findBySetNo(int setNo) {
        return robotMapper.findBySetNo(setNo);
    }

    // 작업 가능한 로봇 세트 조회
    public Integer findAvailableSetNo(int preferredSetNo) {
        return robotMapper.findAvailableSetNo(
                preferredSetNo
        );
    }

    // 로봇 세트 작업 시작
    public int startSet(int setNo) {
        return robotMapper.startSet(setNo);
    }

    // 로봇 세트 작업 종료 후 충전 시작
    public int finishSet(int setNo) {
        return robotMapper.updateSetStatus(
                setNo,
                "WORKING",
                "CHARGING"
        );
    }

    // 충전 중인 로봇의 배터리 잔량 갱신
    public int chargeIdleRobots() {
        List<RobotDTO> robots =
                robotMapper.findChargingRobots();

        int updatedCount = 0;

        for (RobotDTO robot : robots) {
            BigDecimal currentLevel =
                    robot.getBatteryLevel() == null
                            ? BigDecimal.ZERO
                            : robot.getBatteryLevel();

            BigDecimal nextLevel =
                    currentLevel
                            .add(CHARGE_AMOUNT)
                            .min(FULL_BATTERY_LEVEL);

            boolean recoveringLowBattery =
                    "LOW_BATTERY".equals(
                            robot.getRobotStatus()
                    )
                            || currentLevel.compareTo(
                            LOW_BATTERY_LEVEL
                    ) < 0;

            RobotDTO currentState = new RobotDTO();

            currentState.setRobotNo(robot.getRobotNo());
            currentState.setBatteryLevel(nextLevel);
            currentState.setRobotStatus(chargeStatus(
                    nextLevel,
                    recoveringLowBattery
            ));

            updatedCount += robotMapper.updateChargingState(
                    currentState
            );
        }

        return updatedCount;
    }

    // 충전 잔량과 저전력 복구 여부에 따른 상태 결정
    private String chargeStatus(
            BigDecimal batteryLevel,
            boolean recoveringLowBattery
    ) {
        if (recoveringLowBattery
                && batteryLevel.compareTo(
                RECOVERY_BATTERY_LEVEL
        ) < 0) {
            return "LOW_BATTERY";
        }

        return batteryLevel.compareTo(
                FULL_BATTERY_LEVEL
        ) >= 0
                ? "STANDBY"
                : "CHARGING";
    }

    // 로봇 현재 상태 갱신
    public int updateState(
            long robotNo,
            RobotDTO dto
    ) {
        dto.setRobotNo(robotNo);
        return robotMapper.updateState(dto);
    }

    // 관리자 로봇 점검 완료
    public int completeMaintenance(long robotNo) {
        return robotMapper.completeMaintenance(robotNo);
    }

    // 대기 작업에 사용 가능한 로봇 세트 배정
    @Transactional
    public int dispatchWaitingTasks() {
        int dispatchCount = 0;

        while (true) {
            RobotTaskDTO task =
                    robotTaskMapper.findNextWaitingTask();

            if (task == null) {
                break;
            }

            RobotTaskDTO taskDetail =
                    robotTaskMapper.detail(
                            task.getTaskNo()
                    );

            Integer setNo = findAvailableSetNo(
                    preferredSetNo(taskDetail)
            );

            if (setNo == null) {
                break;
            }

            int robotUpdated = startSet(setNo);

            if (robotUpdated != 2) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }

            int assigned = robotTaskMapper.assignSet(
                    task.getTaskNo(),
                    setNo
            );

            if (assigned != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }

            String firstPhase =
                    canStartMoving(
                            task,
                            "MOVING_TO_PICKUP"
                    )
                            ? "MOVING_TO_PICKUP"
                            : "TRAFFIC_WAIT_EMPTY";

            int started = robotTaskMapper.updateRunning(
                    task.getTaskNo(),
                    firstPhase
            );

            if (started != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }

            task.setSetNo(setNo);
            task.setTaskPhase(firstPhase);
            task.setTaskStatus("RUNNING");

            saveSetLogs(task, firstPhase);

            dispatchCount++;
        }

        return dispatchCount;
    }

    // 작업 출입구와 가장 가까운 로봇 세트를 정한다.
    private int preferredSetNo(RobotTaskDTO task) {
        String spaceCode =
                "PARK_IN".equals(task.getTaskType())
                        ? task.getPickupSpaceCode()
                        : task.getDropoffSpaceCode();

        if (spaceCode == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        if (spaceCode.contains("-IN1-")) {
            return 1;
        }

        if (spaceCode.contains("-IN2-")) {
            return 2;
        }

        if (spaceCode.contains("-OUT1-")) {
            return 3;
        }

        if (spaceCode.contains("-OUT2-")) {
            return 4;
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT
        );
    }

    // 실행 중인 로봇 작업을 다음 단계로 진행
    @Transactional
    public int processRunningTasks() {
        List<RobotTaskDTO> tasks =
                robotTaskMapper.findRunningTasks();

        int processCount = 0;

        for (RobotTaskDTO task : tasks) {
            String phase = task.getTaskPhase();

            if (phase == null) {
                continue;
            }

            if (!phaseDurationCompleted(task)) {
                continue;
            }

            switch (phase) {
                case "TRAFFIC_WAIT_EMPTY" -> {
                    if (canStartMoving(
                            task,
                            "MOVING_TO_PICKUP"
                    )) {
                        moveToPhase(
                                task,
                                "MOVING_TO_PICKUP"
                        );
                    }

                    processCount++;
                }

                case "MOVING_TO_PICKUP" -> {
                    moveToPhase(
                            task,
                            "PICKUP_POSITIONING"
                    );

                    processCount++;
                }

                case "PICKUP_POSITIONING" -> {
                    moveToPhase(
                            task,
                            "LIFTING"
                    );

                    processCount++;
                }

                case "LIFTING" -> {
                    if (canStartMoving(
                            task,
                            "MOVING_TO_DROPOFF"
                    )) {
                        moveToPhase(
                                task,
                                "MOVING_TO_DROPOFF"
                        );
                    } else {
                        moveToPhase(
                                task,
                                "TRAFFIC_WAIT_LOADED"
                        );
                    }

                    processCount++;
                }

                case "TRAFFIC_WAIT_LOADED" -> {
                    if (canStartMoving(
                            task,
                            "MOVING_TO_DROPOFF"
                    )) {
                        moveToPhase(
                                task,
                                "MOVING_TO_DROPOFF"
                        );
                    }

                    processCount++;
                }

                case "MOVING_TO_DROPOFF" -> {
                    moveToPhase(
                            task,
                            "DROPOFF_POSITIONING"
                    );

                    processCount++;
                }

                case "DROPOFF_POSITIONING" -> {
                    moveToPhase(
                            task,
                            "LOWERING"
                    );

                    processCount++;
                }

                case "LOWERING" -> {
                    assignDropoff(task);

                    moveToPhase(
                            task,
                            canStartMoving(
                                    task,
                                    "RETURNING_HOME"
                            )
                                    ? "RETURNING_HOME"
                                    : "TRAFFIC_WAIT_RETURN"
                    );

                    processCount++;
                }

                case "TRAFFIC_WAIT_RETURN" -> {
                    if (canStartMoving(
                            task,
                            "RETURNING_HOME"
                    )) {
                        moveToPhase(
                                task,
                                "RETURNING_HOME"
                        );
                    }

                    processCount++;
                }

                case "RETURNING_HOME" -> {
                    completeTask(task);
                    processCount++;
                }

                default -> {
                    // COMPLETED, FAILED 등은 처리하지 않음
                }
            }
        }

        return processCount;
    }

    // 현재 단계의 예정 소요시간이 지났는지 확인
    private boolean phaseDurationCompleted(
            RobotTaskDTO task
    ) {
        int durationMs =
                robotTaskService.phaseDurationMs(task);

        if (durationMs == 0) {
            return true;
        }

        OffsetDateTime phaseStartedAt =
                task.getPhaseUpdatedAt() != null
                        ? task.getPhaseUpdatedAt()
                        : task.getStartedAt();

        return phaseStartedAt == null
                || !OffsetDateTime.now().isBefore(
                        phaseStartedAt.plusNanos(
                                durationMs * 1_000_000L
                        )
                );
    }

    // 이동 수와 같은 통로를 사용하는 작업을 함께 확인한다.
    private boolean canStartMoving(
            RobotTaskDTO task,
            String nextPhase
    ) {
        List<RobotTaskDTO> runningTasks =
                robotTaskMapper.findRunningTasks();

        long movingCount = runningTasks
                .stream()
                .filter(this::isMovingTask)
                .count();

        if (movingCount >= MAX_MOVING_SET_COUNT) {
            return false;
        }

        Set<String> targetLanes = movementLanes(
                task,
                nextPhase
        );

        return runningTasks
                .stream()
                .filter(this::isMovingTask)
                .filter(running ->
                        !running.getTaskNo().equals(
                                task.getTaskNo()
                        )
                )
                .noneMatch(running ->
                        intersects(
                                targetLanes,
                                movementLanes(
                                        running,
                                        running.getTaskPhase()
                                )
                        )
                );
    }

    // 실제 이동 단계 여부
    private boolean isMovingTask(RobotTaskDTO task) {
        return "MOVING_TO_PICKUP".equals(
                task.getTaskPhase()
        )
                || "MOVING_TO_DROPOFF".equals(
                task.getTaskPhase()
        )
                || "RETURNING_HOME".equals(
                task.getTaskPhase()
        );
    }

    // 이동 출발지와 목적지가 사용하는 가로 통로를 구한다.
    private Set<String> movementLanes(
            RobotTaskDTO task,
            String phase
    ) {
        RobotTaskDTO detail = task;

        if (task.getPickupSpaceCode() == null
                || task.getDropoffSpaceCode() == null) {
            detail = robotTaskMapper.detail(
                    task.getTaskNo()
            );
        }

        Set<String> lanes = new HashSet<>();

        if ("MOVING_TO_PICKUP".equals(phase)) {
            lanes.add(homeLane(detail.getSetNo()));
            lanes.add(spaceLane(
                    detail.getPickupSpaceCode()
            ));
        } else if ("MOVING_TO_DROPOFF".equals(phase)) {
            lanes.add(spaceLane(
                    detail.getPickupSpaceCode()
            ));
            lanes.add(spaceLane(
                    detail.getDropoffSpaceCode()
            ));
        } else {
            lanes.add(spaceLane(
                    detail.getDropoffSpaceCode()
            ));
            lanes.add(homeLane(detail.getSetNo()));
        }

        if (lanes.contains("TOP")
                && lanes.contains("BOTTOM")) {
            lanes.add("MIDDLE");
        }

        return lanes;
    }

    // 같은 통로를 하나라도 공유하는지 확인한다.
    private boolean intersects(
            Set<String> first,
            Set<String> second
    ) {
        return first.stream().anyMatch(second::contains);
    }

    // 로봇 세트 대기 위치가 연결된 통로
    private String homeLane(Integer setNo) {
        return setNo != null && setNo <= 2
                ? "TOP"
                : "BOTTOM";
    }

    // 공간 코드가 연결된 통로
    private String spaceLane(String spaceCode) {

        if (spaceCode.contains("-IN")) {
            return "TOP";
        }

        if (spaceCode.contains("-OUT")) {
            return "BOTTOM";
        }

        int parkingNumber = Integer.parseInt(
                spaceCode.substring(
                        spaceCode.lastIndexOf('P') + 1
                )
        );

        if (parkingNumber <= 20) {
            return "TOP";
        }

        if (parkingNumber <= 60) {
            return "MIDDLE";
        }

        return "BOTTOM";
    }

    // 작업 단계 변경과 원시 상태값 저장
    private void moveToPhase(
            RobotTaskDTO task,
            String nextPhase
    ) {
        int updated =
                robotTaskMapper.updateRunning(
                        task.getTaskNo(),
                        nextPhase
                );

        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        // 차량을 들어 올리고 출발하면 기존 공간을 비운다.
        if ("MOVING_TO_DROPOFF".equals(nextPhase)) {
            int released =
                    parkingSpaceService.releaseCarLog(
                            task.getPickupSpaceNo(),
                            task.getCarLogNo()
                    );

            if (released != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }
        }

        task.setTaskPhase(nextPhase);

        saveSetLogs(task, nextPhase);
    }

    // 차량을 도착 공간에 내려놓는다.
    private void assignDropoff(RobotTaskDTO task) {
        int assigned =
                parkingSpaceService.assignCarLog(
                        task.getDropoffSpaceNo(),
                        task.getCarLogNo()
                );

        if (assigned != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }
    }

    // 복귀가 끝난 작업과 로봇 상태를 완료한다.
    private void completeTask(RobotTaskDTO task) {
        int completed =
                robotTaskMapper.complete(
                        task.getTaskNo()
                );

        if (completed != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        int finished = finishSet(task.getSetNo());

        if (finished != 2) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        task.setTaskPhase("COMPLETED");
        task.setTaskStatus("COMPLETED");

        saveSetLogs(task, "COMPLETED");
    }

    // 세트에 포함된 두 로봇의 원시 상태값 저장
    private void saveSetLogs(
            RobotTaskDTO task,
            String taskPhase
    ) {
        List<RobotDTO> robots =
                robotMapper.findBySetNo(task.getSetNo());

        for (RobotDTO robot : robots) {
            RobotLogDTO log = createRobotLog(
                    robot,
                    task,
                    taskPhase
            );

            int inserted = robotLogService.insert(log);

            if (inserted == 1) {
                RobotDTO currentState = new RobotDTO();

                currentState.setRobotNo(log.getRobotNo());
                currentState.setRobotStatus(
                        log.getRobotStatus()
                );
                currentState.setBatteryLevel(
                        log.getBatteryLevel()
                );

                robotMapper.updateState(currentState);
            }
        }
    }

    // 작업 단계에 맞는 로봇 원시 상태값 생성
    private RobotLogDTO createRobotLog(
            RobotDTO robot,
            RobotTaskDTO task,
            String taskPhase
    ) {
        RobotLogDTO log = new RobotLogDTO();

        log.setSourceEventId(
                UUID.randomUUID().toString()
        );

        log.setRobotNo(robot.getRobotNo());
        log.setTaskNo(task.getTaskNo());

        log.setTaskPhase(taskPhase);
        log.setPayloadState(
                getPayloadState(taskPhase)
        );

        double[] range =
                getSensorRange(taskPhase);

        log.setDriveMotorTemperatureC(
                randomValue(
                        range[0],
                        range[1],
                        2
                )
        );

        log.setDriveMotorCurrentA(
                randomValue(
                        range[2],
                        range[3],
                        3
                )
        );

        log.setDriveVibrationMmS(
                randomValue(
                        range[4],
                        range[5],
                        3
                )
        );

        log.setBatteryVoltageV(
                randomValue(
                        46.5,
                        49.0,
                        3
                )
        );

        log.setBatteryTemperatureC(
                randomValue(
                        30.0,
                        40.0,
                        2
                )
        );

        BigDecimal nextBatteryLevel =
                getNextBatteryLevel(robot);

        log.setBatteryLevel(nextBatteryLevel);
        log.setRobotStatus(
                "COMPLETED".equals(taskPhase)
                        ? idleStatus(nextBatteryLevel)
                        : "WORKING"
        );

        log.setObstacleDetected(false);
        log.setSafetyStop(false);
        log.setAlarmCode(null);
        log.setSampledAt(OffsetDateTime.now());

        return log;
    }

    // 작업 종료 후 잔량에 따른 대기·충전 상태 결정
    private String idleStatus(BigDecimal batteryLevel) {
        if (batteryLevel.compareTo(
                LOW_BATTERY_LEVEL
        ) < 0) {
            return "LOW_BATTERY";
        }

        if (batteryLevel.compareTo(
                CHARGE_START_LEVEL
        ) < 0) {
            return "CHARGING";
        }

        return "STANDBY";
    }

    // 작업 단계별 적재 상태
    private String getPayloadState(
            String taskPhase
    ) {
        return switch (taskPhase) {
            case "LIFTING",
                 "TRAFFIC_WAIT_LOADED",
                 "MOVING_TO_DROPOFF",
                 "DROPOFF_POSITIONING",
                 "LOWERING" -> "LOADED";

            default -> "EMPTY";
        };
    }

    // 작업 단계별 정상 센서값 범위
    private double[] getSensorRange(
            String taskPhase
    ) {
        return switch (taskPhase) {
            case "MOVING_TO_PICKUP" ->
                    new double[]{
                            45.0, 55.0,
                            7.0, 10.0,
                            1.0, 2.2
                    };

            case "PICKUP_POSITIONING",
                 "DROPOFF_POSITIONING" ->
                    new double[]{
                            42.0, 50.0,
                            5.8, 8.0,
                            0.6, 1.3
                    };

            case "LIFTING",
                 "LOWERING" ->
                    new double[]{
                            48.0, 58.0,
                            9.0, 12.0,
                            1.2, 2.4
                    };

            case "MOVING_TO_DROPOFF" ->
                    new double[]{
                            52.0, 63.0,
                            9.5, 13.0,
                            1.8, 3.8
                    };

            case "TRAFFIC_WAIT_EMPTY",
                 "TRAFFIC_WAIT_LOADED" ->
                    new double[]{
                            42.0, 49.0,
                            5.8, 7.5,
                            0.6, 1.2
                    };

            default ->
                    new double[]{
                            40.0, 48.0,
                            5.8, 7.0,
                            0.6, 1.0
                    };
        };
    }

    // 작업 단계마다 배터리 잔량 감소
    private BigDecimal getNextBatteryLevel(
            RobotDTO robot
    ) {
        BigDecimal current =
                robot.getBatteryLevel() == null
                        ? BigDecimal.valueOf(100)
                        : robot.getBatteryLevel();

        BigDecimal decrease =
                randomValue(
                        0.05,
                        0.25,
                        2
                );

        BigDecimal result =
                current.subtract(decrease);

        return result.signum() < 0
                ? BigDecimal.ZERO
                : result;
    }

    // 지정 범위의 소수값 생성
    private BigDecimal randomValue(
            double min,
            double max,
            int scale
    ) {
        double value =
                ThreadLocalRandom.current()
                        .nextDouble(min, max);

        return BigDecimal
                .valueOf(value)
                .setScale(
                        scale,
                        RoundingMode.HALF_UP
                );
    }

    // 최근 점검 후 경과일 계산
    private void setDaysSinceMaintenance(
            RobotDTO robot
    ) {
        if (robot.getLastMaintenanceAt() == null) {
            robot.setDaysSinceMaintenance(null);
            return;
        }

        long days = ChronoUnit.DAYS.between(
                robot.getLastMaintenanceAt(),
                OffsetDateTime.now()
        );

        robot.setDaysSinceMaintenance(
                (int) Math.max(days, 0)
        );
    }

    // 주차로봇 등록
    public int signUp(RobotDTO dto) {
        return robotMapper.insert(dto);
    }

    // 주차로봇 삭제
    public int delete(long robotNo) {
        return robotMapper.delete(robotNo);
    }
}
