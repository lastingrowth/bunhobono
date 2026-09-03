package api.predictive_maintenance_p;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpServerErrorException;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PredictiveMaintenanceClientTest {
    PredictiveMaintenanceClient service;
    MockRestServiceServer server;
    @BeforeEach void setUp() {
        RestClient.Builder builder=RestClient.builder().baseUrl("http://test.invalid");
        server=MockRestServiceServer.bindTo(builder).build();
        service=new PredictiveMaintenanceClient("http://test.invalid");
        ReflectionTestUtils.setField(service,"restClient",builder.build());
    }
    static Stream<Arguments> endpoints() {
        return Stream.of(
            Arguments.of("predictNextGate","/demo/predictive-maintenance/gate/next","single"),
            Arguments.of("predictNextGates","/demo/predictive-maintenance/gate/next-all","list"),
            Arguments.of("predictNextCamera","/demo/predictive-maintenance/camera/next","single"),
            Arguments.of("predictNextCameras","/demo/predictive-maintenance/camera/next-all","list"),
            Arguments.of("predictNextRobot","/demo/predictive-maintenance/robot/next","single"),
            Arguments.of("predictNextRobots","/demo/predictive-maintenance/robot/next-all","list"),
            Arguments.of("completeCameraAction","/demo/predictive-maintenance/camera/ANT-003/complete-action","void"),
            Arguments.of("completeGateAction","/demo/predictive-maintenance/gate/GATE-03/complete-action","void"),
            Arguments.of("completeRobotAction","/demo/predictive-maintenance/robot/ROBOT_03/complete-action","void")
        );
    }
    @ParameterizedTest(name="[{index}] {0}") @MethodSource("endpoints")
    @DisplayName("UT-BE-PDMCLIENT-001 | 예측·조치 9개 메서드의 POST 경로와 응답 변환을 검증한다")
    void routesAndResponses(String method,String path,String shape) throws Exception {
        String json="{\"equipment_no\":\"TEST-03\"}";
        server.expect(requestTo("http://test.invalid"+path)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(shape.equals("list")?"["+json+"]":shape.equals("void")?"":json,MediaType.APPLICATION_JSON));
        Object result=invoke(method,shape);
        if(shape.equals("single")) assertEquals("TEST-03",((PredictiveMaintenanceResponseDTO)result).getEquipmentNo());
        if(shape.equals("list")) assertEquals("TEST-03",((PredictiveMaintenanceResponseDTO)((List<?>)result).get(0)).getEquipmentNo());
        server.verify();
    }
    @ParameterizedTest(name="[{index}] {0}") @MethodSource("endpoints")
    @DisplayName("UT-BE-PDMCLIENT-002 | FastAPI 503 응답을 호출자에게 전파한다")
    void propagatesServerFailure(String method,String path,String shape) {
        server.expect(requestTo("http://test.invalid"+path)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        java.lang.reflect.InvocationTargetException error=assertThrows(java.lang.reflect.InvocationTargetException.class,()->invoke(method,shape));
        assertInstanceOf(HttpServerErrorException.class,error.getCause());
        server.verify();
    }
    private Object invoke(String method,String shape) throws Exception {
        return shape.equals("void")?service.getClass().getMethod(method,int.class).invoke(service,3):service.getClass().getMethod(method).invoke(service);
    }
}
