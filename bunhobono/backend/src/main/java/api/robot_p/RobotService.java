package api.robot_p;

import api.parking_space_p.ParkingSpaceService;
import api.robot_log_p.RobotLogDTO;
import api.robot_log_p.RobotLogService;
import api.robot_task_p.RobotTaskDTO;
import api.robot_task_p.RobotTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RobotService {

    // 동시에 이동 가능한 로봇 세트 수
    private static final int MAX_MOVING_SET_COUNT = 2;

    // 충전 단계마다 증가할 배터리 잔량
    private static final BigDecimal CHARGE_AMOUNT =
            BigDecimal.valueOf(5);

    // 충전 완료 기준
    private static final BigDecimal FULL_BATTERY_LEVEL =
            BigDecimal.valueOf(100);

    @Resource
    private RobotMapper robotMapper;

    @Resource
    private RobotTaskMapper robotTaskMapper;

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
    public Integer findAvailableSetNo() {
        return robotMapper.findAvailableSetNo();
    }

    // 로봇 세트 작업 시작
    public int startSet(int setNo) {
        return robotMapper.updateSetStatus(
                setNo,
                "STANDBY",
                "WORKING"
        );
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

            RobotDTO currentState = new RobotDTO();

            currentState.setBatteryLevel(nextLevel);
            currentState.setRobotStatus(
                    nextLevel.compareTo(
                            FULL_BATTERY_LEVEL
                    ) >= 0
                            ? "STANDBY"
                            : "CHARGING"
            );

            updatedCount += updateState(
                    robot.getRobotNo(),
                    currentState
            );
        }

        return updatedCount;
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

            Integer setNo = findAvailableSetNo();

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
                    countMovingTasks()
                            < MAX_MOVING_SET_COUNT
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

    // 실행 중인 로봇 작업을 다음 단계로 진행
    @Transactional
    public int processRunningTasks() {
        List<RobotTaskDTO> tasks =
                robotTaskMapper.findRunningTasks();

        int movingCount = countMovingTasks();
        int processCount = 0;

        for (RobotTaskDTO task : tasks) {
            String phase = task.getTaskPhase();

            if (phase == null) {
                continue;
            }

            switch (phase) {
                case "TRAFFIC_WAIT_EMPTY" -> {
                    if (movingCount
                            < MAX_MOVING_SET_COUNT) {
                        moveToPhase(
                                task,
                                "MOVING_TO_PICKUP"
                        );
                        movingCount++;
                    } else {
                        saveSetLogs(task, phase);
                    }

                    processCount++;
                }

                case "MOVING_TO_PICKUP" -> {
                    moveToPhase(
                            task,
                            "PICKUP_POSITIONING"
                    );

                    movingCount =
                            Math.max(0, movingCount - 1);

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
                    if (movingCount
                            < MAX_MOVING_SET_COUNT) {
                        moveToPhase(
                                task,
                                "MOVING_TO_DROPOFF"
                        );
                        movingCount++;
                    } else {
                        moveToPhase(
                                task,
                                "TRAFFIC_WAIT_LOADED"
                        );
                    }

                    processCount++;
                }

                case "TRAFFIC_WAIT_LOADED" -> {
                    if (movingCount
                            < MAX_MOVING_SET_COUNT) {
                        moveToPhase(
                                task,
                                "MOVING_TO_DROPOFF"
                        );
                        movingCount++;
                    } else {
                        saveSetLogs(task, phase);
                    }

                    processCount++;
                }

                case "MOVING_TO_DROPOFF" -> {
                    moveToPhase(
                            task,
                            "DROPOFF_POSITIONING"
                    );

                    movingCount =
                            Math.max(0, movingCount - 1);

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

    // 현재 이동 중인 로봇 작업 수
    private int countMovingTasks() {
        return (int) robotTaskMapper
                .findRunningTasks()
                .stream()
                .filter(task ->
                        "MOVING_TO_PICKUP".equals(
                                task.getTaskPhase()
                        )
                                || "MOVING_TO_DROPOFF".equals(
                                task.getTaskPhase()
                        )
                )
                .count();
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

        task.setTaskPhase(nextPhase);

        saveSetLogs(task, nextPhase);
    }

    // 작업 완료와 차량 위치 이동
    private void completeTask(RobotTaskDTO task) {
        int moved =
                parkingSpaceService.moveCarLog(
                        task.getCarLogNo(),
                        task.getPickupSpaceNo(),
                        task.getDropoffSpaceNo()
                );

        if (moved != 2) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

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

        log.setRobotStatus(
                "COMPLETED".equals(taskPhase)
                        ? "CHARGING"
                        : "WORKING"
        );

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

        log.setBatteryLevel(
                getNextBatteryLevel(robot)
        );

        log.setObstacleDetected(false);
        log.setSafetyStop(false);
        log.setAlarmCode(null);
        log.setSampledAt(OffsetDateTime.now());

        return log;
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
