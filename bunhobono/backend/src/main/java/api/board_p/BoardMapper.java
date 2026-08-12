package api.board_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BoardMapper {

    // 공지 조회에서 공통으로 사용하는 컬럼과 게시 상태를 정의한다.
    String COLUMNS =
            "board_no, title, content, image_path, image_name, image_type, " +
                    "(image_path IS NOT NULL) AS has_image, " +
                    "start_at, end_at, " +
                    "CASE " +
                    "WHEN start_at > CURRENT_TIMESTAMP THEN '게시예정' " +
                    "WHEN end_at IS NOT NULL " +
                    "AND end_at < CURRENT_TIMESTAMP THEN '기간만료' " +
                    "ELSE '진행중' " +
                    "END AS period_status, " +
                    "active, created_by, created_at, updated_at ";

    // 공지사항을 등록하고 생성된 번호를 DTO에 저장한다.
    @Insert(
            "INSERT INTO board ( " +
                    "title, content, image_path, image_name, image_type, " +
                    "start_at, end_at, active, created_by " +
                    ") " +
                    "VALUES ( " +
                    "#{title}, #{content}, #{imagePath}, #{imageName}, #{imageType}, " +
                    "#{startAt}, #{endAt}, #{active}, #{createdBy} " +
                    ")"
    )
    @Options(useGeneratedKeys = true, keyProperty = "boardNo")
    int insert(BoardDTO dto);

    // 관리자용 전체 공지사항 목록을 조회한다.
    @Select(
            "SELECT " + COLUMNS +
                    "FROM board " +
                    "ORDER BY created_at DESC, board_no DESC"
    )
    List<BoardDTO> list();

    // 입주민용 게시 중인 공지사항 목록을 조회한다.
    @Select(
            "SELECT " + COLUMNS +
                    "FROM board " +
                    "WHERE active = TRUE " +
                    "AND start_at <= CURRENT_TIMESTAMP " +
                    "AND (end_at IS NULL OR end_at >= CURRENT_TIMESTAMP) " +
                    "ORDER BY created_at DESC, board_no DESC"
    )
    List<BoardDTO> activeList();

    // 공지사항 번호로 전체 공지 상세를 조회한다.
    @Select(
            "SELECT " + COLUMNS +
                    "FROM board " +
                    "WHERE board_no = #{boardNo}"
    )
    BoardDTO detail(int boardNo);

    // 게시 중인 공지사항만 상세 조회한다.
    @Select(
            "SELECT " + COLUMNS +
                    "FROM board " +
                    "WHERE board_no = #{boardNo} " +
                    "AND active = TRUE " +
                    "AND start_at <= CURRENT_TIMESTAMP " +
                    "AND (end_at IS NULL OR end_at >= CURRENT_TIMESTAMP)"
    )
    BoardDTO activeDetail(int boardNo);

    // 공지 내용, 게시기간과 이미지 정보를 수정한다.
    @Update(
            "UPDATE board " +
                    "SET title = #{title}, " +
                    "content = #{content}, " +
                    "image_path = #{imagePath}, " +
                    "image_name = #{imageName}, " +
                    "image_type = #{imageType}, " +
                    "start_at = #{startAt}, " +
                    "end_at = #{endAt}, " +
                    "active = #{active}, " +
                    "updated_at = CURRENT_TIMESTAMP " +
                    "WHERE board_no = #{boardNo}"
    )
    int update(BoardDTO dto);

    // 공지사항 번호에 해당하는 행을 삭제한다.
    @Delete(
            "DELETE FROM board " +
                    "WHERE board_no = #{boardNo}"
    )
    int delete(int boardNo);


    // ================================
    // 댓글
    // ================================
    // [댓글] 공지사항의 댓글을 계층 순서로 조회한다.
    @Select("""
            SELECT bc.comment_no,
                   bc.board_no,
                   bc.parent_comment_no,
                   bc.content AS comment_content,
                   bc.created_at AS comment_created_at,
                   bc.updated_at AS comment_updated_at,
                   m.mem_name AS comment_writer_name,
                   m.role AS comment_writer_role,
                   au.dong AS comment_writer_dong,
                   au.ho AS comment_writer_ho,
                   (m.login_id = #{loginId}) AS my_comment
            FROM board_comment bc
            JOIN member m ON m.member_no = bc.member_no
            LEFT JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no
            WHERE bc.board_no = #{boardNo}
            ORDER BY bc.created_at DESC, bc.comment_no DESC
            """)
    List<BoardDTO> commentList(
            @Param("boardNo") int boardNo,
            @Param("loginId") String loginId
    );

    // 로그인한 사용자의 이름을 조회한다.
    @Select("SELECT mem_name FROM member WHERE login_id = #{loginId}")
    String findCommentWriterName(@Param("loginId") String loginId);

    // 댓글 번호로 댓글과 작성자를 조회한다.
    @Select("""
            SELECT bc.comment_no,
                   bc.board_no,
                   bc.parent_comment_no,
                   bc.content AS comment_content,
                   bc.created_at AS comment_created_at,
                   bc.updated_at AS comment_updated_at,
                   m.mem_name AS comment_writer_name,
                   m.role AS comment_writer_role,
                   au.dong AS comment_writer_dong,
                   au.ho AS comment_writer_ho
            FROM board_comment bc
            JOIN member m ON m.member_no = bc.member_no
            LEFT JOIN apartment_unit au ON au.apartment_unit_no = m.unit_no
            WHERE bc.comment_no = #{commentNo}
            """)
    BoardDTO findComment(@Param("commentNo") int commentNo);

    // 선택한 댓글이 몇 단계인지 부모 관계를 따라 계산.
    @Select("""
            WITH RECURSIVE parent_tree AS (
                SELECT comment_no, parent_comment_no, 1 AS comment_depth
                FROM board_comment
                WHERE comment_no = #{commentNo}
                UNION ALL
                SELECT parent.comment_no,
                       parent.parent_comment_no,
                       child.comment_depth + 1
                FROM board_comment parent
                JOIN parent_tree child
                  ON parent.comment_no = child.parent_comment_no
            )
            SELECT MAX(comment_depth) FROM parent_tree
            """)
    Integer findCommentDepth(@Param("commentNo") int commentNo);


    // 로그인 사용자의 회원 번호로 댓글을 등록.
    @Insert("""
            INSERT INTO board_comment (
                board_no, member_no, parent_comment_no, content
            )
            SELECT #{boardNo}, member_no, #{dto.parentCommentNo}, #{dto.commentContent}
            FROM member
            WHERE login_id = #{loginId}
            """)
    @Options(useGeneratedKeys = true, keyProperty = "dto.commentNo")
    int insertComment(
            @Param("boardNo") int boardNo,
            @Param("loginId") String loginId,
            @Param("dto") BoardDTO dto
    );

    // 작성자 본인의 댓글만 수정.
    @Update("""
            UPDATE board_comment bc
            SET content = #{commentContent}, updated_at = CURRENT_TIMESTAMP
            FROM member m
            WHERE bc.comment_no = #{commentNo}
              AND bc.board_no = #{boardNo}
              AND bc.member_no = m.member_no
              AND m.login_id = #{loginId}
            """)
    int updateComment(
            @Param("boardNo") int boardNo,
            @Param("commentNo") int commentNo,
            @Param("loginId") String loginId,
            @Param("commentContent") String commentContent
    );

    // 선택한 댓글과 모든 하위 댓글을 함께 삭제.
    @Delete("""
            WITH RECURSIVE comment_tree AS (
                SELECT bc.comment_no
                FROM board_comment bc
                JOIN member m ON m.member_no = bc.member_no
                WHERE bc.comment_no = #{commentNo}
                  AND bc.board_no = #{boardNo}
                  AND (#{admin} = TRUE OR m.login_id = #{loginId})
                UNION ALL
                SELECT child.comment_no
                FROM board_comment child
                JOIN comment_tree parent
                  ON child.parent_comment_no = parent.comment_no
            )
            DELETE FROM board_comment
            WHERE comment_no IN (SELECT comment_no FROM comment_tree)
            """)
    int deleteCommentTree(
            @Param("boardNo") int boardNo,
            @Param("commentNo") int commentNo,
            @Param("loginId") String loginId,
            @Param("admin") boolean admin
    );

    // 공지사항에 연결된 모든 댓글을 삭제.
    @Delete("DELETE FROM board_comment WHERE board_no = #{boardNo}")
    int deleteCommentsByBoardNo(@Param("boardNo") int boardNo);
}
