package api.mem_purchase_p;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemPurchaseMapper {

    // 로그인한 입주민의 회원번호와 구매 당시 보관할 정보를 조회한다.
    @Select("""
        SELECT
            m.member_no,
            m.login_id AS snapshot_login_id,
            m.mem_name AS snapshot_member_name,
            au.dong AS snapshot_dong,
            au.ho AS snapshot_ho
        FROM member m
        JOIN apartment_unit au
          ON au.apartment_unit_no = m.unit_no
        WHERE m.login_id = #{snapshotLoginId}
          AND m.role = 'RESIDENT'
          AND m.mem_status = 'ACTIVE'
    """)
    MemPurchaseDTO findActiveResident(MemPurchaseDTO dto);

    // 방문차량 추가 이용시간 구매 주문을 생성한다.
    @Insert("""
        INSERT INTO mem_purchase (
            member_no,
            snapshot_login_id,
            snapshot_member_name,
            snapshot_dong,
            snapshot_ho,
            purchase_type,
            purchase_quantity,
            purchase_amount,
            purchase_status,
            payment_order_id
        ) VALUES (
            #{memberNo},
            #{snapshotLoginId},
            #{snapshotMemberName},
            #{snapshotDong},
            #{snapshotHo},
            #{purchaseType},
            #{purchaseQuantity},
            #{purchaseAmount},
            #{purchaseStatus},
            #{paymentOrderId}
        )
    """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "memPurchaseNo",
            keyColumn = "mem_purchase_no"
    )
    int insert(MemPurchaseDTO dto);

    // 토스 주문번호로 구매 주문을 조회한다.
    @Select("""
        SELECT
            mem_purchase_no,
            member_no,
            snapshot_login_id,
            snapshot_member_name,
            snapshot_dong,
            snapshot_ho,
            purchase_type,
            purchase_quantity,
            purchase_amount,
            purchase_status,
            payment_order_id,
            payment_key,
            payment_method,
            created_at,
            paid_at
        FROM mem_purchase
        WHERE payment_order_id = #{paymentOrderId}
    """)
    MemPurchaseDTO findByPaymentOrderId(MemPurchaseDTO dto);

    // 결제 승인 결과를 저장하고 구매 주문을 결제완료로 변경한다.
    @Update("""
        UPDATE mem_purchase
        SET purchase_status = 'PAID',
            payment_key = #{paymentKey},
            payment_method = #{paymentMethod},
            paid_at = CURRENT_TIMESTAMP
        WHERE mem_purchase_no = #{memPurchaseNo}
          AND purchase_status = 'UNPAID'
    """)
    int markPaid(MemPurchaseDTO dto);

    // 로그인한 입주민과 같은 세대가 이번 달 결제한 추가시간(분) 합계
    @Select("""
        SELECT COALESCE(SUM(mp.purchase_quantity), 0)
        FROM member login_member
        JOIN member household_member
          ON household_member.unit_no = login_member.unit_no
        JOIN mem_purchase mp
          ON mp.member_no = household_member.member_no
        WHERE login_member.login_id = #{snapshotLoginId}
          AND mp.purchase_type = 'VISIT_PARKING_MINUTES'
          AND mp.purchase_status = 'PAID'
          AND mp.paid_at >= DATE_TRUNC('month', CURRENT_TIMESTAMP)
          AND mp.paid_at < DATE_TRUNC('month', CURRENT_TIMESTAMP)
                           + INTERVAL '1 month'
    """)
    int sumMonthlyPaidVisitMinutes(MemPurchaseDTO dto);
}
