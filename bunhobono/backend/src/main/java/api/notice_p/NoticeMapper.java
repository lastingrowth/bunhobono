package api.notice_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoticeMapper {

    @Select("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY n.detect_at DESC) AS display_no,
            n.notice_no,
            COALESCE(n.car_log_no, n.snapshot_car_log_no) AS car_log_no,
            COALESCE(vc.car_no, n.snapshot_registered_car_no) AS registered_car_no,
            COALESCE(cd.car_no, cl.snapshot_car_no, n.snapshot_captured_car_no) AS captured_car_no,
            COALESCE(
                CASE
                    WHEN cl.car_log_no IS NULL THEN NULL
                    WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN'
                    WHEN vc.vehicle_type = 'visit' THEN 'VISIT'
                    ELSE 'REGISTERED'
                END,
                n.snapshot_car_kind
            ) AS car_kind,
            n.detect_at,
            n.stay_days,
            n.alert_stat,
            n.handled_by_member_no,
            m.mem_name AS handled_by_member_name,
            n.handled_at,
            COALESCE(cl.in_time, n.snapshot_in_time) AS in_time,
            cl.out_time AS out_time,
            COALESCE(p.parking_name, n.snapshot_parking_name) AS parking_name
        FROM notice n
        LEFT JOIN car_log cl ON n.car_log_no = cl.car_log_no
        LEFT JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no
        LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no
        LEFT JOIN gate g ON cl.in_gate_no = g.gate_no
        LEFT JOIN parking p ON g.parking_no = p.parking_no
        LEFT JOIN member m ON n.handled_by_member_no = m.member_no
        ORDER BY n.detect_at DESC
    """)
    List<NoticeDTO> list();

    @Update("""
        UPDATE notice
        SET alert_stat = #{alertStat},
            handled_by_member_no = CASE
                WHEN #{alertStat} = 'Unresolved' THEN NULL
                ELSE ( SELECT member_no FROM member WHERE login_id = #{handledByMemberName} )
            END,
            handled_at = CASE
                WHEN #{alertStat} = 'Unresolved' THEN NULL
                ELSE CURRENT_TIMESTAMP
            END
        WHERE notice_no = #{noticeNo}
    """)
    int status(NoticeDTO dto);

    //자동삭제
    @Select("""
    SELECT notice_no
    FROM notice
    WHERE alert_stat = 'Resolved'
      AND handled_at IS NOT NULL
      AND handled_at < NOW() - INTERVAL '1 month'
""")
    List<Integer> findResolvedNoticeNosForTrash();
    //삭제기능 1 minute 테스트용

    //carlog에서 notice쪽으로 변경시
    @Insert("INSERT INTO notice " +
            "(car_log_no, detect_at, stay_days, alert_stat, snapshot_car_log_no, " +
            "snapshot_registered_car_no, snapshot_captured_car_no, snapshot_car_kind, " +
            "snapshot_parking_name, snapshot_in_time) " +

            "SELECT cl.car_log_no, NOW(), " +
            "GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (NOW() - cl.in_time)) / 86400)::INT), " +
            "'Unresolved', cl.car_log_no, vc.car_no, " +
            "COALESCE(cd.car_no, cl.snapshot_car_no), " +
            "CASE WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN' " +
            "WHEN vc.vehicle_type = 'visit' THEN 'VISIT' " +
            "ELSE 'REGISTERED' END, " +
            "p.parking_name, cl.in_time " +

            "FROM car_log cl " +
            "LEFT JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no " +
            "LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no " +
            "LEFT JOIN gate g ON cl.in_gate_no = g.gate_no " +
            "LEFT JOIN parking p ON g.parking_no = p.parking_no " +

            "WHERE cl.out_time IS NULL " +
            "AND COALESCE(vc.car_no, cd.car_no, cl.snapshot_car_no) IS NOT NULL " +
            "AND COALESCE(vc.car_no, cd.car_no, cl.snapshot_car_no) != '' " +

//            "AND (" +
//            "(cl.vehicle_car_no IS NULL " +
//            "AND cl.in_time <= NOW() - INTERVAL '1 day') " +
//            "OR " +
//            "(vc.vehicle_status = 'EXPIRED' " +
//            "AND vc.end_date IS NOT NULL " +
//            "AND vc.end_date <= NOW() - INTERVAL '1 day')" +
//            ") " +
            "AND (" +

                // 미등록차량이 하루 이상 주차 중
                "(cl.vehicle_car_no IS NULL " +
                "AND cl.in_time <= NOW() - INTERVAL '1 day') " +

                "OR " +

                // 승인된 등록차량 또는 방문차량이 만기 후 하루 이상 미출차
                "(vc.vehicle_status = 'APPROVED' AND (" +

                // 일반 등록차량: 차량 등록 만기일 기준
                "(vc.vehicle_type = 'normal' " +
                "AND vc.end_date IS NOT NULL " +
                "AND vc.end_date <= NOW() - INTERVAL '1 day') " +

                "OR " +

                // 방문차량: 입차시간 + 신청시간 + 30분 기준
                "(vc.vehicle_type = 'visit' " +
                "AND vc.start_date IS NOT NULL " +
                "AND vc.end_date IS NOT NULL " +
                "AND cl.in_time IS NOT NULL " +
                "AND cl.in_time + (vc.end_date - vc.start_date) " +
                "+ INTERVAL '30 minutes' <= NOW() - INTERVAL '1 day')" +

                "))" +

            ") " +


            "AND NOT EXISTS (" +
            "SELECT 1 FROM notice n " +
            "WHERE COALESCE(n.car_log_no, n.snapshot_car_log_no) = cl.car_log_no" +
            ")")
    int createNoticesFromCarLog();
    // notice 테스트용 '1minute'


    //직접삭제 부분
    @Delete("DELETE FROM notice WHERE notice_no = #{noticeNo}")
    int delete(int noticeNo);

}
