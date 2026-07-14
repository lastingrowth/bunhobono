package api.trash_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrashMapper {
    // 휴지통 전체 또는 유형별 목록 조회
    @Select("<script>" +
            "SELECT trash_no, data_type, original_no, " +
            "data_json::text AS data_json, delete_type, deleted_at, purge_at " +
            "FROM trash_bin " +
            "<if test='dataType != null and dataType != \"\"'>" +
            "WHERE data_type = #{dataType} " +
            "</if>" +
            "ORDER BY deleted_at DESC" +
            "</script>")
    List<TrashDTO> list(
            @Param("dataType") String dataType
    );

    // 휴지통 상세 조회
    @Select("SELECT trash_no, data_type, original_no, " +
            "data_json::text AS data_json, delete_type, deleted_at, purge_at " +
            "FROM trash_bin " +
            "WHERE trash_no = #{trashNo}")
    TrashDTO detail(long trashNo);

    // camera_data를 JSON으로 변환해 휴지통에 저장
    @Insert("INSERT INTO trash_bin " +
            "(data_type, original_no, data_json, delete_type) " +
            "SELECT 'CAMERA_DATA', cd.camera_data_no, to_jsonb(cd), #{deleteType} " +
            "FROM camera_data cd " +
            "WHERE cd.camera_data_no = #{cameraDataNo}")
    int saveCameraData(
            @Param("cameraDataNo") int cameraDataNo,
            @Param("deleteType") String deleteType
    );

    // car_log를 JSON으로 변환해 휴지통에 저장
    @Insert("INSERT INTO trash_bin " +
            "(data_type, original_no, data_json, delete_type) " +
            "SELECT 'CAR_LOG', cl.car_log_no, to_jsonb(cl), #{deleteType} " +
            "FROM car_log cl " +
            "WHERE cl.car_log_no = #{carLogNo}")
    int saveCarLog(
            @Param("carLogNo") int carLogNo,
            @Param("deleteType") String deleteType
    );

    // notice를 JSON으로 변환해 휴지통에 저장
    @Insert("INSERT INTO trash_bin " +
            "(data_type, original_no, data_json, delete_type) " +
            "SELECT 'NOTICE', n.notice_no, to_jsonb(n), #{deleteType} " +
            "FROM notice n " +
            "WHERE n.notice_no = #{noticeNo}")
    int saveNotice(
            @Param("noticeNo") int noticeNo,
            @Param("deleteType") String deleteType
    );

    // 복원 완료 또는 영구 삭제 시 휴지통 행 제거
    @Delete("DELETE FROM trash_bin " +
            "WHERE trash_no = #{trashNo}")
    int deleteTrash(long trashNo);
}