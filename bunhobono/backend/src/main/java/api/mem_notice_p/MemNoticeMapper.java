package api.mem_notice_p;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MemNoticeMapper {

    // JWT의 loginId를 기준으로 로그인한 입주민 본인의 알림만 조회한다.
    @Select("""
        SELECT
            mn.mem_notice_no,
            mn.recipient_member_no,
            mn.reference_table,
            mn.reference_no,
            mn.notice_type,
            mn.title,
            mn.message,
            mn.created_at,
            mn.read_at
        FROM mem_notice mn
        JOIN member recipient
          ON recipient.member_no = mn.recipient_member_no
        WHERE recipient.login_id = #{loginId}
        ORDER BY mn.created_at DESC, mn.mem_notice_no DESC
    """)
    List<MemNoticeDTO> list(MemNoticeDTO dto);

    // 로그인한 입주민이 선택한 알림 한 건만 읽음 처리한다.
    @Update("""
        UPDATE mem_notice mn
        SET read_at = CURRENT_TIMESTAMP
        FROM member recipient
        WHERE recipient.member_no = mn.recipient_member_no
          AND recipient.login_id = #{loginId}
          AND mn.mem_notice_no = #{memNoticeNo}
          AND mn.read_at IS NULL
    """)
    int markRead(MemNoticeDTO dto);

    // 다른 입주민의 알림은 번호를 알아도 삭제할 수 없도록 loginId를 함께 확인한다.
    @Delete("""
        DELETE FROM mem_notice mn
        USING member recipient
        WHERE recipient.member_no = mn.recipient_member_no
          AND recipient.login_id = #{loginId}
          AND mn.mem_notice_no = #{memNoticeNo}
    """)
    int delete(MemNoticeDTO dto);

    // 각 기능의 Service에서 발생시킨 입주민 알림을 저장한다.
    // 동일 원본에서 같은 알림을 다시 요청하면 중복 저장하지 않는다.
    @Insert("""
        INSERT INTO mem_notice (
            recipient_member_no,
            reference_table,
            reference_no,
            notice_type,
            title,
            message
        ) VALUES (
            #{recipientMemberNo},
            #{referenceTable},
            #{referenceNo},
            #{noticeType},
            #{title},
            #{message}
        )
        ON CONFLICT ON CONSTRAINT uq_mem_notice_reference
        DO NOTHING
    """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "memNoticeNo",
            keyColumn = "mem_notice_no"
    )
    int insert(MemNoticeDTO dto);
}
