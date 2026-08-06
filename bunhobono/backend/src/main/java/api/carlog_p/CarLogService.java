package api.carlog_p;

import api.cameradata_p.CameraDataDTO;
import api.parking_space_p.ParkingSpaceDTO;
import api.parking_space_p.ParkingSpaceService;
import api.robot_task_p.RobotTaskService;
import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarLogService {

    private static final String B1 = "B1";
    private static final String B2 = "B2";

    @Resource
    private CarLogMapper carLogMapper;

    @Resource
    private ParkingSpaceService parkingSpaceService;

    @Resource
    private RobotTaskService robotTaskService;

    @Resource
    private TrashService trashService;

    // 차량 입출차 목록 조회
    public List<CarLogDTO> list(CarLogDTO dto) {
        return carLogMapper.list(dto);
    }

    // 차량 입출차 상세 조회
    public CarLogDTO detail(int carLogNo) {
        return carLogMapper.detail(carLogNo);
    }

    // 현재 주차 중인 차량 조회
    public CarLogDTO findCurrentlyParked(
            CameraDataDTO cameraData
    ) {
        if (!hasCarNo(cameraData)) {
            return null;
        }

        return carLogMapper.findOpenLog(cameraData);
    }

    // 현재 주차 여부 확인
    public boolean isCurrentlyParked(
            CameraDataDTO cameraData
    ) {
        return findCurrentlyParked(cameraData) != null;
    }

    // B1·B2 게이트 입출차 처리
    @Transactional
    public int processCameraData(
            CameraDataDTO cameraData
    ) {
        if (!hasCarNo(cameraData)) {
            return 0;
        }

        CarLogDTO gate =
                carLogMapper.findGateByCameraNo(
                        cameraData.getCameraNo()
                );

        if (
                gate == null
                        || gate.getParkingNo() == null
        ) {
            return 0;
        }

        if ("In".equalsIgnoreCase(gate.getGateType())) {
            return enterParking(
                    cameraData,
                    gate
            );
        }

        if ("Out".equalsIgnoreCase(gate.getGateType())) {
            return exitParking(
                    cameraData,
                    gate
            );
        }

        return 0;
    }

    // OCR 관리자 수정사항 반영
    public int correctByCameraData(
            CameraDataDTO cameraData
    ) {
        return carLogMapper.correctByCameraData(
                cameraData
        );
    }

    // 오래된 입출차 기록 휴지통 이동
    public void moveOldCarLogsToTrash() {
        List<Integer> carLogNos =
                carLogMapper.findOldCarLogNosForTrash();

        int moveCount = 0;

        for (Integer carLogNo : carLogNos) {
            try {
                trashService.moveCarLog(
                        carLogNo,
                        "SCHEDULED"
                );
                moveCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println(
                "휴지통으로 이동된 입출차 기록 수: "
                        + moveCount
        );
    }

    // 주차장 입차 처리
    private int enterParking(
            CameraDataDTO cameraData,
            CarLogDTO gate
    ) {
        if (
                !canEnter(
                        cameraData,
                        gate.getGateArea()
                )
                        || carLogMapper.findOpenLog(
                        cameraData
                ) != null
        ) {
            return 0;
        }

        if (
                B2.equalsIgnoreCase(
                        gate.getGateArea()
                )
                        && !carLogMapper.hasAvailableCapacity(
                        gate.getParkingNo()
                )
        ) {
            return 0;
        }

        ParkingSpaceDTO entrySpace = null;

        if (B1.equalsIgnoreCase(gate.getGateArea())) {
            entrySpace =
                    parkingSpaceService
                            .findEmptyWaitingSpace(
                                    gate.getGateNo(),
                                    "ENTRY_WAIT"
                            );

            if (entrySpace == null) {
                return 0;
            }
        }

        CarLogDTO log =
                createEntryLog(
                        cameraData,
                        gate.getGateNo()
                );

        if (carLogMapper.insertEntry(log) != 1) {
            return 0;
        }

        if (entrySpace == null) {
            return 1;
        }

        int assigned =
                parkingSpaceService.assignCarLog(
                        entrySpace.getSpaceNo(),
                        log.getCarLogNo()
                );

        if (assigned != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        if (
                robotTaskService.createParkInTask(
                        log.getCarLogNo()
                ) == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT
            );
        }

        return 1;
    }

    // 주차장 출차 처리
    private int exitParking(
            CameraDataDTO cameraData,
            CarLogDTO gate
    ) {
        CarLogDTO log =
                carLogMapper.findOpenLog(
                        cameraData
                );

        if (
                log == null
                        || log.getParkingNo() == null
                        || !log.getParkingNo().equals(
                        gate.getParkingNo()
                )
        ) {
            return 0;
        }

        ParkingSpaceDTO currentSpace = null;

        if (B1.equalsIgnoreCase(gate.getGateArea())) {
            currentSpace =
                    parkingSpaceService.findByCarLogNo(
                            log.getCarLogNo()
                    );

            if (
                    currentSpace == null
                            || !"EXIT_WAIT".equals(
                            currentSpace.getSpaceType()
                    )
                            || currentSpace.getGateNo() == null
                            || !currentSpace.getGateNo().equals(
                            gate.getGateNo()
                    )
            ) {
                return 0;
            }
        }

        int exited =
                carLogMapper.exitParking(
                        log.getCarLogNo(),
                        cameraData,
                        gate.getGateNo()
                );

        if (exited != 1) {
            return 0;
        }

        if (currentSpace != null) {
            int released =
                    parkingSpaceService.releaseCarLog(
                            currentSpace.getSpaceNo(),
                            log.getCarLogNo()
                    );

            if (released != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT
                );
            }
        }

        return exited;
    }

    // 입차 기록 생성값 구성
    private CarLogDTO createEntryLog(
            CameraDataDTO cameraData,
            int gateNo
    ) {
        CarLogDTO log = new CarLogDTO();

        log.setVehicleCarNo(
                cameraData.getVehicleCarNo()
        );
        log.setCameraDataNo(
                cameraData.getCameraDataNo()
        );
        log.setInGateNo(gateNo);
        log.setInTime(
                captureTime(cameraData)
        );
        log.setFreeTime(0);

        log.setSnapshotCarNo(
                resolvedCarNo(cameraData)
        );
        log.setSnapshotCarKind(
                "visit".equalsIgnoreCase(
                        cameraData.getVehicleType()
                )
                        ? "VISIT"
                        : "REGISTERED"
        );

        return log;
    }

    // 주차장별 차량 입차 자격 확인
    private boolean canEnter(
            CameraDataDTO cameraData,
            String gateArea
    ) {
        if (
                cameraData.getVehicleCarNo() == null
                        || !"APPROVED".equalsIgnoreCase(
                        cameraData.getVehicleStatus()
                )
        ) {
            return false;
        }

        if (B1.equalsIgnoreCase(gateArea)) {
            return "normal".equalsIgnoreCase(
                    cameraData.getVehicleType()
            );
        }

        if (B2.equalsIgnoreCase(gateArea)) {
            return "visit".equalsIgnoreCase(
                    cameraData.getVehicleType()
            ) && isWithinVisitPeriod(cameraData);
        }

        return false;
    }

    // 방문차량 등록기간 확인
    private boolean isWithinVisitPeriod(
            CameraDataDTO cameraData
    ) {
        if (
                cameraData.getStartDate() == null
                        || cameraData.getEndDate() == null
        ) {
            return false;
        }

        LocalDateTime now =
                captureTime(cameraData);

        return !now.isBefore(
                cameraData.getStartDate()
        ) && !now.isAfter(
                cameraData.getEndDate()
        );
    }

    private LocalDateTime captureTime(
            CameraDataDTO cameraData
    ) {
        return cameraData.getCaptureTime() == null
                ? LocalDateTime.now()
                : cameraData.getCaptureTime()
                .toLocalDateTime();
    }

    private String resolvedCarNo(
            CameraDataDTO cameraData
    ) {
        if (
                cameraData.getCarNo() != null
                        && !cameraData.getCarNo().isBlank()
        ) {
            return cameraData.getCarNo();
        }

        return cameraData.getOcrCarNo();
    }

    private boolean hasCarNo(
            CameraDataDTO cameraData
    ) {
        return cameraData != null
                && resolvedCarNo(cameraData) != null
                && !resolvedCarNo(cameraData).isBlank();
    }
}