package api.faq_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FaqMapper {

    // 자주하는 질문 등록
    @Insert("INSERT INTO faq (category,question, answer) " +
            " VALUES (#{category}, #{question}, #{answer})")
    int insert(FaqDTO dto);

    // 자주하는 질문 목록 조회
    @Select("SELECT faq_no, category, question, answer, created_at, updated_at " +
            " FROM faq " +
            " ORDER BY created_at DESC, faq_no DESC")
    List<FaqDTO> list();

    // 자주하는 질문 수정
    @Update("UPDATE faq " +
            " SET category = #{category}, " +
            " question = #{question}, " +
            " answer = #{answer}, " +
            " updated_at = CURRENT_TIMESTAMP " +
            " WHERE faq_no = #{faqNo}")
    int update(FaqDTO dto);

    // 자주하는 질문 삭제
    @Delete("DELETE FROM faq WHERE faq_no = #{faq_no}")
    int delete(int faq_no);

}
