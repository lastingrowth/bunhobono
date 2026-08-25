package api.carlog_p;

import api.bill_p.BillService;
import api.cameradata_p.CameraDataDTO;
import api.gate_p.GateDTO;
import api.gate_p.GateService;
import api.kiosk_p.KioskDTO;
import api.kiosk_p.KioskService;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class CarLogService {

    private static final String B1 = "B1";
    private static final String B2 = "B2";

    @Resource
    private CarLogMapper carLogMapper;

    @Resource
    private BillService billService;

    @Resource
    private GateService gateService;

    @Resource
    private KioskService kioskService;

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

    // 차량번호 뒤 4자리와 키오스크 위치로 현재 주차 중인 입주민 차량 조회
    public List<CarLogDTO> findParkingCars(String lastFourDigits, Integer kioskNo) {
        String digits = lastFourDigits == null ? "" : lastFourDigits.trim();

        if (digits.isBlank() || kioskNo == null || kioskNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        KioskDTO kiosk = kioskService.findByKioskNo(kioskNo);

        if (kiosk == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        CarLogDTO dto = new CarLogDTO();
        dto.setLastFourDigits(digits);
        dto.setParkingState("PARKING");
        dto.setCarKind("REGISTERED");

        List<CarLogDTO> list = carLogMapper.list(dto);

        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<CarLogDTO> sameParkingList = new ArrayList<>();

        for (CarLogDTO carLog : list) {
            if (carLog.getParkingNo() != null && carLog.getParkingNo().equals(kiosk.getParkingNo())) {
                sameParkingList.add(carLog);
            }
        }

        if (sameParkingList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return sameParkingList;
    }

    // B1·B2 게이트 입출차 처리
    @Transactional
    public int processCameraData(CameraDataDTO cameraData) {
        if (!hasCarNo(cameraData)) {
            return 0;
        }

        GateDTO gate = gateService.findByCameraNo(cameraData.getCameraNo());

        if (gate == null || gate.getParkingNo() == null) {
            return 0;
        }

        if ("In".equalsIgnoreCase(gate.getGateType())) {
            return enterParking(cameraData, gate);
        }

        if ("Out".equalsIgnoreCase(gate.getGateType())) {
            return exitParking(cameraData, gate);
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
    private int enterParking(CameraDataDTO cameraData, GateDTO gate) {
        if (!canEnter(cameraData, gate.getGateArea()) || carLogMapper.findOpenLog(cameraData) != null) {
            return 0;
        }

        if (B2.equalsIgnoreCase(gate.getGateArea()) && !carLogMapper.hasAvailableCapacity(gate.getParkingNo())) {
            return 0;
        }

        ParkingSpaceDTO entrySpace = null;

        // B1 일반 입주민 차량만 입차대기면을 배정한다.
        if (B1.equalsIgnoreCase(gate.getGateArea()) && !isEmergencyVisit(cameraData)) {
            entrySpace = parkingSpaceService.findEmptyWaitingSpace(gate.getGateNo(), "ENTRY_WAIT");

            if (entrySpace == null) {
                return 0;
            }
        }

        CarLogDTO log = createEntryLog(cameraData, gate.getGateNo());

        if (carLogMapper.insertEntry(log) != 1) {
            return 0;
        }

        // B2 비입주민 차량의 미결제 정산서를 생성한다.
        if (B2.equalsIgnoreCase(gate.getGateArea())
                && ("VISIT".equalsIgnoreCase(log.getSnapshotCarKind())
                || "UNKNOWN".equalsIgnoreCase(log.getSnapshotCarKind()))) {
            billService.createEntryBill(log.getCarLogNo(), log.getSnapshotCarNo(), log.getInTime());
        }

        // B2 또는 긴급차량은 로봇 작업을 생성하지 않는다.
        if (entrySpace == null) {
            return 1;
        }

        int assigned = parkingSpaceService.assignCarLog(entrySpace.getSpaceNo(), log.getCarLogNo());

        if (assigned != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (robotTaskService.createParkInTask(log.getCarLogNo()) == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return 1;
    }

    // 주차장 출차 처리
    private int exitParking(CameraDataDTO cameraData, GateDTO gate) {
        CarLogDTO log = carLogMapper.findOpenLog(cameraData);

        if (log == null
                || log.getParkingNo() == null
                || !log.getParkingNo().equals(gate.getParkingNo())) {
            return 0;
        }

        LocalDateTime outTime = captureTime(cameraData);

        boolean isPaymentRequired = B2.equalsIgnoreCase(gate.getGateArea())
                && ("VISIT".equalsIgnoreCase(log.getSnapshotCarKind())
                || "UNKNOWN".equalsIgnoreCase(log.getSnapshotCarKind()));

        // B2 비입주민 차량은 결제 완료 후 출차 유예시간 안에서만 출차할 수 있다.
        if (isPaymentRequired && !billService.isExitAllowed(log.getCarLogNo(), outTime)) {
            return 0;
        }

        ParkingSpaceDTO currentSpace = null;

        // 일반 B1 차량만 출차대기면과 연결 게이트를 확인한다.
        if (B1.equalsIgnoreCase(gate.getGateArea()) && !isEmergencyLog(log)) {
            currentSpace = parkingSpaceService.findByCarLogNo(log.getCarLogNo());

            if (currentSpace == null
                    || !"EXIT_WAIT".equals(currentSpace.getSpaceType())
                    || currentSpace.getGateNo() == null
                    || !currentSpace.getGateNo().equals(gate.getGateNo())) {
                return 0;
            }
        }

        int exited = carLogMapper.exitParking(log.getCarLogNo(), cameraData, gate.getGateNo());

        if (exited != 1) {
            return 0;
        }

        if (currentSpace != null) {
            int released = parkingSpaceService.releaseCarLog(currentSpace.getSpaceNo(), log.getCarLogNo());

            if (released != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
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

        // 관리실 또는 시스템이 등록한 긴급차량인지 확인한다.
        boolean emergencyVisit =
                isEmergencyVisit(cameraData);

        // 입주민 방문차량은 24시간, 긴급차량은 72시간 무료
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

        if (visitVehicle) {
            log.setSnapshotCarKind("VISIT");
        } else {
            log.setSnapshotCarKind("REGISTERED");
        }

        return log;
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