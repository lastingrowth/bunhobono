package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    @Resource MemberMapper mapper;

    @Resource
    PasswordEncoder passwordEncoder;

    // 회원가입
    public void signup(MemberDTO dto) {
        // 비밀번호 암호화
        dto.setLoginPwd(passwordEncoder.encode(dto.getLoginPwd()));
        // DB 저장
        mapper.signup(dto);
    }

    // 회원 전체 조회
    public List<MemberDTO> list(){
        return mapper.list();
    }

    // 회원 상세 조회
    public MemberDTO detail(int memberNo){
        return mapper.detail(memberNo);
    }

    // 회원 검색(조건에 따라 검색)
    public List<MemberDTO> search(String type, String keyword, Integer dong, Integer ho){
        return mapper.search(type, keyword, dong, ho);
    }
    // 이름으로 검색
    public List<MemberDTO> searchByName(String keyword){
        return mapper.searchByName(keyword);
    }

    // 권한으로 검색
    public List<MemberDTO> searchByRole(String keyword){
        return mapper.searchByRole(keyword);
    }

    // 동,호수로 검색
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

    // 입주민 마이페이지
    public MemberDTO residentMypage(String loginId) {
        return mapper.residentMypage(loginId);
    }

    // 입주민 직접 마이페이지 수정
    public void residentMypageEdit(MemberDTO dto){
        mapper.residentMypageEdit(dto);
    }

    // 입주민 본인 회원 탈퇴
    public void residentDelete(String loginId) {
        mapper.residentDelete(loginId);
    }

    // 아이디 중복확인
    public boolean checkLoginId(String loginId){
        // exists = true(아이디 있다) 가 되면 사용 거부
        // exists = false(아이디 없다) 가 되면 사용 가능
        return  mapper.checkLoginId(loginId);
    }


}
