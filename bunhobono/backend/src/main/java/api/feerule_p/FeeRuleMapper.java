package api.feerule_p;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeeRuleMapper {

    // 조건에 맞는 요금 규칙 목록을 조회한다.
    @Select("<script> " +
            " SELECT fee_rule_no, rule_name, unit_minutes, unit_fee, daily_max_fee, " +
            " exit_grace_minutes, is_default, created_at, effective_from, effective_to " +
            " FROM fee_rule " +
            " <where> " +
            " <if test = 'isDefault != null'> " +
            " AND is_default = #{isDefault} " +
            " </if> " +
            " <if test = 'activeAt != null'> " +
            " AND effective_from &lt;= #{activeAt} " +
            " AND (effective_to IS NULL OR effective_to &gt; #{activeAt}) " +
            " </if> " +
            " </where> " +
            " ORDER BY effective_from DESC, fee_rule_no DESC " +
            " </script> ")
    List<FeeRuleDTO> list (
            @Param("isDefault") Boolean isDefault,
            @Param("activeAt") LocalDateTime activeAt
    );

    // 요금 규칙 번호로 상세정보를 조회한다.
    @Select(" SELECT fee_rule_no, rule_name, unit_minutes, unit_fee, daily_max_fee, " +
            " exit_grace_minutes, is_default, created_at, effective_from, effective_to " +
            " FROM fee_rule " +
            " WHERE fee_rule_no = #{feeRuleNo} ")
    FeeRuleDTO detail(int feeRuleNo);

    // 새로운 요금 규칙을 등록한다.
    @Insert("INSERT INTO fee_rule " +
            " (rule_name, unit_minutes, unit_fee, daily_max_fee, " +
            " exit_grace_minutes, is_default, effective_from, effective_to) " +
            " VALUES ( #{ruleName}, #{unitMinutes}, #{unitFee}, #{dailyMaxFee}, " +
            " #{exitGraceMinutes}, #{isDefault}, #{effectiveFrom}, #{effectiveTo}) ")
    @Options(useGeneratedKeys = true, keyProperty = "feeRuleNo", keyColumn = "fee_rule_no")
    int insert(FeeRuleDTO dto);

    // 요금 규칙의 저장 가능한 값을 수정한다.
    @Update("UPDATE fee_rule " +
            " SET rule_name = #{ruleName}, " +
            " unit_minutes = #{unitMinutes}, " +
            " unit_fee = #{unitFee}, " +
            " daily_max_fee = #{dailyMaxFee}, " +
            " exit_grace_minutes = #{exitGraceMinutes}, " +
            " is_default = #{isDefault}, " +
            " effective_from = #{effectiveFrom}, " +
            " effective_to = #{effectiveTo} " +
            " WHERE fee_rule_no = #{feeRuleNo}")
    int update(FeeRuleDTO dto);
}
