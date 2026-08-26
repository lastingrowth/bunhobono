package api.carlog_p;

import api.billing_p.BillingService;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarLogService {

    private static final String B1 = "B1";
    private static final String B2 = "B2";

    @Resource
    private CarLogMapper carLogMapper;

    @Resource
    private BillingService billingService;

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

        boolean emergencyVisit =
                isEmergencyVisit(cameraData);

        ParkingSpaceDTO entrySpace = null;

        // B1 일반 입주민 차량만 입차대기면을 배정한다.
        if (
                B1.equalsIgnoreCase(
                        gate.getGateArea()
                )
                        && !emergencyVisit
        ) {
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

        // B2 비입주민 차량의 미결제 정산서 생성
        if (
                B2.equalsIgnoreCase(
                        gate.getGateArea()
                )
                        && (
                        "VISIT".equals(
                                log.getSnapshotCarKind()
                        )
                                || "UNKNOWN".equals(
                                log.getSnapshotCarKind()
                        )
                )
        ) {
            billingService.createEntryBill(
                    log.getCarLogNo(),
                    log.getInTime()
            );
        }

        // B2 또는 긴급차량은 로봇 작업을 생성하지 않는다.
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

        boolean emergencyLog =
                isEmergencyLog(log);

        ParkingSpaceDTO currentSpace = null;

        // 일반 B1 차량만 출차대기면과 연결 게이트를 확인한다.
        if (
                B1.equalsIgnoreCase(
                        gate.getGateArea()
                )
                        && !emergencyLog
        ) {
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
        boolean residentVisit =
                visitVehicle
                        && cameraData.getApartmentUnitNo() != null;

        // 입주민 방문차량은 등록할 때 선택한 이용시간만큼 무료,
        // 관리실 긴급차량은 B2 입차 시점부터 72시간 무료,
        // 관리실 일반 방문차량과 그 외 차량은 무료시간이 없다.
        // 관리실 또는 시스템이 등록한 긴급차량인지 확인한다.
        boolean emergencyVisit =
                isEmergencyVisit(cameraData);
        if (residentVisit) {
            log.setFreeTime(
                    calculateResidentVisitFreeMinutes(cameraData)
            );
        } else if (emergencyVisit) {
            log.setFreeTime(4320);
        } else {
            log.setFreeTime(0);
        }

        log.setSnapshotCarNo(
                resolvedCarNo(cameraData)
        );

        if (visitVehicle) {
            log.setSnapshotCarKind("VISIT");
        } else {
            log.setSnapshotCarKind("REGISTERED");
        }

        return log;
    }

    // 방문 시작·종료시각의 차이를 해당 입차 기록의 무료시간으로 사용한다.
    private int calculateResidentVisitFreeMinutes(
            CameraDataDTO cameraData
    ) {
        if (cameraData.getStartDate() == null
                || cameraData.getEndDate() == null
                || !cameraData.getEndDate().isAfter(
                cameraData.getStartDate()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "방문차량 등록시간을 확인할 수 없습니다."
            );
        }

        long freeMinutes = Duration.between(
                cameraData.getStartDate(),
                cameraData.getEndDate()
        ).toMinutes();

        if (freeMinutes < 60
                || freeMinutes > 24 * 60
                || freeMinutes % 60 != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "방문차량 등록시간은 1시간 단위로 최대 24시간이어야 합니다."
            );
        }

        return Math.toIntExact(freeMinutes);
    }

    // 72시간 긴급·작업 방문차량인지 확인한다.
    private boolean isEmergencyVisit(
            CameraDataDTO cameraData
    ) {
        return cameraData != null
                && "visit".equalsIgnoreCase(
                cameraData.getVehicleType()
        )
                && cameraData.getApartmentUnitNo() == null
                && cameraData.getStartDate() != null
                && cameraData.getEndDate() != null
                && cameraData.getEndDate().equals(
                cameraData.getStartDate().plusHours(72)
        )
                && isWithinVisitPeriod(cameraData);
    }

    // 입차 기록이 긴급·작업 차량인지 확인한다.
    private boolean isEmergencyLog(
            CarLogDTO log
    ) {
        return log != null
                && "VISIT".equalsIgnoreCase(
                log.getSnapshotCarKind()
        )
                && Integer.valueOf(4320).equals(
                log.getFreeTime()
        );
    }

    // 주차장별 차량 입차 자격을 확인한다.
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

        // 긴급차량은 B1과 B2 게이트를 통과할 수 있다.
        if (isEmergencyVisit(cameraData)) {
            return B1.equalsIgnoreCase(gateArea)
                    || B2.equalsIgnoreCase(gateArea);
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

            // 방문차량은 등록기간 안에 B2로 입차할 수 있다.
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

    // OCR 촬영시각을 입출차 처리시각으로 변환한다.
    private LocalDateTime captureTime(
            CameraDataDTO cameraData
    ) {
        return cameraData.getCaptureTime() == null
                ? LocalDateTime.now()
                : cameraData.getCaptureTime()
                .toLocalDateTime();
    }

    // 보정된 차량번호를 우선 반환한다.
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

    // 처리 가능한 차량번호가 있는지 확인한다.
    private boolean hasCarNo(
            CameraDataDTO cameraData
    ) {
        return cameraData != null
                && resolvedCarNo(cameraData) != null
                && !resolvedCarNo(cameraData).isBlank();
    }
}
