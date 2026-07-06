package api.policy_p;

import lombok.Data;

@Data
public class PolicyDTO {

    int feePolicyNo, parkingNo, freeMinutes, unitMinutes, unitFee, dailyMaxFee;
    String vehicleType;
    boolean isActive;
}
