package apt.member_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@Controller
public class MemberController {

    @Resource
    MemberService service;

    // 테스트용
    @ResponseBody
    @GetMapping("/home")
    public String home(){
        return "home server";
    }

    // 회원가입
    @ResponseBody
    @RequestMapping("/members")
    @PostMapping("/members")
    public void signup(@RequestBody  MemberDTO dto ) {
        service.signup(dto);

    }

    // 로그인 (url 연결확인만)
    @ResponseBody
    @GetMapping("/login")
    public String login(MemberDTO dto) {

        service.signup(dto);

        return "로그인 성공";
    }

    // 회원 리스트
    @ResponseBody
    @GetMapping("/members")
    public List<MemberDTO> list(){
        return service.list();
    }

    // 회원 상세내용
    @ResponseBody
    @GetMapping("/members/detail/{memberNo}")
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

}


// @ResponseBody : return 값 서버에서 바로 표시
