package api.memberarchive_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MemberArchiveMapper {

    // 전출 확정되어 archive에 보관된 회원 목록을 조회한다.
    // 최근 전출 확정된 기록이 위에 오도록 정렬한다.
    @Select("""
        SELECT
            archive_no,
            original_member_no,
            login_id,
            mem_name,
            mem_phone,
            role,
            mem_status,
            mem_dong,
            mem_ho,
            create_at,
            delete_at,
            archived_at
        FROM member_archive
        ORDER BY archived_at DESC, archive_no DESC
        """)
    List<MemberArchiveDTO> list();

    // 전출 회원 이력을 archive에서 영구 삭제한다.
    // member 원본이 아니라 member_archive 기록만 삭제한다.
    @Delete("""
        DELETE FROM member_archive
        WHERE archive_no = #{archiveNo}
        """)
    int delete(long archiveNo);
}
