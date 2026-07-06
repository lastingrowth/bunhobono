package api.notice_p;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeDTO {

    String plateNo, alertStat;
    int noticeNo,parkingNo, stayDays;
    LocalDateTime entryAt,detectAt;

    String vehicleType, vehicleStatus, expireStatus; //vehicle_car 테이블 참조용
    LocalDateTime startDate, endDate;

    String chargeStatus, paymentStatus;//parking_charge, parking_payment 두 테이블 참조용
    int amount;
    LocalDateTime paidAt;

}
