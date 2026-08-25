package api.bill_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BillMapper {

    // 조건에 맞는 정산서 목록 조회
    @Select("<script>" +
            " SELECT * " +
            " FROM bill_detail " +
            " <where> " +
            " <if test='snapshotCarNo != null and snapshotCarNo != \"\"'> " +
            " AND snapshot_car_no LIKE CONCAT('%', #{snapshotCarNo}) " +
            " </if> " +
            " <if test='carLogNo != null'> " +
            " AND car_log_no = #{carLogNo} " +
            " </if> " +
            " <if test='snapshotCarLogNo != null'> " +
            " AND snapshot_car_log_no = #{snapshotCarLogNo} " +
            " </if> " +
            " <if test='billStatus != null and billStatus != \"\"'> " +
            " AND bill_status = #{billStatus} " +
            " </if> " +
            " <if test='parkingCode != null and parkingCode != \"\"'> " +
            " AND parking_code = #{parkingCode} " +
            " </if> " +
            " <if test='carKind != null and carKind != \"\"'> " +
            " AND car_kind = #{carKind} " +
            " </if> " +
            " </where> " +
            " ORDER BY issued_at DESC, bill_no DESC " +
            "</script>")
    List<BillDTO> list(BillDTO condition);

    // 정산서 번호 또는 결제 주문번호로 정산서 상세 조회
    @Select("<script>" +
            " SELECT * " +
            " FROM bill_detail " +
            " WHERE " +
            " <choose> " +
            " <when test='billNo != null'> " +
            " bill_no = #{billNo} " +
            " </when> " +
            " <when test='paymentOrderId != null and paymentOrderId != \"\"'> " +
            " payment_order_id = #{paymentOrderId} " +
            " </when> " +
            " <otherwise> " +
            " FALSE " +
            " </otherwise> " +
            " </choose> " +
            "</script>")
    BillDTO detail(BillDTO condition);

    // 정산서 등록
    @Insert("INSERT INTO bill " +
            " (car_log_no, snapshot_car_log_no, snapshot_car_no, fee_rule_no, " +
            " kiosk_no, charge_minutes, bill_amount, bill_status, issued_at) " +
            " VALUES (#{carLogNo}, #{snapshotCarLogNo}, #{snapshotCarNo}, #{feeRuleNo}, " +
            " #{kioskNo}, #{chargeMinutes}, #{billAmount}, #{billStatus}, #{issuedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "billNo", keyColumn = "bill_no")
    int insert(BillDTO dto);

    // 전달된 값으로 정산서 정보 수정
    @Update("<script>" +
            " UPDATE bill " +
            " <set> " +
            " <if test='feeRuleNo != null'> " +
            " fee_rule_no = #{feeRuleNo}, " +
            " </if> " +
            " <if test='kioskNo != null'> " +
            " kiosk_no = #{kioskNo}, " +
            " </if> " +
            " <if test='chargeMinutes != null'> " +
            " charge_minutes = #{chargeMinutes}, " +
            " </if> " +
            " <if test='billAmount != null'> " +
            " bill_amount = #{billAmount}, " +
            " </if> " +
            " <if test='billStatus != null and billStatus != \"\"'> " +
            " bill_status = #{billStatus}, " +
            " </if> " +
            " <if test='paymentOrderId != null and paymentOrderId != \"\"'> " +
            " payment_order_id = #{paymentOrderId}, " +
            " </if> " +
            " <if test='paymentKey != null and paymentKey != \"\"'> " +
            " payment_key = #{paymentKey}, " +
            " </if> " +
            " <if test='paymentMethod != null and paymentMethod != \"\"'> " +
            " payment_method = #{paymentMethod}, " +
            " </if> " +
            " <if test='paidAt != null'> " +
            " paid_at = #{paidAt}, " +
            " </if> " +
            " </set> " +
            " WHERE bill_no = #{billNo} " +
            " <if test='billStatus != null and billStatus != \"\"'> " +
            " AND bill_status = 'UNPAID' " +
            " </if> " +
            "</script>")
    int update(BillDTO dto);

    // 동일 주차 건의 정산서 중복 생성을 막기 위해 입출차 기록을 잠근다.
    @Select("SELECT car_log_no " +
            " FROM car_log " +
            " WHERE car_log_no = #{carLogNo} " +
            " FOR UPDATE")
    Integer lockCarLog(@Param("carLogNo") int carLogNo);

    // 정산서 삭제
    @Delete("DELETE FROM bill " +
            " WHERE bill_no = #{billNo}")
    int delete(@Param("billNo") int billNo);

}
