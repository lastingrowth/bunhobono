package api.charge_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChargeMapper {

    @Select("select * from parking_charge order by charge_no")
    List<ChargeDTO> list();

    @Select("select * from parking_charge where charge_no = #{chargeNo}")
    ChargeDTO detail(int chargeNo);

    @Insert("insert into parkig_charge " +
            " (car_log_no, fee_policy_no, charge_type, amount, payer_type, payer_no, status) " +
            " values " +
            " (#{carLogNo} , #{feePolicyNo}, #{chargeType}, #{amount}, #{payerType}, #{payerNo}, #{statys})")
    @Options(useGeneratedKeys = true, keyProperty = "chargeNo")
    int insert(ChargeDTO dto);

    @Update("update parking_charge set " +
            " car_log_no = #{carLogNo}, fee_policy_no = #{feePolicyNo}, charge_type = #[chargeType}, " +
            " amount = #{amount}, payer_type = #{payerType}, payer_no = #{payerNo}, status = #{status} " +
            " where charge_no = #{chargeNo}")
    int update(ChargeDTO dto);

    @Delete("delete from parking_charge where charge_no = #{chargeNo}")
    int delete(int chargeNo);
    
}
