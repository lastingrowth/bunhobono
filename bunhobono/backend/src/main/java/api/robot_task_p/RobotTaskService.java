package api.robot_task_p;

import api.parking_space_p.ParkingSpaceDTO;
import api.parking_space_p.ParkingSpaceService;
import api.robot_p.RobotService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RobotTaskService {

    @Resource
    private RobotTaskMapper robotTaskMapper;

    @Resource
    private ParkingSpaceService parkingSpaceService;

    @Resource
    private RobotService robotService;

    // 전체 로봇 작업 조회
    public List<RobotTaskDTO> list() {
        return robotTaskMapper.list();
    }

    // 로봇 작업 상세 조회
    public RobotTaskDTO detail(long taskNo) {
        return robotTaskMapper.detail(taskNo);
    }

    // 로봇 세트에 배정된 대기 작업 조회
    public RobotTaskDTO findAssignedTask(int setNo) {
        return robotTaskMapper.findAssignedTask(setNo);
    }

    // 입차대기면에서 일반 주차면으로 이동하는 작업 생성
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
                        pickupSpace.getParkingNo()
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

    // 일반 주차면에서 출차대기면으로 이동하는 작업 생성
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

    // 대기 작업에 사용 가능한 로봇 세트 배정
    @Transactional
    public int dispatchWaitingTasks() {
        int dispatchCount = 0;

        while (true) {
            RobotTaskDTO task =
                    robotTaskMapper.findNextWaitingTask();

            Integer setNo =
                    robotService.findAvailableSetNo();

            if (task == null || setNo == null) {
                break;
            }

            int robotUpdated =
                    robotService.startSet(setNo);

            if (robotUpdated != 2) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }

            int taskUpdated =
                    robotTaskMapper.assignSet(
                            task.getTaskNo(),
                            setNo
                    );

            if (taskUpdated != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }

            dispatchCount++;
        }

        return dispatchCount;
    }

    // 가상 로봇 작업 시작
    @Transactional
    public int start(long taskNo) {
        return robotTaskMapper.start(taskNo);
    }

    // 가상 로봇 작업 단계 갱신
    @Transactional
    public int updatePhase(
            long taskNo,
            String taskPhase
    ) {
        return robotTaskMapper.updatePhase(
                taskNo,
                taskPhase
        );
    }

    // 작업 완료 후 차량 위치 이동
    @Transactional
    public int complete(long taskNo) {

        RobotTaskDTO task =
                robotTaskMapper.findByTaskNoForUpdate(
                        taskNo
                );

        if (
                task == null
                        || !"RUNNING".equals(
                        task.getTaskStatus()
                )
        ) {
            return 0;
        }

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
                robotTaskMapper.complete(taskNo);

        if (completed != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        if (task.getSetNo() != null) {
            robotService.finishSet(
                    task.getSetNo()
            );
        }

        return completed;
    }

    // 작업 실패 처리
    @Transactional
    public int fail(
            long taskNo,
            String failureReason
    ) {
        RobotTaskDTO task =
                robotTaskMapper.findByTaskNoForUpdate(
                        taskNo
                );

        if (task == null) {
            return 0;
        }

        int failed =
                robotTaskMapper.fail(
                        taskNo,
                        failureReason
                );

        if (
                failed == 1
                        && task.getSetNo() != null
        ) {
            robotService.finishSet(
                    task.getSetNo()
            );
        }

        return failed;
    }

    // 로봇 작업 공통 등록
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