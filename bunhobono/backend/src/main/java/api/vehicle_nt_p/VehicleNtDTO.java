package api.vehicle_nt_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VehicleNtDTO {

    // 알림 고유번호
    private Integer vehicleNtNo;

    // 알림을 받는 입주민
    private Integer recipientMemberNo;

    // 승인 또는 반려를 처리한 관리자
    // 자동 알림이면 null
    private Integer senderMemberNo;

    // 관련 차량
    // 신청이 삭제되면 null이 될 수 있음
    private Integer vehicleCarNo;

    // 관련 입출차 로그
    // 입차하지 않은 알림이면 null
    private Integer carLogNo;

    // 관련 차량이 삭제되어도 보관되는 차량번호
    private String snapshotCarNo;

    // 알림 종류
    // ADMIN_APPROVED: 관리자 승인
    // ADMIN_REJECTED: 관리자 직접 반려
    // APPROVAL_TIMEOUT: 승인시간 초과
    // NO_ENTRY_EXPIRED: 승인 후 미입차 만기
    // VISIT_OVERDUE: 방문 주차시간 초과
    // VISIT_OVERDUE_EXIT: 시간 초과 후 출차
    private String notificationType;

    // 입주민에게 보여줄 알림 내용
    private String message;

    // 등록시간을 초과한 분
    // 시간 초과 알림이 아니면 null
    private Integer overdueMinutes;

    // 알림 생성시간
    private LocalDateTime createdAt;

    // 알림 확인시간
    // 아직 읽지 않았으면 null
    private LocalDateTime readAt;

    // member 테이블 JOIN으로 가져오는 관리자 이름
    // 자동 알림이면 null
    private String senderName;
}