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

        boolean visitVehicle =
                "visit".equalsIgnoreCase(
                        cameraData.getVehicleType()
                );

        // 입주민이 등록한 방문차량인지 확인한다.
        // 입주민 차량은 member를 통해 실제 세대와 연결되어 있다.
        boolean residentVisit =
                visitVehicle && cameraData.getApartmentUnitNo() != null;

        // 관리실에 등록된 72시간 긴급차량인지 확인한다.
        // 관리실 차량은 apartment_unit_no가 없으며,
        // 긴급차량 등록기간은 정확히 72시간으로 설정된다.
        boolean emergencyVisit =
                visitVehicle
                        && cameraData.getApartmentUnitNo() == null
                        && cameraData.getStartDate() != null
                        && cameraData.getEndDate() != null
                        && cameraData.getEndDate().equals(
                        cameraData.getStartDate().plusHours(72)
                );

        // 입주민 방문차량은 B2 입차 시점부터 24시간 무료,
        // 관리실 긴급차량은 B2 입차 시점부터 72시간 무료,
        // 관리실 일반 방문차량과 그 외 차량은 무료시간이 없다.
        if (residentVisit) {
            log.setFreeTime(1440);
        } else if (emergencyVisit) {
            log.setFreeTime(4320);
        } else {
            log.setFreeTime(0);
        }

        log.setSnapshotCarNo(
                resolvedCarNo(cameraData)
        );

        // 입주민 방문차량과 관리실 일반·긴급차량은
        // 모두 vehicle_car에 방문차량으로 등록된다.
        if (visitVehicle) {
            log.setSnapshotCarKind("VISIT");
        }

        // 나머지는 입주민 등록차량이다.
        else {
            log.setSnapshotCarKind("REGISTERED");
        }

        return log;
    }

    // 주차장별 차량 입차 자격을 확인한다.
    // 정문·후문에서 승인된 일반·긴급차량도 vehicle_car에 등록되므로
    // 등록번호와 승인상태가 확인된 차량만 내부 주차장에 입차할 수 있다.
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

        // B1은 입주민 등록차량만 입차할 수 있다.
        if (B1.equalsIgnoreCase(gateArea)) {
            return "normal".equalsIgnoreCase(
                    cameraData.getVehicleType()
            );
        }

        if (B2.equalsIgnoreCase(gateArea)) {

            // 입주민 등록차량은 B2에도 입차할 수 있다.
            if (
                    "normal".equalsIgnoreCase(
                            cameraData.getVehicleType()
                    )
            ) {
                return true;
            }

            // 일반 방문차량과 긴급·작업 차량은
            // 등록된 방문기간 안에 B2로 입차할 수 있다.
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
