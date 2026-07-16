package api.gate_p;

import api.cameradata_p.CameraDataDTO;
import api.cameradata_p.CameraDataMapper;
import api.carlog_p.CarLogService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GateService {

    private static final long AUTO_CLOSE_SECONDS = 5L;

    @Resource
    GateMapper gateMapper;

    @Resource
    CameraDataMapper cameraDataMapper;

    @Resource
    CarLogService carLogService;

    @Resource
    TaskScheduler taskScheduler;

    // OCR 직후 호출된다. 입차는 승인 차량만, 출차는 열린 입차 기록이 있을 때만 작동한다.
    @Transactional
    public void processAutomaticPassage(CameraDataDTO cameraData) {
        if (cameraData == null || cameraData.getCarNo() == null
                || cameraData.getCarNo().isBlank()) {
            return;
        }

        GateDTO gate = gateMapper.findByCameraNo(cameraData.getCameraNo());
        if (gate == null) {
            return;
        }

        if ("In".equalsIgnoreCase(gate.getGateType())) {
            if (!canAutoEnter(cameraData) || carLogService.isAlreadyParking(cameraData)) {
                return;
            }
        } else if ("Out".equalsIgnoreCase(gate.getGateType())) {
            if (!carLogService.isAlreadyParking(cameraData)) {
                return;
            }
        } else {
            return;
        }

        openAndRecord(cameraData, gate);
    }

    // 미등록/대기 차량은 관리자가 촬영 건을 지정한 경우에만 입차시킨다.
    @Transactional
    public void manualOpen(int gateNo, int cameraDataNo) {
        GateDTO requestedGate = requireInGate(gateNo);
        CameraDataDTO cameraData = cameraDataMapper.detail(cameraDataNo);

        if (cameraData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "카메라 데이터를 찾을 수 없습니다.");
        }

        GateDTO cameraGate = gateMapper.findByCameraNo(cameraData.getCameraNo());
        if (cameraGate == null || cameraGate.getGateNo() != requestedGate.getGateNo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "해당 차량이 감지된 입차 게이트와 요청한 게이트가 다릅니다.");
        }

        if (carLogService.isAlreadyParking(cameraData)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 입차 중인 차량입니다.");
        }

        openAndRecord(cameraData, requestedGate);
    }

    public void manualClose(int gateNo) {
        requireInGate(gateNo);
        closeGate(gateNo);
    }

    private boolean canAutoEnter(CameraDataDTO data) {
        if (data.getVehicleCarNo() == null
                || !"APPROVED".equalsIgnoreCase(data.getVehicleStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return (data.getStartDate() == null || !now.isBefore(data.getStartDate()))
                && (data.getEndDate() == null || !now.isAfter(data.getEndDate()));
    }

    private void openAndRecord(CameraDataDTO cameraData, GateDTO gate) {
        if (gateMapper.open(gate.getGateNo()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 열려 있거나 존재하지 않는 게이트입니다.");
        }

        int changed = carLogService.processCameraData(cameraData, gate);
        if (changed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "입출차 기록을 처리할 수 없습니다.");
        }

        taskScheduler.schedule(
                () -> closeGate(gate.getGateNo()),
                Instant.now().plusSeconds(AUTO_CLOSE_SECONDS)
        );
    }

    private GateDTO requireInGate(int gateNo) {
        GateDTO gate = gateMapper.findByGateNo(gateNo);
        if (gate == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "게이트를 찾을 수 없습니다.");
        }
        if (!"In".equalsIgnoreCase(gate.getGateType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "입차 게이트만 수동으로 조작할 수 있습니다.");
        }
        return gate;
    }

    private void closeGate(int gateNo) {
        if (gateMapper.close(gateNo) != 1) {
            System.err.println(gateNo + "번 게이트 닫기 실패");
        }
    }

    // 게이트 목록
    public List<GateDTO> listservice(GateDTO dto) {
        return gateMapper.list(dto);
    }

    // 게이트 등록
    public int signUp(GateDTO dto) {
        return gateMapper.insert(dto);
    }

//    // 게이트 상세
//    public GateDTO getGateDetail(int gateNo) {
//        return gateMapper.detail(gateNo);
//    }

    // 게이트 삭제
    public int delete(int gateNo) {
        return gateMapper.delete(gateNo);
    }

    // 게이트 정보 수정
    public int updateGate(GateDTO dto) {
        return gateMapper.update(dto);
    }
}
