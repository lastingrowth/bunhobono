package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    // 차량 목록 조회
    // loginId가 없으면 ADMIN 전체 목록
    // loginId가 있으면 RESIDENT 본인 차량 목록
    // UNKNOWN은 반려/취소 상태로 보고 목록에서 제외한다.
    @Select("""
        <script>
        SELECT
            ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no ASC) AS display_no,
            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.member_no,
            m.mem_dong AS mem_dong,
            m.mem_ho AS mem_ho,
            m.mem_name AS approved_member_name,
            vc.approved_at,
            cl.in_time AS in_time,
            cl.out_time AS out_time,
            CASE
                WHEN vc.vehicle_type = 'visit'
                     AND cl.in_time IS NOT NULL
                     AND vc.start_date IS NOT NULL
                     AND vc.end_date IS NOT NULL
                THEN cl.in_time + (vc.end_date - vc.start_date)
                ELSE vc.end_date
            END AS real_end_date
        FROM vehicle_car vc
        LEFT JOIN member m
            ON vc.member_no = m.member_no
        LEFT JOIN LATERAL (
            SELECT
                car_log.in_time,
                car_log.out_time
            FROM car_log
            WHERE car_log.vehicle_car_no = vc.vehicle_car_no
            ORDER BY car_log.in_time DESC
            LIMIT 1
        ) cl ON true

        WHERE vc.vehicle_status != 'UNKNOWN'

        <if test="loginId != null and loginId != ''">
            AND m.login_id = #{loginId}
        </if>

        ORDER BY vc.vehicle_car_no ASC
        </script>
    """)
    List<VehicleDTO> list(@Param("loginId") String loginId);

    // 차량 등록 화면에서 선택 가능한 회원 검색
    // normal: 등록차량 2대 미만인 회원
    // visit + RESIDENT: 유효한 방문차량이 없는 입주민
    // visit + ADMIN: 관리자 전체
    @Select("""
        <script>
        SELECT
            m.member_no,
            m.mem_name,
            m.mem_dong,
            m.mem_ho,
            m.role
        FROM member m
        LEFT JOIN vehicle_car vc
            ON vc.member_no = m.member_no
        WHERE UPPER(TRIM(m.role)) = #{role}
          AND m.delete_at IS NULL

        GROUP BY
            m.member_no,
            m.mem_name,
            m.mem_dong,
            m.mem_ho,
            m.role

        <choose>
            <when test="vehicleType == 'normal'">
                HAVING COUNT(
                    CASE
                        WHEN vc.vehicle_type = 'normal'
                         AND vc.vehicle_status IN ('WAITING', 'APPROVED')
                        THEN 1
                    END
                ) &lt; 2
            </when>

            <when test="vehicleType == 'visit' and role == 'RESIDENT'">
                HAVING COUNT(
                    CASE
                        WHEN vc.vehicle_type = 'visit'
                         AND vc.vehicle_status IN ('WAITING', 'APPROVED')
                         AND (
                                vc.end_date IS NULL
                                OR vc.end_date > CURRENT_TIMESTAMP
                             )
                        THEN 1
                    END
                ) = 0
            </when>
        </choose>

        ORDER BY m.mem_dong, m.mem_ho, m.mem_name
        </script>
    """)
    List<VehicleDTO> search(
            @Param("vehicleType") String vehicleType,
            @Param("role") String role
    );

    // ADMIN 차량 등록
    // 관리자가 등록하면 approved_at도 바로 찍힌다.
    @Insert("""
        INSERT INTO vehicle_car (
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            member_no,
            approved_at
        )
        VALUES (
            #{vehicleType},
            #{carNo},
            #{vehicleStatus},
            #{startDate},
            #{endDate},
            #{memberNo},
            CURRENT_TIMESTAMP
        )
    """)
    int insert(VehicleDTO dto);

    // RESIDENT 방문차량 신청
    // member_no는 로그인 ID로 찾아 넣는다.
    @Insert("""
        INSERT INTO vehicle_car (
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            member_no
        )
        VALUES (
            #{dto.vehicleType},
            #{dto.carNo},
            #{dto.vehicleStatus},
            #{dto.startDate},
            #{dto.endDate},
            (
                SELECT member_no
                FROM member
                WHERE login_id = #{loginId}
            )
        )
    """)
    int insertResidentVisit(
            @Param("loginId") String loginId,
            @Param("dto") VehicleDTO dto
    );

    // 같은 차량번호가 이미 등록 또는 승인대기 중인지 확인
    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car
        WHERE car_no = #{carNo}
          AND vehicle_status IN ('WAITING', 'APPROVED')
          AND (
                end_date IS NULL
                OR end_date > CURRENT_TIMESTAMP
          )
    """)
    int countActiveByCarNo(String carNo);

    // 입주민 본인의 유효 방문차량이 있는지 확인
    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car vc
        JOIN member m
            ON vc.member_no = m.member_no
        WHERE m.login_id = #{loginId}
          AND vc.vehicle_type = 'visit'
          AND vc.vehicle_status IN ('WAITING', 'APPROVED')
          AND (
                vc.end_date IS NULL
                OR vc.end_date > CURRENT_TIMESTAMP
          )
    """)
    int countActiveVisitByLoginId(String loginId);

    // 등록차량은 회원 1명당 2대까지만 가능
    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car
        WHERE member_no = #{memberNo}
          AND vehicle_type = 'normal'
          AND vehicle_status IN ('WAITING', 'APPROVED')
    """)
    int countActiveNormalByMemberNo(Integer memberNo);

    // 차량 기본 정보 수정
    @Update("""
        UPDATE vehicle_car
        SET vehicle_type = #{vehicleType},
            car_no = #{carNo},
            start_date = #{startDate},
            end_date = #{endDate}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int update(VehicleDTO dto);

    // 차량 상태 변경
    @Update("""
        UPDATE vehicle_car
        SET vehicle_status = #{vehicleStatus},
            member_no = COALESCE(#{memberNo}, member_no),
            start_date = #{startDate},
            end_date = #{endDate},
            approved_at = CASE
                WHEN #{vehicleStatus} = 'APPROVED'
                THEN CURRENT_TIMESTAMP
                ELSE approved_at
            END
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int updateStatus(VehicleDTO dto);

    // 차량 삭제
    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int delete(int vehicleCarNo);
}