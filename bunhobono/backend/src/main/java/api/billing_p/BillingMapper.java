package api.billing_p;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BillingMapper {

    // 입차시각에 적용 가능한 최신 요금 규칙 조회
    @Select("SELECT fee_rule_no, rule_name, unit_minutes, unit_fee, " +
            " daily_max_fee, created_at, effective_from, effective_to " +
            " FROM fee_rule " +
            " WHERE effective_from <= #{inTime} " +
            " AND (effective_to IS NULL " +
            " OR effective_to > #{inTime}) " +
            " ORDER BY effective_from DESC, fee_rule_no DESC " +
            " LIMIT 1")
    FeeRuleDTO findFeeRuleByInTime(@Param("inTime") LocalDateTime inTime);

    // 등록된 요금 규칙을 적용 시작시각 역순으로 조회
    @Select("SELECT fee_rule_no, rule_name, unit_minutes, unit_fee, " +
            " daily_max_fee, created_at, effective_from, effective_to " +
            " FROM fee_rule " +
            " ORDER BY effective_from DESC, fee_rule_no DESC")
    List<FeeRuleDTO> findFeeRuleList();

    // 요금 규칙 번호로 요금 규칙 조회
    @Select("SELECT fee_rule_no, rule_name, unit_minutes, unit_fee, " +
            " daily_max_fee, created_at, effective_from, effective_to " +
            " FROM fee_rule " +
            " WHERE fee_rule_no = #{feeRuleNo}")
    FeeRuleDTO findFeeRuleByNo(int feeRuleNo);

    // 요금 규칙의 적용 종료일시 수정
    @Update("UPDATE fee_rule " +
            " SET effective_to = #{effectiveTo} " +
            " WHERE fee_rule_no = #{feeRuleNo}")
    int updateFeeRuleEffectiveTo(FeeRuleDTO dto);

    // 예약 상태의 요금 규칙 전체 수정
    @Update("UPDATE fee_rule " +
            " SET rule_name = #{ruleName}, " +
            " unit_minutes = #{unitMinutes}, " +
            " unit_fee = #{unitFee}, " +
            " daily_max_fee = #{dailyMaxFee}, " +
            " effective_from = #{effectiveFrom}, " +
            " effective_to = #{effectiveTo} " +
            " WHERE fee_rule_no = #{feeRuleNo}")
    int updateScheduledFeeRule(FeeRuleDTO dto);

    // 새로운 요금 규칙 등록
    @Insert("INSERT INTO fee_rule (" +
            " rule_name, unit_minutes, unit_fee, daily_max_fee, " +
            " effective_from, effective_to) " +
            " VALUES (#{ruleName}, #{unitMinutes}, #{unitFee}, " +
            " #{dailyMaxFee}, #{effectiveFrom}, #{effectiveTo})")
    @Options(
            useGeneratedKeys = true,
            keyProperty = "feeRuleNo",
            keyColumn = "fee_rule_no"
    )
    int insertFeeRule(FeeRuleDTO dto);

    // 차량번호로 현재 주차 중인 입출차 기록과 입차 주차장을 조회
    @Select("SELECT cl.car_log_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time, " +
            " p.parking_no, p.parking_code " +
            " FROM car_log cl " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " JOIN gate in_gate " +
            " ON in_gate.gate_no = cl.in_gate_no " +
            " JOIN parking p " +
            " ON p.parking_no = in_gate.parking_no " +
            " WHERE cl.out_time IS NULL " +
            " AND (cl.snapshot_car_no = #{carNo} " +
            " OR vc.car_no = #{carNo}) " +
            " ORDER BY cl.in_time DESC " +
            " LIMIT 1")
    BillDTO findOpenCarLogByCarNo(String carNo);

    // 출차 유형과 차량번호 뒤 4자리로 현재 주차 중인 차량과 입차 주차장을 조회
    @Select("SELECT cl.car_log_no, cl.camera_data_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time, " +
            " p.parking_no, p.parking_code " +
            " FROM car_log cl " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " JOIN gate in_gate " +
            " ON in_gate.gate_no = cl.in_gate_no " +
            " JOIN parking p " +
            " ON p.parking_no = in_gate.parking_no " +
            " WHERE cl.out_time IS NULL " +
            " AND RIGHT(COALESCE(cl.snapshot_car_no, vc.car_no), 4) " +
            " = #{lastFourDigits} " +
            " AND ((#{exitType} = 'RESIDENT' " +
            " AND cl.snapshot_car_kind = 'REGISTERED') " +
            " OR (#{exitType} = 'NON_RESIDENT' " +
            " AND cl.snapshot_car_kind IN ('VISIT', 'UNKNOWN'))) " +
            " ORDER BY cl.in_time DESC")
    List<BillDTO> findOpenCarLogsByLastFourDigits(
            @Param("lastFourDigits") String lastFourDigits,
            @Param("exitType") String exitType
    );

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

    // 지난 기록으로 이동하지 않은 비입주민 차량의 정산 목록 조회
    @Select("SELECT cl.car_log_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.snapshot_car_kind AS car_kind, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time, " +
            " p.parking_code, " +
            " b.bill_no, b.kiosk_no, b.charge_minutes, " +
            " b.bill_amount, b.bill_status, b.payment_method, " +
            " b.issued_at, b.paid_at " +
            " FROM car_log cl " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " JOIN gate in_gate " +
            " ON in_gate.gate_no = cl.in_gate_no " +
            " JOIN parking p " +
            " ON p.parking_no = in_gate.parking_no " +
            " JOIN bill b " +
            " ON b.car_log_no = cl.car_log_no " +
            " WHERE cl.snapshot_car_kind IN ('VISIT', 'UNKNOWN') " +
            " AND p.parking_code = 'B2' " +
            " AND (cl.out_time IS NULL " +
            " OR b.bill_status = 'PAID') " +
            " ORDER BY cl.in_time DESC")
    List<BillDTO> findAdminBillingList();

    // 입출차 기록 번호로 관리자 정산 상세정보 조회
    @Select("SELECT cl.car_log_no, cl.camera_data_no, " +
            " COALESCE(cl.snapshot_car_no, vc.car_no) AS car_no, " +
            " cl.snapshot_car_kind AS car_kind, " +
            " cl.in_time, cl.out_time, " +
            " COALESCE(cl.free_time, 0) AS free_time, " +
            " p.parking_no, p.parking_code, " +
            " b.bill_no, b.fee_rule_no, b.kiosk_no, " +
            " b.charge_minutes, b.bill_amount, b.bill_status, " +
            " b.payment_order_id, b.payment_key, " +
            " b.payment_method, b.issued_at, b.paid_at, " +
            " fr.rule_name, fr.unit_minutes, " +
            " fr.unit_fee, fr.daily_max_fee " +
            " FROM car_log cl " +
            " LEFT JOIN vehicle_car vc " +
            " ON cl.vehicle_car_no = vc.vehicle_car_no " +
            " JOIN gate in_gate " +
            " ON in_gate.gate_no = cl.in_gate_no " +
            " JOIN parking p " +
            " ON p.parking_no = in_gate.parking_no " +
            " LEFT JOIN bill b " +
            " ON b.car_log_no = cl.car_log_no " +
            " LEFT JOIN fee_rule fr " +
            " ON fr.fee_rule_no = b.fee_rule_no " +
            " WHERE cl.car_log_no = #{carLogNo} " +
            " AND cl.snapshot_car_kind IN ('VISIT', 'UNKNOWN')")
    BillDTO findAdminBillingDetail(int carLogNo);

    // 정산완료 전 입출차 기록에 적용된 무료시간 수정
    @Update("UPDATE car_log " +
            " SET free_time = #{freeTime} " +
            " WHERE car_log_no = #{carLogNo} " +
            " AND NOT EXISTS (" +
            " SELECT 1 " +
            " FROM bill " +
            " WHERE bill.car_log_no = car_log.car_log_no " +
            " AND bill.bill_status = 'PAID'" +
            " )")
    int updateAdminFreeTime(BillDTO dto);

    // 차량이 입차한 주차장과 같은 층의 활성 출차 게이트 번호 조회
    @Select("SELECT out_gate.gate_no " +
            " FROM car_log " +
            " JOIN gate in_gate " +
            " ON car_log.in_gate_no = in_gate.gate_no " +
            " JOIN gate out_gate " +
            " ON out_gate.parking_no = in_gate.parking_no " +
            " WHERE car_log.car_log_no = #{carLogNo} " +
            " AND car_log.out_time IS NULL " +
            " AND out_gate.gate_type = 'Out' " +
            " AND out_gate.active = TRUE " +
            " ORDER BY out_gate.gate_no " +
            " LIMIT 1")
    Integer findExitGateNoByCarLogNo(int carLogNo);

    // 출차 완료 후 3개월이 지난 완료 정산서 번호 조회
    @Select("SELECT b.bill_no " +
            " FROM bill b " +
            " JOIN car_log cl " +
            " ON cl.car_log_no = b.car_log_no " +
            " WHERE b.bill_status = 'PAID' " +
            " AND cl.out_time IS NOT NULL " +
            " AND cl.out_time < CURRENT_TIMESTAMP - INTERVAL '3 months'")
    List<Integer> findOldPaidBillNosForTrash();

    // 휴지통 저장이 끝난 완료 정산서 삭제
    @Delete("DELETE FROM bill " +
            "WHERE bill_no = #{billNo} " +
            "AND bill_status = 'PAID'")
    int deletePaidBill(int billNo);
}