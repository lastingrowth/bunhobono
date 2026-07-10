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
            notice.notice_no,
            notice.plate_no,
            notice.entry_at,
            notice.detect_at,
            notice.stay_days,
            notice.alert_stat,
            CASE
                WHEN vehicle_car.vehicle_status = 'EXPIRED' THEN '만료'
                WHEN vehicle_car.vehicle_status IS NULL THEN '미등록'
                ELSE '만료아님'
            END expire_status
        FROM notice
        LEFT JOIN vehicle_car
            ON notice.plate_no = vehicle_car.car_no
        ORDER BY notice.notice_no DESC
    """)
    List<NoticeDTO> list(NoticeDTO dto);

    @Select("""
        SELECT
            notice.notice_no,
            notice.parking_no,
            notice.plate_no,
            notice.entry_at,
            notice.detect_at,
            notice.stay_days,
            notice.alert_stat,

            vehicle_car.vehicle_type,
            vehicle_car.vehicle_status,
            CASE
                WHEN vehicle_car.vehicle_status = 'EXPIRED' THEN '만료'
                WHEN vehicle_car.vehicle_status IS NULL THEN '미등록'
                ELSE '만료아님'
            END expire_status,
            vehicle_car.start_date,
            vehicle_car.end_date,

            parking_charge.amount,
            parking_charge.status charge_status,

            parking_payment.payment_status,
            parking_payment.paid_at
        FROM notice
        LEFT JOIN car_log
            ON notice.plate_no = car_log.car_no
            AND notice.entry_at = car_log.in_time
        LEFT JOIN vehicle_car
            ON car_log.vehicle_car_no = vehicle_car.vehicle_car_no
        LEFT JOIN parking_charge
            ON car_log.car_log_no = parking_charge.car_log_no
        LEFT JOIN parking_payment
            ON parking_charge.charge_no = parking_payment.charge_no
        WHERE notice.notice_no = #{noticeNo}
    """)
    NoticeDTO detail(int noticeNo);

    @Update("""
        UPDATE notice
        SET alert_stat = #{alertStat}
        WHERE notice_no = #{noticeNo}
          AND (
              #{alertStat} != 'Resolved'
              OR EXISTS (
                  SELECT 1
                  FROM car_log
                  JOIN parking_charge
                      ON car_log.car_log_no = parking_charge.car_log_no
                  JOIN parking_payment
                      ON parking_charge.charge_no = parking_payment.charge_no
                  WHERE car_log.car_no = notice.plate_no
                    AND car_log.in_time = notice.entry_at
                    AND parking_payment.payment_status = 'SUCCESS'
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
            parking.parking_no,
            COALESCE(car_log.car_no, vehicle_car.car_no),
            car_log.in_time,
            NOW(),
            GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (NOW() - car_log.in_time)) / 86400)::INT),
            'Unresolved'
        FROM car_log
        JOIN gate
            ON car_log.in_gate_no = gate.gate_no
        JOIN parking
            ON gate.parking_no = parking.parking_no
        LEFT JOIN vehicle_car
            ON car_log.vehicle_car_no = vehicle_car.vehicle_car_no
        WHERE car_log.out_time IS NULL
          AND COALESCE(car_log.car_no, vehicle_car.car_no) IS NOT NULL
          AND COALESCE(car_log.car_no, vehicle_car.car_no) != ''
          AND (
              car_log.vehicle_car_no IS NULL
              OR (
                  vehicle_car.vehicle_status = 'EXPIRED'
                  AND car_log.in_time <= NOW() - INTERVAL '1 day'
              )
          )
          AND NOT EXISTS (
              SELECT 1
              FROM notice
              WHERE notice.plate_no = COALESCE(car_log.car_no, vehicle_car.car_no)
                AND notice.entry_at = car_log.in_time
          )
    """)
    int createNoticeFromCarLog();
}