package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@Controller
public class MemberController {

    @Resource
    MemberService service;

    // 회원가입
    @ResponseBody
    @PostMapping("/members")
    public void signup(@RequestBody  MemberDTO dto ) {
        service.signup(dto);
    }

    // 회원 수정
    @ResponseBody
    @PutMapping("/members/{memberNo}/edit")
    public void update(@PathVariable int memberNo,
                       @RequestBody MemberDTO dto) {
        dto.setMemberNo(memberNo);
        service.update(dto);
    }

    // 회원 리스트
    @ResponseBody
    @GetMapping("/members")
    public List<MemberDTO> list(){
        return service.list();
    }

    // 회원 상세내용
    @ResponseBody
    @GetMapping("/members/{memberNo}/detail")
    public MemberDTO detail(@PathVariable int memberNo){
        return service.detail(memberNo);
    }

    // 회원 검색
    @ResponseBody
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
    @ResponseBody
    @DeleteMapping("/members/{memberNo}/delete")
    public void delete(@PathVariable int memberNo) {
        service.delete(memberNo);
    }

    // 입주민 마이페이지
    @ResponseBody
    @GetMapping("/resident/{loginId}/mypage")
    public MemberDTO residentMypage(@PathVariable String loginId) {
        return service.residentMypage(loginId);
    }

    // 입주민이 마이페이지에 로그인했을 때, 본인 정보 수정
    @ResponseBody
    @PutMapping("/resident/{loginId}/edit")
    public void residentMypageEdit(@PathVariable String loginId, @RequestBody MemberDTO dto){
        dto.setLoginId(loginId);
        service.residentMypageEdit(dto);
    }

}


// @ResponseBody : return 값 서버에서 바로 표시
