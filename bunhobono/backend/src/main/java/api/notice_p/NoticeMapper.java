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
            n.car_log_no,
            vc.car_no AS registered_car_no,
            cd.car_no AS captured_car_no,
            CASE
                WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN'
                ELSE 'REGISTERED'
            END AS car_kind,
            n.detect_at,
            n.stay_days,
            n.alert_stat,
            n.handled_by_member_no,
            m.mem_name AS handled_by_member_name,
            n.handled_at,
            cl.in_time,
            p.parking_name
        FROM notice n
        JOIN car_log cl ON n.car_log_no = cl.car_log_no
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

    @Insert("""
        INSERT INTO notice (car_log_no, detect_at, stay_days, alert_stat)
        SELECT
            cl.car_log_no,
            NOW(),
            GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (NOW() - cl.in_time)) / 86400)::INT),
            'Unresolved'
        FROM car_log cl
        LEFT JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no
        LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no
        WHERE cl.out_time IS NULL
          AND COALESCE(vc.car_no, cd.car_no) IS NOT NULL
          AND COALESCE(vc.car_no, cd.car_no) != ''
          AND (
                cl.vehicle_car_no IS NULL
                OR (
                    vc.vehicle_status = 'EXPIRED'
                    AND cl.in_time <= NOW() - INTERVAL '1 day'
                )
          )
          AND NOT EXISTS (
              SELECT 1
              FROM notice n
              WHERE n.car_log_no = cl.car_log_no
          )
    """)
    int createNoticesFromCarLog();
}
