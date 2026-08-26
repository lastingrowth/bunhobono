package api.gate_p;

import api.carlog_p.CarLogDTO;
import api.carlog_p.CarLogMapper;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class GateService {

    @Resource
    GateMapper gateMapper;

    @Resource
    CarLogMapper carLogMapper;

    @Resource
    TaskScheduler taskScheduler;

    // 게이트 닫힘 상태
    private static final int GATE_CLOSED = 0;

    // 게이트 열림 상태
    private static final int GATE_OPEN = 1;

    // 게이트가 열린 뒤 자동으로 닫히기까지 기다릴 시간
    private static final long CLOSE_DELAY_SECONDS = 5L;

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

    // 게이트 상태 변경
    // 관리자 화면에서 수동으로 열기/닫기 버튼을 눌렀을 때 사용
    public int updateStatus(GateDTO dto) {
        return gateMapper.updateStatus(dto);
    }

    // 게이트 열기
    // OCR 자동 처리에서 등록 차량이 확인되었을 때 사용
    public int open(int gateNo) {
        GateDTO dto = new GateDTO();
        dto.setGateNo(gateNo);
        dto.setGateStatus(GATE_OPEN);

        return gateMapper.updateStatus(dto);
    }

    // 게이트 닫기
    // 수동 닫기 또는 자동 닫기 처리에서 사용
    public int close(int gateNo) {
        GateDTO dto = new GateDTO();
        dto.setGateNo(gateNo);
        dto.setGateStatus(GATE_CLOSED);

        return gateMapper.updateStatus(dto);
    }

    // 게이트 자동 닫기 예약
    // 게이트를 연 뒤 일정 시간이 지나면 자동으로 닫히도록 예약
    public void scheduleClose(int gateNo) {
        taskScheduler.schedule(
                () -> close(gateNo),
                Instant.now().plusSeconds(CLOSE_DELAY_SECONDS)
        );
    }

    // 입출차 기록과 키오스크 위치에 맞는 활성 출차 게이트 번호를 조회한다.
    public int findExitGateNo(int carLogNo, Integer kioskNo) {
        if (carLogNo <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        CarLogDTO carLog = carLogMapper.detail(carLogNo);

        if (carLog == null || carLog.getParkingNo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        GateDTO dto = new GateDTO();
        dto.setParkingNo(carLog.getParkingNo());
        dto.setGateType("Out");

        if (Integer.valueOf(1).equals(kioskNo)) {
            dto.setGateCode("B1-OUT-1");
        } else if (Integer.valueOf(2).equals(kioskNo)) {
            dto.setGateCode("B1-OUT-2");
        }

        List<GateDTO> list = gateMapper.list(dto);

        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return list.get(0).getGateNo();
    }

    // 카메라 번호로 연결된 게이트 조회
    // OCR 자동 처리에서 cameraNo를 기준으로 게이트 정보를 확인할 때 사용
    public GateDTO findByCameraNo(int cameraNo) {
        return gateMapper.findByCameraNo(cameraNo);
    }
}
