package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
public class MemberController {

    @Resource
    MemberService service;

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
                       @RequestBody MemberDTO dto) {
        dto.setMemberNo(memberNo);
        service.update(dto);
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

    // 탈퇴 후 3일이 지나 보관 삭제 확인이 필요한 회원 알림 목록.
    @GetMapping("/members/archive-alerts")
    public List<MemberDTO> archiveAlerts() {
        return service.getArchiveAlerts();
    }

    // 탈퇴 후 3일 경과 목록에서 선택한 회원을 실제 삭제할 수 있도록.
    @DeleteMapping("/members/archive")
    public int deleteArchivedMembers(@RequestBody List<Long> memberNos) {
        return service.deleteArchivedMembers(memberNos);
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
    // 삭제
    @DeleteMapping("/members/{memberNo}/delete")
    public void delete(@PathVariable int memberNo) {
        service.delete(memberNo);
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

    // 입주민 본인 회원 탈퇴
    @DeleteMapping("/resident/mypage/delete")
    public void residentDelete(@RequestBody MemberDTO dto) {
        service.residentDelete(dto.getLoginId());
    }

    // 아이디 중복 확인
    @GetMapping("/signup/check-id")
    public boolean checkLoginId(@RequestParam String loginId){
        return service.checkLoginId(loginId);
    }

}
