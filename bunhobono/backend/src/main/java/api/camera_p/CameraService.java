package api.camera_p;


import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class CameraService {

    @Resource
    CameraMapper cameraMapper;

    public List<CameraDTO> listservice(CameraDTO dto){
        return cameraMapper.list(dto);
    }

    public int signUp(CameraDTO dto) {
        return cameraMapper.insert(dto);
    }

    public CameraDTO getCamera(int cameraNo) {
        return cameraMapper.detail(cameraNo);
    }


}






