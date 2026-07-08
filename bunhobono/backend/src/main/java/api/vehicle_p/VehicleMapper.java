package api.vehicle_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    // 차량 전체 목록 조회 - 관리자
    @Select("""
        SELECT
            vehicle_car_no,
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> list();

    // 차량번호 검색 - 관리자
    @Select("""
        SELECT
            vehicle_car_no,
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE car_no LIKE CONCAT('%', #{carNo}, '%')
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> search(String carNo);

    // 차량 상세 조회 - 관리자
    @Select("""
        SELECT
            vehicle_car_no,
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleNo}
    """)
    VehicleDTO detail(Integer vehicleNo);

    // 차량 등록 - 관리자
    @Insert("""
        INSERT INTO vehicle_car (
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        )
        VALUES (
            #{registeredNo},
            #{vehicleType},
            #{carNo},
            #{vehicleStatus},
            #{startDate},
            #{endDate},
            #{approvedNo},
            #{approvedAt}
        )
    """)
    int insert(VehicleDTO dto);

    // 차량 정보 수정 - 관리자
    @Update("""
        UPDATE vehicle_car
        SET
            registered_no = #{registeredNo},
            vehicle_type = #{vehicleType},
            car_no = #{carNo},
            vehicle_status = #{vehicleStatus},
            start_date = #{startDate},
            end_date = #{endDate},
            approved_no = #{approvedNo},
            approved_at = #{approvedAt}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int update(VehicleDTO dto);

    // 차량 삭제 - 관리자
    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleNo}
    """)
    int delete(Integer vehicleNo);

    // 승인 대기 차량 목록 - 관리자
    @Select("""
        SELECT
            vehicle_car_no,
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE vehicle_status = 'WAITING'
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> approveList();

    // 승인 상태 변경 - 관리자
    @Update("""
        UPDATE vehicle_car
        SET
            vehicle_status = #{vehicleStatus},
            start_date = #{startDate},
            end_date = #{endDate},
            approved_no = #{approvedNo},
            approved_at = #{approvedAt}
        WHERE vehicle_car_no = #{vehicleCarNo}
    """)
    int changeStatus(VehicleDTO dto);

    // 로그인 ID로 member_no 조회 - 입주민
    @Select("""
        SELECT member_no
        FROM member
        WHERE login_id = #{loginId}
    """)
    Integer findMemberNoByLoginId(String loginId);

    // 본인 차량 목록 조회 - 입주민
    @Select("""
        SELECT
            vehicle_car_no,
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE registered_no = #{registeredNo}
        ORDER BY vehicle_car_no DESC
    """)
    List<VehicleDTO> resList(Integer registeredNo);

    // 본인 차량 상세 조회 - 입주민
    @Select("""
        SELECT
            vehicle_car_no,
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleNo}
          AND registered_no = #{registeredNo}
    """)
    VehicleDTO resDetail(
            @Param("registeredNo") Integer registeredNo,
            @Param("vehicleNo") Integer vehicleNo
    );

    // 차량 등록 신청 - 입주민
    @Insert("""
        INSERT INTO vehicle_car (
            registered_no,
            vehicle_type,
            car_no,
            vehicle_status,
            start_date,
            end_date,
            approved_no,
            approved_at
        )
        VALUES (
            #{registeredNo},
            #{vehicleType},
            #{carNo},
            #{vehicleStatus},
            #{startDate},
            #{endDate},
            #{approvedNo},
            #{approvedAt}
        )
    """)
    int resInsert(VehicleDTO dto);

    // 본인 차량 수정 신청 - 입주민
    @Update("""
        UPDATE vehicle_car
        SET
            vehicle_type = #{vehicleType},
            car_no = #{carNo},
            vehicle_status = #{vehicleStatus},
            start_date = #{startDate},
            end_date = #{endDate},
            approved_no = #{approvedNo},
            approved_at = #{approvedAt}
        WHERE vehicle_car_no = #{vehicleCarNo}
          AND registered_no = #{registeredNo}
    """)
    int resUpdate(VehicleDTO dto);

    // 본인 차량 삭제 - 입주민
    @Delete("""
        DELETE FROM vehicle_car
        WHERE vehicle_car_no = #{vehicleNo}
          AND registered_no = #{registeredNo}
    """)
    int resDelete(
            @Param("registeredNo") Integer registeredNo,
            @Param("vehicleNo") Integer vehicleNo
    );


    // 로그인 ID로 입주민 화면 표시용 정보 조회
    @Select("""
    SELECT
        member_no,
        login_id,
        mem_name,
        mem_dong,
        mem_ho
    FROM member
    WHERE login_id = #{loginId}
    """)
    ResVehicleMemberDTO findLoginMemberInfo(String loginId);



}