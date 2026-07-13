package api.member_p;

import lombok.Data;

import java.util.List;

@Data
public class MemberApprovalRequest {
    // 회원 목록에서 일괄 승인 상태를 변경할 회원 번호와 변경할 상태를 전달한다.
    private List<Long> memberNos;
    private String approvalStatus;
}
