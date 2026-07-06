package api.carlog_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CarLogMapper {

    // 차량 입출차 로그 목록 조회
    @Select("""
        <script>
        SELECT
            cl.car_log_no,
            cl.vehicle_car_no,
            cl.in_gate_no,
            cl.out_gate_no,
            cl.in_time,
            cl.out_time,

            vc.car_no,
            vc.vehicle_type,
            vc.vehicle_status,

            ig.gate_name AS in_gate_name,
            og.gate_name AS out_gate_name,

            p.parking_no,
            p.parking_name,

            pc.amount AS fee,
            pc.status AS charge_status,
            pp.payment_status,

            wc.wrong_car_no,
            wc.reason_type AS wrong_reason_type,
            wc.description AS wrong_description

        FROM car_log cl

        LEFT JOIN vehicle_car vc
            ON cl.vehicle_car_no = vc.vehicle_car_no

        LEFT JOIN gate ig
            ON cl.in_gate_no = ig.gate_no

        LEFT JOIN gate og
            ON cl.out_gate_no = og.gate_no

        LEFT JOIN parking p
            ON ig.parking_no = p.parking_no

        LEFT JOIN parking_charge pc
            ON cl.car_log_no = pc.car_log_no

        LEFT JOIN parking_payment pp
            ON pc.charge_no = pp.charge_no

        LEFT JOIN wrong_car wc
            ON cl.car_log_no = wc.car_log_no

        WHERE 1 = 1

        <if test="gateNo != null">
            AND (cl.in_gate_no = #{gateNo} OR cl.out_gate_no = #{gateNo})
        </if>

        <if test="parkingNo != null">
            AND p.parking_no = #{parkingNo}
        </if>

        <if test="parkingState != null and parkingState != ''">
            <choose>
                <when test="parkingState == 'PARKING'">
                    AND cl.out_time IS NULL
                </when>
                <when test="parkingState == 'OUT'">
                    AND cl.out_time IS NOT NULL
                </when>
            </choose>
        </if>

        <if test="carKind != null and carKind != ''">
            <choose>
                <when test="carKind == 'UNKNOWN'">
                    AND vc.vehicle_car_no IS NULL
                </when>
                <when test="carKind == 'VISIT'">
                    AND vc.vehicle_type = 'visit'
                </when>
                <when test="carKind == 'REGISTERED'">
                    AND vc.vehicle_type = 'normal'
                </when>
            </choose>
        </if>

        <if test="carNo != null and carNo != ''">
            AND vc.car_no LIKE CONCAT('%', #{carNo}, '%')
        </if>

        <choose>
            <when test="sort == 'oldest'">
                ORDER BY cl.in_time ASC
            </when>
            <otherwise>
                ORDER BY cl.in_time DESC
            </otherwise>
        </choose>
        </script>
    """)
    List<CarLogDTO> list(CarLogDTO dto);

    // 출차 완료 후 15일 지난 로그 자동삭제
    @Delete("""
        DELETE FROM car_log cl
        WHERE cl.out_time IS NOT NULL
          AND cl.out_time < NOW() - INTERVAL '15 days'
          AND NOT EXISTS (
              SELECT 1
              FROM wrong_car wc
              WHERE wc.car_log_no = cl.car_log_no
          )
          AND (
              NOT EXISTS (
                  SELECT 1
                  FROM parking_charge pc
                  WHERE pc.car_log_no = cl.car_log_no
              )
              OR
              EXISTS (
                  SELECT 1
                  FROM parking_charge pc
                  JOIN parking_payment pp
                    ON pc.charge_no = pp.charge_no
                  WHERE pc.car_log_no = cl.car_log_no
                    AND pp.payment_status = 'SUCCESS'
              )
          )
    """)
    void deleteOldLogs();
}