package api.carlog_p;

import api.cameradata_p.CameraDataDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CarLogMapper {

    @Select("""
        <script>
        SELECT
            ROW_NUMBER() OVER (ORDER BY cl.in_time DESC) AS display_no,
            cl.car_log_no,
            COALESCE(vc.car_no, cd.car_no) AS car_no,
            CASE
                WHEN cl.vehicle_car_no IS NULL THEN 'UNKNOWN'
                WHEN vc.vehicle_type = 'visit' THEN 'VISIT'
                ELSE 'REGISTERED'
            END AS car_kind,
            CASE WHEN cl.out_time IS NULL THEN 'PARKING' ELSE 'OUT' END AS parking_state,
            cl.vehicle_car_no,
            vc.vehicle_type,
            vc.vehicle_status,
            p.parking_no,
            p.parking_name,
            cl.camera_data_no,
            cl.in_gate_no,
            ig.gate_name AS in_gate_name,
            cl.in_time,
            cl.out_gate_no,
            og.gate_name AS out_gate_name,
            cl.out_time
        FROM car_log cl
        LEFT JOIN vehicle_car vc ON cl.vehicle_car_no = vc.vehicle_car_no
        LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no
        LEFT JOIN gate ig ON cl.in_gate_no = ig.gate_no
        LEFT JOIN gate og ON cl.out_gate_no = og.gate_no
        LEFT JOIN parking p ON ig.parking_no = p.parking_no
        WHERE 1 = 1
        <if test="gateNo != null">
            AND (cl.in_gate_no = #{gateNo} OR cl.out_gate_no = #{gateNo})
        </if>
        <if test="parkingNo != null">
            AND p.parking_no = #{parkingNo}
        </if>
        <if test="parkingState == 'PARKING'">
            AND cl.out_time IS NULL
        </if>
        <if test="parkingState == 'OUT'">
            AND cl.out_time IS NOT NULL
        </if>
        <if test="carKind == 'UNKNOWN'">
            AND cl.vehicle_car_no IS NULL
        </if>
        <if test="carKind == 'VISIT'">
            AND vc.vehicle_type = 'visit'
        </if>
        <if test="carKind == 'REGISTERED'">
            AND vc.vehicle_type = 'normal'
        </if>
        <if test="carNo != null and carNo != ''">
            AND COALESCE(vc.car_no, cd.car_no) LIKE CONCAT('%', #{carNo}, '%')
        </if>
        <choose>
            <when test="sort == 'oldest'">ORDER BY cl.in_time ASC</when>
            <otherwise>ORDER BY cl.in_time DESC</otherwise>
        </choose>
        </script>
    """)
    List<CarLogDTO> list(CarLogDTO dto);

    @Select("""
        SELECT g.gate_no AS gate_no, g.gate_type AS gate_type
        FROM camera c
        JOIN gate g ON c.gate_no = g.gate_no
        WHERE c.camera_no = #{cameraNo}
    """)
    CarLogDTO findGateByCameraNo(int cameraNo);

    @Select("""
        <script>
        SELECT EXISTS (
            SELECT 1
            FROM car_log cl
            LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no
            WHERE cl.out_time IS NULL
            <choose>
                <when test="vehicleCarNo != null">
                    AND cl.vehicle_car_no = #{vehicleCarNo}
                </when>
                <otherwise>
                    AND cl.vehicle_car_no IS NULL AND cd.car_no = #{carNo}
                </otherwise>
            </choose>
        )
        </script>
    """)
    boolean existsOpenLog(CameraDataDTO dto);

    @Insert("""
        INSERT INTO car_log (vehicle_car_no, camera_data_no, in_gate_no, in_time)
        VALUES (#{data.vehicleCarNo}, #{data.cameraDataNo}, #{gateNo}, #{data.captureTime})
    """)
    int insertEntry(@Param("data") CameraDataDTO data, @Param("gateNo") int gateNo);

    @Update("""
        <script>
        UPDATE car_log
        SET out_gate_no = #{gateNo},
            out_time = #{data.captureTime}
        WHERE car_log_no = (
            SELECT cl.car_log_no
            FROM car_log cl
            LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no
            WHERE cl.out_time IS NULL
            <choose>
                <when test="data.vehicleCarNo != null">
                    AND cl.vehicle_car_no = #{data.vehicleCarNo}
                </when>
                <otherwise>
                    AND cl.vehicle_car_no IS NULL AND cd.car_no = #{data.carNo}
                </otherwise>
            </choose>
            ORDER BY cl.in_time DESC
            LIMIT 1
        )
        </script>
    """)
    int completeExit(@Param("data") CameraDataDTO data, @Param("gateNo") int gateNo);

    @Delete("DELETE FROM car_log WHERE car_log_no = #{carLogNo}")
    int delete(int carLogNo);

    @Delete("""
        DELETE FROM car_log
        WHERE out_time IS NOT NULL
          AND out_time < NOW() - INTERVAL '15 days'
--        AND out_time < NOW() - INTERVAL '1 minute'
    """)
    int deleteOldLogs();
}
