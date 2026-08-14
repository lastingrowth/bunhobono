package api.kiosk_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KioskService {

    @Resource
    KioskMapper kioskMapper;

    // 키오스크 전체 목록 조회
    public List<KioskDTO> list() {
        return kioskMapper.list();
    }

    // 키오스크 번호로 키오스크가 설치된 주차장 정보를 조회
    public KioskDTO findByKioskNo(int kioskNo) {
        return kioskMapper.findByKioskNo(kioskNo);
    }

    // 키오스크 삭제
    public int delete(int kioskNo) {
        return kioskMapper.delete(kioskNo);
    }

    // 키오스크 등록
    public int signUp(KioskDTO kioskDTO) {
        return kioskMapper.signUp(kioskDTO);
    }

}
