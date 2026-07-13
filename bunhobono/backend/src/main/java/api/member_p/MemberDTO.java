package api.member_p;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberDTO {

    private  long memberNo;
    private  String loginId, loginPwd, memName, memPhone, role, memStatus;
    // 관리자 회원 승인 상태: PENDING(승인 대기), APPROVED(승인 완료), REJECTED(승인 거절)
    private String approvalStatus;
    private int memDong, memHo;
    private LocalDate memCreateAt, memDeleteAt;
}

/*
Controller : 사용자가 보낸 값 받기
Service    : 아이디 중복 확인 → 저장할지 판단 → Mapper 호출
Mapper     : DB에 insert/select 실행
DTO        : 데이터 담는 통

회원가입 insert 확인
→ 로그인 select 확인
→ 로그인 성공 시 문자열 반환
→ 그다음 JWT 토큰 반환으로 변경
*/
