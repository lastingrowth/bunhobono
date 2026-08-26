package api.carlog_p;

import api.cameradata_p.CameraDataDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CarLogMapper {

    // 차량 입출차 목록 동적 조회
    @Select("<script>" +
            " SELECT ROW_NUMBER() OVER (ORDER BY detail.in_time) AS display_no, " +
            " detail.*, detail.snapshot_car_kind AS car_kind, " +
            " space.space_code, space.space_type, space.updated_at AS space_updated_at " +
            " FROM car_log_detail detail " +
            " LEFT JOIN parking_space space " +
            " ON space.car_log_no = detail.car_log_no " +
            " AND space.active = TRUE " +
            " <where> " +
            " <if test='gateNo != null'> " +
            " AND (detail.in_gate_no = #{gateNo} " +
            " OR detail.out_gate_no = #{gateNo}) " +
            " </if> " +
            " <if test='parkingNo != null'> " +
            " AND detail.parking_no = #{parkingNo} " +
            " </if> " +
            " <if test='parkingState != null and parkingState != \"\"'> " +
            " AND detail.parking_state = #{parkingState} " +
            " </if> " +
            " <if test='carKind != null and carKind != \"\"'> " +
            " AND detail.snapshot_car_kind = #{carKind} " +
            " </if> " +
            " <if test='carNo != null and carNo != \"\"'> " +
            " AND detail.car_no LIKE CONCAT('%', #{carNo}, '%') " +
            " </if> " +
            " <if test='lastFourDigits != null and lastFourDigits != \"\"'> " +
            " AND RIGHT(detail.car_no, 4) = #{lastFourDigits} " +
            " </if> " +
            " </where> " +
            " <choose> " +
            " <when test='sort == \"oldest\"'> " +
            " ORDER BY detail.in_time " +
            " </when> " +
            " <otherwise> " +
            " ORDER BY detail.in_time DESC " +
            " </otherwise> " +
            " </choose> " +
            "</script>")
    List<CarLogDTO> list(CarLogDTO dto);

    // 차량 입출차 상세 조회
    @Select("""
        SELECT
            detail.*,
            detail.snapshot_car_kind AS car_kind
        FROM car_log_detail detail
        WHERE detail.car_log_no = #{carLogNo}
    """)
    CarLogDTO detail(
            @Param("carLogNo") int carLogNo
    );

    // 입출차 기록의 무료시간 수정
    @Update("UPDATE car_log " +
            " SET free_time = #{freeTime} " +
            " WHERE car_log_no = #{carLogNo}")
    int updateFreeTime(CarLogDTO dto);

    // 촬영 카메라가 연결된 게이트 조회
    @Select("""
        SELECT
            gate.gate_no,
            gate.parking_no,
            gate.gate_type,
            gate.gate_area
        FROM camera

        JOIN gate
            ON camera.gate_no = gate.gate_no

        WHERE camera.camera_no = #{cameraNo}
          AND camera.active = TRUE
          AND gate.active = TRUE
    """)
    CarLogDTO findGateByCameraNo(
            @Param("cameraNo") int cameraNo
    );

    // 아직 출차하지 않은 차량 기록 조회
    @Select("""
        <script>
        SELECT
            detail.*,
            detail.snapshot_car_kind AS car_kind
        FROM car_log_detail detail
        WHERE detail.out_time IS NULL

        <choose>
            <when test="vehicleCarNo != null">
                AND detail.vehicle_car_no =
                    #{vehicleCarNo}
            </when>

            <otherwise>
                AND (
                    detail.car_no = #{carNo}
                    OR detail.car_no = #{ocrCarNo}
                    OR detail.snapshot_car_no = #{carNo}
                    OR detail.snapshot_car_no =
                        #{ocrCarNo}
                )
            </otherwise>
        </choose>

        ORDER BY detail.in_time DESC
        LIMIT 1
        </script>
    """)
    CarLogDTO findOpenLog(
            CameraDataDTO dto
    );

    // B1·B2 입차 기록 생성
    @Insert("""
        INSERT INTO car_log (
            vehicle_car_no,
            camera_data_no,
            in_gate_no,
            in_time,
            free_time,
            snapshot_car_no,
            snapshot_car_kind
        )
        VALUES (
            #{vehicleCarNo},
            #{cameraDataNo},
            #{inGateNo},
            #{inTime},
            COALESCE(#{freeTime}, 0),
            #{snapshotCarNo},
            #{snapshotCarKind}
        )
    """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "carLogNo",
            keyColumn = "car_log_no"
    )
    int insertEntry(CarLogDTO dto);

    // B1·B2 출차 처리
    @Update("""
        UPDATE car_log
        SET out_camera_data_no =
                #{data.cameraDataNo},
            out_gate_no = #{gateNo},
            out_time = #{data.captureTime}
        WHERE car_log_no = #{carLogNo}
          AND out_time IS NULL
    """)
    int exitParking(
            @Param("carLogNo") int carLogNo,
            @Param("data") CameraDataDTO data,
            @Param("gateNo") int gateNo
    );

    // OCR 관리자 수정사항 반영
    @Update("""
        <script>
        UPDATE car_log

        <choose>
            <when test="vehicleCarNo != null">
                SET vehicle_car_no = #{vehicleCarNo},
                    snapshot_car_no = #{carNo},
                    snapshot_car_kind =
                        CASE
                            WHEN #{vehicleType} = 'visit'
                                THEN 'VISIT'
                            ELSE 'REGISTERED'
                        END
            </when>

            <otherwise>
                SET vehicle_car_no = NULL,
                    snapshot_car_no = #{carNo},
                    snapshot_car_kind = 'UNKNOWN'
            </otherwise>
        </choose>

        WHERE camera_data_no = #{cameraDataNo}
           OR out_camera_data_no = #{cameraDataNo}
        </script>
    """)
    int correctByCameraData(
            CameraDataDTO dto
    );

    // 입출차 기록 삭제
    @Delete("""
        DELETE FROM car_log
        WHERE car_log_no = #{carLogNo}
    """)
    int delete(
            @Param("carLogNo") int carLogNo
    );

    // 출차 후 3개월이 지난 기록 조회
    @Select("""
        SELECT car_log_no
        FROM car_log
        WHERE out_time IS NOT NULL
          AND out_time
              < CURRENT_TIMESTAMP
                - INTERVAL '3 months'
    """)
    List<Integer> findOldCarLogNosForTrash();


    // 주차장 수용 가능 여부 확인
    @Select("""
    SELECT EXISTS (
        SELECT 1
        FROM parking
        WHERE parking_no = #{parkingNo}
          AND active = TRUE
          AND (
              SELECT COUNT(*)
              FROM car_log log

              JOIN gate
                  ON log.in_gate_no = gate.gate_no

              WHERE gate.parking_no = #{parkingNo}
                AND log.out_time IS NULL
          ) < parking_spaces
    )
""")
    boolean hasAvailableCapacity(
            @Param("parkingNo") int parkingNo
    );
}