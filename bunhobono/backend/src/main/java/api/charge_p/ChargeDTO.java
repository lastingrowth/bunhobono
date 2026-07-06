package api.charge_p;

import lombok.Data;

@Data
public class ChargeDTO {

    int chargeNo, carLogNo, feePolicyNo, amount, payerNo;
    String chargeType, payerType, status;
}
