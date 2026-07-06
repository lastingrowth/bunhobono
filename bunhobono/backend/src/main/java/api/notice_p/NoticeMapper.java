package api.notice_p;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoticeMapper {

    @Select("""
        SELECT
            n.notice_no AS noticeNo,
            n.plate_no AS plateNo,
            n.entry_at AS entryAt,
            n.detect_at AS detectAt,
            n.stay_days AS stayDays,
            n.alert_stat AS alertStat,

            CASE
                WHEN v.vehicle_status = 'EXPIRED' THEN '만료'
                WHEN v.vehicle_status IS NULL THEN '미등록'
                ELSE '만료아님'
            END AS expireStatus
            FROM notice n
            LEFT JOIN vehicle_car v
            ON n.plate_no = v.car_no
            ORDER BY n.notice_no DESC
    """)
    List<NoticeDTO> list(NoticeDTO dto);

    @Select("""
        SELECT
            n.notice_no AS noticeNo,
            n.parking_no AS parkingNo,
            n.plate_no AS plateNo,
            n.entry_at AS entryAt,
            n.detect_at AS detectAt,
            n.stay_days AS stayDays,
            n.alert_stat AS alertStat,

            v.vehicle_type AS vehicleType,
            v.vehicle_status AS vehicleStatus,
            CASE
                WHEN v.vehicle_status = 'EXPIRED' THEN '만료'
                WHEN v.vehicle_status IS NULL THEN '미등록'
                ELSE '만료아님'
            END AS expireStatus,
            v.start_date AS startDate,
            v.end_date AS endDate,

            pc.amount AS amount,
            pc.status AS chargeStatus,

            pp.payment_status AS paymentStatus,
            pp.paid_at AS paidAt
        FROM notice n
        LEFT JOIN car_log cl
            ON n.plate_no = cl.car_no
            AND n.entry_at = cl.in_time
        LEFT JOIN vehicle_car v
            ON cl.vehicle_car_no = v.vehicle_car_no
        LEFT JOIN parking_charge pc
            ON cl.car_log_no = pc.car_log_no
        LEFT JOIN parking_payment pp
            ON pc.charge_no = pp.charge_no
        WHERE n.notice_no = #{noticeNo}
    """)
    NoticeDTO detail(int noticeNo);

    @Update("""
        UPDATE notice n
        SET alert_stat = #{alertStat}
        WHERE n.notice_no = #{noticeNo}
          AND (
              #{alertStat} != 'Resolved'
              OR EXISTS (
                  SELECT 1
                  FROM car_log cl
                  JOIN parking_charge pc
                      ON cl.car_log_no = pc.car_log_no
                  JOIN parking_payment pp
                      ON pc.charge_no = pp.charge_no
                  WHERE cl.car_no = n.plate_no
                    AND cl.in_time = n.entry_at
                    AND pp.payment_status = 'SUCCESS'
              )
          )
    """)
    void status(NoticeDTO dto);

    @Delete("""
        DELETE FROM notice
        WHERE alert_stat = 'Resolved'
          AND detect_at < NOW() - INTERVAL '1 year'
    """)
    int aftAYearDelete();

    @Insert("""
        INSERT INTO notice (
            parking_no,
            plate_no,
            entry_at,
            detect_at,
            stay_days,
            alert_stat
        )
        SELECT
            p.parking_no,
            COALESCE(cl.car_no, v.car_no),
            cl.in_time,
            NOW(),
            GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (NOW() - cl.in_time)) / 86400)::INT),
            'Unresolved'
        FROM car_log cl
        JOIN gate g
            ON cl.in_gate_no = g.gate_no
        JOIN parking p
            ON g.parking_no = p.parking_no
        LEFT JOIN vehicle_car v
            ON cl.vehicle_car_no = v.vehicle_car_no
        WHERE cl.out_time IS NULL
          AND COALESCE(cl.car_no, v.car_no) IS NOT NULL
          AND COALESCE(cl.car_no, v.car_no) != ''
          AND (
              cl.vehicle_car_no IS NULL
              OR (
                  v.vehicle_status = 'EXPIRED'
                  AND cl.in_time <= NOW() - INTERVAL '1 day'
              )
          )
          AND NOT EXISTS (
              SELECT 1
              FROM notice n
              WHERE n.plate_no = COALESCE(cl.car_no, v.car_no)
                AND n.entry_at = cl.in_time
          )
    """)
    int createNoticeFromCarLog();
}
