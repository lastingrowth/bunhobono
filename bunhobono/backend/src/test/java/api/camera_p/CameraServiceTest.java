package api.camera_p;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CameraServiceTest {
    @Mock CameraMapper mapper;
    @InjectMocks CameraService service;
    CameraDTO dto;
    @BeforeEach void setUp(){ dto=new CameraDTO(); }

    @Test @DisplayName("UT-BE-CAMERA-001 | 조회 조건을 Mapper에 전달하고 카메라 목록을 반환한다")
    void listservice(){ List<CameraDTO> expected=List.of(dto); when(mapper.list(dto)).thenReturn(expected); assertThat(service.listservice(dto)).isSameAs(expected); verify(mapper).list(dto); }
    @Test @DisplayName("UT-BE-CAMERA-002 | 신규 카메라 등록 결과를 반환한다")
    void signUp(){ when(mapper.insert(dto)).thenReturn(1); assertThat(service.signUp(dto)).isEqualTo(1); verify(mapper).insert(dto); }
    @Test @DisplayName("UT-BE-CAMERA-003 | 카메라 삭제 번호와 처리 결과를 전달한다")
    void delete(){ when(mapper.delete(7)).thenReturn(1); assertThat(service.delete(7)).isEqualTo(1); verify(mapper).delete(7); }
    @Test @DisplayName("UT-BE-CAMERA-004 | 수정할 카메라 정보와 처리 결과를 전달한다")
    void update(){ when(mapper.update(dto)).thenReturn(1); assertThat(service.update(dto)).isEqualTo(1); verify(mapper).update(dto); }
    @Test @DisplayName("UT-BE-CAMERA-005 | Mapper 처리 실패 건수 0을 변경하지 않고 반환한다")
    void mapperFailure(){ when(mapper.insert(dto)).thenReturn(0); assertThat(service.signUp(dto)).isZero(); verify(mapper).insert(dto); }
}
