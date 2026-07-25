package api.vehicle_nt_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VehicleNtMapper {

    // JWT loginId를 기준으로 입주민 본인의 알림만 최신순 조회
    @Select("""
        SELECT
            nt.vehicle_nt_no,
            nt.recipient_member_no,
            nt.sender_member_no,
            nt.vehicle_car_no,
            nt.car_log_no,
            nt.snapshot_car_no,
            nt.notification_type,
            nt.message,
            nt.overdue_minutes,
            nt.created_at,
            nt.read_at,
            sender.mem_name AS sender_name

        FROM vehicle_nt nt

        JOIN member recipient
            ON nt.recipient_member_no = recipient.member_no

        LEFT JOIN member sender
            ON nt.sender_member_no = sender.member_no

        WHERE recipient.login_id = #{loginId}

        ORDER BY
            nt.created_at DESC,
            nt.vehicle_nt_no DESC
    """)
    List<VehicleNtDTO> list(
            @Param("loginId") String loginId
    );


    // 로그인한 입주민의 읽지 않은 차량 알림을 모두 읽음 처리
    // 알림을 삭제하지 않고 read_at에 읽은 시간을 기록한다.
    @Update("""
    UPDATE vehicle_nt nt
    SET read_at = CURRENT_TIMESTAMP
    FROM member recipient
    WHERE nt.recipient_member_no = recipient.member_no
      AND recipient.login_id = #{loginId}
      AND nt.read_at IS NULL
    """)
    int markAllRead(@Param("loginId") String loginId);


    // WAITING 방문차량 승인과 승인 알림 저장을 한 번에 처리한다.
    @Insert("""
    WITH sender AS (
        SELECT member_no
        FROM member
        WHERE login_id = #{adminLoginId}
    ),

    approved AS (
        UPDATE vehicle_car vc
        SET vehicle_status = 'APPROVED',
            approved_at = CURRENT_TIMESTAMP
        FROM sender
        WHERE vc.vehicle_car_no = #{vehicleCarNo}
          AND vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'WAITING'
          AND vc.member_no IS NOT NULL

        RETURNING
            vc.vehicle_car_no,
            vc.member_no,
            vc.car_no,
            sender.member_no AS sender_member_no
    )

    INSERT INTO vehicle_nt (
        recipient_member_no,
        sender_member_no,
        vehicle_car_no,
        snapshot_car_no,
        notification_type,
        message
    )
    SELECT
        approved.member_no,
        approved.sender_member_no,
        approved.vehicle_car_no,
        approved.car_no,
        'ADMIN_APPROVED',
        '방문차량 신청이 승인되었습니다.'
    FROM approved
    """)
    int approveRequest(
            @Param("vehicleCarNo") int vehicleCarNo,
            @Param("adminLoginId") String adminLoginId
    );


    // 로그인한 입주민 본인의 일반 알림만 직접 삭제한다.
    // 초과 후 출차 알림은 직접 삭제할 수 없다.
    @Delete("""
    DELETE FROM vehicle_nt nt
    USING member recipient
    WHERE nt.recipient_member_no = recipient.member_no
      AND recipient.login_id = #{loginId}
      AND nt.vehicle_nt_no = #{vehicleNtNo}
      AND nt.notification_type <> 'VISIT_OVERDUE_EXIT'
    """)
    int deleteNotification(
            @Param("loginId") String loginId,
            @Param("vehicleNtNo") int vehicleNtNo
    );


    // 관리자가 직접 방문차량 신청 반려
    // 신청을 삭제하면서 차량번호와 신청 회원을 확보한 뒤 알림을 저장한다.
    // 삭제되는 차량의 외래키 대신 snapshot_car_no를 알림에 보관한다.
    @Insert("""
    WITH sender AS (
        SELECT member_no
        FROM member
        WHERE login_id = #{adminLoginId}
    ),

    rejected AS (
        DELETE FROM vehicle_car vc
        USING sender
        WHERE vc.vehicle_car_no = #{vehicleCarNo}
          AND vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'WAITING'
          AND vc.member_no IS NOT NULL

        RETURNING
            vc.member_no,
            vc.car_no,
            sender.member_no AS sender_member_no
    )

    INSERT INTO vehicle_nt (
        recipient_member_no,
        sender_member_no,
        snapshot_car_no,
        notification_type,
        message
    )
    SELECT
        rejected.member_no,
        rejected.sender_member_no,
        rejected.car_no,
        'ADMIN_REJECTED',
        #{rejectReason}
    FROM rejected
    """)
    int rejectRequest(
            @Param("vehicleCarNo") int vehicleCarNo,
            @Param("adminLoginId") String adminLoginId,
            @Param("rejectReason") String rejectReason
    );


    // 예상 방문시간에서 1시간이 지날 때까지 승인되지 않은 신청
    // 신청을 삭제하면서 차량번호와 신청 회원을 확보한 뒤
    // 승인시간 초과 알림을 저장한다.
    @Insert("""
    WITH timed_out AS (
        DELETE FROM vehicle_car vc
        WHERE vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'WAITING'
          AND vc.member_no IS NOT NULL
          AND vc.start_date IS NOT NULL
          AND CURRENT_TIMESTAMP >
              vc.start_date + INTERVAL '1 hour'

        RETURNING
            vc.member_no,
            vc.car_no
    )

    INSERT INTO vehicle_nt (
        recipient_member_no,
        snapshot_car_no,
        notification_type,
        message
    )
    SELECT
        timed_out.member_no,
        timed_out.car_no,
        'APPROVAL_TIMEOUT',
        '승인 처리 가능 시간이 지나 방문차량 신청이 자동으로 취소되었습니다. 방문이 필요한 경우 다시 신청해 주세요.'
    FROM timed_out
    """)
    int rejectApprovalTimeoutRequests();


    // 승인됐지만 예상 방문시간 + 1시간까지 입차하지 않은 차량
    // 같은 차량에는 한 번만 알림을 저장한다.
    @Insert("""
        INSERT INTO vehicle_nt (
            recipient_member_no,
            vehicle_car_no,
            snapshot_car_no,
            notification_type,
            message
        )
        SELECT
            vc.member_no,
            vc.vehicle_car_no,
            vc.car_no,
            'NO_ENTRY_EXPIRED',
            '승인된 방문차량이 예상 방문시간 내에 입차하지 않아 만기 처리되었습니다.'
        FROM vehicle_car vc

        WHERE vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'APPROVED'
          AND vc.member_no IS NOT NULL
          AND vc.start_date IS NOT NULL
          AND CURRENT_TIMESTAMP >
              vc.start_date + INTERVAL '1 hour'

          AND NOT EXISTS (
                SELECT 1
                FROM car_log cl
                WHERE cl.vehicle_car_no = vc.vehicle_car_no
          )

          AND NOT EXISTS (
                SELECT 1
                FROM vehicle_nt nt
                WHERE nt.vehicle_car_no = vc.vehicle_car_no
                  AND nt.notification_type = 'NO_ENTRY_EXPIRED'
          )
    """)
    int createNoEntryExpiredNotifications();


    // 입차 후 등록시간 + 30분을 초과하여 주차 중인 방문차량
    // 첫 알림 이후 출차하지 않으면 12시간마다 다시 알림을 저장한다.
    @Insert("""
        INSERT INTO vehicle_nt (
            recipient_member_no,
            vehicle_car_no,
            car_log_no,
            snapshot_car_no,
            notification_type,
            message,
            overdue_minutes
        )
        SELECT
            vc.member_no,
            vc.vehicle_car_no,
            cl.car_log_no,
            vc.car_no,
            'VISIT_OVERDUE',

            CONCAT(
                '방문차량 등록시간이 ',
                FLOOR(
                    EXTRACT(
                        EPOCH FROM (
                            CURRENT_TIMESTAMP
                            - expiry.real_end_date
                        )
                    ) / 60
                )::INTEGER,
                '분 초과되었습니다.'
            ),

            FLOOR(
                EXTRACT(
                    EPOCH FROM (
                        CURRENT_TIMESTAMP
                        - expiry.real_end_date
                    )
                ) / 60
            )::INTEGER

        FROM vehicle_car vc

        JOIN LATERAL (
            SELECT
                car_log.car_log_no,
                car_log.in_time
            FROM car_log
            WHERE car_log.vehicle_car_no =
                  vc.vehicle_car_no
              AND car_log.out_time IS NULL
            ORDER BY car_log.in_time DESC
            LIMIT 1
        ) cl ON TRUE

        CROSS JOIN LATERAL (
            SELECT
                cl.in_time
                + (vc.end_date - vc.start_date)
                + INTERVAL '30 minutes'
                    AS real_end_date
        ) expiry

        WHERE vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'APPROVED'
          AND vc.member_no IS NOT NULL
          AND vc.start_date IS NOT NULL
          AND vc.end_date IS NOT NULL
          AND CURRENT_TIMESTAMP >
              expiry.real_end_date

          AND NOT EXISTS (
                SELECT 1
                FROM vehicle_nt nt
                WHERE nt.vehicle_car_no =
                      vc.vehicle_car_no
                  AND nt.notification_type =
                      'VISIT_OVERDUE'
                  AND nt.created_at >
                      CURRENT_TIMESTAMP
                      - INTERVAL '12 hours'
          )
    """)
    int createVisitOverdueNotifications();


    // 실제 만기시간을 초과한 뒤 출차한 방문차량
    // 입출차 로그 한 건마다 한 번만 알림을 저장한다.
    @Insert("""
        INSERT INTO vehicle_nt (
            recipient_member_no,
            vehicle_car_no,
            car_log_no,
            snapshot_car_no,
            notification_type,
            message,
            overdue_minutes
        )
        SELECT
            vc.member_no,
            vc.vehicle_car_no,
            cl.car_log_no,
            vc.car_no,
            'VISIT_OVERDUE_EXIT',

            CONCAT(
                '방문차량이 등록시간보다 ',
                FLOOR(
                    EXTRACT(
                        EPOCH FROM (
                            cl.out_time
                            - expiry.real_end_date
                        )
                    ) / 60
                )::INTEGER,
                '분 초과하여 출차했습니다.'
            ),

            FLOOR(
                EXTRACT(
                    EPOCH FROM (
                        cl.out_time
                        - expiry.real_end_date
                    )
                ) / 60
            )::INTEGER

        FROM vehicle_car vc

        JOIN car_log cl
            ON cl.vehicle_car_no =
               vc.vehicle_car_no

        CROSS JOIN LATERAL (
            SELECT
                cl.in_time
                + (vc.end_date - vc.start_date)
                + INTERVAL '30 minutes'
                    AS real_end_date
        ) expiry

        WHERE vc.vehicle_type = 'visit'
          AND vc.vehicle_status = 'APPROVED'
          AND vc.member_no IS NOT NULL
          AND vc.start_date IS NOT NULL
          AND vc.end_date IS NOT NULL
          AND cl.in_time IS NOT NULL
          AND cl.out_time IS NOT NULL
          AND cl.out_time >
              expiry.real_end_date

          AND NOT EXISTS (
                SELECT 1
                FROM vehicle_nt nt
                WHERE nt.car_log_no =
                      cl.car_log_no
                  AND nt.notification_type =
                      'VISIT_OVERDUE_EXIT'
          )
    """)
    int createVisitOverdueExitNotifications();


    // 읽은 지 7일이 지난 일반 알림만 자동 삭제
    // 초과 후 출차 알림은 자동 삭제하지 않는다.
    @Delete("""
    DELETE FROM vehicle_nt
    WHERE read_at IS NOT NULL
      AND read_at < CURRENT_TIMESTAMP - INTERVAL '7 days'
      AND notification_type <> 'VISIT_OVERDUE_EXIT'
    """)
    int deleteOldCompletedNotifications();
}
