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

    @Delete("DELETE FROM notice WHERE notice_no = #{noticeNo}")
    int delete(int noticeNo);

    @Delete("""
        DELETE FROM notice
        WHERE alert_stat = 'Resolved'
          AND detect_at < NOW() - INTERVAL '1 year'
    """)
    int deleteResolvedNoticesAfterOneYear();

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

            "AND (" +
            "(cl.vehicle_car_no IS NULL " +
            "AND cl.in_time <= NOW() - INTERVAL '1 day') " +
            "OR " +
            "(vc.vehicle_status = 'EXPIRED' " +
            "AND vc.end_date IS NOT NULL " +
            "AND vc.end_date <= NOW() - INTERVAL '1 day')" +
            ") " +

            "AND NOT EXISTS (" +
            "SELECT 1 FROM notice n " +
            "WHERE COALESCE(n.car_log_no, n.snapshot_car_log_no) = cl.car_log_no" +
            ")")
    int createNoticesFromCarLog();

    @Delete("DELETE FROM notice WHERE notice_no = #{noticeNo}")
    int delete(int noticeNo);
}
