package api.feerule_p;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeeRuleDTO {

    private Integer feeRuleNo;

    private String ruleName;

    private int unitMinutes;

    private BigDecimal unitFee;

    private BigDecimal dailyMaxFee;

    private Integer exitGraceMinutes;

    private LocalDateTime createdAt;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private Boolean isDefault;
}
