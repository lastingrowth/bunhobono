package api.camera_p;

import api.parking_p.ParkingDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    @Resource
    CameraService cameraService;

    @GetMapping("")
    public List<CameraDTO> list(CameraDTO dto){
        List<CameraDTO> list = cameraService.listservice(dto);
        System.out.println("카메라정보 확인: " + list);
        System.out.println(cameraService);
        return list;
    }

    @PostMapping("/signUp")
    public int signUp(@RequestBody CameraDTO dto) {
        return cameraService.signUp(dto);
    }

    @DeleteMapping("/{cameraNo}/delete")
    public int deleteCamera(@PathVariable int cameraNo) {
        return cameraService.delete(cameraNo);
    }

    @PutMapping("/{cameraNo}/edit")
    public int updateCamera(@RequestBody CameraDTO dto) {
        return cameraService.update(dto);
    }
}
