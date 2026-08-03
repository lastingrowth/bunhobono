package api.camera_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 카메라 관리 API 요청을 처리하는 Controller이다.
 *
 * <p>클라이언트의 카메라 조회, 등록, 수정, 삭제 요청을 전달받아
 * CameraService를 호출하고 처리 결과를 반환한다.</p>
 */
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    /** 카메라 업무 처리를 담당하는 Service이다. */
    @Resource
    CameraService cameraService;

    /**
     * 전체 카메라 목록을 조회한다.
     *
     * @param dto 카메라 조회 조건
     * @return 카메라 목록
     */
    @GetMapping("")
    public List<CameraDTO> list(CameraDTO dto){
        List<CameraDTO> list = cameraService.listservice(dto);
        System.out.println("카메라정보 확인: " + list);
        System.out.println(cameraService);
        return list;
    }

    /**
     * 신규 카메라 정보를 등록한다.
     *
     * @param dto 등록할 카메라 정보
     * @return 등록 처리 건수
     */
    @PostMapping("/signUp")
    public int signUp(@RequestBody CameraDTO dto) {
        return cameraService.signUp(dto);
    }

    /**
     * 카메라 고유번호에 해당하는 카메라 정보를 삭제한다.
     *
     * @param cameraNo 카메라 고유번호
     * @return 삭제 처리 건수
     */
    @DeleteMapping("/{cameraNo}/delete")
    public int deleteCamera(@PathVariable int cameraNo) {
        return cameraService.delete(cameraNo);
    }

    /**
     * 카메라 고유번호에 해당하는 카메라 정보를 수정한다.
     *
     * @param cameraNo 카메라 고유번호
     * @param dto 수정할 카메라 정보
     * @return 수정 처리 건수
     */
    @PutMapping("/{cameraNo}/edit")
    public int updateCamera(@PathVariable int cameraNo,
                            @RequestBody CameraDTO dto) {
        dto.setCameraNo(cameraNo);
        return cameraService.update(dto);
    }
}
