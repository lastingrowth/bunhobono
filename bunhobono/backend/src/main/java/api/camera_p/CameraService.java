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
    //Delete
    public int delete(int cameraNo) {
        return cameraMapper.delete(cameraNo);
    }

    public int update(CameraDTO dto) {
        return cameraMapper.update(dto);
    }

}






