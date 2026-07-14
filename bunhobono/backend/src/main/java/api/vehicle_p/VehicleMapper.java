package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    // 관리자 차량 전체 목록
    // ADMIN은 모든 차량을 조회한다.
    @Select("""
        SELECT ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no) AS display_no,
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
        ORDER BY vc.vehicle_car_no
    """)
    List<VehicleDTO> list();

    // 기존 차량 등록 메서드
    // 기존 코드 호환용으로 남겨둔다.
    // 새 권한 구조에서는 insertRequest()를 주로 사용한다.
    @Insert("""
        INSERT INTO vehicle_car (
            vehicle_type,
            car_no,
            start_date,
            end_date
        )
        VALUES (
            #{vehicleType},
            #{carNo},
            #{startDate},
            #{endDate}
        )
    """)
    int insert(VehicleDTO dto);

    // ADMIN 또는 RESIDENT가 차량 등록 신청할 때 사용하는 새 INSERT
    // 등록 즉시 APPROVED가 아니라 WAITING 상태로 들어간다.
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
    int insertRequest(VehicleDTO dto);

    // login_id로 member_no 조회
    // RESIDENT가 본인 차량을 조회하거나 방문차량을 신청할 때 사용한다.
    @Select("""
        SELECT member_no
        FROM member
        WHERE login_id = #{loginId}
    """)
    Integer findMemberNoByLoginId(String loginId);

    // RESIDENT 본인 차량 목록
    // 본인의 normal 차량과 본인이 신청한 visit 차량만 조회된다.
    @Select("""
        SELECT ROW_NUMBER() OVER (ORDER BY vc.vehicle_car_no DESC) AS display_no,
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
        WHERE vc.member_no = #{memberNo}
        ORDER BY vc.vehicle_car_no DESC
    """)
    List<VehicleDTO> listByMemberNo(Integer memberNo);

    // 차량번호 중복 확인
    // WAITING 또는 APPROVED 상태이고 아직 만료되지 않은 같은 차량번호가 있으면 중복으로 본다.
    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car
        WHERE car_no = #{carNo}
          AND vehicle_status IN ('WAITING', 'APPROVED')
          AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)
    """)
    int countActiveByCarNo(String carNo);

    // RESIDENT 방문차량 1대 제한 확인
    // 본인이 신청한 visit 차량 중 WAITING 또는 APPROVED 상태이며 아직 유효한 차량이 있으면 추가 신청 불가.
    @Select("""
        SELECT COUNT(*)
        FROM vehicle_car
        WHERE member_no = #{memberNo}
          AND vehicle_type = 'visit'
          AND vehicle_status IN ('WAITING', 'APPROVED')
          AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)
    """)
    int countActiveVisitByMemberNo(Integer memberNo);

    // 차량 삭제
    // 현재 구조에서는 관리자 기능으로 사용한다.
    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int delete(int vehicleCarNo);

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

    // 관리자 차량 상태 변경
    // APPROVED, UNKNOWN, EXPIRED 처리에 사용한다.
    // startDate/endDate가 null이면 기존 DB 값을 유지한다.
    // 방문차량 승인 시 신청 시간 정보를 보존하기 위한 구조다.
    @Update("""
    UPDATE vehicle_car
    SET vehicle_status = #{vehicleStatus},
        member_no = COALESCE(#{memberNo}, member_no),
        start_date = COALESCE(#{startDate}, start_date),
        end_date = COALESCE(#{endDate}, end_date),
        approved_at = CASE
            WHEN #{vehicleStatus} = 'APPROVED'
            THEN CURRENT_TIMESTAMP
            ELSE approved_at
        END
    WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int updateStatus(VehicleDTO dto);
}