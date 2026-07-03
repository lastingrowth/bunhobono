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

    @ResponseBody
    @GetMapping("/home")
    public String home(){
        return "home server";
    }

    @ResponseBody
    @RequestMapping("/members")
    public void signup(@RequestBody  MemberDTO dto ) {
        service.signup(dto);

    }

    @ResponseBody
    @GetMapping("/login")
    public String login(MemberDTO dto) {

        service.signup(dto);

        return "로그인 성공";
    }

    @ResponseBody
    @GetMapping("/members")
    public List<MemberDTO> list(){
        return service.list();
    }

    @ResponseBody
    @GetMapping("/members/detail/{memberNo}")
    public MemberDTO detail(@PathVariable int memberNo){
        return service.detail(memberNo);
    }
}


// @ResponseBody : return 값 서버에서 바로 표시
