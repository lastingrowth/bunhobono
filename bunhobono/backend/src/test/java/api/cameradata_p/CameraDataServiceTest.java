package api.cameradata_p;

import api.carlog_p.CarLogService;
import api.gate_p.GateDTO;
import api.gate_p.GateService;
import api.notice_p.NoticeService;
import api.trash_p.TrashService;
import api.vehicle_p.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CameraDataServiceTest {

    @Mock
    private CameraDataMapper cameraDataMapper;

    @Mock
    private CarLogService carLogService;

    @Mock
    private GateService gateService;

    @Mock
    private TrashService trashService;

    @Mock
    private NoticeService noticeService;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private CameraDataService cameraDataService;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                cameraDataService,
                "uploadDir",
                temporaryDirectory.toString()
        );
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-001 | 페이지·크기·검색어를 안전한 값으로 보정한다"
    )
    void page_clampsArgumentsAndCalculatesPages() {
        CameraDataDTO item = new CameraDataDTO();
        when(cameraDataMapper.page("1234", 10, 100, 0))
                .thenReturn(List.of(item));
        when(cameraDataMapper.countPage("1234", 10))
                .thenReturn(201L);

        CameraDataDTO.PageResponse result =
                cameraDataService.page(0, 200, " 1234 ", 10);

        assertEquals(List.of(item), result.items());
        assertEquals(201L, result.totalCount());
        assertEquals(1, result.page());
        assertEquals(100, result.size());
        assertEquals(3, result.totalPages());
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-002 | 원본 이미지가 없는 OCR 요청을 거부한다"
    )
    void ocr_rejectsMissingOriginalImage() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraDataService.ocr(
                        1,
                        "123가1234",
                        99.0,
                        null,
                        null
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(cameraDataMapper, never()).insert(any());
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-003 | OCR 이미지를 임시 폴더에 저장하고 저신뢰 검토 알림을 생성한다"
    )
    void ocr_savesImagesAndCreatesLowConfidenceNotice()
            throws Exception {
        MockMultipartFile original = new MockMultipartFile(
                "file",
                "original.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
        MockMultipartFile crop = new MockMultipartFile(
                "cropFile",
                "crop.png",
                "image/png",
                new byte[]{4, 5, 6}
        );

        when(cameraDataMapper.findVehicleCarNo("123가1234"))
                .thenReturn(null);
        when(cameraDataMapper.findApprovedVehicleByAlias("123가1234"))
                .thenReturn(null);
        doAnswer(invocation -> {
            CameraDataDTO inserted = invocation.getArgument(0);
            inserted.setCameraDataNo(100);
            return 1;
        }).when(cameraDataMapper).insert(any(CameraDataDTO.class));
        when(gateService.findByCameraNo(1)).thenReturn(null);

        CameraDataDTO result = cameraDataService.ocr(
                1,
                " 123가 1234 ",
                90.0,
                original,
                crop
        );

        assertEquals("123가1234", result.getCarNo());
        assertEquals("123가1234", result.getOcrCarNo());
        assertFalse(result.getRecognitionState());
        assertTrue(result.getSaved());
        assertFalse(result.getRegistered());
        assertFalse(result.getGateOpened());
        assertNull(result.getGateNo());
        assertTrue(Files.isRegularFile(Path.of(result.getImagePath())));
        assertTrue(Files.isRegularFile(Path.of(result.getCropImagePath())));
        verify(noticeService).createOcrReviewNotice(100);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-004 | 카메라 데이터 상세 조회를 Mapper에 위임한다"
    )
    void getCameraData_returnsMapperResult() {
        CameraDataDTO expected = new CameraDataDTO();
        when(cameraDataMapper.detail(1)).thenReturn(expected);

        CameraDataDTO result = cameraDataService.getCameraData(1);

        assertSame(expected, result);
        verify(cameraDataMapper).detail(1);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-005 | 차량번호를 정규화해 수정하고 입출차 기록에도 반영한다"
    )
    void editCarNo_normalizesAndUpdatesRelatedLog() {
        CameraDataDTO current = new CameraDataDTO();
        current.setCarNo("12가3456");

        CameraDataDTO updated = new CameraDataDTO();
        updated.setCameraDataNo(1);
        when(cameraDataMapper.detail(1)).thenReturn(current, updated);
        when(cameraDataMapper.findVehicleCarNo("123가1234"))
                .thenReturn(77);
        when(cameraDataMapper.updateCarNo(any(CameraDataDTO.class)))
                .thenReturn(1);

        CameraDataDTO request = new CameraDataDTO();
        request.setCarNo(" 123가 1234 ");

        CameraDataDTO result = cameraDataService.editCarNo(1, request);

        ArgumentCaptor<CameraDataDTO> captor =
                ArgumentCaptor.forClass(CameraDataDTO.class);
        verify(cameraDataMapper).updateCarNo(captor.capture());
        CameraDataDTO updateDto = captor.getValue();

        assertSame(updated, result);
        assertEquals("123가1234", updateDto.getCarNo());
        assertEquals(77, updateDto.getVehicleCarNo());
        assertTrue(result.getRegistered());
        assertFalse(result.getAliasSaved());
        verify(carLogService).correctByCameraData(updateDto);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-006 | 잘못된 차량번호 형식의 수정을 거부한다"
    )
    void editCarNo_rejectsInvalidPlateNumber() {
        when(cameraDataMapper.detail(1))
                .thenReturn(new CameraDataDTO());

        CameraDataDTO request = new CameraDataDTO();
        request.setCarNo("1234");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraDataService.editCarNo(1, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(cameraDataMapper, never()).updateCarNo(any());
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-007 | 저신뢰 OCR의 승인 차량을 확인한 뒤 SITE 게이트를 연다"
    )
    void confirmLowConfidenceGate_opensSiteGateForRegisteredVehicle() {
        CameraDataDTO current = new CameraDataDTO();
        current.setCarNo("123가1234");

        CameraDataDTO confirmed = new CameraDataDTO();
        confirmed.setCameraDataNo(1);
        confirmed.setCameraNo(5);
        confirmed.setVehicleCarNo(77);

        when(cameraDataMapper.detail(1))
                .thenReturn(current, confirmed);
        when(cameraDataMapper.findVehicleCarNo("123가1234"))
                .thenReturn(77);
        when(cameraDataMapper.updateCarNo(any(CameraDataDTO.class)))
                .thenReturn(1);

        GateDTO gate = gate(9, "SITE", "In");
        when(gateService.findByCameraNo(5)).thenReturn(gate);
        when(gateService.open(9)).thenReturn(1);
        when(cameraDataMapper.markGateOpened(1)).thenReturn(1);

        CameraDataDTO request = new CameraDataDTO();
        request.setCarNo("123가1234");

        CameraDataDTO result = cameraDataService
                .confirmLowConfidenceGate(1, request);

        assertTrue(result.getRegistered());
        assertTrue(result.getGateOpened());
        assertEquals(9, result.getGateNo());
        verify(gateService).scheduleClose(9);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-008 | SITE 입구의 미등록 일반 차량을 등록하고 게이트를 연다"
    )
    void openVisitGateByCameraData_registersAndOpensGate() {
        CameraDataDTO cameraData = new CameraDataDTO();
        cameraData.setCameraNo(5);
        cameraData.setCarNo(" 123가 1234 ");
        when(cameraDataMapper.detail(1)).thenReturn(cameraData);

        GateDTO gate = gate(9, "SITE", "In");
        when(gateService.findByCameraNo(5)).thenReturn(gate);
        when(vehicleService.registerAdminVisit(
                "admin01",
                "123가1234"
        )).thenReturn(77);
        when(gateService.open(9)).thenReturn(1);

        int result = cameraDataService.openVisitGateByCameraData(
                "admin01",
                1
        );

        assertEquals(1, result);
        verify(cameraDataMapper).applyMatchedCarNo(
                argThatCameraData(1, 77, "123가1234")
        );
        verify(cameraDataMapper).markGateOpened(1);
        verify(gateService).scheduleClose(9);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-009 | SITE 입구의 미등록 긴급차량을 등록하고 게이트를 연다"
    )
    void openEmergencyGateByCameraData_registersAndOpensGate() {
        CameraDataDTO cameraData = new CameraDataDTO();
        cameraData.setCameraNo(5);
        cameraData.setOcrCarNo("998가1234");
        when(cameraDataMapper.detail(1)).thenReturn(cameraData);

        GateDTO gate = gate(9, "SITE", "In");
        when(gateService.findByCameraNo(5)).thenReturn(gate);
        when(vehicleService.registerEmergencyVisit(
                "admin01",
                "998가1234"
        )).thenReturn(88);
        when(gateService.open(9)).thenReturn(1);

        int result = cameraDataService.openEmergencyGateByCameraData(
                "admin01",
                1
        );

        assertEquals(1, result);
        verify(cameraDataMapper).applyMatchedCarNo(
                argThatCameraData(1, 88, "998가1234")
        );
        verify(cameraDataMapper).markGateOpened(1);
        verify(gateService).scheduleClose(9);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-010 | 차량번호 검색을 Mapper에 위임한다"
    )
    void searchByCarNo_returnsMapperResult() {
        List<CameraDataDTO> expected = List.of(new CameraDataDTO());
        when(cameraDataMapper.searchByCarNo("1234"))
                .thenReturn(expected);

        List<CameraDataDTO> result =
                cameraDataService.searchByCarNo("1234");

        assertSame(expected, result);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-011 | 한 건 이동 실패 후에도 다음 오래된 기록을 계속 처리한다"
    )
    void deleteData_continuesAfterIndividualFailure() {
        CameraDataDTO first = cameraData(1);
        CameraDataDTO second = cameraData(2);
        when(cameraDataMapper.deleteTarget())
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("move failed"))
                .when(trashService)
                .moveCameraData(1, "SCHEDULED");

        int result = cameraDataService.deleteData();

        assertEquals(1, result);
        verify(trashService).moveCameraData(1, "SCHEDULED");
        verify(trashService).moveCameraData(2, "SCHEDULED");
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-012 | 자동 삭제가 오래된 기록 조회를 실행한다"
    )
    void autoDelete_runsScheduledCleanup() {
        when(cameraDataMapper.deleteTarget()).thenReturn(List.of());

        cameraDataService.autoDelete();

        verify(cameraDataMapper).deleteTarget();
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-013 | 존재하고 읽을 수 있는 원본 이미지 경로를 반환한다"
    )
    void getCameraImagePath_returnsReadableFile() throws Exception {
        Path image = temporaryDirectory.resolve("original.jpg");
        Files.write(image, new byte[]{1});
        CameraDataDTO dto = new CameraDataDTO();
        dto.setImagePath(image.toString());
        when(cameraDataMapper.detail(1)).thenReturn(dto);

        Path result = cameraDataService.getCameraImagePath(1);

        assertEquals(image, result);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-014 | 이미지 경로가 없는 기록을 찾을 수 없음으로 처리한다"
    )
    void getCameraImagePath_rejectsMissingPath() {
        when(cameraDataMapper.detail(1))
                .thenReturn(new CameraDataDTO());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraDataService.getCameraImagePath(1)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-015 | 카메라 메모를 수정하고 최신 상세를 반환한다"
    )
    void updateNote_returnsUpdatedCameraData() {
        CameraDataDTO updated = new CameraDataDTO();
        updated.setCamNote("확인 완료");
        when(cameraDataMapper.updateNote(1, "확인 완료"))
                .thenReturn(1);
        when(cameraDataMapper.detail(1)).thenReturn(updated);

        CameraDataDTO result =
                cameraDataService.updateNote(1, "확인 완료");

        assertSame(updated, result);
    }

    @Test
    @DisplayName(
            "UT-BE-CAMERADATA-016 | 존재하고 읽을 수 있는 크롭 이미지 경로를 반환한다"
    )
    void getCameraCropImagePath_returnsReadableFile() throws Exception {
        Path crop = temporaryDirectory.resolve("crop.jpg");
        Files.write(crop, new byte[]{1});
        CameraDataDTO dto = new CameraDataDTO();
        dto.setCropImagePath(crop.toString());
        when(cameraDataMapper.detail(1)).thenReturn(dto);

        Path result = cameraDataService.getCameraCropImagePath(1);

        assertEquals(crop, result);
    }

    @Test @DisplayName("UT-BE-CAMERADATA-017 | 존재하지 않는 카메라 데이터의 차량번호 수정을 거부한다")
    void editCarNo_rejectsMissingData(){ when(cameraDataMapper.detail(9)).thenReturn(null); assertStatus(HttpStatus.NOT_FOUND,() -> cameraDataService.editCarNo(9,new CameraDataDTO())); }

    @Test @DisplayName("UT-BE-CAMERADATA-018 | 차량번호 DB 갱신 실패를 서버 오류로 처리한다")
    void editCarNo_rejectsUpdateFailure(){ when(cameraDataMapper.detail(1)).thenReturn(new CameraDataDTO()); CameraDataDTO request=new CameraDataDTO(); request.setCarNo("12가3456"); when(cameraDataMapper.updateCarNo(any())).thenReturn(0); assertStatus(HttpStatus.INTERNAL_SERVER_ERROR,() -> cameraDataService.editCarNo(1,request)); verify(carLogService,never()).correctByCameraData(any()); }

    @Test @DisplayName("UT-BE-CAMERADATA-019 | 미등록 저신뢰 OCR 차량은 게이트를 열지 않고 반환한다")
    void confirmLowConfidenceGate_returnsUnregistered(){ CameraDataDTO current=new CameraDataDTO(); CameraDataDTO confirmed=new CameraDataDTO(); confirmed.setCameraDataNo(1); when(cameraDataMapper.detail(1)).thenReturn(current,confirmed); when(cameraDataMapper.updateCarNo(any())).thenReturn(1); CameraDataDTO request=new CameraDataDTO(); request.setCarNo("12가3456"); CameraDataDTO result=cameraDataService.confirmLowConfidenceGate(1,request); assertFalse(result.getRegistered()); assertFalse(result.getGateOpened()); verifyNoInteractions(gateService); }

    @Test @DisplayName("UT-BE-CAMERADATA-020 | 주차장 게이트의 입출차 처리가 실패하면 개방하지 않는다")
    void confirmLowConfidenceGate_rejectsParkingProcessFailure(){ CameraDataDTO current=new CameraDataDTO(); CameraDataDTO confirmed=new CameraDataDTO(); confirmed.setCameraNo(2); confirmed.setVehicleCarNo(7); when(cameraDataMapper.detail(1)).thenReturn(current,confirmed); when(cameraDataMapper.findVehicleCarNo("12가3456")).thenReturn(7); when(cameraDataMapper.updateCarNo(any())).thenReturn(1); when(gateService.findByCameraNo(2)).thenReturn(gate(3,"B1","In")); when(carLogService.processCameraData(confirmed)).thenReturn(0); CameraDataDTO request=new CameraDataDTO(); request.setCarNo("12가3456"); assertStatus(HttpStatus.CONFLICT,() -> cameraDataService.confirmLowConfidenceGate(1,request)); verify(gateService,never()).open(any(Integer.class)); }

    @Test @DisplayName("UT-BE-CAMERADATA-021 | 일반 방문차량 승인은 SITE 입구에서만 허용한다")
    void openVisitGate_rejectsWrongGate(){ CameraDataDTO data=new CameraDataDTO(); data.setCameraNo(2); when(cameraDataMapper.detail(1)).thenReturn(data); when(gateService.findByCameraNo(2)).thenReturn(gate(3,"B1","In")); assertStatus(HttpStatus.BAD_REQUEST,() -> cameraDataService.openVisitGateByCameraData("admin",1)); verifyNoInteractions(vehicleService); }

    @Test @DisplayName("UT-BE-CAMERADATA-022 | 긴급차량 승인은 등록차량과 빈 차량번호를 거부한다")
    void openEmergencyGate_rejectsInvalidVehicle(){ CameraDataDTO registered=new CameraDataDTO(); registered.setCameraNo(2); registered.setVehicleCarNo(7); when(cameraDataMapper.detail(1)).thenReturn(registered); when(gateService.findByCameraNo(2)).thenReturn(gate(3,"SITE","In")); assertStatus(HttpStatus.CONFLICT,() -> cameraDataService.openEmergencyGateByCameraData("admin",1)); CameraDataDTO blank=new CameraDataDTO(); blank.setCameraNo(2); when(cameraDataMapper.detail(2)).thenReturn(blank); assertStatus(HttpStatus.BAD_REQUEST,() -> cameraDataService.openEmergencyGateByCameraData("admin",2)); }

    @Test @DisplayName("UT-BE-CAMERADATA-023 | 존재하지 않는 메모 수정 대상을 거부한다")
    void updateNote_rejectsMissingData(){ when(cameraDataMapper.updateNote(9,"메모")).thenReturn(0); assertStatus(HttpStatus.NOT_FOUND,() -> cameraDataService.updateNote(9,"메모")); verify(cameraDataMapper,never()).detail(9); }

    @Test @DisplayName("UT-BE-CAMERADATA-024 | 경로가 없거나 파일이 없는 크롭 이미지 조회를 거부한다")
    void getCameraCropImagePath_rejectsMissingFile(){ when(cameraDataMapper.detail(1)).thenReturn(new CameraDataDTO()); assertStatus(HttpStatus.NOT_FOUND,() -> cameraDataService.getCameraCropImagePath(1)); CameraDataDTO missing=new CameraDataDTO(); missing.setCropImagePath(temporaryDirectory.resolve("missing.jpg").toString()); when(cameraDataMapper.detail(2)).thenReturn(missing); assertStatus(HttpStatus.NOT_FOUND,() -> cameraDataService.getCameraCropImagePath(2)); }

    private void assertStatus(HttpStatus status,Runnable action){ ResponseStatusException e=assertThrows(ResponseStatusException.class,action::run); assertEquals(status,e.getStatusCode()); }

    private static GateDTO gate(
            int gateNo,
            String area,
            String type
    ) {
        GateDTO gate = new GateDTO();
        gate.setGateNo(gateNo);
        gate.setGateArea(area);
        gate.setGateType(type);
        return gate;
    }

    private static CameraDataDTO cameraData(int cameraDataNo) {
        CameraDataDTO dto = new CameraDataDTO();
        dto.setCameraDataNo(cameraDataNo);
        return dto;
    }

    private static CameraDataDTO argThatCameraData(
            int cameraDataNo,
            int vehicleCarNo,
            String carNo
    ) {
        return org.mockito.ArgumentMatchers.argThat(dto ->
                dto != null
                        && dto.getCameraDataNo() == cameraDataNo
                        && Integer.valueOf(vehicleCarNo)
                        .equals(dto.getVehicleCarNo())
                        && carNo.equals(dto.getCarNo())
        );
    }
}
