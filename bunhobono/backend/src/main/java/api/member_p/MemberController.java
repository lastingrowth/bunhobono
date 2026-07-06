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

    // 테스트용
    @ResponseBody
    @GetMapping("/home")
    public String home(){
        return "home server";
    }

    // 회원가입
    @ResponseBody
    @PostMapping("/members")
    public void signup(@RequestBody  MemberDTO dto ) {
        service.signup(dto);

    }

    // 회원 수정
    @ResponseBody
    @PutMapping("/members/{memberNo}")
    public void update(@PathVariable int memberNo,
                       @RequestBody MemberDTO dto) {

        System.out.println("수정 API 진입");
        System.out.println("memberNo = " + memberNo);

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
    @GetMapping("/members/{memberNo}")
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

}


// @ResponseBody : return 값 서버에서 바로 표시
