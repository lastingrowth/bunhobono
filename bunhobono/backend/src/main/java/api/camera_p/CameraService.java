package api.camera_p;


import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 카메라 관리 업무 로직을 처리하는 Service이다.
 *
 * <p>Controller에서 전달받은 요청을 처리하고 CameraMapper를 호출하여
 * 카메라 데이터의 조회, 등록, 수정, 삭제를 수행한다.</p>
 */
@Service
public class CameraService {

    /** 카메라 데이터 처리를 담당하는 Mapper이다. */
    @Resource
    CameraMapper cameraMapper;

    /**
     * 전체 카메라 목록을 조회한다.
     *
     * @param dto 카메라 조회 조건
     * @return 카메라 목록
     */
    public List<CameraDTO> listservice(CameraDTO dto){
        return cameraMapper.list(dto);
    }

    /**
     * 신규 카메라 정보를 등록한다.
     *
     * @param dto 등록할 카메라 정보
     * @return 등록 처리 건수
     */
    public int signUp(CameraDTO dto) {
        return cameraMapper.insert(dto);
    }

    /**
     * 카메라 고유번호에 해당하는 카메라 정보를 삭제한다.
     *
     * @param cameraNo 카메라 고유번호
     * @return 삭제 처리 건수
     */
    public int delete(int cameraNo) {
        return cameraMapper.delete(cameraNo);
    }

    /**
     * 카메라 정보를 수정한다.
     *
     * @param dto 수정할 카메라 정보
     * @return 수정 처리 건수
     */
    public int update(CameraDTO dto) {
        return cameraMapper.update(dto);
    }

}






