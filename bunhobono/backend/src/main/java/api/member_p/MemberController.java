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

    @Resource
    MemberService service;

    // 인증된 사용자의 로그인 아이디를 반환하고 비로그인 요청은 거부한다.
    private String authenticatedLoginId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return authentication.getName();
    }

    // 보안 검증 실패 상태와 안내 문구를 응답 본문에 담는다.
    private ResponseEntity<Map<String, String>> securityFailure(ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "요청을 처리하지 못했습니다."
                : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of("message", message));
    }

    // =====================================================
    // 1. 외부 회원가입과 관리자 회원 추가 요청을 구분한다.
    // =====================================================

    // 외부 가입은 PENDING·거주로 고정하고 관리자 요청은 입력한 역할과 상태를 사용한다.
    @PostMapping("/members")
    public void signup(@RequestBody MemberDTO dto, Authentication authentication) {
        // ADMIN 권한 여부에 따라 '외부 가입'과 '관리자의 회원추가'를 구분한다.
        boolean adminRequest = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ADMIN".equalsIgnoreCase(authority.getAuthority()));
        if (!adminRequest) {
            // 외부 회원은 관리자 승인 전까지 로그인할 수 없는 대기 역할로 저장한다.
            dto.setRole("PENDING");
            dto.setMemStatus("거주");
        }
        service.signup(dto);
    }

    // 입력한 로그인 아이디가 이미 존재하는지 확인한다.
    @GetMapping("/signup/check-id")
    public boolean checkLoginId(@RequestParam String loginId){
        return service.checkLoginId(loginId);
    }

    // 전출 이력이 있고 현재 활성 회원이 없는 동·호수만 반환한다.
    @GetMapping("/signup/available-units")
    public List<MemberDTO> availableSignupUnits() {
        return service.availableSignupUnits();
    }

    // =====================================================
    // 2. 관리자가 회원을 조회·검색·수정·승인한다.
    // =====================================================

    // 관리자가 회원의 연락처, 비밀번호와 상태를 수정한다.
    @PutMapping("/members/{memberNo}/edit")
    public void update(@PathVariable int memberNo,
                       @RequestBody MemberDTO dto,
                       Authentication authentication) {
        dto.setMemberNo(memberNo);
        // 로그인한 관리자 본인은 본인을 탈퇴시킬 수 없도록 하는 조치.
        service.update(dto, authentication.getName());
    }

    // 선택한 PENDING 회원의 역할을 RESIDENT로 변경한다.
    @PutMapping("/members/approve")
    public void approvePendingMembers(@RequestBody List<Long> memberNos) {
        service.approvePendingMembers(memberNos);
    }

    // 관리자 회원관리 화면에 표시할 전체 회원을 조회한다.
    @GetMapping("/members")
    public List<MemberDTO> list(){
        return service.list();
    }

    // 회원 번호에 해당하는 상세 정보를 조회한다.
    @GetMapping("/members/{memberNo}/detail")
    public MemberDTO detail(@PathVariable int memberNo){
        return service.detail(memberNo);
    }

    // 이름, 역할 또는 동·호수 조건으로 회원을 검색한다.
    @GetMapping("/members/search")
    public List<MemberDTO> search(
            @RequestParam String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer dong,
            @RequestParam(required = false) Integer ho
    ) {
        return service.search(type, keyword, dong, ho);
    }

    // =====================================================
    // 3. 전출 신청 회원을 복원하거나 전출을 확정한다.
    // =====================================================

    // 선택한 전출 신청 회원들을 다시 거주 상태로 복원한다.
    @PutMapping("/members/restore")
    public int restoreWithdrawnMembers(@RequestBody List<Long> memberNos) {
        return service.restoreWithdrawnMembers(memberNos);
    }

    // 선택한 전출 신청 회원의 이력을 보관하고 원본 회원 행을 미등록 상태로 초기화한다.
    @DeleteMapping("/members/withdrawn")
    public int permanentlyDeleteWithdrawnMembers(@RequestBody List<Long> memberNos) {
        return service.permanentlyDeleteWithdrawnMembers(memberNos);
    }

    // 관리자가 회원을 전출 신청 상태로 변경하고 탈퇴일을 기록한다.
    @DeleteMapping("/members/{memberNo}/delete")
    public void delete(@PathVariable int memberNo, Authentication authentication) {
        service.delete(memberNo, authentication.getName());
    }

    // 전출 신청된 회원 한 명을 다시 거주 상태로 복원한다.
    @PutMapping("/members/{memberNo}/restore")
    public void restoreWithdrawnMember(@PathVariable int memberNo) {
        service.restoreWithdrawnMember(memberNo);
    }

    // 전출 신청된 회원 한 명의 이력을 보관하고 원본 회원 행을 초기화한다.
    @PutMapping("/members/{memberNo}/confirm-withdrawn")
    public void confirmWithdrawnMember(@PathVariable int memberNo) {
        service.confirmWithdrawnMember(memberNo);
    }

    // =====================================================
    // 4. 로그인 입주민의 마이페이지와 대시보드 정보를 제공한다.
    // =====================================================

    // 로그인 아이디를 기준으로 입주민 본인 정보를 조회한다.
    @GetMapping("/resident/mypage")
    public MemberDTO residentMypage(Authentication authentication) {

        String loginId = authentication.getName();

        return service.residentMypage(loginId);
    }


    // 본인 정보, 등록 차량과 입출차 기록을 대시보드 응답으로 반환한다.
    @GetMapping("/resident/mypage/dashboard")
    public MemberDTO.ResidentDashboard residentDashboard(Authentication authentication) {
        return service.residentDashboard(authentication.getName());
    }


    // 로그인 아이디를 고정해 본인의 연락처와 입력된 비밀번호만 수정한다.
    @PutMapping("/resident/mypage/edit")
    public void residentMypageEdit(Authentication authentication, @RequestBody MemberDTO dto){

        String loginId = authentication.getName();

        dto.setLoginId(loginId);
        service.residentMypageEdit(dto);
    }

    // =====================================================
    // 5. 회원탈퇴와 비밀번호 변경을 위한 본인 확인을 처리한다.
    // =====================================================

    // 로그인 입주민에게 3분 동안 사용할 일회용 보안문자를 발급한다.
    @GetMapping("/resident/security-challenge")
    public Map<String, String> issueSecurityChallenge(Authentication authentication) {
        authenticatedLoginId(authentication);
        return service.issueSecurityChallenge();
    }

    // 실제 탈퇴 전에 현재 비밀번호와 보안문자가 맞는지만 확인한다.
    @PostMapping("/resident/mypage/delete/verify")
    public ResponseEntity<Map<String, String>> verifyResidentWithdrawal(
            Authentication authentication,
            @RequestBody MemberDTO.ResidentSecurityRequest request
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

    // 비밀번호와 보안문자를 다시 확인한 뒤 회원 상태를 전출로 변경한다.
    @DeleteMapping("/resident/mypage/delete")
    public ResponseEntity<Map<String, String>> residentDelete(
            Authentication authentication,
            @RequestBody MemberDTO.ResidentSecurityRequest request
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

    // 현재 비밀번호와 보안문자를 확인한 뒤 새 비밀번호를 저장한다.
    @PutMapping("/resident/mypage/password")
    public ResponseEntity<Map<String, String>> changeResidentPassword(
            Authentication authentication,
            @RequestBody MemberDTO.ResidentSecurityRequest request
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

}
