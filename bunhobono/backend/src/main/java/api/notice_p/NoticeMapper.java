package api.notice_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoticeMapper {

    // 관리자 알림 전체 조회
    @Select("""
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY detect_at DESC, notice_no DESC
            ) AS display_no,
            v.*
        FROM notice_detail v
        ORDER BY detect_at DESC, notice_no DESC
    """)
    List<NoticeDTO> list();

    // 관리자 알림 상세 조회
    @Select("""
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY detect_at DESC, notice_no DESC
            ) AS display_no,
            v.*
        FROM notice_detail v
        WHERE notice_no = #{noticeNo}
    """)
    NoticeDTO detail(@Param("noticeNo") int noticeNo);

    // 차량번호 검색
    @Select("""
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY detect_at DESC, notice_no DESC
            ) AS display_no,
            v.*
        FROM notice_detail v
        WHERE REPLACE(
            COALESCE(registered_car_no, ''),
            ' ',
            ''
        ) ILIKE CONCAT(
            '%',
            REPLACE(#{carNo}, ' ', ''),
            '%'
        )
        OR REPLACE(
            COALESCE(captured_car_no, ''),
            ' ',
            ''
        ) ILIKE CONCAT(
            '%',
            REPLACE(#{carNo}, ' ', ''),
            '%'
        )
        ORDER BY detect_at DESC, notice_no DESC
    """)
    List<NoticeDTO> search(@Param("carNo") String carNo);

    // 로그인 아이디로 관리자 회원번호 조회
    @Select("""
        SELECT member_no
        FROM member
        WHERE login_id = #{loginId}
          AND UPPER(TRIM(role)) = 'ADMIN'
        ORDER BY member_no DESC
        LIMIT 1
    """)
    Integer findAdminMemberNoByLoginId(
            @Param("loginId") String loginId
    );

    // 방문차량 또는 미등록차량은 출차 후 처리완료 가능
    @Update("""
        UPDATE notice n
        SET alert_stat = 'Resolved',
            handled_by_member_no = #{adminMemberNo},
            handled_at = CURRENT_TIMESTAMP
        WHERE n.notice_no = #{noticeNo}
          AND n.alert_stat = 'Unresolved'
          AND n.notice_type IN (
              'VISIT_OVERDUE',
              'UNKNOWN_OVERSTAY'
          )
          AND EXISTS (
              SELECT 1
              FROM car_log cl
              WHERE cl.car_log_no = n.car_log_no
                AND cl.out_time IS NOT NULL
          )
    """)
    int resolveAfterExit(
            @Param("noticeNo") int noticeNo,
            @Param("adminMemberNo") int adminMemberNo
    );

    // OCR 알림은 관리자가 확인 후 처리완료
    @Update("""
        UPDATE notice
        SET alert_stat = 'Resolved',
            handled_by_member_no = #{adminMemberNo},
            handled_at = CURRENT_TIMESTAMP
        WHERE notice_no = #{noticeNo}
          AND alert_stat = 'Unresolved'
          AND notice_type = 'OCR_REVIEW'
    """)
    int resolveOcrReview(
            @Param("noticeNo") int noticeNo,
            @Param("adminMemberNo") int adminMemberNo
    );

    // 입차기록 없는 출차 알림 처리완료
    @Update("""
        UPDATE notice
        SET alert_stat = 'Resolved',
            handled_by_member_no = #{adminMemberNo},
            handled_at = CURRENT_TIMESTAMP
        WHERE notice_type = 'EXIT_WITHOUT_ENTRY'
          AND alert_stat = 'Unresolved'
          AND (
              camera_data_no = #{cameraDataNo}
              OR snapshot_camera_data_no = #{cameraDataNo}
          )
    """)
    int resolveExitWithoutEntry(
            @Param("cameraDataNo") int cameraDataNo,
            @Param("adminMemberNo") int adminMemberNo
    );



    // 방문차량 초과 및 미등록차량 24시간 초과 알림 생성
    // 방문차량 초과 및 미등록차량 24시간 초과 알림 생성
    @Insert("""
    INSERT INTO notice (
        notice_type,
        car_log_no,
        camera_data_no,
        detect_at,
        due_at,
        alert_stat,
        snapshot_car_log_no,
        snapshot_camera_data_no,
        snapshot_registered_car_no,
        snapshot_captured_car_no,
        snapshot_car_kind,
        snapshot_parking_name,
        snapshot_in_time,
        snapshot_image_path,
        snapshot_confidence_score
    )
    SELECT
        notice_type,
        car_log_no,
        NULL,
        CURRENT_TIMESTAMP,
        due_at,
        'Unresolved',
        car_log_no,
        snapshot_camera_data_no,
        snapshot_registered_car_no,
        snapshot_captured_car_no,
        snapshot_car_kind,
        snapshot_parking_name,
        snapshot_in_time,
        snapshot_image_path,
        snapshot_confidence_score
    FROM notice_overstay
    WHERE CURRENT_TIMESTAMP >= due_at
    ON CONFLICT DO NOTHING
""")
    int createNoticesFromCarLog();

    // OCR 인식 실패 또는 신뢰도 미달 알림 생성
    @Insert("""
        INSERT INTO notice (
            notice_type,
            car_log_no,
            camera_data_no,
            detect_at,
            due_at,
            alert_stat,
            snapshot_car_log_no,
            snapshot_camera_data_no,
            snapshot_registered_car_no,
            snapshot_captured_car_no,
            snapshot_car_kind,
            snapshot_parking_name,
            snapshot_in_time,
            snapshot_image_path,
            snapshot_confidence_score
        )
        SELECT
            'OCR_REVIEW',
            NULL,
            cd.camera_data_no,
            CURRENT_TIMESTAMP,
            NULL,
            'Unresolved',
            NULL,
            cd.camera_data_no,
            vc.car_no,
            COALESCE(cd.ocr_car_no, cd.car_no),
            CASE
                WHEN cd.vehicle_car_no IS NULL THEN 'UNKNOWN'
                WHEN vc.vehicle_type = 'visit' THEN 'VISIT'
                ELSE 'REGISTERED'
            END,
            p.parking_name,
            NULL,
            cd.image_path,
            cd.confidence_score
        FROM camera_data cd
        LEFT JOIN vehicle_car vc
            ON cd.vehicle_car_no = vc.vehicle_car_no
        LEFT JOIN camera c
            ON cd.camera_no = c.camera_no
        LEFT JOIN gate g
            ON c.gate_no = g.gate_no
        LEFT JOIN parking p
            ON g.parking_no = p.parking_no
        WHERE cd.camera_data_no = #{cameraDataNo}
          AND cd.recognition_state IS NOT TRUE
        ON CONFLICT DO NOTHING
    """)
    int createOcrReviewNotice(
            @Param("cameraDataNo") int cameraDataNo
    );

    // 출차 게이트에 왔지만 열린 입차기록이 없는 차량 알림
    @Insert("""
        INSERT INTO notice (
            notice_type,
            car_log_no,
            camera_data_no,
            detect_at,
            due_at,
            alert_stat,
            snapshot_car_log_no,
            snapshot_camera_data_no,
            snapshot_registered_car_no,
            snapshot_captured_car_no,
            snapshot_car_kind,
            snapshot_parking_name,
            snapshot_in_time,
            snapshot_image_path,
            snapshot_confidence_score
        )
        SELECT
            'EXIT_WITHOUT_ENTRY',
            NULL,
            cd.camera_data_no,
            CURRENT_TIMESTAMP,
            NULL,
            'Unresolved',
            NULL,
            cd.camera_data_no,
            vc.car_no,
            COALESCE(cd.ocr_car_no, cd.car_no),
            CASE
                WHEN cd.vehicle_car_no IS NULL THEN 'UNKNOWN'
                WHEN vc.vehicle_type = 'visit' THEN 'VISIT'
                ELSE 'REGISTERED'
            END,
            p.parking_name,
            NULL,
            cd.image_path,
            cd.confidence_score
        FROM camera_data cd
        JOIN camera c
            ON cd.camera_no = c.camera_no
        JOIN gate g
            ON c.gate_no = g.gate_no
        JOIN parking p
            ON g.parking_no = p.parking_no
        LEFT JOIN vehicle_car vc
            ON cd.vehicle_car_no = vc.vehicle_car_no
        WHERE cd.camera_data_no = #{cameraDataNo}
          AND cd.recognition_state IS TRUE
          AND UPPER(TRIM(g.gate_type)) = 'OUT'
          AND NOT EXISTS (
              SELECT 1
              FROM car_log cl
              LEFT JOIN camera_data entry_cd
                  ON cl.camera_data_no = entry_cd.camera_data_no
              WHERE cl.out_time IS NULL
                AND (
                    (
                        cd.vehicle_car_no IS NOT NULL
                        AND cl.vehicle_car_no = cd.vehicle_car_no
                    )
                    OR
                    (
                        cd.vehicle_car_no IS NULL
                        AND cl.vehicle_car_no IS NULL
                        AND (
                            entry_cd.car_no = cd.car_no
                            OR entry_cd.ocr_car_no = cd.car_no
                            OR entry_cd.car_no = cd.ocr_car_no
                            OR entry_cd.ocr_car_no = cd.ocr_car_no
                            OR cl.snapshot_car_no = cd.car_no
                            OR cl.snapshot_car_no = cd.ocr_car_no
                        )
                    )
                )
          )
        ON CONFLICT DO NOTHING
    """)
    int createExitWithoutEntryNotice(
            @Param("cameraDataNo") int cameraDataNo
    );

    // 처리완료 후 3개월이 지난 알림번호 조회
    @Select("""
        SELECT notice_no
        FROM notice
        WHERE alert_stat = 'Resolved'
          AND handled_at IS NOT NULL
          AND handled_at <
              CURRENT_TIMESTAMP - INTERVAL '3 months'
        ORDER BY notice_no
    """)
    List<Integer> findResolvedNoticeNosForTrash();

    // 알림 삭제
    @Delete("""
        DELETE FROM notice
        WHERE notice_no = #{noticeNo}
    """)
    int delete(@Param("noticeNo") int noticeNo);
}