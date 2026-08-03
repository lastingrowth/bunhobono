package api.camera_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 카메라 데이터의 조회, 등록, 수정, 삭제를 담당하는 MyBatis Mapper이다.
 */
@Mapper
public interface CameraMapper {

    /**
     * 카메라, 차단기, 주차장 정보를 조인하여 전체 카메라 목록을 조회한다.
     *
     * @param dto 카메라 조회 조건
     * @return 카메라 목록
     */
    @Select(
            "SELECT ROW_NUMBER() OVER (ORDER BY c.camera_no) AS display_no, " +
                    "c.camera_no, " +
                    "c.gate_no, " +
                    "g.gate_name, " +
                    "c.camera_name, " +
                    "c.camera_type, " +
                    "c.install_date, " +
                    "p.parking_name " +
                    "FROM camera c " +
                    "LEFT JOIN gate g ON c.gate_no = g.gate_no " +
                    "LEFT JOIN parking p ON g.parking_no = p.parking_no " +
                    "ORDER BY c.camera_no"
    )
    List<CameraDTO> list(CameraDTO dto);

    /**
     * 신규 카메라 정보를 등록하고 생성된 카메라 고유번호를 DTO에 저장한다.
     *
     * @param dto 등록할 카메라 정보
     * @return 등록 처리 건수
     */
    @Insert("INSERT INTO camera (gate_no, camera_name, camera_type, install_date) " +
            "VALUES (#{gateNo}, #{cameraName}, #{cameraType}, #{installDate})")
    @Options(useGeneratedKeys = true, keyProperty = "cameraNo")
    int insert(CameraDTO dto);

    /**
     * 카메라 고유번호에 해당하는 카메라 정보를 삭제한다.
     *
     * @param cameraNo 카메라 고유번호
     * @return 삭제 처리 건수
     */
    @Delete("DELETE FROM camera WHERE camera_no = #{cameraNo}")
    int delete(int cameraNo);

    /**
     * 카메라 고유번호에 해당하는 카메라 정보를 수정한다.
     *
     * @param dto 수정할 카메라 정보
     * @return 수정 처리 건수
     */
    @Update("UPDATE camera " +
            "SET gate_no = #{gateNo}, " +
            "    camera_name = #{cameraName}, " +
            "    camera_type = #{cameraType}, " +
            "    install_date = #{installDate} " +
            "WHERE camera_no = #{cameraNo}")
    int update(CameraDTO dto);
}


