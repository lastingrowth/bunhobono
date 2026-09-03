package api.cameradata_p;

import api.carlog_p.CarLogService;
import api.carlog_p.CarLogDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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


    // Additional service-only scenarios. External collaborators remain Mockito mocks.
    @ParameterizedTest(name = "[{index}] page={0}, size={1}, keyword={2}")
    @MethodSource("pageBoundaries")
    @DisplayName("UT-BE-CAMERADATA-025 | 페이지 하한과 빈 검색어를 정규화한다")
    void page_normalizesLowerBounds(int page, int size, String keyword, String normalized) {
        // Arrange
        when(cameraDataMapper.page(normalized, null, 1, 0)).thenReturn(List.of());
        // Act
        CameraDataDTO.PageResponse result = cameraDataService.page(page, size, keyword, null);
        // Assert
        assertTrue(result.items().isEmpty());
        assertEquals(1, result.page());
        assertEquals(1, result.size());
        assertEquals(1, result.totalPages());
        verify(cameraDataMapper).page(normalized, null, 1, 0);
        verify(cameraDataMapper).countPage(normalized, null);
    }

    static Stream<Arguments> pageBoundaries() {
        return Stream.of(
                Arguments.of(-1, -10, null, null),
                Arguments.of(0, 0, "", ""),
                Arguments.of(1, 1, "   ", ""));
    }

    @ParameterizedTest(name = "[{index}] total={0}, totalPages={1}")
    @CsvSource({"20,2", "21,3", "0,1"})
    @DisplayName("UT-BE-CAMERADATA-026 | 다음 페이지 offset과 총 페이지 올림을 계산한다")
    void page_calculatesOffsetAndTotal(long total, int totalPages) {
        when(cameraDataMapper.page("12", 7, 10, 20)).thenReturn(List.of());
        when(cameraDataMapper.countPage("12", 7)).thenReturn(total);

        CameraDataDTO.PageResponse result = cameraDataService.page(3, 10, " 12 ", 7);

        assertEquals(3, result.page());
        assertEquals(total, result.totalCount());
        assertEquals(totalPages, result.totalPages());
        verify(cameraDataMapper).page("12", 7, 10, 20);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-027 | 빈 원본 파일을 저장 전에 거부한다")
    void ocr_rejectsEmptyOriginal() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> cameraDataService.ocr(5, "12가3456", 99.0, empty, null));

        verifyNoInteractions(cameraDataMapper, gateService, carLogService, noticeService);
    }

    @ParameterizedTest(name = "[{index}] plate={0}, score={1}, confirmed={2}")
    @MethodSource("recognitionBoundaries")
    @DisplayName("UT-BE-CAMERADATA-028 | OCR 신뢰도 95 경계와 미인식 입력을 판정한다")
    void ocr_checksRecognitionBoundaries(String plate, Double score, boolean confirmed) {
        savedOcr();

        CameraDataDTO result = cameraDataService.ocr(5, plate, score, original(), null);

        assertEquals(confirmed, result.getRecognitionState());
        assertEquals(score, result.getConfidenceScore());
        assertEquals(plate == null ? "" : plate.replaceAll("\\s+", ""), result.getOcrCarNo());
        verify(noticeService, org.mockito.Mockito.times(confirmed ? 0 : 1))
                .createOcrReviewNotice(100);
        verifyNoInteractions(carLogService, vehicleService);
    }

    static Stream<Arguments> recognitionBoundaries() {
        return Stream.of(
                Arguments.of("12가3456", null, false),
                Arguments.of("12가3456", 94.99, false),
                Arguments.of("12가3456", 95.0, false),
                Arguments.of("12가3456", 95.01, true),
                Arguments.of("12가3456", 100.0, true),
                Arguments.of(null, 99.0, false),
                Arguments.of("", 99.0, false),
                Arguments.of("   ", 99.0, false),
                Arguments.of("미인식", 99.0, false));
    }

    @ParameterizedTest(name = "[{index}] originalName={0}, cropName={1}")
    @CsvSource({"photo,crop", "'',''", "photo.JPG,crop.PNG"})
    @DisplayName("UT-BE-CAMERADATA-029 | 파일명 기본 확장자와 임시 폴더 내 저장 내용을 확인한다")
    void ocr_handlesFileNames(String originalName, String cropName) throws Exception {
        savedOcr();
        MockMultipartFile file = new MockMultipartFile("file", originalName, null, new byte[]{1, 2});
        MockMultipartFile crop = new MockMultipartFile("crop", cropName, null, new byte[]{3, 4});

        CameraDataDTO result = cameraDataService.ocr(5, "../12가3456", 90.0, file, crop);

        Path imagePath = Path.of(result.getImagePath()).toAbsolutePath().normalize();
        Path cropPath = Path.of(result.getCropImagePath()).toAbsolutePath().normalize();
        assertEquals(temporaryDirectory.toAbsolutePath().normalize(), imagePath.getParent());
        assertEquals(temporaryDirectory.toAbsolutePath().normalize(), cropPath.getParent());
        assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(imagePath));
        assertArrayEquals(new byte[]{3, 4}, Files.readAllBytes(cropPath));
        assertTrue(cropPath.toString().endsWith(cropName.contains(".") ? ".PNG" : ".jpg"));
        assertNotNull(result.getCaptureTime());
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-030 | 승인 별칭으로 차량을 보정하되 OCR 원본은 유지한다")
    void ocr_correctsApprovedAlias() {
        savedOcr();
        when(cameraDataMapper.findVehicleCarNo("12가3458")).thenReturn(null);
        CameraDataDTO alias = data(77, "12가3456");
        when(cameraDataMapper.findApprovedVehicleByAlias("12가3458")).thenReturn(alias);

        CameraDataDTO result = cameraDataService.ocr(5, "12가3458", 99.0, original(), null);

        assertEquals("12가3458", result.getOcrCarNo());
        assertEquals("12가3456", result.getCarNo());
        assertEquals(77, result.getVehicleCarNo());
        assertTrue(result.getAutoCorrected());
        assertTrue(result.getRegistered());
        assertNull(result.getCropImagePath());
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-031 | OCR 저장 0건이면 후속 알림과 게이트 처리를 중단한다")
    void ocr_stopsOnInsertFailure() {
        when(cameraDataMapper.insert(any())).thenReturn(0);

        CameraDataDTO result = cameraDataService.ocr(5, "12가3456", 90.0, original(), null);

        assertFalse(result.getSaved());
        assertFalse(result.getGateOpened());
        verifyNoInteractions(gateService, carLogService, noticeService, vehicleService);
        verify(cameraDataMapper, never()).detail(org.mockito.ArgumentMatchers.anyInt());
    }

    @ParameterizedTest(name = "[{index}] area={0}, type={1}, vehicle={2}, score={3}")
    @CsvSource({
            "SITE,In,77,99,true", "site,out,77,99,true",
            "SITE,In,77,95,false", "SITE,In,,99,false",
            "OTHER,In,77,99,false"
    })
    @DisplayName("UT-BE-CAMERADATA-032 | SITE 게이트는 확정된 등록 차량만 개방한다")
    void ocr_checksSiteGate(String area, String type, Integer vehicle, double score, boolean opens) {
        CameraDataDTO processing = ocrGate(area, type, vehicle, "12가3456");
        if (vehicle != null) when(cameraDataMapper.findVehicleCarNo("12가3456")).thenReturn(vehicle);
        if (opens) when(gateService.open(9)).thenReturn(1);

        CameraDataDTO result = cameraDataService.ocr(5, "12가3456", score, original(), null);

        assertEquals(opens, result.getGateOpened());
        assertEquals(vehicle != null, result.getRegistered());
        assertEquals(9, result.getGateNo());
        verify(gateService, org.mockito.Mockito.times(opens ? 1 : 0)).open(9);
        verify(gateService, org.mockito.Mockito.times(opens ? 1 : 0)).scheduleClose(9);
        verify(cameraDataMapper, org.mockito.Mockito.times(opens ? 1 : 0)).markGateOpened(100);
        verifyNoInteractions(carLogService, vehicleService);
        if (vehicle != null) verify(cameraDataMapper, never()).findApprovedVehicleByAlias(any());
        assertEquals(100, processing.getCameraDataNo());
    }

    @ParameterizedTest(name = "[{index}] area={0}, vehicle={1}, score={2}, processed={3}")
    @CsvSource({
            "B1,77,99,1,true", "B2,77,99,1,true", "b1,77,99,0,false",
            "B2,77,95,0,false", "B1,,99,0,false"
    })
    @DisplayName("UT-BE-CAMERADATA-033 | 주차장 입차는 확정 등록과 입출차 처리 성공을 요구한다")
    void ocr_checksParkingEntry(String area, Integer vehicle, double score, int processed, boolean opens) {
        CameraDataDTO processing = ocrGate(area, "In", vehicle, "12가3456");
        boolean canProcess = vehicle != null && score > 95;
        if (canProcess) when(carLogService.processCameraData(processing)).thenReturn(processed);
        if (opens) when(gateService.open(9)).thenReturn(1);

        CameraDataDTO result = cameraDataService.ocr(5, "12가3456", score, original(), null);

        assertEquals(opens, result.getGateOpened());
        verify(carLogService, org.mockito.Mockito.times(canProcess ? 1 : 0)).processCameraData(processing);
        verify(gateService, org.mockito.Mockito.times(opens ? 1 : 0)).open(9);
        verify(carLogService, never()).findCurrentlyParked(any());
    }

    @ParameterizedTest(name = "[{index}] area={0}, score={1}")
    @CsvSource({"B1,90", "B2,99"})
    @DisplayName("UT-BE-CAMERADATA-034 | 주차 중 출차는 확정 차량번호를 복원하고 게이트를 연다")
    void ocr_matchesParkedExit(String area, double score) {
        CameraDataDTO processing = ocrGate(area, "Out", 77, "12가3456");
        CarLogDTO parked = new CarLogDTO();
        parked.setCarNo("12가3456");
        parked.setVehicleCarNo(77);
        when(carLogService.findCurrentlyParked(any())).thenReturn(parked);
        when(carLogService.processCameraData(processing)).thenReturn(1);
        when(gateService.open(9)).thenReturn(1);

        CameraDataDTO result = cameraDataService.ocr(5, "12가3458", score, original(), null);

        assertEquals("12가3458", result.getOcrCarNo());
        assertEquals("12가3456", result.getCarNo());
        assertTrue(result.getRecognitionState());
        assertTrue(result.getGateOpened());
        verify(cameraDataMapper).applyMatchedCarNo(argThatCameraData(100, 77, "12가3456"));
        verify(noticeService, never()).createExitWithoutEntryNotice(100);
        verify(gateService).scheduleClose(9);
    }

    @ParameterizedTest(name = "[{index}] score={0}, exitNotice={1}")
    @CsvSource({"99,true", "95,false"})
    @DisplayName("UT-BE-CAMERADATA-035 | 입차 기록 없는 출차는 차단하고 확정 OCR에만 알림을 만든다")
    void ocr_rejectsExitWithoutEntry(double score, boolean notice) {
        ocrGate("B2", "Out", null, "12가3456");

        CameraDataDTO result = cameraDataService.ocr(5, "12가3456", score, original(), null);

        assertFalse(result.getGateOpened());
        verify(noticeService, org.mockito.Mockito.times(notice ? 1 : 0)).createExitWithoutEntryNotice(100);
        verify(carLogService, never()).processCameraData(any());
        verify(gateService, never()).open(9);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-036 | OCR 저장 후 상세가 없으면 충돌로 처리한다")
    void ocr_rejectsMissingProcessingData() {
        savedOcr();
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, "SITE", "In"));

        assertStatus(HttpStatus.CONFLICT,
                () -> cameraDataService.ocr(5, "12가3456", 99.0, original(), null));

        verify(gateService, never()).open(9);
    }

    @ParameterizedTest(name = "[{index}] plate={0}")
    @ValueSource(strings = {"998가1234", "999나1234"})
    @DisplayName("UT-BE-CAMERADATA-037 | SITE 입구의 998·999 차량은 자동 긴급 등록 후 개방한다")
    void ocr_registersEmergencyNumber(String plate) {
        CameraDataDTO first = ocrGate("SITE", "In", null, plate);
        CameraDataDTO registered = data(88, plate);
        when(cameraDataMapper.detail(100)).thenReturn(first, registered);
        when(vehicleService.registerEmergencyVisit(null, plate)).thenReturn(88);
        when(cameraDataMapper.applyMatchedCarNo(any())).thenReturn(1);
        when(gateService.open(9)).thenReturn(1);

        CameraDataDTO result = cameraDataService.ocr(5, plate, 99.0, original(), null);

        assertEquals(88, result.getVehicleCarNo());
        assertTrue(result.getRegistered());
        assertTrue(result.getGateOpened());
        verify(cameraDataMapper).applyMatchedCarNo(argThatCameraData(100, 88, plate));
        verify(vehicleService).registerEmergencyVisit(null, plate);
        verifyNoInteractions(carLogService);
    }

    @ParameterizedTest(name = "[{index}] failure={0}")
    @ValueSource(strings = {"MATCH_ZERO", "REREAD_MISSING"})
    @DisplayName("UT-BE-CAMERADATA-038 | 긴급 자동등록의 연결 실패 또는 상세 소실을 거부한다")
    void ocr_rejectsEmergencyPersistenceFailure(String failure) {
        CameraDataDTO first = ocrGate("SITE", "In", null, "998가1234");
        when(vehicleService.registerEmergencyVisit(null, "998가1234")).thenReturn(88);
        if (failure.equals("REREAD_MISSING")) {
            when(cameraDataMapper.applyMatchedCarNo(any())).thenReturn(1);
            when(cameraDataMapper.detail(100)).thenReturn(first, (CameraDataDTO) null);
        }

        assertStatus(HttpStatus.CONFLICT,
                () -> cameraDataService.ocr(5, "998가1234", 99.0, original(), null));

        verify(gateService, never()).open(9);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-039 | OCR 게이트 개방 실패는 충돌이며 닫기 예약을 하지 않는다")
    void ocr_rejectsGateOpenFailure() {
        ocrGate("SITE", "In", 77, "12가3456");

        assertStatus(HttpStatus.CONFLICT,
                () -> cameraDataService.ocr(5, "12가3456", 99.0, original(), null));

        verify(cameraDataMapper, never()).markGateOpened(100);
        verify(gateService, never()).scheduleClose(9);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-040 | 이미지 읽기 예외를 원인 보존 RuntimeException으로 변환한다")
    void ocr_wrapsImageReadFailure() throws Exception {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        IOException cause = new IOException("simulated image read failure");
        when(file.getInputStream()).thenThrow(cause);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> cameraDataService.ocr(5, "12가3456", 99.0, file, null));

        assertEquals("카메라 OCR 이미지 저장 실패", error.getMessage());
        assertSame(cause, error.getCause());
        verifyNoInteractions(cameraDataMapper, gateService);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-041 | OCR Mapper 예외를 원인 보존 RuntimeException으로 변환한다")
    void ocr_wrapsMapperFailure() {
        IllegalStateException cause = new IllegalStateException("simulated insert failure");
        when(cameraDataMapper.insert(any())).thenThrow(cause);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> cameraDataService.ocr(5, "12가3456", 99.0, original(), null));

        assertSame(cause, error.getCause());
        verifyNoInteractions(gateService, noticeService, carLogService);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-042 | 상세 데이터가 없으면 Mapper의 null을 그대로 반환한다")
    void getCameraData_returnsNullForMissingData() {
        assertNull(cameraDataService.getCameraData(999));
        verify(cameraDataMapper).detail(999);
    }

    @ParameterizedTest(name = "[{index}] plate={0}")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "1234", "12A3456", "1가3456", "1234가3456"})
    @DisplayName("UT-BE-CAMERADATA-043 | null·빈 값·형식 오류 차량번호는 수정하지 않는다")
    void editCarNo_rejectsInvalidInputs(String plate) {
        when(cameraDataMapper.detail(1)).thenReturn(data(null, "old"));
        CameraDataDTO request = request(plate);

        assertStatus(HttpStatus.BAD_REQUEST, () -> cameraDataService.editCarNo(1, request));

        verify(cameraDataMapper, never()).findVehicleCarNo(any());
        verify(cameraDataMapper, never()).updateCarNo(any());
        verifyNoInteractions(carLogService);
    }

    @Test
    @DisplayName("UT-BE-CAMERADATA-044 | 차량번호 수정 요청 객체가 null이면 거부한다")
    void editCarNo_rejectsNullRequest() {
        when(cameraDataMapper.detail(1)).thenReturn(new CameraDataDTO());

        assertStatus(HttpStatus.BAD_REQUEST, () -> cameraDataService.editCarNo(1, null));

        verify(cameraDataMapper, never()).updateCarNo(any());
    }

    @ParameterizedTest(name = "[{index}] plate={0}")
    @ValueSource(strings = {"12가3456", "123가3456", "서울12가3456"})
    @DisplayName("UT-BE-CAMERADATA-045 | 허용 차량번호 형식을 확정하고 미등록 상태를 반환한다")
    void editCarNo_acceptsPlateFormats(String plate) {
        CameraDataDTO updated = stubEdit(null, plate);

        CameraDataDTO result = cameraDataService.editCarNo(1, request(plate));

        assertSame(updated, result);
        assertFalse(result.getRegistered());
        assertFalse(result.getAliasSaved());
        verify(cameraDataMapper).updateCarNo(org.mockito.ArgumentMatchers.argThat(
                dto -> plate.equals(dto.getCarNo()) && dto.getVehicleCarNo() == null));
        verify(carLogService).correctByCameraData(any());
    }

    @ParameterizedTest(name = "[{index}] existingAliasOwner={0}")
    @CsvSource({",", "77,77"})
    @DisplayName("UT-BE-CAMERADATA-046 | 별칭이 비어 있거나 동일 차량 소유면 저장한다")
    void editCarNo_savesAvailableAlias(Integer actualOwner, Integer aliasOwner) {
        CameraDataDTO current = data(null, "12가3458");
        CameraDataDTO updated = data(77, "12가3456");
        when(cameraDataMapper.detail(1)).thenReturn(current, updated);
        when(cameraDataMapper.findVehicleCarNo("12가3456")).thenReturn(77);
        when(cameraDataMapper.findVehicleCarNo("12가3458")).thenReturn(actualOwner);
        when(cameraDataMapper.findAliasVehicleCarNo("12가3458")).thenReturn(aliasOwner);
        when(cameraDataMapper.updateAlias(77, "12가3458")).thenReturn(1);
        when(cameraDataMapper.updateCarNo(any())).thenReturn(1);
        CameraDataDTO request = request("12가3456");
        request.setSaveAlias(true);

        CameraDataDTO result = cameraDataService.editCarNo(1, request);

        assertTrue(result.getAliasSaved());
        verify(cameraDataMapper).updateAlias(77, "12가3458");
        verify(carLogService).correctByCameraData(any());
    }

    @ParameterizedTest(name = "[{index}] conflict={0}")
    @ValueSource(strings = {"ACTUAL_NUMBER", "ALIAS"})
    @DisplayName("UT-BE-CAMERADATA-047 | 다른 차량 실제 번호 또는 별칭과 충돌하면 거부한다")
    void editCarNo_rejectsAliasConflict(String conflict) {
        when(cameraDataMapper.detail(1)).thenReturn(data(null, "12가3458"));
        when(cameraDataMapper.findVehicleCarNo("12가3456")).thenReturn(77);
        if (conflict.equals("ACTUAL_NUMBER"))
            when(cameraDataMapper.findVehicleCarNo("12가3458")).thenReturn(88);
        else {
            when(cameraDataMapper.findVehicleCarNo("12가3458")).thenReturn(null);
            when(cameraDataMapper.findAliasVehicleCarNo("12가3458")).thenReturn(88);
        }
        CameraDataDTO request = request("12가3456");
        request.setSaveAlias(true);

        assertStatus(HttpStatus.CONFLICT, () -> cameraDataService.editCarNo(1, request));

        verify(cameraDataMapper, never()).updateAlias(org.mockito.ArgumentMatchers.anyInt(), any());
        verify(cameraDataMapper, never()).updateCarNo(any());
        verifyNoInteractions(carLogService);
    }

    @ParameterizedTest(name = "[{index}] previous={0}, vehicle={1}, save={2}")
    @CsvSource({"12가3456,77,true", ",77,true", "'   ',77,true",
            "12가3458,,true", "12가3458,77,false", "12가3458,77,"})
    @DisplayName("UT-BE-CAMERADATA-048 | 별칭 저장 조건을 충족하지 않으면 별칭을 변경하지 않는다")
    void editCarNo_skipsUnneededAlias(String previous, Integer vehicle, Boolean save) {
        when(cameraDataMapper.detail(1)).thenReturn(data(null, previous), data(vehicle, "12가3456"));
        when(cameraDataMapper.findVehicleCarNo("12가3456")).thenReturn(vehicle);
        when(cameraDataMapper.updateCarNo(any())).thenReturn(1);
        CameraDataDTO request = request("12가3456");
        request.setSaveAlias(save);

        CameraDataDTO result = cameraDataService.editCarNo(1, request);

        assertFalse(result.getAliasSaved());
        verify(cameraDataMapper, never()).updateAlias(org.mockito.ArgumentMatchers.anyInt(), any());
        verify(cameraDataMapper, never()).findAliasVehicleCarNo(any());
    }

    @ParameterizedTest(name = "[{index}] gate={0}")
    @ValueSource(strings = {"MISSING", "UNKNOWN"})
    @DisplayName("UT-BE-CAMERADATA-049 | 저신뢰 확인 시 게이트 없음 또는 미지원 영역을 거부한다")
    void confirmLowConfidenceGate_rejectsInvalidGate(String state) {
        stubEdit(77, "12가3456");
        if (state.equals("UNKNOWN"))
            when(gateService.findByCameraNo(5)).thenReturn(gate(9, "UNKNOWN", "In"));

        assertStatus(state.equals("MISSING") ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT,
                () -> cameraDataService.confirmLowConfidenceGate(1, request("12가3456")));

        verify(gateService, never()).open(9);
    }

    @ParameterizedTest(name = "[{index}] area={0}")
    @ValueSource(strings = {"B1", "B2"})
    @DisplayName("UT-BE-CAMERADATA-050 | 저신뢰 확인 후 주차장 입출차 처리 성공 시 개방한다")
    void confirmLowConfidenceGate_opensParkingGate(String area) {
        CameraDataDTO confirmed = stubEdit(77, "12가3456");
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, area, "In"));
        when(carLogService.processCameraData(confirmed)).thenReturn(1);
        when(gateService.open(9)).thenReturn(1);
        when(cameraDataMapper.markGateOpened(1)).thenReturn(1);

        CameraDataDTO result = cameraDataService.confirmLowConfidenceGate(1, request("12가3456"));

        assertTrue(result.getRegistered());
        assertTrue(result.getGateOpened());
        assertEquals(9, result.getGateNo());
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(carLogService, gateService, cameraDataMapper);
        order.verify(carLogService).processCameraData(confirmed);
        order.verify(gateService).open(9);
        order.verify(cameraDataMapper).markGateOpened(1);
        order.verify(gateService).scheduleClose(9);
    }

    @ParameterizedTest(name = "[{index}] failure={0}")
    @ValueSource(strings = {"OPEN_ZERO", "MARK_ZERO"})
    @DisplayName("UT-BE-CAMERADATA-051 | 저신뢰 확인의 개방 실패와 개방 기록 실패를 충돌로 처리한다")
    void confirmLowConfidenceGate_rejectsOpenOrMarkFailure(String failure) {
        stubEdit(77, "12가3456");
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, "SITE", "In"));
        if (failure.equals("MARK_ZERO")) when(gateService.open(9)).thenReturn(1);

        assertStatus(HttpStatus.CONFLICT,
                () -> cameraDataService.confirmLowConfidenceGate(1, request("12가3456")));

        verify(gateService, never()).scheduleClose(9);
        if (failure.equals("OPEN_ZERO")) verify(cameraDataMapper, never()).markGateOpened(1);
    }

    @ParameterizedTest(name = "[{index}] emergency={0}, invalid={1}")
    @MethodSource("manualGateInvalidInputs")
    @DisplayName("UT-BE-CAMERADATA-052 | 수동 일반·긴급 승인의 데이터·게이트·차량 조건을 검사한다")
    void manualGate_rejectsInvalidInputs(boolean emergency, String invalid, HttpStatus status) {
        if (!invalid.equals("DATA_MISSING")) {
            CameraDataDTO current = data(invalid.equals("REGISTERED") ? 77 : null,
                    invalid.equals("EMPTY_PLATE") ? "   " : "12가3456");
            when(cameraDataMapper.detail(1)).thenReturn(current);
            if (!invalid.equals("GATE_MISSING")) {
                String area = invalid.equals("PARKING") ? "B1" : "SITE";
                String type = invalid.equals("EXIT") ? "Out" : "In";
                when(gateService.findByCameraNo(5)).thenReturn(gate(9, area, type));
            }
        }

        assertStatus(status, () -> manualGate(emergency));

        verifyNoInteractions(vehicleService);
        verify(gateService, never()).open(9);
        verify(cameraDataMapper, never()).applyMatchedCarNo(any());
    }

    static Stream<Arguments> manualGateInvalidInputs() {
        return Stream.of(false, true).flatMap(emergency -> Stream.of(
                Arguments.of(emergency, "DATA_MISSING", HttpStatus.NOT_FOUND),
                Arguments.of(emergency, "GATE_MISSING", HttpStatus.NOT_FOUND),
                Arguments.of(emergency, "PARKING", HttpStatus.BAD_REQUEST),
                Arguments.of(emergency, "EXIT", HttpStatus.BAD_REQUEST),
                Arguments.of(emergency, "REGISTERED", HttpStatus.CONFLICT),
                Arguments.of(emergency, "EMPTY_PLATE", HttpStatus.BAD_REQUEST)));
    }

    @ParameterizedTest(name = "[{index}] emergency={0}, fallback={1}")
    @CsvSource({"false,true", "true,true", "false,false", "true,false"})
    @DisplayName("UT-BE-CAMERADATA-053 | 수동 승인에서 보정 번호를 우선하고 없으면 OCR 번호를 사용한다")
    void manualGate_usesCorrectedOrOcrPlate(boolean emergency, boolean fallback) {
        CameraDataDTO current = data(null, fallback ? " " : " 12가 3456 ");
        current.setOcrCarNo(" 99나 9999 ");
        when(cameraDataMapper.detail(1)).thenReturn(current);
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, "site", "in"));
        String plate = fallback ? "99나9999" : "12가3456";
        stubVisitRegistration(emergency, plate);
        when(cameraDataMapper.applyMatchedCarNo(any())).thenReturn(1);
        when(cameraDataMapper.markGateOpened(1)).thenReturn(1);
        when(gateService.open(9)).thenReturn(1);

        assertEquals(1, manualGate(emergency));

        verify(cameraDataMapper).applyMatchedCarNo(argThatCameraData(1, 88, plate));
        verify(gateService).scheduleClose(9);
        verifyNoInteractions(carLogService);
    }

    @ParameterizedTest(name = "[{index}] emergency={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("UT-BE-CAMERADATA-054 | 수동 승인에서 게이트 개방 실패 시 성공 처리를 중단한다")
    void manualGate_rejectsOpenFailure(boolean emergency) {
        stubManualGate(emergency);
        when(cameraDataMapper.applyMatchedCarNo(any())).thenReturn(1);

        assertStatus(HttpStatus.CONFLICT, () -> manualGate(emergency));

        verify(cameraDataMapper, never()).markGateOpened(1);
        verify(gateService, never()).scheduleClose(9);
    }

    @ParameterizedTest(name = "[{index}] keyword={0}")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "9999"})
    @DisplayName("UT-BE-CAMERADATA-055 | 검색어를 그대로 Mapper에 전달하고 빈 결과를 반환한다")
    void searchByCarNo_preservesInputAndEmptyResult(String keyword) {
        when(cameraDataMapper.searchByCarNo(keyword)).thenReturn(List.of());

        assertTrue(cameraDataService.searchByCarNo(keyword).isEmpty());

        verify(cameraDataMapper).searchByCarNo(keyword);
    }

    @ParameterizedTest(name = "[{index}] mode={0}, count={1}")
    @CsvSource({"EMPTY,0", "SUCCESS,2", "ALL_FAIL,0"})
    @DisplayName("UT-BE-CAMERADATA-056 | 휴지통 이동은 성공 건수만 집계한다")
    void deleteData_countsOnlySuccessfulMoves(String mode, int count) {
        when(cameraDataMapper.deleteTarget()).thenReturn(
                mode.equals("EMPTY") ? List.of() : List.of(cameraData(1), cameraData(2)));
        if (mode.equals("ALL_FAIL"))
            doThrow(new IllegalStateException("simulated move failure"))
                    .when(trashService).moveCameraData(org.mockito.ArgumentMatchers.anyInt(), any());

        assertEquals(count, cameraDataService.deleteData());

        if (mode.equals("EMPTY")) verifyNoInteractions(trashService);
        else {
            verify(trashService).moveCameraData(1, "SCHEDULED");
            verify(trashService).moveCameraData(2, "SCHEDULED");
        }
        verify(cameraDataMapper, never()).delete(org.mockito.ArgumentMatchers.anyInt());
    }

    @ParameterizedTest(name = "[{index}] crop={0}, missing={1}")
    @MethodSource("missingImageCases")
    @DisplayName("UT-BE-CAMERADATA-057 | 원본·크롭의 데이터·경로·파일이 없으면 404로 처리한다")
    void imagePath_rejectsMissingDataPathOrFile(boolean crop, String missing) {
        if (!missing.equals("DATA")) {
            CameraDataDTO dto = new CameraDataDTO();
            String path = switch (missing) {
                case "NULL" -> null;
                case "EMPTY" -> "";
                case "BLANK" -> "   ";
                default -> temporaryDirectory.resolve("nonexistent.jpg").toString();
            };
            if (crop) dto.setCropImagePath(path); else dto.setImagePath(path);
            when(cameraDataMapper.detail(1)).thenReturn(dto);
        }

        assertStatus(HttpStatus.NOT_FOUND, () -> {
            if (crop) cameraDataService.getCameraCropImagePath(1);
            else cameraDataService.getCameraImagePath(1);
        });
    }

    static Stream<Arguments> missingImageCases() {
        return Stream.of(false, true).flatMap(crop ->
                Stream.of("DATA", "NULL", "EMPTY", "BLANK", "FILE")
                        .map(missing -> Arguments.of(crop, missing)));
    }

    @ParameterizedTest(name = "[{index}] note={0}")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("UT-BE-CAMERADATA-058 | null·빈 메모를 그대로 저장하고 재조회한다")
    void updateNote_preservesNullableText(String note) {
        CameraDataDTO updated = cameraData(1);
        when(cameraDataMapper.updateNote(1, note)).thenReturn(1);
        when(cameraDataMapper.detail(1)).thenReturn(updated);

        assertSame(updated, cameraDataService.updateNote(1, note));

        verify(cameraDataMapper).updateNote(1, note);
        verify(cameraDataMapper).detail(1);
    }

    @ParameterizedTest(name = "[{index}] emergency={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("UT-BE-CAMERADATA-059 | 수동 등록 Service 예외를 보존하고 게이트를 열지 않는다")
    void manualGate_propagatesRegistrationFailure(boolean emergency) {
        when(cameraDataMapper.detail(1)).thenReturn(data(null, "12가3456"));
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, "SITE", "In"));
        ResponseStatusException cause = new ResponseStatusException(HttpStatus.CONFLICT, "duplicate");
        if (emergency) when(vehicleService.registerEmergencyVisit("admin", "12가3456")).thenThrow(cause);
        else when(vehicleService.registerAdminVisit("admin", "12가3456")).thenThrow(cause);

        assertSame(cause, assertThrows(ResponseStatusException.class, () -> manualGate(emergency)));

        verify(cameraDataMapper, never()).applyMatchedCarNo(any());
        verify(gateService, never()).open(9);
    }

    // Failure-oriented regression requirements: do not change production to make these green.
    @ParameterizedTest(name = "[{index}] emergency={0}")
    @ValueSource(booleans = {false, true})
    @org.junit.jupiter.api.Tag("defect-regression")
    @DisplayName("UT-BE-CAMERADATA-060 | 결함검출: 수동 차량 연결 저장 0건이면 게이트를 열면 안 된다")
    void manualGate_mustRejectFailedVehicleLink(boolean emergency) {
        stubManualGate(emergency);
        // A real Mapper can return zero updated rows; successful gate opening must not mask that.
        when(cameraDataMapper.applyMatchedCarNo(any())).thenReturn(0);
        when(gateService.open(9)).thenReturn(1);

        assertThrows(ResponseStatusException.class, () -> manualGate(emergency));
        verify(gateService, never()).open(9);
    }

    @ParameterizedTest(name = "[{index}] emergency={0}")
    @ValueSource(booleans = {false, true})
    @org.junit.jupiter.api.Tag("defect-regression")
    @DisplayName("UT-BE-CAMERADATA-061 | 결함검출: 수동 개방 기록 저장 0건을 성공으로 반환하면 안 된다")
    void manualGate_mustRejectFailedGateRecord(boolean emergency) {
        stubManualGate(emergency);
        when(cameraDataMapper.applyMatchedCarNo(any())).thenReturn(1);
        when(gateService.open(9)).thenReturn(1);
        when(cameraDataMapper.markGateOpened(1)).thenReturn(0);

        assertThrows(ResponseStatusException.class, () -> manualGate(emergency));
    }

    @Test
    @org.junit.jupiter.api.Tag("defect-regression")
    @DisplayName("UT-BE-CAMERADATA-062 | 결함검출: OCR 개방 기록 저장 0건을 성공으로 반환하면 안 된다")
    void ocr_mustRejectFailedGateRecord() {
        ocrGate("SITE", "In", 77, "12가3456");
        when(gateService.open(9)).thenReturn(1);
        when(cameraDataMapper.markGateOpened(100)).thenReturn(0);

        assertThrows(ResponseStatusException.class,
                () -> cameraDataService.ocr(5, "12가3456", 99.0, original(), null));
    }

    @Test
    @org.junit.jupiter.api.Tag("defect-regression")
    @DisplayName("UT-BE-CAMERADATA-063 | 결함검출: 별칭 저장 0건을 aliasSaved 성공으로 표시하면 안 된다")
    void editCarNo_mustNotReportUnsavedAlias() {
        when(cameraDataMapper.detail(1)).thenReturn(data(null, "12가3458"), data(77, "12가3456"));
        when(cameraDataMapper.findVehicleCarNo("12가3456")).thenReturn(77);
        when(cameraDataMapper.findVehicleCarNo("12가3458")).thenReturn(null);
        when(cameraDataMapper.findAliasVehicleCarNo("12가3458")).thenReturn(null);
        when(cameraDataMapper.updateAlias(77, "12가3458")).thenReturn(0);
        when(cameraDataMapper.updateCarNo(any())).thenReturn(1);
        CameraDataDTO request = request("12가3456");
        request.setSaveAlias(true);

        CameraDataDTO result = cameraDataService.editCarNo(1, request);

        assertFalse(result.getAliasSaved(), "저장 0건을 별칭 저장 성공으로 표시하지 않아야 한다");
    }

    @Test
    @org.junit.jupiter.api.Tag("defect-regression")
    @DisplayName("UT-BE-CAMERADATA-064 | 결함검출: 공백 포함 미인식 번호를 인식 확정으로 표시하면 안 된다")
    void ocr_mustNormalizeBeforeRejectingUnrecognizedMarker() {
        savedOcr();

        CameraDataDTO result = cameraDataService.ocr(5, " 미인식 ", 99.0, original(), null);

        assertFalse(result.getRecognitionState());
        verify(noticeService).createOcrReviewNotice(100);
    }

    private MockMultipartFile original() {
        return new MockMultipartFile("file", "original.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private void savedOcr() {
        doAnswer(invocation -> {
            CameraDataDTO dto = invocation.getArgument(0);
            dto.setCameraDataNo(100);
            return 1;
        }).when(cameraDataMapper).insert(any());
    }

    private CameraDataDTO ocrGate(String area, String type, Integer vehicle, String plate) {
        savedOcr();
        CameraDataDTO processing = data(vehicle, plate);
        processing.setCameraDataNo(100);
        when(cameraDataMapper.detail(100)).thenReturn(processing);
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, area, type));
        return processing;
    }

    private static CameraDataDTO data(Integer vehicle, String plate) {
        CameraDataDTO dto = cameraData(1);
        dto.setCameraNo(5);
        dto.setVehicleCarNo(vehicle);
        dto.setCarNo(plate);
        return dto;
    }

    private static CameraDataDTO request(String plate) {
        CameraDataDTO dto = new CameraDataDTO();
        dto.setCarNo(plate);
        return dto;
    }

    private CameraDataDTO stubEdit(Integer vehicle, String plate) {
        CameraDataDTO confirmed = data(vehicle, plate);
        when(cameraDataMapper.detail(1)).thenReturn(data(null, "old"), confirmed);
        when(cameraDataMapper.findVehicleCarNo(plate)).thenReturn(vehicle);
        when(cameraDataMapper.updateCarNo(any())).thenReturn(1);
        return confirmed;
    }

    private int manualGate(boolean emergency) {
        return emergency
                ? cameraDataService.openEmergencyGateByCameraData("admin", 1)
                : cameraDataService.openVisitGateByCameraData("admin", 1);
    }

    private void stubVisitRegistration(boolean emergency, String plate) {
        if (emergency) when(vehicleService.registerEmergencyVisit("admin", plate)).thenReturn(88);
        else when(vehicleService.registerAdminVisit("admin", plate)).thenReturn(88);
    }

    private void stubManualGate(boolean emergency) {
        when(cameraDataMapper.detail(1)).thenReturn(data(null, "12가3456"));
        when(gateService.findByCameraNo(5)).thenReturn(gate(9, "SITE", "In"));
        stubVisitRegistration(emergency, "12가3456");
    }


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
