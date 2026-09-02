package api.kiosk_p;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import java.util.List; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class KioskServiceTest { @Mock KioskMapper mapper; @InjectMocks KioskService service;
 @Test @DisplayName("UT-BE-KIOSK-001 | 전체 키오스크 목록을 반환한다") void list(){ List<KioskDTO> expected=List.of(new KioskDTO()); when(mapper.list()).thenReturn(expected); assertThat(service.list()).isSameAs(expected); verify(mapper).list(); }
 @Test @DisplayName("UT-BE-KIOSK-002 | 키오스크 번호로 설치 주차장 정보를 반환한다") void findByKioskNo(){ KioskDTO expected=new KioskDTO(); when(mapper.findByKioskNo(1)).thenReturn(expected); assertThat(service.findByKioskNo(1)).isSameAs(expected); verify(mapper).findByKioskNo(1); }
 @Test @DisplayName("UT-BE-KIOSK-003 | 키오스크 삭제 결과를 반환한다") void delete(){ when(mapper.delete(1)).thenReturn(1); assertThat(service.delete(1)).isEqualTo(1); verify(mapper).delete(1); }
 @Test @DisplayName("UT-BE-KIOSK-004 | 신규 키오스크 등록 결과를 반환한다") void signUp(){ KioskDTO d=new KioskDTO(); when(mapper.signUp(d)).thenReturn(1); assertThat(service.signUp(d)).isEqualTo(1); verify(mapper).signUp(d); }
 @Test @DisplayName("UT-BE-KIOSK-005 | Mapper 처리 실패 건수 0을 그대로 반환한다") void mapperFailure(){ when(mapper.delete(9)).thenReturn(0); assertThat(service.delete(9)).isZero(); }
}
