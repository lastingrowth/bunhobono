package api.carlog_p;

import api.bill_p.BillService;
import api.cameradata_p.CameraDataDTO;
import api.gate_p.*;
import api.kiosk_p.*;
import api.parking_space_p.*;
import api.robot_task_p.*;
import api.trash_p.TrashService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarLogServiceTest {
 @Mock CarLogMapper mapper; @Mock BillService billService; @Mock GateService gateService; @Mock KioskService kioskService; @Mock ParkingSpaceService parkingSpaceService; @Mock RobotTaskService robotTaskService; @Mock TrashService trashService; @InjectMocks CarLogService service;

 @Test @DisplayName("UT-BE-CARLOG-001 | 조회 조건으로 입출차 목록을 반환한다") void list(){ CarLogDTO q=new CarLogDTO(); List<CarLogDTO> expected=List.of(new CarLogDTO()); when(mapper.list(q)).thenReturn(expected); assertThat(service.list(q)).isSameAs(expected); verify(mapper).list(q); }
 @Test @DisplayName("UT-BE-CARLOG-002 | 입출차 번호로 상세 기록을 반환한다") void detail(){ CarLogDTO expected=new CarLogDTO(); when(mapper.detail(3)).thenReturn(expected); assertThat(service.detail(3)).isSameAs(expected); verify(mapper).detail(3); }
 @Test @DisplayName("UT-BE-CARLOG-003 | 차량번호가 없으면 현재 주차 조회를 생략한다") void findCurrentlyParked_blank(){ assertThat(service.findCurrentlyParked(null)).isNull(); CameraDataDTO d=new CameraDataDTO(); d.setCarNo(" "); assertThat(service.findCurrentlyParked(d)).isNull(); verifyNoInteractions(mapper); }
 @Test @DisplayName("UT-BE-CARLOG-004 | 차량번호가 있으면 현재 열린 주차 기록을 반환한다") void findCurrentlyParked(){ CameraDataDTO d=data(); CarLogDTO expected=new CarLogDTO(); when(mapper.findOpenLog(d)).thenReturn(expected); assertThat(service.findCurrentlyParked(d)).isSameAs(expected); }
 @Test @DisplayName("UT-BE-CARLOG-005 | 키오스크 차량검색의 차량번호와 번호를 검증한다") void findParkingCars_invalidInput(){ assertStatus(HttpStatus.BAD_REQUEST,() -> service.findParkingCars("",1)); assertStatus(HttpStatus.BAD_REQUEST,() -> service.findParkingCars("1234",0)); }
 @Test @DisplayName("UT-BE-CARLOG-006 | 존재하지 않는 키오스크의 차량검색을 거부한다") void findParkingCars_missingKiosk(){ assertStatus(HttpStatus.NOT_FOUND,() -> service.findParkingCars("1234",1)); verify(kioskService).findByKioskNo(1); }
 @Test @DisplayName("UT-BE-CARLOG-007 | 같은 주차장의 현재 입주민 차량만 반환한다") void findParkingCars_filtersParking(){ KioskDTO k=new KioskDTO(); k.setParkingNo(10); when(kioskService.findByKioskNo(1)).thenReturn(k); CarLogDTO same=new CarLogDTO(); same.setParkingNo(10); CarLogDTO other=new CarLogDTO(); other.setParkingNo(20); when(mapper.list(any())).thenReturn(List.of(same,other)); assertThat(service.findParkingCars(" 1234 ",1)).containsExactly(same); verify(mapper).list(argThat(q -> "1234".equals(q.getLastFourDigits()) && "PARKING".equals(q.getParkingState()) && "REGISTERED".equals(q.getCarKind()))); }
 @Test @DisplayName("UT-BE-CARLOG-008 | 검색 차량이 없거나 다른 주차장에만 있으면 구분해 거부한다") void findParkingCars_noMatch(){ KioskDTO k=new KioskDTO(); k.setParkingNo(10); when(kioskService.findByKioskNo(1)).thenReturn(k); when(mapper.list(any())).thenReturn(List.of(),List.of(logAtParking(20))); assertStatus(HttpStatus.NOT_FOUND,() -> service.findParkingCars("1234",1)); assertStatus(HttpStatus.CONFLICT,() -> service.findParkingCars("1234",1)); }
 @Test @DisplayName("UT-BE-CARLOG-009 | 차량번호나 연결 게이트가 없으면 카메라 처리를 중단한다") void processCameraData_missingInput(){ assertThat(service.processCameraData(new CameraDataDTO())).isZero(); CameraDataDTO d=data(); when(gateService.findByCameraNo(2)).thenReturn(null); assertThat(service.processCameraData(d)).isZero(); verifyNoInteractions(billService,parkingSpaceService,robotTaskService); }
 @Test @DisplayName("UT-BE-CARLOG-010 | B1 일반 입주민 입차는 대기면 배정과 로봇 작업을 생성한다") void processCameraData_b1Entry(){ CameraDataDTO d=approvedNormal(); GateDTO gate=gate(3,10,"B1","In"); ParkingSpaceDTO entry=space(100,"ENTRY_WAIT"); when(gateService.findByCameraNo(2)).thenReturn(gate); when(parkingSpaceService.findEmptyWaitingSpace(3,"ENTRY_WAIT")).thenReturn(entry); doAnswer(i->{ CarLogDTO log=i.getArgument(0); log.setCarLogNo(50); return 1; }).when(mapper).insertEntry(any()); when(parkingSpaceService.assignCarLog(100,50)).thenReturn(1); when(robotTaskService.createParkInTask(50)).thenReturn(new RobotTaskDTO()); assertThat(service.processCameraData(d)).isEqualTo(1); verify(parkingSpaceService).assignCarLog(100,50); verify(robotTaskService).createParkInTask(50); }
 @Test @DisplayName("UT-BE-CARLOG-011 | B1 입차대기면이 없으면 입차 기록을 생성하지 않는다") void processCameraData_b1NoWaitingSpace(){ CameraDataDTO d=approvedNormal(); when(gateService.findByCameraNo(2)).thenReturn(gate(3,10,"B1","In")); assertThat(service.processCameraData(d)).isZero(); verify(mapper,never()).insertEntry(any()); }
 @Test @DisplayName("UT-BE-CARLOG-012 | B2 방문차량 입차는 미결제 정산서를 생성한다") void processCameraData_b2VisitEntry(){ CameraDataDTO d=approvedVisit(); when(gateService.findByCameraNo(2)).thenReturn(gate(4,20,"B2","In")); when(mapper.hasAvailableCapacity(20)).thenReturn(true); doAnswer(i->{ CarLogDTO log=i.getArgument(0); log.setCarLogNo(60); return 1; }).when(mapper).insertEntry(any()); assertThat(service.processCameraData(d)).isEqualTo(1); verify(billService).createEntryBill(eq(60),eq("12가3456"),any(LocalDateTime.class)); verifyNoInteractions(robotTaskService); }
 @Test @DisplayName("UT-BE-CARLOG-013 | B2 주차용량 부족과 중복 입차를 거부한다") void processCameraData_entryRejected(){ CameraDataDTO d=approvedNormal(); GateDTO gate=gate(4,20,"B2","In"); when(gateService.findByCameraNo(2)).thenReturn(gate); when(mapper.hasAvailableCapacity(20)).thenReturn(false); assertThat(service.processCameraData(d)).isZero(); when(mapper.findOpenLog(d)).thenReturn(new CarLogDTO()); assertThat(service.processCameraData(d)).isZero(); verify(mapper,never()).insertEntry(any()); }
 @Test @DisplayName("UT-BE-CARLOG-014 | B2 비입주민 차량은 결제 유예시간 안에서 출차한다") void processCameraData_b2Exit(){ CameraDataDTO d=approvedVisit(); GateDTO gate=gate(4,20,"B2","Out"); CarLogDTO open=openLog(70,20,"VISIT"); when(gateService.findByCameraNo(2)).thenReturn(gate); when(mapper.findOpenLog(d)).thenReturn(open); when(billService.isExitAllowed(eq(70),any())).thenReturn(true); when(mapper.exitParking(70,d,4)).thenReturn(1); assertThat(service.processCameraData(d)).isEqualTo(1); verify(mapper).exitParking(70,d,4); }
 @Test @DisplayName("UT-BE-CARLOG-015 | 결제되지 않은 B2 비입주민 차량의 출차를 거부한다") void processCameraData_b2PaymentDenied(){ CameraDataDTO d=approvedVisit(); when(gateService.findByCameraNo(2)).thenReturn(gate(4,20,"B2","Out")); when(mapper.findOpenLog(d)).thenReturn(openLog(70,20,"VISIT")); when(billService.isExitAllowed(eq(70),any())).thenReturn(false); assertThat(service.processCameraData(d)).isZero(); verify(mapper,never()).exitParking(anyInt(),any(),anyInt()); }
 @Test @DisplayName("UT-BE-CARLOG-016 | B1 차량은 지정 출차대기면에서 출차하고 주차면을 해제한다") void processCameraData_b1Exit(){ CameraDataDTO d=approvedNormal(); GateDTO gate=gate(3,10,"B1","Out"); CarLogDTO open=openLog(80,10,"REGISTERED"); ParkingSpaceDTO exit=space(101,"EXIT_WAIT"); exit.setGateNo(3); when(gateService.findByCameraNo(2)).thenReturn(gate); when(mapper.findOpenLog(d)).thenReturn(open); when(parkingSpaceService.findByCarLogNo(80)).thenReturn(exit); when(mapper.exitParking(80,d,3)).thenReturn(1); when(parkingSpaceService.releaseCarLog(101,80)).thenReturn(1); assertThat(service.processCameraData(d)).isEqualTo(1); verify(parkingSpaceService).releaseCarLog(101,80); }
 @Test @DisplayName("UT-BE-CARLOG-017 | B1 출차대기면 해제 실패를 충돌로 처리한다") void processCameraData_b1ReleaseConflict(){ CameraDataDTO d=approvedNormal(); GateDTO gate=gate(3,10,"B1","Out"); ParkingSpaceDTO exit=space(101,"EXIT_WAIT"); exit.setGateNo(3); when(gateService.findByCameraNo(2)).thenReturn(gate); when(mapper.findOpenLog(d)).thenReturn(openLog(80,10,"REGISTERED")); when(parkingSpaceService.findByCarLogNo(80)).thenReturn(exit); when(mapper.exitParking(80,d,3)).thenReturn(1); assertStatus(HttpStatus.CONFLICT,() -> service.processCameraData(d)); }
 @Test @DisplayName("UT-BE-CARLOG-018 | OCR 관리자 수정사항을 입출차 기록에 반영한다") void correctByCameraData(){ CameraDataDTO d=data(); when(mapper.correctByCameraData(d)).thenReturn(1); assertThat(service.correctByCameraData(d)).isEqualTo(1); verify(mapper).correctByCameraData(d); }
 @Test @DisplayName("UT-BE-CARLOG-019 | 오래된 기록 한 건 이동 실패 후에도 다음 건을 계속 처리한다") void moveOldCarLogsToTrash(){ when(mapper.findOldCarLogNosForTrash()).thenReturn(List.of(1,2)); doThrow(new RuntimeException()).when(trashService).moveCarLog(1,"SCHEDULED"); service.moveOldCarLogsToTrash(); verify(trashService).moveCarLog(1,"SCHEDULED"); verify(trashService).moveCarLog(2,"SCHEDULED"); }

 private CameraDataDTO data(){ CameraDataDTO d=new CameraDataDTO(); d.setCameraNo(2); d.setCameraDataNo(9); d.setCarNo("12가3456"); return d; }
 private CameraDataDTO approvedNormal(){ CameraDataDTO d=data(); d.setVehicleCarNo(7); d.setVehicleStatus("APPROVED"); d.setVehicleType("normal"); return d; }
 private CameraDataDTO approvedVisit(){ CameraDataDTO d=data(); d.setVehicleCarNo(8); d.setVehicleStatus("APPROVED"); d.setVehicleType("visit"); d.setApartmentUnitNo(1); d.setStartDate(LocalDateTime.now().minusHours(1)); d.setEndDate(LocalDateTime.now().plusHours(1)); return d; }
 private GateDTO gate(int no,int parking,String area,String type){ GateDTO g=new GateDTO(); g.setGateNo(no); g.setParkingNo(parking); g.setGateArea(area); g.setGateType(type); return g; }
 private ParkingSpaceDTO space(long no,String type){ ParkingSpaceDTO s=new ParkingSpaceDTO(); s.setSpaceNo(no); s.setSpaceType(type); return s; }
 private CarLogDTO openLog(int no,int parking,String kind){ CarLogDTO l=new CarLogDTO(); l.setCarLogNo(no); l.setParkingNo(parking); l.setSnapshotCarKind(kind); return l; }
 private CarLogDTO logAtParking(int parking){ CarLogDTO l=new CarLogDTO(); l.setParkingNo(parking); return l; }
 private void assertStatus(HttpStatus status,Runnable action){ ResponseStatusException e=catchThrowableOfType(action::run,ResponseStatusException.class); assertThat(e).isNotNull(); assertThat(e.getStatusCode()).isEqualTo(status); }
}
