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
            dto.setRole("RESIDENT");
            dto.setMemStatus("거주");
            // 공개 입주민 회원가입은 관리자 승인 전까지 승인 대기로 저장한다.
            dto.setApprovalStatus("PENDING");
        } else {
            // 관리자 페이지에서 직접 추가한 회원은 별도의 승인 절차 없이 즉시 승인한다.
            dto.setApprovalStatus("APPROVED");
        }
        service.signup(dto);
    }

    // 회원 수정
    @PutMapping("/members/{memberNo}/edit")
    public void update(@PathVariable int memberNo,
                       @RequestBody MemberDTO dto) {
        dto.setMemberNo(memberNo);
        service.update(dto);
    }

    // 관리자 회원 목록에서 선택한 회원들의 승인 상태를 한 번에 변경한다.
    @PutMapping("/members/approval-status")
    public void updateApprovalStatus(@RequestBody MemberDTO.ApprovalRequest request) {
        service.updateApprovalStatus(request);
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
        if(type.equals("name")){
            return service.searchByName(keyword);
        }
        else if(type.equals("role")){
            return service.searchByRole(keyword);
        }
        else if(type.equals("dongHo")){
            return service.searchByDongHo(dong, ho);
        }

        return service.list();
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

    // 입주민이 마이페이지에 로그인했을 때, 본인 정보 수정
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
