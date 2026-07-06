package api.cameradata_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/camera-data")
public class CameraDataController {

    @Resource
    CameraDataService cameraDataService;

    @GetMapping("")
    public List<CameraDataDTO> list(CameraDataDTO dto){
        List<CameraDataDTO> list = cameraDataService.listservice(dto);
        System.out.println("카메라정보 확인: " + list);
        System.out.println(cameraDataService);
        return list;
    }

    // 카메라 장치가 호출하는 API  이거 하드웨어도 post로 api처리한다고 함
    @PostMapping("/signUp")
    public int signUp(@RequestBody CameraDataDTO dto) {
        return cameraDataService.signUp(dto);
    }


    @GetMapping("/{cameraDataNo}/detail")
    public CameraDataDTO getCameraData(@PathVariable int cameraDataNo) {
        return cameraDataService.getCameraData(cameraDataNo);
    }

    // 차량번호 검색 API
    @GetMapping("/search")
    public List<CameraDataDTO> search(@RequestParam String carNo) {
        return cameraDataService.searchByCarNo(carNo);
    }

}
