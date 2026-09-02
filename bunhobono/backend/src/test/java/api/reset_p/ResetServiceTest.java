package api.reset_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResetServiceTest {

    @Test
    @DisplayName("UT-BE-RESET-001 | DB 초기화 실패 시 전체 데모 초기화를 즉시 중단한다")
    void resetDemo_stopsWhenDatabaseResetFails() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("test database unavailable"));

        ResetService service = new ResetService(dataSource);

        assertThatThrownBy(service::resetDemo)
                .hasRootCauseInstanceOf(SQLException.class)
                .hasRootCauseMessage("test database unavailable");
    }
}
