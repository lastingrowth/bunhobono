package api.policy_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PolicyMapper {

    @Select("select * from parking_fee_policy order by fee_policy_no")
    List<PolicyDTO> list();

    @Select("select * from parking_fee_policy where fee_policy_no = #{feePolicyNo}")
    PolicyDTO detail (int feePolicyNo);

    @Insert("insert into parking_fee_policy " +
            " (parking_no, vehicle_type, free_minutes, unit_minutes, unit_fee, daily_max_fee, is_active " +
            " values (#{parkingNo}, #{vehicleType}, #{freeMinutes}, #{unitMinutes}, #{unitFee}, #{dailyMaxFee}, #{isActive} ")
    @Options(useGeneratedKeys = true, keyProperty = "feePolicyNo")
    int insert(PolicyDTO dto);

    @Update("update parking_fee_policy set " +
            " parking_no = #{parkingNo}, vehicle_type = #{vehicleType}, " +
            " free_minutes = #{freeMinutes}, unit_minutes = #{unitMinutes}, unit_fee = #{unitFee}, " +
            " daily_max_fee = #{dailyMaxFee}, is_active = #{isActive} " +
            " where fee_policy_no = #{feePolicyNo}")
    int update(PolicyDTO dto);

    @Delete("delete from parking_fee_policy where fee_policy_no = #{feePolicyNo}")
    int delete(int feePolicyNo);

}
