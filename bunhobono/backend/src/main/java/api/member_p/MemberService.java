package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    @Resource MemberMapper mapper;
    public void signup(MemberDTO dto) {
        mapper.signup(dto);
    }

    public void login(MemberDTO dto){
        mapper.login(dto);
    }

    public List<MemberDTO> list(){
        return mapper.list();
    }
    public MemberDTO detail(int memberNo){
        return mapper.detail(memberNo);
    }
    public List<MemberDTO> search(String type, String keyword, Integer dong, Integer ho){
        return mapper.search(type, keyword, dong, ho);
    }
    public List<MemberDTO> searchByName(String keyword){
        return mapper.searchByName(keyword);
    }

    public List<MemberDTO> searchByRole(String keyword){
        return mapper.searchByRole(keyword);
    }

    public List<MemberDTO> searchByDongHo(Integer dong, Integer ho){
        return mapper.searchByDongHo(dong, ho);
    }

    // 수정
    public void update(MemberDTO dto) {
        mapper.update(dto);
    }

    // 삭제
    public void delete(int memberNo) {
        mapper.delete(memberNo);
    }

}
