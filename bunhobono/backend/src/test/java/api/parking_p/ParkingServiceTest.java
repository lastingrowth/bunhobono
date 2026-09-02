package api.parking_p;
import org.junit.jupiter.api.*; import org.springframework.test.util.ReflectionTestUtils; import java.util.List;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
class ParkingServiceTest {
 @Test @DisplayName("UT-BE-PARKING-001 주차장 목록을 반환한다") void list(){ ParkingMapper m=mock(ParkingMapper.class); ParkingService s=service(m); List<ParkingDTO> x=List.of(new ParkingDTO()); when(m.list()).thenReturn(x); assertThat(s.list()).isSameAs(x); }
 @Test @DisplayName("UT-BE-PARKING-002 주차장을 등록한다") void insert(){ ParkingMapper m=mock(ParkingMapper.class); ParkingService s=service(m); ParkingDTO d=new ParkingDTO(); when(m.insert(d)).thenReturn(1); assertThat(s.insert(d)).isEqualTo(1); verify(m).insert(d); }
 @Test @DisplayName("UT-BE-PARKING-003 주차장을 수정한다") void update(){ ParkingMapper m=mock(ParkingMapper.class); ParkingService s=service(m); ParkingDTO d=new ParkingDTO(); when(m.updateParking(d)).thenReturn(1); assertThat(s.update(d)).isEqualTo(1); verify(m).updateParking(d); }
 @Test @DisplayName("UT-BE-PARKING-004 주차장을 삭제한다") void delete(){ ParkingMapper m=mock(ParkingMapper.class); ParkingService s=service(m); when(m.delete(1)).thenReturn(1); assertThat(s.delete(1)).isEqualTo(1); verify(m).delete(1); }
 private ParkingService service(ParkingMapper m){ ParkingService s=new ParkingService(); ReflectionTestUtils.setField(s,"parkingMapper",m); return s; }
}
