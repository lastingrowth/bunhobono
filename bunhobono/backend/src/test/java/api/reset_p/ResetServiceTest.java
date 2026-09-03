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
    @DisplayName("UT-BE-RESET-002 | DB 초기화 다음 FastAPI 초기화 요청을 보내며 실제 자원은 사용하지 않는다")
    void resetsDatabaseThenFastApi() {
        DataSource dataSource=mock(DataSource.class);
        ResetService service=new ResetService(dataSource);
        org.springframework.web.client.RestClient.Builder builder=org.springframework.web.client.RestClient.builder().baseUrl("http://test.invalid");
        var server=org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        org.springframework.test.util.ReflectionTestUtils.setField(service,"fastApiClient",builder.build());
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://test.invalid/demo/reset"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess());
        try(var constructors=org.mockito.Mockito.mockConstruction(org.springframework.jdbc.datasource.init.ResourceDatabasePopulator.class)) {
            service.resetDemo();
            var populator=constructors.constructed().get(0);
            org.mockito.Mockito.verify(populator).setContinueOnError(false);
            org.mockito.Mockito.verify(populator).execute(dataSource);
            server.verify();
            org.mockito.Mockito.verifyNoInteractions(dataSource);
        }
    }

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
