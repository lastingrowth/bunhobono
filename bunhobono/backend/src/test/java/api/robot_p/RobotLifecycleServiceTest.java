package api.robot_p;

import api.parking_space_p.ParkingSpaceService;
import api.robot_log_p.*;
import api.robot_task_p.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RobotLifecycleServiceTest {
    @Mock RobotMapper robotMapper;
    @Mock RobotTaskMapper robotTaskMapper;
    @Mock RobotTaskService robotTaskService;
    @Mock ParkingSpaceService parkingSpaceService;
    @Mock RobotLogService robotLogService;
    @InjectMocks RobotService service;

    private RobotTaskDTO task(String phase) {
        RobotTaskDTO t=new RobotTaskDTO();
        t.setTaskNo(1L); t.setSetNo(1); t.setCarLogNo(3);
        t.setPickupSpaceNo(11L); t.setDropoffSpaceNo(12L);
        t.setPickupSpaceCode("B1-IN1-01"); t.setDropoffSpaceCode("B1-P21");
        t.setTaskType("PARK_IN"); t.setTaskStatus("RUNNING"); t.setTaskPhase(phase);
        return t;
    }
    private RobotDTO robot(double battery) {
        RobotDTO r=new RobotDTO(); r.setRobotNo(1L); r.setBatteryLevel(BigDecimal.valueOf(battery)); return r;
    }

    @ParameterizedTest(name="{index}: {0} -> {1}")
    @CsvSource({"TRAFFIC_WAIT_EMPTY,MOVING_TO_PICKUP", "MOVING_TO_PICKUP,PICKUP_POSITIONING", "PICKUP_POSITIONING,LIFTING", "LIFTING,MOVING_TO_DROPOFF", "TRAFFIC_WAIT_LOADED,MOVING_TO_DROPOFF", "MOVING_TO_DROPOFF,DROPOFF_POSITIONING", "DROPOFF_POSITIONING,LOWERING", "LOWERING,RETURNING_HOME", "TRAFFIC_WAIT_RETURN,RETURNING_HOME"})
    @DisplayName("UT-BE-ROBOT-018 | 정상 로봇 작업의 각 단계가 진행되고 센서 로그가 저장된다")
    void advancesEveryPhase(String from,String to) {
        // Arrange: 시간 완료, 충돌 없는 통로, 정상 저장을 구성한다.
        RobotTaskDTO t=task(from);
        when(robotTaskMapper.findRunningTasks()).thenReturn(List.of(t));
        when(robotTaskMapper.updateRunning(1,to)).thenReturn(1);
        if(to.equals("MOVING_TO_DROPOFF")) when(parkingSpaceService.releaseCarLog(11L,3)).thenReturn(1);
        if(from.equals("LOWERING")) when(parkingSpaceService.assignCarLog(12L,3)).thenReturn(1);
        when(robotMapper.findBySetNo(1)).thenReturn(List.of(robot(80)));
        when(robotLogService.insert(any())).thenReturn(1);
        // Act / Assert: 단계, 배터리 범위, 로그와 상태 갱신을 검증한다.
        assertThat(service.processRunningTasks()).isEqualTo(1);
        assertThat(t.getTaskPhase()).isEqualTo(to);
        ArgumentCaptor<RobotLogDTO> logs=ArgumentCaptor.forClass(RobotLogDTO.class);
        verify(robotLogService).insert(logs.capture());
        RobotLogDTO log=logs.getValue();
        assertThat(log.getTaskNo()).isEqualTo(1L);
        assertThat(log.getTaskPhase()).isEqualTo(to);
        assertThat(log.getSourceEventId()).isNotBlank();
        assertThat(log.getBatteryLevel()).isBetween(new BigDecimal("79.75"),new BigDecimal("79.95"));
        assertThat(log.getDriveMotorTemperatureC()).isBetween(new BigDecimal("40"),new BigDecimal("63"));
        assertThat(log.getRobotStatus()).isEqualTo("WORKING");
        verify(robotMapper).updateState(argThat(r -> r.getBatteryLevel().equals(log.getBatteryLevel())));
    }

    @ParameterizedTest(name="{index}: battery={0}, status={1}")
    @CsvSource({"0,LOW_BATTERY", "20,LOW_BATTERY", "40,CHARGING", "80,STANDBY"})
    @DisplayName("UT-BE-ROBOT-019 | 복귀 완료 시 작업 종료와 배터리 수준별 로봇 상태를 기록한다")
    void completes(double battery,String status) {
        RobotTaskDTO t=task("RETURNING_HOME");
        when(robotTaskMapper.findRunningTasks()).thenReturn(List.of(t));
        when(robotTaskMapper.complete(1)).thenReturn(1);
        when(robotMapper.updateSetStatus(1,"WORKING","CHARGING")).thenReturn(2);
        when(robotMapper.findBySetNo(1)).thenReturn(List.of(robot(battery)));
        when(robotLogService.insert(any())).thenReturn(1);
        assertThat(service.processRunningTasks()).isEqualTo(1);
        assertThat(t.getTaskStatus()).isEqualTo("COMPLETED");
        verify(robotLogService).insert(argThat(l -> l.getTaskPhase().equals("COMPLETED") && l.getRobotStatus().equals(status) && l.getBatteryLevel().signum()>=0));
    }

    @Test @DisplayName("UT-BE-ROBOT-020 | 대기 작업을 두 로봇 세트에 배정하고 실행을 시작한다")
    void dispatches() {
        RobotTaskDTO t=task("WAITING");
        when(robotTaskMapper.findNextWaitingTask()).thenReturn(t,(RobotTaskDTO)null);
        when(robotTaskMapper.detail(1)).thenReturn(t);
        when(robotMapper.findAvailableSetNo(1)).thenReturn(1);
        when(robotMapper.startSet(1)).thenReturn(2);
        when(robotTaskMapper.assignSet(1,1)).thenReturn(1);
        when(robotTaskMapper.updateRunning(1,"MOVING_TO_PICKUP")).thenReturn(1);
        assertThat(service.dispatchWaitingTasks()).isEqualTo(1);
        assertThat(t.getTaskPhase()).isEqualTo("MOVING_TO_PICKUP");
        assertThat(t.getTaskStatus()).isEqualTo("RUNNING");
        verify(robotMapper).findBySetNo(1);
    }

    @Test @DisplayName("UT-BE-ROBOT-021 | 같은 통로의 다른 이동 작업이 있으면 적재 차량을 대기시킨다")
    void laneConflict() {
        RobotTaskDTO t=task("LIFTING"), other=task("MOVING_TO_PICKUP"); other.setTaskNo(2L);
        when(robotTaskMapper.findRunningTasks()).thenReturn(List.of(t),List.of(other));
        when(robotTaskMapper.updateRunning(1,"TRAFFIC_WAIT_LOADED")).thenReturn(1);
        assertThat(service.processRunningTasks()).isEqualTo(1);
        assertThat(t.getTaskPhase()).isEqualTo("TRAFFIC_WAIT_LOADED");
        verifyNoInteractions(parkingSpaceService);
    }

    @ParameterizedTest(name="{index}: {0}")
    @CsvSource({"UPDATE", "RELEASE", "ASSIGN", "COMPLETE", "FINISH"})
    @DisplayName("UT-BE-ROBOT-022 | 단계 변경·공간 이동·종료 저장 실패는 충돌 오류로 반환한다")
    void persistenceFailures(String operation) {
        RobotTaskDTO t=task(operation.equals("ASSIGN")?"LOWERING":operation.equals("COMPLETE")||operation.equals("FINISH")?"RETURNING_HOME":"LIFTING");
        when(robotTaskMapper.findRunningTasks()).thenReturn(List.of(t));
        if(operation.equals("RELEASE")) when(robotTaskMapper.updateRunning(1,"MOVING_TO_DROPOFF")).thenReturn(1);
        if(operation.equals("FINISH")) when(robotTaskMapper.complete(1)).thenReturn(1);
        assertThatThrownBy(service::processRunningTasks).isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
          .satisfies(e -> assertThat(((org.springframework.web.server.ResponseStatusException)e).getStatusCode().value()).isEqualTo(409));
        verifyNoInteractions(robotLogService);
    }

    @Test @DisplayName("UT-BE-ROBOT-023 | 로봇 로그 저장 실패 시 해당 상태 갱신을 건너뛴다")
    void failedLogDoesNotUpdateState() {
        RobotTaskDTO t=task("PICKUP_POSITIONING");
        when(robotTaskMapper.findRunningTasks()).thenReturn(List.of(t));
        when(robotTaskMapper.updateRunning(1,"LIFTING")).thenReturn(1);
        when(robotMapper.findBySetNo(1)).thenReturn(List.of(robot(80)));
        assertThat(service.processRunningTasks()).isEqualTo(1);
        verify(robotLogService).insert(any());
        verify(robotMapper,never()).updateState(any());
    }
}
