package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RequestMapping("/api")
@RestController
public class MemberController {

    public record ResidentSecurityRequest(
            String currentPassword,
            String newPassword,
            String challengeId,
            String challengeAnswer
    ) {}

    @Resource
    MemberService service;

    private String authenticatedLoginId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return authentication.getName();
    }

    // 비밀번호·보안문자 검증 실패 사유를 화면에서 그대로 안내할 수 있도록 응답한다.
    private ResponseEntity<Map<String, String>> securityFailure(ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "요청을 처리하지 못했습니다."
                : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }

    // 회원가입
    @PostMapping("/members")
    public void signup(@RequestBody MemberDTO dto, Authentication authentication) {
        // 공개 회원가입에서는 요청 Body와 관계없이 입주민 권한만 허용한다.
        boolean adminRequest = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ADMIN".equalsIgnoreCase(authority.getAuthority()));
        if (!adminRequest) {
            // 외부 회원가입은 승인 전까지 대기 역할로 고정한다.
            dto.setRole("PENDING");
            dto.setMemStatus("거주");
        }
        service.signup(dto);
    }

    // 관리자 회원 수정: 연락처, 비밀번호, 회원 상태만 변경
    @PutMapping("/members/{memberNo}/edit")
    public void update(@PathVariable int memberNo,
                       @RequestBody MemberDTO dto,
                       Authentication authentication) {
        dto.setMemberNo(memberNo);
        service.update(dto, authentication.getName());
    }

    // 대기 역할 회원을 입주민 역할로 변경해 승인한다.
    @PutMapping("/members/approve")
    public void approvePendingMembers(@RequestBody List<Long> memberNos) {
        service.approvePendingMembers(memberNos);
    }

    // 회원 리스트
    @GetMapping("/members")
    public List<MemberDTO> list(){
        return service.list();
    }


    // 탈퇴 처리된 선택 회원을 영구 삭제한다.
    @DeleteMapping("/members/withdrawn")
    public int permanentlyDeleteWithdrawnMembers(@RequestBody List<Long> memberNos) {
        return service.permanentlyDeleteWithdrawnMembers(memberNos);
    }

    // 회원 상세내용
    @GetMapping("/members/{memberNo}/detail")
    public MemberDTO detail(@PathVariable int memberNo){
        return service.detail(memberNo);
    }

    // 회원 검색
    @GetMapping("/members/search")
    public List<MemberDTO> search(
            @RequestParam String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer dong,
            @RequestParam(required = false) Integer ho
    ) {
        return service.search(type, keyword, dong, ho);
    }
    // 관리자 회원 탈퇴 처리
    @DeleteMapping("/members/{memberNo}/delete")
    public void delete(@PathVariable int memberNo, Authentication authentication) {
        service.delete(memberNo, authentication.getName());
    }

    // 입주민 마이페이지
    @GetMapping("/resident/mypage")
    public MemberDTO residentMypage(Authentication authentication) {

        String loginId = authentication.getName();

        return service.residentMypage(loginId);
    }


    // 로그인 입주민 정보와 차량·입출차 기록을 한 번의 요청으로 반환한다.
    @GetMapping("/resident/mypage/dashboard")
    public MemberDTO.ResidentDashboard residentDashboard(Authentication authentication) {
        return service.residentDashboard(authentication.getName());
    }


    // 입주민이 마이페이지에서 본인 연락처와 비밀번호 수정
    @PutMapping("/resident/mypage/edit")
    public void residentMypageEdit(Authentication authentication, @RequestBody MemberDTO dto){

        String loginId = authentication.getName();

        dto.setLoginId(loginId);
        service.residentMypageEdit(dto);
    }

    // 로그인 입주민이 민감한 작업에 사용할 일회용 보안문자를 발급한다.
    @GetMapping("/resident/security-challenge")
    public Map<String, String> issueSecurityChallenge(Authentication authentication) {
        authenticatedLoginId(authentication);
        return service.issueSecurityChallenge();
    }

    // 입주민 본인 회원 탈퇴
    @PostMapping("/resident/mypage/delete/verify")
    public ResponseEntity<Map<String, String>> verifyResidentWithdrawal(
            Authentication authentication,
            @RequestBody ResidentSecurityRequest request
    ) {
        try {
            service.verifyResidentWithdrawal(
                    authenticatedLoginId(authentication),
                    request.currentPassword(),
                    request.challengeId(),
                    request.challengeAnswer()
            );
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException exception) {
            return securityFailure(exception);
        }
    }

    // 비밀번호와 보안문자 확인 후 최종 동의를 받은 입주민 본인 회원 탈퇴
    @DeleteMapping("/resident/mypage/delete")
    public ResponseEntity<Map<String, String>> residentDelete(
            Authentication authentication,
            @RequestBody ResidentSecurityRequest request
    ) {
        try {
            service.residentDelete(
                    authenticatedLoginId(authentication),
                    request.currentPassword(),
                    request.challengeId(),
                    request.challengeAnswer()
            );
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException exception) {
            return securityFailure(exception);
        }
    }

    // 입주민이 현재 비밀번호와 보안문자를 확인한 뒤 비밀번호를 변경한다.
    @PutMapping("/resident/mypage/password")
    public ResponseEntity<Map<String, String>> changeResidentPassword(
            Authentication authentication,
            @RequestBody ResidentSecurityRequest request
    ) {
        try {
            service.changeResidentPassword(
                    authenticatedLoginId(authentication),
                    request.currentPassword(),
                    request.newPassword(),
                    request.challengeId(),
                    request.challengeAnswer()
            );
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException exception) {
            return securityFailure(exception);
        }
    }

    // 아이디 중복 확인
    @GetMapping("/signup/check-id")
    public boolean checkLoginId(@RequestParam String loginId){
        return service.checkLoginId(loginId);
    }

    // 전출 처리되어 공개 회원가입이 가능한 세대의 동·호수만 반환한다.
    @GetMapping("/signup/available-units")
    public List<MemberDTO> availableSignupUnits() {
        return service.availableSignupUnits();
    }

}
