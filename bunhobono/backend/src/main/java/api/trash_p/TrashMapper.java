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
            "COALESCE(data_json ->> 'captured_car_no', data_json ->> 'car_no', " +
            "data_json ->> 'snapshot_car_no', data_json ->> 'snapshot_captured_car_no', " +
            "(SELECT cd.car_no FROM camera_data cd " +
            "WHERE cd.camera_data_no = NULLIF(data_json ->> 'camera_data_no', '')::int)) AS \"carNo\", " +
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
            "COALESCE(data_json ->> 'captured_car_no', data_json ->> 'car_no', " +
            "data_json ->> 'snapshot_car_no', data_json ->> 'snapshot_captured_car_no', " +
            "(SELECT cd.car_no FROM camera_data cd " +
            "WHERE cd.camera_data_no = NULLIF(data_json ->> 'camera_data_no', '')::int)) AS \"carNo\", " +
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
            "SELECT 'CAR_LOG', cl.car_log_no, " +
            "to_jsonb(cl) || jsonb_build_object(" +
            "'captured_car_no', COALESCE(cd.car_no, cl.snapshot_car_no)), " +
            "#{deleteType} " +
            "FROM car_log cl " +
            "LEFT JOIN camera_data cd ON cl.camera_data_no = cd.camera_data_no " +
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

    @Insert("""
        INSERT INTO camera_data
            (camera_data_no, camera_no, vehicle_car_no, car_no, capture_time,
             image_path, recognition_state, confidence_score)
        SELECT
            (tb.data_json ->> 'camera_data_no')::int,
            (tb.data_json ->> 'camera_no')::int,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM vehicle_car vc
                    WHERE vc.vehicle_car_no = NULLIF(tb.data_json ->> 'vehicle_car_no', '')::int
                ) THEN NULLIF(tb.data_json ->> 'vehicle_car_no', '')::int
                ELSE NULL
            END,
            tb.data_json ->> 'car_no',
            NULLIF(tb.data_json ->> 'capture_time', '')::timestamp,
            tb.data_json ->> 'image_path',
            NULLIF(tb.data_json ->> 'recognition_state', '')::boolean,
            NULLIF(tb.data_json ->> 'confidence_score', '')::double precision
        FROM trash_bin tb
        WHERE tb.trash_no = #{trashNo}
          AND tb.data_type = 'CAMERA_DATA'
        """)
    int restoreCameraData(long trashNo);

    @Insert("""
        INSERT INTO car_log
            (car_log_no, vehicle_car_no, camera_data_no, in_gate_no, in_time,
             out_gate_no, out_time, free_time, snapshot_car_no)
        SELECT
            (tb.data_json ->> 'car_log_no')::int,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM vehicle_car vc
                    WHERE vc.vehicle_car_no = NULLIF(tb.data_json ->> 'vehicle_car_no', '')::int
                ) THEN NULLIF(tb.data_json ->> 'vehicle_car_no', '')::int
                ELSE NULL
            END,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM camera_data cd
                    WHERE cd.camera_data_no = NULLIF(tb.data_json ->> 'camera_data_no', '')::int
                ) THEN NULLIF(tb.data_json ->> 'camera_data_no', '')::int
                ELSE NULL
            END,
            NULLIF(tb.data_json ->> 'in_gate_no', '')::int,
            NULLIF(tb.data_json ->> 'in_time', '')::timestamp,
            NULLIF(tb.data_json ->> 'out_gate_no', '')::int,
            NULLIF(tb.data_json ->> 'out_time', '')::timestamp,
            NULLIF(tb.data_json ->> 'free_time', '')::int,
            COALESCE(tb.data_json ->> 'snapshot_car_no', tb.data_json ->> 'captured_car_no')
        FROM trash_bin tb
        WHERE tb.trash_no = #{trashNo}
          AND tb.data_type = 'CAR_LOG'
        """)
    int restoreCarLog(long trashNo);

    @Insert("""
        INSERT INTO notice
            (notice_no, car_log_no, detect_at, stay_days, alert_stat,
             handled_by_member_no, handled_at, snapshot_car_log_no,
             snapshot_registered_car_no, snapshot_captured_car_no,
             snapshot_car_kind, snapshot_parking_name, snapshot_in_time)
        SELECT
            (tb.data_json ->> 'notice_no')::int,
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM car_log cl
                    WHERE cl.car_log_no = NULLIF(tb.data_json ->> 'car_log_no', '')::int
                ) THEN NULLIF(tb.data_json ->> 'car_log_no', '')::int
                ELSE NULL
            END,
            NULLIF(tb.data_json ->> 'detect_at', '')::timestamp,
            NULLIF(tb.data_json ->> 'stay_days', '')::int,
            tb.data_json ->> 'alert_stat',
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM member m
                    WHERE m.member_no = NULLIF(tb.data_json ->> 'handled_by_member_no', '')::int
                ) THEN NULLIF(tb.data_json ->> 'handled_by_member_no', '')::int
                ELSE NULL
            END,
            NULLIF(tb.data_json ->> 'handled_at', '')::timestamp,
            NULLIF(tb.data_json ->> 'snapshot_car_log_no', '')::int,
            tb.data_json ->> 'snapshot_registered_car_no',
            tb.data_json ->> 'snapshot_captured_car_no',
            tb.data_json ->> 'snapshot_car_kind',
            tb.data_json ->> 'snapshot_parking_name',
            NULLIF(tb.data_json ->> 'snapshot_in_time', '')::timestamp
        FROM trash_bin tb
        WHERE tb.trash_no = #{trashNo}
          AND tb.data_type = 'NOTICE'
        """)
    int restoreNotice(long trashNo);

    //검색
    @Select("SELECT trash_no, data_type, original_no, " +
            "COALESCE(data_json ->> 'captured_car_no', data_json ->> 'car_no', " +
            "data_json ->> 'snapshot_car_no', data_json ->> 'snapshot_captured_car_no', " +
            "(SELECT cd.car_no FROM camera_data cd " +
            "WHERE cd.camera_data_no = NULLIF(data_json ->> 'camera_data_no', '')::int)) AS \"carNo\", " +
            "data_json::text AS data_json, delete_type, deleted_at, purge_at " +
            "FROM trash_bin " +
            "WHERE COALESCE(" +
            "data_json ->> 'captured_car_no', " +
            "data_json ->> 'car_no', " +
            "data_json ->> 'snapshot_car_no', " +
            "data_json ->> 'snapshot_captured_car_no', " +
            "(SELECT cd.car_no FROM camera_data cd " +
            "WHERE cd.camera_data_no = NULLIF(data_json ->> 'camera_data_no', '')::int)" +
            ") LIKE CONCAT('%', #{carNo}, '%') " +
            "ORDER BY deleted_at DESC")
    List<TrashDTO> searchByCarNo(@Param("carNo") String carNo);
}
