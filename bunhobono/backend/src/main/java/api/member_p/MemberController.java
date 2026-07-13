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
    public void signup(@RequestBody  MemberDTO dto ) {
        service.signup(dto);
    }

    // 회원 수정
    @PutMapping("/members/{memberNo}/edit")
    public void update(@PathVariable int memberNo,
                       @RequestBody MemberDTO dto) {
        dto.setMemberNo(memberNo);
        service.update(dto);
    }

    // 회원 리스트
    @GetMapping("/members")
    public List<MemberDTO> list(){
        return service.list();
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
