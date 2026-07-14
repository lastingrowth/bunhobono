package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    // ADMIN 전체 차량 목록
    // 최근 등록 차량이 위로 오도록 vehicle_car_no DESC 정렬
    @Select("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no DESC) AS display_no,
            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.member_no,
            m.mem_name AS approved_member_name,
            vc.approved_at
        FROM vehicle_car vc
        LEFT JOIN member m
            ON vc.member_no = m.member_no
        ORDER BY vc.vehicle_car_no DESC
    """)
    List<VehicleDTO> list();


    // RESIDENT 본인 차량 목록
    // JWT에서 받은 loginId로 member와 vehicle_car를 조인해서 본인 차량만 조회
    @Select("""
        SELECT
            ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no DESC) AS display_no,
            vc.vehicle_car_no,
            vc.vehicle_type,
            vc.car_no,
            vc.vehicle_status,
            vc.start_date,
            vc.end_date,
            vc.member_no,
            m.mem_name AS approved_member_name,
            vc.approved_at
        FROM vehicle_car vc
        JOIN member m
            ON vc.member_no = m.member_no
        WHERE m.login_id = #{loginId}
        ORDER BY vc.vehicle_car_no DESC
    """)
    List<VehicleDTO> listByLoginId(String loginId);


    // ADMIN 차량 등록 신청
    // normal, visit 모두 이 insert 하나로 처리
    // 등록 상태는 Service에서 WAITING으로 세팅해서 들어온다.
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
            #{vehicleType},
            #{carNo},
            #{vehicleStatus},
            #{startDate},
            #{endDate},
            #{memberNo}
        )
    """)
    int insert(VehicleDTO dto);


    // RESIDENT 방문 차량 등록 신청
    // loginId로 member_no를 찾아서 바로 INSERT
    // Service에서 따로 memberNo를 찾지 않아도 된다.
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
    int insertResidentVisit(@Param("loginId") String loginId,
                            @Param("dto") VehicleDTO dto);


    // 같은 차량번호가 WAITING 또는 APPROVED 상태로 살아있는지 확인
    // 이미 등록/승인대기 중인 차량번호 중복 방지
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


    // RESIDENT가 이미 유효한 방문차량을 가지고 있는지 확인
    // WAITING 또는 APPROVED 상태이고, 아직 만료되지 않았으면 추가 신청 불가
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


    // 차량 승인 상태 변경
    // APPROVED / UNKNOWN / EXPIRED 처리
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