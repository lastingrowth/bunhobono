package apt.cameradata_p;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CameraDataService {


    @Resource
    CameraDataMapper cameraDataMapper;

    public List<CameraDataDTO> listservice(CameraDataDTO dto) {
        return cameraDataMapper.list(dto);
    }

    public int signUp(CameraDataDTO dto) {
        return cameraDataMapper.insert(dto);
    }

    public CameraDataDTO getCameraData(int cameraDataNo) {
        return cameraDataMapper.detail(cameraDataNo);
    }
}

