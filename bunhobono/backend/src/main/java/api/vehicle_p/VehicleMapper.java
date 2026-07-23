package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    // ADMIN 전체 차량, RESIDENT 본인 차량 조회
    @Select("""
        <script>
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY vc.vehicle_car_no ASC
            ) AS display_no,

            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.member_no,

            m.mem_dong,
            m.mem_ho,
            m.mem_name AS approved_member_name,

            vc.approved_at,
            cl.in_time,
            cl.out_time,

            expiry.real_end_date,

            CASE
                -- 승인됐지만 입차하지 않고
                -- 예상 방문시간 + 1시간이 지난 방문차량
                WHEN vc.vehicle_status = 'APPROVED'
                     AND vc.vehicle_type = 'visit'
                     AND cl.in_time IS NULL
                     AND cl.out_time IS NULL
                     AND expiry.real_end_date IS NOT NULL
                     AND CURRENT_TIMESTAMP >
                         expiry.real_end_date
                THEN 'NO_ENTRY'

                -- 입차 후 실제 만기시간을 지났지만 미출차
                WHEN vc.vehicle_status = 'APPROVED'
                     AND cl.in_time IS NOT NULL
                     AND cl.out_time IS NULL
                     AND expiry.real_end_date IS NOT NULL
                     AND CURRENT_TIMESTAMP >
                         expiry.real_end_date
                THEN 'OVERSTAY'

                ELSE NULL
            END AS expiry_type,

            -- 양수: 남은 분, 음수: 초과한 분
            CASE
                WHEN vc.vehicle_status = 'APPROVED'
                     AND (
                         vc.vehicle_type = 'normal'
                         OR cl.out_time IS NULL
                     )
                     AND expiry.real_end_date IS NOT NULL
                THEN TRUNC(
                    EXTRACT(
                        EPOCH FROM (
                            expiry.real_end_date
                            - CURRENT_TIMESTAMP
                        )
                    ) / 60
                )::BIGINT

                ELSE NULL
            END AS remaining_minutes

        FROM vehicle_car vc

        LEFT JOIN member m
            ON vc.member_no = m.member_no

        -- 가장 최근 입출차 로그
        LEFT JOIN LATERAL (
            SELECT
                car_log.in_time,
                car_log.out_time
            FROM car_log
            WHERE car_log.vehicle_car_no =
                  vc.vehicle_car_no
            ORDER BY car_log.in_time DESC
            LIMIT 1
        ) cl ON TRUE

        -- 실제 만기시간을 한 번만 계산
        LEFT JOIN LATERAL (
            SELECT
                CASE
                    -- 방문차량 입차 후:
                    -- 입차시간 + 신청시간 + 30분
                    WHEN vc.vehicle_type = 'visit'
                         AND cl.in_time IS NOT NULL
                         AND vc.start_date IS NOT NULL
                         AND vc.end_date IS NOT NULL
                    THEN cl.in_time
                         + (vc.end_date - vc.start_date)
                         + INTERVAL '30 minutes'

                    -- 방문차량 입차 전:
                    -- 예상 방문시간 + 1시간
                    WHEN vc.vehicle_type = 'visit'
                         AND cl.in_time IS NULL
                         AND vc.start_date IS NOT NULL
                    THEN vc.start_date
                         + INTERVAL '1 hour'

                    -- 일반 등록차량
                    ELSE vc.end_date
                END AS real_end_date
        ) expiry ON TRUE

        WHERE 1 = 1

        <if test="loginId != null and loginId != ''">
            AND m.login_id = #{loginId}
        </if>

        ORDER BY vc.vehicle_car_no ASC
        </script>
    """)
    List<VehicleDTO> list(
            @Param("loginId") String loginId
    );


    // 차량 등록 화면에서 선택 가능한 회원 조회
    @Select("""
        <script>
        SELECT
            m.member_no,
            m.mem_name,
            m.mem_dong,
            m.mem_ho,
            m.role

        FROM member m

        WHERE UPPER(TRIM(m.role)) = #{role}
          AND m.delete_at IS NULL

        <choose>
            <!-- 등록차량은 유효한 차량이 2대 미만인 회원 -->
            <when test="vehicleType == 'normal'">
                AND (
                    SELECT COUNT(*)
                    FROM vehicle_car vc
                    WHERE vc.member_no = m.member_no
                      AND vc.vehicle_type = 'normal'
                      AND vc.vehicle_status
                          IN ('WAITING', 'APPROVED')
                      AND (
                            vc.end_date IS NULL
                            OR vc.end_date
                               &gt; CURRENT_TIMESTAMP
                      )
                ) &lt; 2
            </when>

            <!-- 입주민 방문차량은 유효한 신청이 없어야 함 -->
            <when test="vehicleType == 'visit'
                        and role == 'RESIDENT'">
                AND NOT EXISTS (
                    SELECT 1
                    FROM vehicle_car vc
                    WHERE vc.member_no = m.member_no
                      AND vc.vehicle_type = 'visit'
                      AND (
                            (
                                vc.vehicle_status = 'WAITING'
                                AND vc.start_date IS NOT NULL
                                AND CURRENT_TIMESTAMP
                                    &lt;= vc.start_date
                                        + INTERVAL '1 hour'
                            )
                            OR
                            (
                                vc.vehicle_status = 'APPROVED'
                                AND (
                                    EXISTS (
                                        SELECT 1
                                        FROM car_log cl
                                        WHERE cl.vehicle_car_no =
                                              vc.vehicle_car_no
                                          AND cl.out_time IS NULL
                                    )
                                    OR
                                    (
                                        NOT EXISTS (
                                            SELECT 1
                                            FROM car_log cl
                                            WHERE cl.vehicle_car_no =
                                                  vc.vehicle_car_no
                                        )
                                        AND vc.start_date IS NOT NULL
                                        AND CURRENT_TIMESTAMP
                                            &lt;= vc.start_date
                                                + INTERVAL '1 hour'
                                    )
                                )
                            )
                      )
                )
            </when>
        </choose>

        ORDER BY
            m.mem_dong,
            m.mem_ho,
            m.mem_name
        </script>
    """)
    List<VehicleDTO> search(
            @Param("vehicleType") String vehicleType,
            @Param("role") String role
    );


    // ADMIN 등록과 RESIDENT 방문 신청 공용 INSERT
    // loginId가 있으면 로그인 회원, 없으면 dto.memberNo 사용
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
            #{dto.vehicleType},
            #{dto.carNo},
            #{dto.vehicleStatus},
            #{dto.startDate},
            #{dto.endDate},

            CASE
                WHEN CAST(#{loginId} AS VARCHAR) IS NOT NULL
                THEN (
                    SELECT m.member_no
                    FROM member m
                    WHERE m.login_id = CAST(#{loginId} AS VARCHAR)
                )
                ELSE #{dto.memberNo}
            END,

            CASE
                WHEN #{dto.vehicleStatus} = 'APPROVED'
                THEN CURRENT_TIMESTAMP
                ELSE NULL
            END
        )
    """)
    int insert(
            @Param("loginId") String loginId,
            @Param("dto") VehicleDTO dto
    );


    // 같은 차량번호의 유효한 차량 확인
    @Select("""
        SELECT COUNT(*)

        FROM vehicle_car vc

        WHERE vc.car_no = #{carNo}
          AND (
                (
                    vc.vehicle_type = 'normal'
                    AND vc.vehicle_status
                        IN ('WAITING', 'APPROVED')
                    AND (
                        vc.end_date IS NULL
                        OR vc.end_date >
                           CURRENT_TIMESTAMP
                    )
                )
                OR
                (
                    vc.vehicle_type = 'visit'
                    AND (
                        (
                            vc.vehicle_status = 'WAITING'
                            AND vc.start_date IS NOT NULL
                            AND CURRENT_TIMESTAMP
                                <= vc.start_date
                                   + INTERVAL '1 hour'
                        )
                        OR
                        (
                            vc.vehicle_status = 'APPROVED'
                            AND (
                                EXISTS (
                                    SELECT 1
                                    FROM car_log cl
                                    WHERE cl.vehicle_car_no =
                                          vc.vehicle_car_no
                                      AND cl.out_time IS NULL
                                )
                                OR
                                (
                                    NOT EXISTS (
                                        SELECT 1
                                        FROM car_log cl
                                        WHERE cl.vehicle_car_no =
                                              vc.vehicle_car_no
                                    )
                                    AND vc.start_date IS NOT NULL
                                    AND CURRENT_TIMESTAMP
                                        <= vc.start_date
                                           + INTERVAL '1 hour'
                                )
                            )
                        )
                    )
                )
          )
    """)
    int countActiveByCarNo(
            @Param("carNo") String carNo
    );


    // 입주민의 유효한 방문차량 신청 확인
    @Select("""
        SELECT COUNT(*)

        FROM vehicle_car vc

        JOIN member m
            ON vc.member_no = m.member_no

        WHERE m.login_id = #{loginId}
          AND vc.vehicle_type = 'visit'
          AND (
                (
                    vc.vehicle_status = 'WAITING'
                    AND vc.start_date IS NOT NULL
                    AND CURRENT_TIMESTAMP
                        <= vc.start_date
                           + INTERVAL '1 hour'
                )
                OR
                (
                    vc.vehicle_status = 'APPROVED'
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM car_log cl
                            WHERE cl.vehicle_car_no =
                                  vc.vehicle_car_no
                              AND cl.out_time IS NULL
                        )
                        OR
                        (
                            NOT EXISTS (
                                SELECT 1
                                FROM car_log cl
                                WHERE cl.vehicle_car_no =
                                      vc.vehicle_car_no
                            )
                            AND vc.start_date IS NOT NULL
                            AND CURRENT_TIMESTAMP
                                <= vc.start_date
                                   + INTERVAL '1 hour'
                        )
                    )
                )
          )
    """)
    int countActiveVisitByLoginId(
            @Param("loginId") String loginId
    );


    // 회원 한 명당 유효한 등록차량은 최대 2대
    @Select("""
        SELECT COUNT(*)

        FROM vehicle_car

        WHERE member_no = #{memberNo}
          AND vehicle_type = 'normal'
          AND vehicle_status IN ('WAITING', 'APPROVED')
          AND (
                end_date IS NULL
                OR end_date > CURRENT_TIMESTAMP
          )
    """)
    int countActiveNormalByMemberNo(
            @Param("memberNo") Integer memberNo
    );


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


    // WAITING 방문 신청 승인
    @Update("""
        UPDATE vehicle_car

        SET vehicle_status = 'APPROVED',
            approved_at = CURRENT_TIMESTAMP

        WHERE vehicle_car_no = #{vehicleCarNo}
          AND vehicle_status = 'WAITING'
    """)
    int updateStatus(VehicleDTO dto);


    // 차량 삭제
    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int delete(
            @Param("vehicleCarNo") int vehicleCarNo
    );
}
