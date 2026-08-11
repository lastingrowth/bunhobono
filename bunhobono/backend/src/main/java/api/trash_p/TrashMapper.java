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
    TrashDTO detail(int trashNo);

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

    // 1:1 문의를 JSON 으로 변환해 휴지통에 저장
    @Insert("INSERT INTO trash_bin " +
            " (data_type, original_no, data_json, delete_type) " +
            " SELECT 'INQUIRY', i.inquiry_no, to_jsonb(i), #{deleteType} " +
            " FROM inquiry i " +
            " WHERE i.inquiry_no = #{inquiryNo}")
    int saveInquiry(
            @Param("inquiryNo") int inquiryNo,
            @Param("deleteType") String deleteType
    );

    // 복원 완료 또는 영구 삭제 시 휴지통 행 제거
    @Delete("DELETE FROM trash_bin " +
            "WHERE trash_no = #{trashNo}")
    int deleteTrash(int trashNo);

    // 카메라 데이터 복원
    @Insert("""
    INSERT INTO camera_data (
        camera_data_no,
        camera_no,
        vehicle_car_no,
        car_no,
        ocr_car_no,
        capture_time,
        image_path,
        crop_image_path,
        recognition_state,
        confidence_score,
        cam_note,
        gate_opened,
        gate_opened_at
    )
    SELECT
        (tb.data_json ->> 'camera_data_no')::INT,
        (tb.data_json ->> 'camera_no')::INT,

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM vehicle_car vc
                WHERE vc.vehicle_car_no =
                      NULLIF(
                          tb.data_json ->> 'vehicle_car_no',
                          ''
                      )::INT
            )
            THEN NULLIF(
                tb.data_json ->> 'vehicle_car_no',
                ''
            )::INT
            ELSE NULL
        END,

        tb.data_json ->> 'car_no',

        COALESCE(
            tb.data_json ->> 'ocr_car_no',
            tb.data_json ->> 'car_no'
        ),

        NULLIF(
            tb.data_json ->> 'capture_time',
            ''
        )::TIMESTAMP,

        tb.data_json ->> 'image_path',
        tb.data_json ->> 'crop_image_path',

        NULLIF(
            tb.data_json ->> 'recognition_state',
            ''
        )::BOOLEAN,

        NULLIF(
            tb.data_json ->> 'confidence_score',
            ''
        )::DOUBLE PRECISION,

        tb.data_json ->> 'cam_note',

        COALESCE(
            NULLIF(
                tb.data_json ->> 'gate_opened',
                ''
            )::BOOLEAN,
            FALSE
        ),

        NULLIF(
            tb.data_json ->> 'gate_opened_at',
            ''
        )::TIMESTAMP

    FROM trash_bin tb
    WHERE tb.trash_no = #{trashNo}
      AND tb.data_type = 'CAMERA_DATA'
    """)
    int restoreCameraData(int trashNo);

    // 입출차 기록 복원
    @Insert("""
    INSERT INTO car_log (
        car_log_no,
        vehicle_car_no,
        camera_data_no,
        out_camera_data_no,
        in_gate_no,
        in_time,
        out_gate_no,
        out_time,
        free_time,
        snapshot_car_no,
        snapshot_car_kind
    )
    SELECT
        (tb.data_json ->> 'car_log_no')::INT,

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM vehicle_car vc
                WHERE vc.vehicle_car_no =
                      NULLIF(
                          tb.data_json ->> 'vehicle_car_no',
                          ''
                      )::INT
            )
            THEN NULLIF(
                tb.data_json ->> 'vehicle_car_no',
                ''
            )::INT
            ELSE NULL
        END,

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM camera_data cd
                WHERE cd.camera_data_no =
                      NULLIF(
                          tb.data_json ->> 'camera_data_no',
                          ''
                      )::INT
            )
            THEN NULLIF(
                tb.data_json ->> 'camera_data_no',
                ''
            )::INT
            ELSE NULL
        END,

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM camera_data cd
                WHERE cd.camera_data_no =
                      NULLIF(
                          tb.data_json ->> 'out_camera_data_no',
                          ''
                      )::INT
            )
            THEN NULLIF(
                tb.data_json ->> 'out_camera_data_no',
                ''
            )::INT
            ELSE NULL
        END,

        NULLIF(tb.data_json ->> 'in_gate_no', '')::INT,
        NULLIF(tb.data_json ->> 'in_time', '')::TIMESTAMP,
        NULLIF(tb.data_json ->> 'out_gate_no', '')::INT,
        NULLIF(tb.data_json ->> 'out_time', '')::TIMESTAMP,
        NULLIF(tb.data_json ->> 'free_time', '')::INT,

        COALESCE(
            tb.data_json ->> 'snapshot_car_no',
            tb.data_json ->> 'captured_car_no'
        ),

        COALESCE(
            tb.data_json ->> 'snapshot_car_kind',
            tb.data_json ->> 'car_kind',
            'UNKNOWN'
        )

    FROM trash_bin tb
    WHERE tb.trash_no = #{trashNo}
      AND tb.data_type = 'CAR_LOG'
    """)
    int restoreCarLog(int trashNo);

    // 휴지통에 보관된 관리자 알림 복원
    @Insert("""
    INSERT INTO notice (
        notice_no,
        notice_type,
        car_log_no,
        camera_data_no,
        detect_at,
        due_at,
        alert_stat,
        handled_by_member_no,
        handled_at,
        snapshot_car_log_no,
        snapshot_camera_data_no,
        snapshot_registered_car_no,
        snapshot_captured_car_no,
        snapshot_car_kind,
        snapshot_parking_name,
        snapshot_in_time,
        snapshot_image_path,
        snapshot_confidence_score
    )
    SELECT
        (tb.data_json ->> 'notice_no')::int,
        tb.data_json ->> 'notice_type',

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM car_log cl
                WHERE cl.car_log_no =
                    NULLIF(
                        tb.data_json ->> 'car_log_no',
                        ''
                    )::int
            )
            THEN NULLIF(
                tb.data_json ->> 'car_log_no',
                ''
            )::int
            ELSE NULL
        END,

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM camera_data cd
                WHERE cd.camera_data_no =
                    NULLIF(
                        tb.data_json ->> 'camera_data_no',
                        ''
                    )::int
            )
            THEN NULLIF(
                tb.data_json ->> 'camera_data_no',
                ''
            )::int
            ELSE NULL
        END,

        COALESCE(
            NULLIF(
                tb.data_json ->> 'detect_at',
                ''
            )::timestamp,
            CURRENT_TIMESTAMP
        ),

        NULLIF(
            tb.data_json ->> 'due_at',
            ''
        )::timestamp,

        COALESCE(
            NULLIF(
                tb.data_json ->> 'alert_stat',
                ''
            ),
            'Unresolved'
        ),

        CASE
            WHEN EXISTS (
                SELECT 1
                FROM member m
                WHERE m.member_no =
                    NULLIF(
                        tb.data_json ->> 'handled_by_member_no',
                        ''
                    )::int
            )
            THEN NULLIF(
                tb.data_json ->> 'handled_by_member_no',
                ''
            )::int
            ELSE NULL
        END,

        NULLIF(
            tb.data_json ->> 'handled_at',
            ''
        )::timestamp,

        NULLIF(
            tb.data_json ->> 'snapshot_car_log_no',
            ''
        )::int,

        NULLIF(
            tb.data_json ->> 'snapshot_camera_data_no',
            ''
        )::int,

        tb.data_json ->> 'snapshot_registered_car_no',
        tb.data_json ->> 'snapshot_captured_car_no',
        tb.data_json ->> 'snapshot_car_kind',
        tb.data_json ->> 'snapshot_parking_name',

        NULLIF(
            tb.data_json ->> 'snapshot_in_time',
            ''
        )::timestamp,

        tb.data_json ->> 'snapshot_image_path',

        NULLIF(
            tb.data_json ->> 'snapshot_confidence_score',
            ''
        )::numeric

    FROM trash_bin tb
    WHERE tb.trash_no = #{trashNo}
      AND tb.data_type = 'NOTICE'
""")
    int restoreNotice(int trashNo);

    // 휴지통에 보관된 1:1 문의 복원
    @Insert("""
        INSERT INTO inquiry
            (inquiry_no, member_no, root_inquiry_no, category, title, content,
             status, answer_content, answered_by, answered_at, created_at)
        SELECT
            (tb.data_json ->> 'inquiry_no')::int,
            (tb.data_json ->> 'member_no')::int,
            NULLIF(tb.data_json ->> 'root_inquiry_no', '')::int,
            tb.data_json ->> 'category',
            tb.data_json ->> 'title',
            tb.data_json ->> 'content',
            COALESCE(
                tb.data_json ->> 'status',
                'WAITING'
            ),
            tb.data_json ->> 'answer_content',
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM member m
                    WHERE m.member_no = NULLIF(tb.data_json ->> 'answered_by', '')::int
                ) THEN NULLIF(tb.data_json ->> 'answered_by', '')::int
                ELSE NULL
            END,
            NULLIF(tb.data_json ->> 'answered_at', '')::timestamp,
            COALESCE(
                NULLIF(tb.data_json ->> 'created_at', '')::timestamp,
                CURRENT_TIMESTAMP
            )
        FROM trash_bin tb
        WHERE tb.trash_no = #{trashNo}
          AND tb.data_type = 'INQUIRY'
        """)
    int restoreInquiry(int trashNo);

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
