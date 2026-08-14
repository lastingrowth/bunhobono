package api.billing_p;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BillingMapper {

    // 현재 적용할 활성 요금 규칙 조회
    @Select("SELECT fee_rule_no, rule_name, unit_minutes, unit_fee, " +
            " daily_max_fee, active, created_at " +
            " FROM fee_rule " +
            " WHERE active = TRUE " +
            " ORDER BY fee_rule_no DESC " +
            " LIMIT 1")
    FeeRuleDTO findActiveFeeRule();

    // 차량번호로 현재 주차 중인 입출차 기록 조회
    @Select("SELECT cl.car_log_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time " +
            " FROM car_log cl " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " WHERE cl.out_time IS NULL " +
            " AND (cl.snapshot_car_no = #{carNo} " +
            " OR vc.car_no = #{carNo}) " +
            " ORDER BY cl.in_time DESC " +
            " LIMIT 1")
    BillDTO findOpenCarLogByCarNo(String carNo);

    // 차량번호 뒤 4자리로 현재 주차 중인 차량 목록 조회
    @Select("SELECT cl.car_log_no, cl.camera_data_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time " +
            " FROM car_log cl " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " WHERE cl.out_time IS NULL " +
            " AND RIGHT(COALESCE(cl.snapshot_car_no, vc.car_no), 4) = #{lastFourDigits} " +
            " ORDER BY cl.in_time DESC")
    List<BillDTO> findOpenCarLogsByLastFourDigits(String lastFourDigits);

    // 입출차 기록에 연결된 정산서 조회
    @Select("SELECT b.bill_no, b.car_log_no, b.fee_rule_no, " +
            " b.kiosk_no, b.charge_minutes, b.bill_amount, " +
            " b.bill_status, b.payment_order_id, b.payment_key, " +
            " b.payment_method, b.issued_at, b.paid_at, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time, " +
            " fr.rule_name, fr.unit_minutes, " +
            " fr.unit_fee, fr.daily_max_fee " +
            " FROM bill b " +
            " JOIN car_log cl ON b.car_log_no = cl.car_log_no " +
            " JOIN fee_rule fr ON b.fee_rule_no = fr.fee_rule_no " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " WHERE b.car_log_no = #{carLogNo}")
    BillDTO findByCarLogNo(int carLogNo);

    // 정산 대상이 입주민이 등록한 방문차량이면 등록자와 차량번호 조회
    @Select("SELECT vc.member_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no " +
            " FROM car_log cl " +
            " JOIN vehicle_car vc " +
            " ON vc.vehicle_car_no = cl.vehicle_car_no " +
            " WHERE cl.car_log_no = #{carLogNo} " +
            " AND vc.vehicle_type = 'visit' " +
            " AND vc.member_no IS NOT NULL")
    BillDTO findVisitRegistrantByCarLogNo(int carLogNo);

    // 정산서 등록
    @Insert("INSERT INTO bill (" +
            " car_log_no, fee_rule_no, kiosk_no, " +
            " charge_minutes, bill_amount, bill_status" +
            " ) VALUES (" +
            " #{carLogNo}, #{feeRuleNo}, #{kioskNo}, " +
            " #{chargeMinutes}, #{billAmount}, #{billStatus}" +
            " )")
    @Options(
            useGeneratedKeys = true,
            keyProperty = "billNo",
            keyColumn = "bill_no"
    )
    int insert(BillDTO dto);

    // 미결제 정산서의 요금 갱신
    @Update("UPDATE bill " +
            " SET kiosk_no = #{kioskNo}, " +
            " charge_minutes = #{chargeMinutes}, " +
            " bill_amount = #{billAmount} " +
            " WHERE bill_no = #{billNo} " +
            " AND bill_status = 'UNPAID'")
    int updateUnpaidAmount(BillDTO dto);

    // 토스페이먼츠 결제 주문번호 저장
    @Update("UPDATE bill " +
            " SET kiosk_no = #{kioskNo}, " +
            " payment_order_id = #{paymentOrderId} " +
            " WHERE bill_no = #{billNo} " +
            " AND bill_status = 'UNPAID'")
    int updatePaymentOrder(BillDTO dto);

    // 토스페이먼츠 주문번호로 정산서 조회
    @Select("SELECT bill_no, car_log_no, fee_rule_no, kiosk_no, " +
            " charge_minutes, bill_amount, bill_status, " +
            " payment_order_id, payment_key, payment_method, " +
            " issued_at, paid_at " +
            " FROM bill " +
            " WHERE payment_order_id = #{paymentOrderId}")
    BillDTO findByPaymentOrderId(String paymentOrderId);

    // 토스페이먼츠 결제 승인 결과 저장
    @Update("UPDATE bill " +
            " SET bill_status = 'PAID', " +
            " payment_key = #{paymentKey}, " +
            " payment_method = #{paymentMethod}, " +
            " paid_at = CURRENT_TIMESTAMP " +
            " WHERE bill_no = #{billNo} " +
            " AND bill_status = 'UNPAID'")
    int markPaid(BillDTO dto);

    // 정산금액이 0원인 정산서 완료 처리
    @Update("UPDATE bill " +
            " SET bill_status = 'PAID', " +
            " paid_at = CURRENT_TIMESTAMP " +
            " WHERE bill_no = #{billNo} " +
            " AND bill_status = 'UNPAID' " +
            " AND bill_amount = 0")
    int markZeroAmountPaid(int billNo);

}
