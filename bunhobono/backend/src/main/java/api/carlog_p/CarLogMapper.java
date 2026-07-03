package apt.carlog_p;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CarLogMapper {

    @Select("""
            <script>
            /* =====================================================
               1. 기본 조회 컬럼
               - car_log를 중심으로 로그 기본 정보 조회
               - 게이트, 주차장, 차량 정보를 같이 가져오기 위해 조인 컬럼 포함
               ===================================================== */
            SELECT
                cl.car_log_no,
                cl.vehicle_car_no,
        
                /* 입차 게이트 정보 */
                cl.in_gate_no,
                ig.gate_name AS in_gate_name,
        
                /* 출차 게이트 정보 */
                cl.out_gate_no,
                og.gate_name AS out_gate_name,
        
                /* 주차장 정보 */
                p.parking_no,
                p.parking_name,
        
                /* 차량 정보 */
                vc.car_no,
                vc.vehicle_type,
                vc.vehicle_status,
        
                /* 입출차 시간 */
                cl.in_time,
                cl.out_time,
                cl.free_time,
        
                /* =====================================================
                   2. 현재 주차 상태 계산
                   - out_time이 없으면 아직 주차중
                   - out_time이 있으면 출차 완료
                   ===================================================== */
                CASE
                    WHEN cl.out_time IS NULL THEN 'PARKING'
                    ELSE 'OUT'
                END AS parking_state,
        
                /* =====================================================
                   3. 차량 종류 계산
                   - vehicle_car와 연결이 없으면 미등록/알 수 없는 차량
                   - vehicle_type이 visit이면 방문 차량
                   - 그 외는 등록 차량
                   ===================================================== */
                CASE
                    WHEN vc.vehicle_car_no IS NULL THEN 'UNKNOWN'
                    WHEN vc.vehicle_type = 'visit' THEN 'VISIT'
                    ELSE 'REGISTERED'
                END AS car_kind
        
            /* =====================================================
               4. 기준 테이블
               - car_log를 중심으로 전체 로그를 조회
               ===================================================== */
            FROM car_log cl
        
            /* =====================================================
               5. 차량 테이블 조인
               - 로그에 연결된 차량 번호, 차량 타입, 승인 상태 조회
               - LEFT JOIN이라 차량 정보가 없어도 로그는 남김
               ===================================================== */
            LEFT JOIN vehicle_car vc
                ON cl.vehicle_car_no = vc.vehicle_car_no
        
            /* =====================================================
               6. 입차 게이트 조인
               - in_gate_no로 입차 게이트 이름 조회
               ===================================================== */
            LEFT JOIN gate ig
                ON cl.in_gate_no = ig.gate_no
        
            /* =====================================================
               7. 출차 게이트 조인
               - out_gate_no로 출차 게이트 이름 조회
               ===================================================== */
            LEFT JOIN gate og
                ON cl.out_gate_no = og.gate_no
        
            /* =====================================================
               8. 주차장 조인
               - 입차 게이트가 속한 주차장 정보 조회
               ===================================================== */
            LEFT JOIN parking p
                ON ig.parking_no = p.parking_no
        
            /* =====================================================
               9. 기본 WHERE
               - 동적 조건을 계속 붙이기 위해 항상 참인 조건 사용
               ===================================================== */
            WHERE 1 = 1
        
            /* =====================================================
               10. 게이트별 필터
               - gateNo가 들어오면 입차 또는 출차 게이트가 해당 번호인 로그만 조회
               ===================================================== */
            <if test="gateNo != null">
                AND (cl.in_gate_no = #{gateNo} OR cl.out_gate_no = #{gateNo})
            </if>
        
            /* =====================================================
               11. 주차장별 필터
               - parkingNo가 들어오면 해당 주차장 로그만 조회
               ===================================================== */
            <if test="parkingNo != null">
                AND p.parking_no = #{parkingNo}
            </if>
        
            /* =====================================================
               12. 주차 상태 필터
               - PARKING: 아직 출차 시간이 없는 차량
               - OUT: 출차 시간이 있는 차량
               ===================================================== */
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
        
            /* =====================================================
               13. 차량 종류 필터
               - UNKNOWN: 차량 테이블과 연결되지 않은 미등록 차량
               - VISIT: 방문 차량
               - REGISTERED: 일반 등록 차량
               ===================================================== */
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
        
            /* =====================================================
               14. 차량번호 검색
               - carNo가 들어오면 차량번호 일부 검색
               ===================================================== */
            <if test="carNo != null and carNo != ''">
                AND vc.car_no LIKE CONCAT('%', #{carNo}, '%')
            </if>
        
            /* =====================================================
               15. 정렬
               - sort가 oldest면 오래된순
               - 기본값은 최신순
               ===================================================== */
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
}