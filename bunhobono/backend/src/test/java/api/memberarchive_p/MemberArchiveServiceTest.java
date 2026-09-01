package api.memberarchive_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberArchiveServiceTest {
    @Test
    @DisplayName("UT-BE-MEMBERARCHIVE-001 | 전출 회원 이력 목록을 조회한다")
    void list_returnsMapperResult() {
        MemberArchiveMapper mapper = mock(MemberArchiveMapper.class);
        MemberArchiveService service = service(mapper);
        List<MemberArchiveDTO> expected = List.of(new MemberArchiveDTO());
        when(mapper.list()).thenReturn(expected);
        assertSame(expected, service.list());
    }

    @Test
    @DisplayName("UT-BE-MEMBERARCHIVE-002 | 전출 회원 이력을 삭제한다")
    void delete_removesArchive() {
        MemberArchiveMapper mapper = mock(MemberArchiveMapper.class);
        MemberArchiveService service = service(mapper);
        when(mapper.delete(1)).thenReturn(1);

        service.delete(1);

        verify(mapper).delete(1);
    }

    @Test
    @DisplayName("UT-BE-MEMBERARCHIVE-003 | 없는 전출 이력 삭제를 거부한다")
    void delete_rejectsMissingArchive() {
        MemberArchiveMapper mapper = mock(MemberArchiveMapper.class);
        MemberArchiveService service = service(mapper);
        when(mapper.delete(1)).thenReturn(0);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.delete(1));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private static MemberArchiveService service(MemberArchiveMapper mapper) {
        MemberArchiveService service = new MemberArchiveService();
        service.mapper = mapper;
        return service;
    }
}
