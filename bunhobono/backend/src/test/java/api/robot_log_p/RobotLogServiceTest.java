package api.robot_log_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RobotLogServiceTest {
    @Test
    @DisplayName("UT-BE-ROBOTLOG-001 | 로봇 로그 목록 조건을 Mapper에 전달한다")
    void list_returnsMapperResult() {
        RobotLogMapper mapper = mock(RobotLogMapper.class);
        RobotLogService service = service(mapper);
        List<RobotLogDTO> expected = List.of(new RobotLogDTO());
        when(mapper.list(1L, 2L)).thenReturn(expected);
        assertSame(expected, service.list(1L, 2L));
    }

    @Test
    @DisplayName("UT-BE-ROBOTLOG-002 | 이벤트 ID가 없으면 UUID를 생성해 저장한다")
    void insert_generatesMissingEventId() {
        RobotLogMapper mapper = mock(RobotLogMapper.class);
        RobotLogService service = service(mapper);
        RobotLogDTO dto = new RobotLogDTO();
        when(mapper.insert(dto)).thenReturn(1);
        assertEquals(1, service.insert(dto));
        assertFalse(dto.getSourceEventId().isBlank());
        verify(mapper).insert(dto);
    }

    @Test
    @DisplayName("UT-BE-ROBOTLOG-003 | 기존 이벤트 ID는 변경하지 않고 저장한다")
    void insert_preservesExistingEventId() {
        RobotLogMapper mapper = mock(RobotLogMapper.class);
        RobotLogService service = service(mapper);
        RobotLogDTO dto = new RobotLogDTO();
        dto.setSourceEventId("event-1");
        when(mapper.insert(dto)).thenReturn(1);

        assertEquals(1, service.insert(dto));
        assertEquals("event-1", dto.getSourceEventId());
        verify(mapper).insert(dto);
    }

    private static RobotLogService service(RobotLogMapper mapper) {
        RobotLogService service = new RobotLogService();
        ReflectionTestUtils.setField(service, "robotLogMapper", mapper);
        return service;
    }
}
