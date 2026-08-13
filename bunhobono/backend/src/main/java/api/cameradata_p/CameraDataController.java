package api.cameradata_p;

import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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

    @GetMapping
    public CameraDataDTO.PageResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer parkingNo) {
        return cameraDataService.page(page, size, keyword, parkingNo);
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

    // 관리자가 저신뢰 OCR 차량번호를 확인하거나 수정한 뒤
    // 기존 승인 차량을 다시 조회하여 입출차 처리와 게이트 개방을 수행한다.
    @PostMapping("/{cameraDataNo}/confirm-gate")
    public CameraDataDTO confirmLowConfidenceGate(
            @PathVariable int cameraDataNo,
            @RequestBody CameraDataDTO dto
    ) {
        return cameraDataService.confirmLowConfidenceGate(cameraDataNo, dto);
    }

    // SITE 정문·후문에서 일반 미등록 차량을 관리실 방문차량으로 등록하고
    // 촬영 데이터와 등록차량을 연결한 뒤 게이트를 연다.
    @PostMapping("/{cameraDataNo}/open-visit-gate")
    public int openVisitGate(
            Authentication authentication,
            @PathVariable int cameraDataNo
    ) {
        return cameraDataService.openVisitGateByCameraData(
                authentication.getName(),
                cameraDataNo
        );
    }

    // SITE 정문·후문에서 미등록 긴급차량을 관리실 방문차량으로 등록하고
    // 72시간 등록기간을 설정한 뒤 게이트를 연다.
    @PostMapping("/{cameraDataNo}/open-emergency-gate")
    public int openEmergencyGate(
            Authentication authentication,
            @PathVariable int cameraDataNo
    ) {
        return cameraDataService.openEmergencyGateByCameraData(
                authentication.getName(),
                cameraDataNo
        );
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

    //메모
    @PatchMapping("/{cameraDataNo}/note")
    public CameraDataDTO updateNote(
            @PathVariable int cameraDataNo,
            @RequestBody CameraDataDTO dto) {

        return cameraDataService.updateNote(
                cameraDataNo,
                dto.getCamNote()
        );
    }

}
