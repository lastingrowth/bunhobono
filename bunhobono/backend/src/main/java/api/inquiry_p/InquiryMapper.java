package api.inquiry_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InquiryMapper {

    // 문의 등록
    @Insert("INSERT INTO inquiry (member_no, root_inquiry_no, category, title, content) " +
            " VALUES (#{memberNo}, #{rootInquiryNo}, #{category}, #{title}, #{content})")
    int insert(InquiryDTO dto);

    // 입주민 문의 목록 조회 (본인 문의 목록만)
    @Select("SELECT inquiry_no, member_no, root_inquiry_no, category, title, status, created_at, answered_at " +
            " FROM inquiry " +
            " WHERE member_no = #{memberNo} " +
            " ORDER BY created_at DESC, inquiry_no DESC")
    List<InquiryDTO> listByMemberNo(int memberNo);

    // 관리자 목록 조회 (답변 대기 / 답변 완료)
    @Select("SELECT inquiry_no, member_no, root_inquiry_no, category, title, status, created_at, answered_at " +
            " FROM inquiry " +
            " WHERE status = #{status} " +
            " ORDER BY created_at DESC, inquiry_no DESC ")
    List<InquiryDTO> listByStatus(String status);

    // 입주민 본인 문의 상세 조회
    @Select("SELECT inquiry_no, member_no, root_inquiry_no, category, title, content, " +
            " status, answer_content, answered_by, answered_at, created_at " +
            " FROM inquiry " +
            " WHERE inquiry_no = #{inquiryNo} " +
            " AND member_no = #{memberNo} ")
    InquiryDTO detailByMember(@Param("inquiryNo") int inquiryNo, @Param("memberNo") int memberNo);

    // 관리자 문의 상세 조회
    @Select("SELECT inquiry_no, member_no, root_inquiry_no, category, title, content, " +
            " status, answer_content, answered_by, answered_at, created_at " +
            " FROM inquiry " +
            " WHERE inquiry_no = #{inquiryNo}")
    InquiryDTO detail(int inquiryNo);

    // 같은 문의 흐름에 답변대기 문의가 있는지 확인
    @Select("SELECT COUNT(*) " +
            " FROM inquiry " +
            " WHERE (inquiry_no = #{inquiryNo} " +
            "   OR root_inquiry_no = #{rootInquiryNo}) " +
            " AND status = 'WAITING' ")
    int countWaitingByRoot(int rootInquiryNo);

    // 관리자 답변 등록
    @Update("UPDATE inquiry " +
            " SET answer_content = #{answerContent}, " +
            " answered_by = #{answeredBy}, " +
            " answered_at = CURRENT_TIMESTAMP, " +
            " status = 'ANSWERED' " +
            " WHERE inquiry_no = #{inquiryNo} " +
            " AND status = 'WAITING' ")
    int answer(InquiryDTO dto);

    // 답변 완료 후 3개월이 지난 문의 조회
    @Select("SELECT i.inquiry_no " +
            " FROM inquiry i " +
            " WHERE COALESCE(i.root_inquiry_no, i.inquiry_no) IN ( " +
            "   SELECT COALESCE(root_inquiry_no, inquiry_no) " +
            "   FROM inquiry " +
            "   GROUP BY COALESCE(root_inquiry_no, inquiry_no) " +
            "   HAVING COUNT(*) FILTER ( " +
            "       WHERE status <> 'ANSWERED' " +
            "           OR answered_at IS NULL ) = 0 " +
            "       AND MAX(answered_at) < CURRENT_TIMESTAMP - INTERVAL '3 months' " +
            " )" +
            " ORDER BY " +
            "   COALESCE(i.root_inquiry_no, i.inquiry_no), " +
            "   CASE" +
            "       WHEN i.root_inquiry_no IS NULL THEN 1" +
            "       ELSE 0 " +
            "   END, " +
            "   i.inquiry_no DESC ")
    List<Integer> findInquiryNosForTrash();

    // 휴지통 저장이 끝난 문의 삭제
    @Delete("DELETE FROM inquiry WHERE inquiry_no = #{inquiryNo}")
    int delete(int inquiryNo);

    // 입주민 본인 지난 문의 목록 조회
    @Select("""
        SELECT
            tb.trash_no,
            (tb.data_json ->> 'inquiry_no')::int AS inquiry_no,
            (tb.data_json ->> 'member_no')::int AS member_no,
            NULLIF(
                tb.data_json ->> 'root_inquiry_no',
                ''
            )::int AS root_inquiry_no,
            tb.data_json ->> 'category' AS category,
            tb.data_json ->> 'title' AS title,
            tb.data_json ->> 'content' AS content,
            tb.data_json ->> 'status' AS status,
            tb.data_json ->> 'answer_content' AS answer_content,
            NULLIF(
                tb.data_json ->> 'answered_by',
                ''
            )::int AS answered_by,
            NULLIF(
                tb.data_json ->> 'answered_at',
                ''
            )::timestamp AS answered_at,
            NULLIF(
                tb.data_json ->> 'created_at',
                ''
            )::timestamp AS created_at
        FROM trash_bin tb
        WHERE tb.data_type = 'INQUIRY'
          AND tb.delete_type = 'SCHEDULED'
          AND (tb.data_json ->> 'member_no')::int = #{memberNo}
        ORDER BY
            NULLIF(
                tb.data_json ->> 'answered_at',
                ''
            )::timestamp DESC,
            tb.trash_no DESC
        """)
    List<InquiryDTO> archivedListByMemberNo(
            @Param("memberNo") int memberNo
    );

}
