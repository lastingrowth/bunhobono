package api.gate_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class GateService {

    // 게이트 상태
    private static final int GATE_CLOSED = 0;
    private static final int GATE_OPEN = 1;

    // 게이트를 닫기 전 유지 시간
    private static final long CLOSE_DELAY_SECONDS = 5L;

    @Resource
    GateMapper gateMapper;

    @Resource
    TaskScheduler taskScheduler;

    // 카메라 번호로 연결된 게이트 찾기
    public GateDTO findByCameraNo(int cameraNo) {
        return gateMapper.findByCameraNo(cameraNo);
    }

    // 게이트 상태 직접 변경
    public int updateStatus(GateDTO dto) {
        if (dto.getGateStatus() != GATE_CLOSED
                && dto.getGateStatus() != GATE_OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "게이트 상태는 0 또는 1이어야 합니다."
            );
        }
        return gateMapper.updateStatus(dto);
    }

    // 게이트 열기: gate_status = 1
    public void open(int gateNo) {
        GateDTO dto = new GateDTO();
        dto.setGateNo(gateNo);

        int updated = gateMapper.open(dto);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 열려 있거나 존재하지 않는 게이트입니다."
            );
        }
    }

    // CarLog 처리 후 호출: 5초 뒤 게이트 닫기
    public void scheduleClose(int gateNo) {
        taskScheduler.schedule(
                () -> close(gateNo),
                Instant.now().plusSeconds(
                        CLOSE_DELAY_SECONDS
                )
        );
    }

    // 게이트 닫기: gate_status = 0
    private void close(int gateNo) {
        GateDTO dto = new GateDTO();
        dto.setGateNo(gateNo);
        dto.setGateStatus(GATE_CLOSED);

        int updated = gateMapper.updateStatus(dto);

        if (updated != 1) {
            System.err.println(
                    gateNo + "번 게이트 닫기 실패"
            );
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
