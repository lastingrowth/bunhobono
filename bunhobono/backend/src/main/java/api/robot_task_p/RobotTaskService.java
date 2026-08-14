package api.robot_task_p;

import api.parking_space_p.ParkingSpaceDTO;
import api.parking_space_p.ParkingSpaceService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RobotTaskService {

    // 논리 주차면 두 칸을 1초에 이동하는 가상 로봇 속도
    private static final double LOGICAL_SPEED_PER_SECOND = 2.0;
    private static final int MIN_MOVE_DURATION_MS = 3000;
    private static final int MAX_MOVE_DURATION_MS = 15000;
    private static final int POSITIONING_DURATION_MS = 3000;
    private static final int LIFTING_DURATION_MS = 5000;

    @Resource
    private RobotTaskMapper robotTaskMapper;

    @Resource
    private ParkingSpaceService parkingSpaceService;

    // 전체 로봇 작업 조회
    public List<RobotTaskDTO> list() {
        List<RobotTaskDTO> tasks = robotTaskMapper.list();

        tasks.forEach(this::setPhaseDuration);

        return tasks;
    }

    // 로봇 작업 상세 조회
    public RobotTaskDTO detail(long taskNo) {
        RobotTaskDTO task = robotTaskMapper.detail(taskNo);

        if (task != null) {
            setPhaseDuration(task);
        }

        return task;
    }

    // 현재 단계의 이동거리 또는 고정 작업시간을 계산
    public int phaseDurationMs(RobotTaskDTO task) {
        if (task.getTaskPhase() == null) {
            return 0;
        }

        return switch (task.getTaskPhase()) {
            case "MOVING_TO_PICKUP" -> movementDuration(
                    homePoint(task.getSetNo()),
                    spacePoint(task.getPickupSpaceCode())
            );
            case "MOVING_TO_DROPOFF" -> movementDuration(
                    spacePoint(task.getPickupSpaceCode()),
                    spacePoint(task.getDropoffSpaceCode())
            );
            case "RETURNING_HOME" -> movementDuration(
                    spacePoint(task.getDropoffSpaceCode()),
                    homePoint(task.getSetNo())
            );
            case "PICKUP_POSITIONING", "DROPOFF_POSITIONING" ->
                    POSITIONING_DURATION_MS;
            case "LIFTING", "LOWERING" ->
                    LIFTING_DURATION_MS;
            default -> 0;
        };
    }

    private void setPhaseDuration(RobotTaskDTO task) {
        task.setPhaseDurationMs(
                phaseDurationMs(task)
        );
    }

    // 맨해튼 거리와 가상 로봇 속도로 이동시간 계산
    private int movementDuration(
            int[] start,
            int[] destination
    ) {
        int distance = Math.abs(start[0] - destination[0])
                + Math.abs(start[1] - destination[1]);

        int duration = (int) Math.ceil(
                distance / LOGICAL_SPEED_PER_SECOND * 1000
        );

        return Math.max(
                MIN_MOVE_DURATION_MS,
                Math.min(duration, MAX_MOVE_DURATION_MS)
        );
    }

    // 로봇 세트의 논리 대기 위치
    private int[] homePoint(Integer setNo) {
        return switch (setNo == null ? 1 : setNo) {
            case 2 -> new int[]{15, 0};
            case 3 -> new int[]{4, 6};
            case 4 -> new int[]{15, 6};
            default -> new int[]{4, 0};
        };
    }

    // 공간 코드를 20열 5행 논리 좌표로 변환
    private int[] spacePoint(String spaceCode) {
        if (spaceCode == null) {
            return new int[]{10, 3};
        }

        if (spaceCode.contains("-IN1-")) return new int[]{-1, 0};
        if (spaceCode.contains("-IN2-")) return new int[]{20, 0};
        if (spaceCode.contains("-OUT1-")) return new int[]{-1, 6};
        if (spaceCode.contains("-OUT2-")) return new int[]{20, 6};

        int parkingNumber = Integer.parseInt(
                spaceCode.substring(
                        spaceCode.lastIndexOf('P') + 1
                )
        );

        return new int[]{
                (parkingNumber - 1) % 20,
                (parkingNumber - 1) / 20 + 1
        };
    }

    // 입차대기면에서 주차면으로 이동하는 작업 생성
    @Transactional
    public RobotTaskDTO createParkInTask(int carLogNo) {

        RobotTaskDTO existing =
                robotTaskMapper.findActiveTask(
                        carLogNo,
                        "PARK_IN"
                );

        if (existing != null) {
            return existing;
        }

        ParkingSpaceDTO pickupSpace =
                parkingSpaceService.findByCarLogNo(
                        carLogNo
                );

        if (
                pickupSpace == null
                        || !"ENTRY_WAIT".equals(
                        pickupSpace.getSpaceType()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        ParkingSpaceDTO dropoffSpace =
                parkingSpaceService.findEmptyParkingSpace(
                        pickupSpace.getParkingNo(),
                        pickupSpace.getGateNo()
                );

        if (dropoffSpace == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        return createTask(
                carLogNo,
                pickupSpace.getSpaceNo(),
                dropoffSpace.getSpaceNo(),
                "PARK_IN",
                0
        );
    }

    // 주차면에서 출차대기면으로 이동하는 작업 생성
    @Transactional
    public RobotTaskDTO createParkOutTask(
            int carLogNo,
            int exitGateNo
    ) {
        RobotTaskDTO existing =
                robotTaskMapper.findActiveTask(
                        carLogNo,
                        "PARK_OUT"
                );

        if (existing != null) {
            return existing;
        }

        ParkingSpaceDTO pickupSpace =
                parkingSpaceService.findByCarLogNo(
                        carLogNo
                );

        if (
                pickupSpace == null
                        || !"PARKING".equals(
                        pickupSpace.getSpaceType()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        ParkingSpaceDTO dropoffSpace =
                parkingSpaceService.findEmptyWaitingSpace(
                        exitGateNo,
                        "EXIT_WAIT"
                );

        if (
                dropoffSpace == null
                        || !pickupSpace.getParkingNo().equals(
                        dropoffSpace.getParkingNo()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        return createTask(
                carLogNo,
                pickupSpace.getSpaceNo(),
                dropoffSpace.getSpaceNo(),
                "PARK_OUT",
                10
        );
    }

    // 출차대기 차량을 주차면으로 다시 이동하는 작업 생성
    @Transactional
    public RobotTaskDTO createReparkTask(int carLogNo) {
        return createRepark(carLogNo);
    }

    // 10분 동안 출차하지 않은 차량의 재입차 작업 자동 생성
    @Transactional
    public int createTimedOutReparkTasks() {
        int created = 0;

        for (int carLogNo
                : parkingSpaceService.findTimedOutExitWaitCarLogNos()) {
            try {
                createRepark(carLogNo);
                created++;
            } catch (ResponseStatusException ignored) {
                // 빈 주차면이 없거나 이미 다른 작업이 시작된 차량은 다음 주기에 재확인한다.
            }
        }

        return created;
    }

    private RobotTaskDTO createRepark(int carLogNo) {
        RobotTaskDTO existing = robotTaskMapper.findActiveTask(
                carLogNo,
                "PARK_IN"
        );

        if (existing != null) {
            return existing;
        }

        ParkingSpaceDTO pickupSpace =
                parkingSpaceService.findByCarLogNo(carLogNo);

        if (pickupSpace == null
                || !"EXIT_WAIT".equals(pickupSpace.getSpaceType())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "출차대기면에 있는 차량이 아닙니다."
            );
        }

        ParkingSpaceDTO dropoffSpace =
                parkingSpaceService.findEmptyParkingSpace(
                        pickupSpace.getParkingNo(),
                        pickupSpace.getGateNo()
                );

        if (dropoffSpace == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "차량을 다시 주차할 빈 주차면이 없습니다."
            );
        }

        return createTask(
                carLogNo,
                pickupSpace.getSpaceNo(),
                dropoffSpace.getSpaceNo(),
                "PARK_IN",
                0
        );
    }

    // 로봇 작업 등록
    private RobotTaskDTO createTask(
            int carLogNo,
            long pickupSpaceNo,
            long dropoffSpaceNo,
            String taskType,
            int priority
    ) {
        RobotTaskDTO task = new RobotTaskDTO();

        task.setCarLogNo(carLogNo);
        task.setPickupSpaceNo(pickupSpaceNo);
        task.setDropoffSpaceNo(dropoffSpaceNo);
        task.setTaskType(taskType);
        task.setPriority(priority);

        robotTaskMapper.insert(task);

        return robotTaskMapper.detail(
                task.getTaskNo()
        );
    }
}
