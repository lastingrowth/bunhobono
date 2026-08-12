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

    @Resource
    private RobotTaskMapper robotTaskMapper;

    @Resource
    private ParkingSpaceService parkingSpaceService;

    // 전체 로봇 작업 조회
    public List<RobotTaskDTO> list() {
        return robotTaskMapper.list();
    }

    // 로봇 작업 상세 조회
    public RobotTaskDTO detail(long taskNo) {
        return robotTaskMapper.detail(taskNo);
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