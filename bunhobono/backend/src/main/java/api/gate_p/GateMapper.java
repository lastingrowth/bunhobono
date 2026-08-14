package api.gate_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GateMapper {

    // 활성 게이트 목록 조회
    @Select("""
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY g.gate_no
            ) AS display_no,
            g.gate_no,
            g.parking_no,
            g.gate_code,
            g.gate_name,
            g.gate_type,
            g.gate_area,
            g.gate_status,
            p.parking_name,
            p.parking_location
        FROM gate g
        LEFT JOIN parking p
            ON g.parking_no = p.parking_no
        WHERE g.active = TRUE
        ORDER BY g.gate_no
    """)
    List<GateDTO> list(GateDTO dto);

    // 게이트 등록
    @Insert("""
        INSERT INTO gate (
            parking_no,
            gate_code,
            gate_name,
            gate_type,
            gate_area,
            gate_status,
            active
        )
        VALUES (
            #{parkingNo},
            #{gateCode},
            #{gateName},
            #{gateType},
            #{gateArea},
            0,
            TRUE
        )
    """)
    int insert(GateDTO dto);

    // 게이트 소프트 삭제
    @Update("""
        UPDATE gate g
        SET active = FALSE
        WHERE g.gate_no = #{gateNo}
          AND g.active = TRUE
          AND NOT EXISTS (
              SELECT 1
              FROM camera c
              WHERE c.gate_no = g.gate_no
          )
          AND NOT EXISTS (
              SELECT 1
              FROM car_log cl
              WHERE cl.in_gate_no = g.gate_no
                 OR cl.out_gate_no = g.gate_no
          )
          AND NOT EXISTS (
              SELECT 1
              FROM parking_space ps
              WHERE ps.gate_no = g.gate_no
          )
    """)
    int delete(int gateNo);

    // 게이트 정보 수정
    @Update("""
        UPDATE gate
        SET parking_no = #{parkingNo},
            gate_code = #{gateCode},
            gate_name = #{gateName},
            gate_type = #{gateType},
            gate_area = #{gateArea}
        WHERE gate_no = #{gateNo}
          AND active = TRUE
    """)
    int update(GateDTO dto);

    // 게이트 상태 변경
    @Update("""
        UPDATE gate
        SET gate_status = #{gateStatus}
        WHERE gate_no = #{gateNo}
          AND active = TRUE
    """)
    int updateStatus(GateDTO dto);

    // 카메라가 연결된 활성 게이트 조회
    @Select("""
        SELECT
            g.gate_no,
            g.parking_no,
            g.gate_code,
            g.gate_name,
            g.gate_type,
            g.gate_area,
            g.gate_status
        FROM camera c
        JOIN gate g
            ON c.gate_no = g.gate_no
        WHERE c.camera_no = #{cameraNo}
          AND c.active = TRUE
          AND g.active = TRUE
    """)
    GateDTO findByCameraNo(
            @Param("cameraNo") int cameraNo
    );
}
