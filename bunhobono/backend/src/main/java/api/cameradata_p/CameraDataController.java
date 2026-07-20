package api.cameradata_p;

import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/camera-data")
public class CameraDataController {

    @Resource
    CameraDataService cameraDataService;

    @Resource
    private TrashService trashService;

    @GetMapping("")
    public List<CameraDataDTO> list(CameraDataDTO dto){
        List<CameraDataDTO> list = cameraDataService.listservice(dto);
        System.out.println("카메라정보 확인: " + list);
        System.out.println(cameraDataService);
        return list;
    }

    // 카메라 장치가 호출하는 API  이거 하드웨어도 post로 api처리한다고 함
    @PostMapping("/ocr")
    public CameraDataDTO ocr(@RequestParam("cameraNo") int cameraNo,
                           @RequestParam("carNo") String carNo,
                           @RequestParam("confidenceScore") Double confidenceScore,
                           @RequestParam("file") MultipartFile file,
                           @RequestParam(value = "cropFile", required = false) MultipartFile cropFile) {

        return cameraDataService.ocr(cameraNo, carNo, confidenceScore, file, cropFile);
    }

    @GetMapping("/{cameraDataNo}/detail")
    public CameraDataDTO getCameraData(@PathVariable int cameraDataNo) {
        return cameraDataService.getCameraData(cameraDataNo);
    }

    @PatchMapping("/{cameraDataNo}/edit")
    public CameraDataDTO editCarNo(
            @PathVariable int cameraDataNo,
            @RequestBody CameraDataDTO dto) {
        return cameraDataService.editCarNo(cameraDataNo, dto);
    }

    // 관리자 수동 게이트 열기
    // 자동 통과되지 않은 camera_data를 관리자가 확인한 뒤 게이트를 열 때 호출
    @PostMapping("/{cameraDataNo}/open-gate")
    public int openGate(@PathVariable int cameraDataNo) {
        return cameraDataService.openGateByCameraData(cameraDataNo);
    }

    // 차량번호 검색 API
    @GetMapping("/search")
    public List<CameraDataDTO> search(@RequestParam String carNo) {
        return cameraDataService.searchByCarNo(carNo);
    }

    @DeleteMapping("/{cameraDataNo}/delete")
    public int delete(@PathVariable int cameraDataNo) {
        trashService.moveCameraData(cameraDataNo, "MANUAL");
        return 1;
    }

    @GetMapping("/{cameraDataNo}/image")
    public ResponseEntity<org.springframework.core.io.Resource> getImage(
            @PathVariable int cameraDataNo) throws IOException {

        Path path =
                cameraDataService.getCameraImagePath(cameraDataNo);

        org.springframework.core.io.Resource image =
                new UrlResource(path.toUri());

        String contentType = Files.probeContentType(path);

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }

    @GetMapping("/{cameraDataNo}/crop-image")
    public ResponseEntity<org.springframework.core.io.Resource> getCropImage(
            @PathVariable int cameraDataNo) throws IOException {

        Path path = cameraDataService.getCameraCropImagePath(cameraDataNo);

        org.springframework.core.io.Resource image =
                new UrlResource(path.toUri());

        String contentType = Files.probeContentType(path);

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }

}
