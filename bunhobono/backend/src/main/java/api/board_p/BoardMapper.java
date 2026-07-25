package api.board_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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
}