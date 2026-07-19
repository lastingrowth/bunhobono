package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MemberService {


    // 회원가입 계정은 영문·숫자·특수기호 조합, 이름은 한글만 허용한다.
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{3,20}$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^\\d{4,20}$"
    );
    private static final Pattern NAME_PATTERN = Pattern.compile("^(?=.*[가-힣])(?=.*\\d)[가-힣\\d]{2,20}$");


    @Resource MemberMapper mapper;

    @Resource
    PasswordEncoder passwordEncoder;

    public int permanentlyDeleteWithdrawnMembers(List<Long> memberNos) {
        if (memberNos == null || memberNos.isEmpty()) {
            throw new IllegalArgumentException("영구 삭제할 회원을 선택해 주세요.");
        }
        return mapper.permanentlyDeleteWithdrawnMembers(memberNos);
    }

    // 회원가입
    @Transactional
    public void signup(MemberDTO dto) {
        validateSignup(dto);

        // 프론트 입력값과 관계없이 DB 권한 형식을 대문자로 통일한다.
        if (dto.getRole() != null) {
            dto.setRole(dto.getRole().toUpperCase());
        }
        if (!Set.of("PENDING", "RESIDENT", "ADMIN").contains(dto.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 회원 역할입니다.");
        }
        // 공개 가입은 전출로 비어 있고 다른 승인 대기·거주 회원이 없는 세대만 허용한다.
        if ("PENDING".equals(dto.getRole())) {
            validateAvailableSignupUnit(dto.getMemDong(), dto.getMemHo());
        }
        // 비밀번호 암호화
        dto.setLoginPwd(passwordEncoder.encode(dto.getLoginPwd()));
        // DB 저장
        mapper.signup(dto);
    }

    public List<MemberDTO> availableSignupUnits() {
        return mapper.availableSignupUnits();
    }

    private void validateAvailableSignupUnit(int dong, int ho) {
        if (!Set.of(101, 102, 201, 202, 301, 302, 401, 402).contains(dong)
                || ho / 100 < 1 || ho / 100 > 15
                || ho % 100 < 1 || ho % 100 > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 동·호수입니다.");
        }

        // 같은 전출 세대의 동시 가입 요청을 순서대로 처리한다.
        Long withdrawnMemberNo = mapper.lockWithdrawnUnit(dong, ho);
        if (withdrawnMemberNo == null || mapper.countActiveMembersAtUnit(dong, ho) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 거주 중이거나 가입 신청이 접수된 세대입니다.");
        }
    }

    // 회원가입 요청을 서버에서도 검증해 잘못된 값의 저장을 차단.
    private void validateSignup(MemberDTO dto) {
        if (!matches(NAME_PATTERN, dto.getMemName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이름은 한글과 숫자를 조합해 2~20자로 입력하세요."
            );
        }
        if (!matches(LOGIN_ID_PATTERN, dto.getLoginId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "아이디는 영문과 숫자를 조합해 3~20자로 입력하세요."
            );
        }
        if (!matches(PASSWORD_PATTERN, dto.getLoginPwd())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "비밀번호는 숫자 4~20자로 입력하세요."
            );
        }
    }

    // 정규식 검증 전 빈 값을 함께 차단한다.
    private boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
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
        if (!Set.of("name", "role", "dongHo").contains(type)) {
            return mapper.list();
        }
        return mapper.search(type, keyword, dong, ho);
    }
    @Transactional
    public void update(MemberDTO dto, String currentLoginId) {
        MemberDTO savedMember =
                mapper.detail((int) dto.getMemberNo());

        if (savedMember == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회원을 찾을 수 없습니다."
            );
        }

        if (savedMember.getLoginId().equals(currentLoginId)
                && "ADMIN".equalsIgnoreCase(savedMember.getRole())
                && "퇴사".equals(dto.getMemStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "로그인한 관리자는 본인 계정을 퇴사 처리할 수 없습니다."
            );
        }

        // 입주민 전출이면 일반 수정 대신 차량 삭제·공실 처리
        if ("RESIDENT".equalsIgnoreCase(savedMember.getRole())
                && "전출".equals(dto.getMemStatus())) {

            delete((int) dto.getMemberNo(), currentLoginId);
            return;
        }

        // 그 외에는 일반 수정
        if (dto.getLoginPwd() == null
                || dto.getLoginPwd().isBlank()) {
            dto.setLoginPwd(savedMember.getLoginPwd());

        } else if (!dto.getLoginPwd()
                .equals(savedMember.getLoginPwd())) {
            dto.setLoginPwd(
                    passwordEncoder.encode(dto.getLoginPwd())
            );
        }

        mapper.update(dto);
    }

    // 승인 대기 회원의 역할을 입주민으로 변경한다.
    public void approvePendingMembers(List<Long> memberNos) {
        if (memberNos == null || memberNos.isEmpty()) {
            throw new IllegalArgumentException("승인할 회원을 선택해 주세요.");
        }
        mapper.approvePendingMembers(memberNos);
    }

    // 전출 하고나면 차량을 삭제한다.
    @Transactional
    public void delete(int memberNo, String currentLoginId) {
        MemberDTO savedMember = mapper.detail(memberNo);

        if (savedMember == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회원을 찾을 수 없습니다."
            );
        }

        if (savedMember.getLoginId() != null
                && savedMember.getLoginId().equals(currentLoginId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "로그인한 관리자는 본인 계정을 전출 처리할 수 없습니다."
            );
        }

        // 차량 삭제
        // 관련 카메라·입출차 FK는 DB에서 자동 NULL 처리
        mapper.deleteVehiclesByMemberNo(memberNo);

        // 회원을 전출·공실 상태로 변경
        int updated = mapper.delete(memberNo);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "전출 처리할 회원이 없습니다."
            );
        }
    }

    // 입주민 마이페이지
    public MemberDTO residentMypage(String loginId) {
        MemberDTO member = mapper.residentMypage(loginId);
        if (member != null) {
            // 암호화된 비밀번호도 클라이언트에 노출하지 않는다.
            member.setLoginPwd(null);
        }
        return member;
    }


    // 회원 정보와 본인 차량, 최근 입출차 기록을 Spring Service에서 하나로 집계한다.
    public MemberDTO.ResidentDashboard residentDashboard(String loginId) {
        MemberDTO.ResidentDashboard dashboard = new MemberDTO.ResidentDashboard();
        dashboard.setMember(residentMypage(loginId));
        dashboard.setVehicles(mapper.residentVehicles(loginId));
        dashboard.setRecentCarLogs(mapper.residentCarLogs(loginId));
        return dashboard;
    }


    // 입주민 마이페이지 연락처 및 비밀번호 수정
    public void residentMypageEdit(MemberDTO dto){
        // 새 비밀번호를 입력한 경우에만 암호화하고, 빈 값이면 기존 비밀번호를 유지한다.
        if (dto.getLoginPwd() != null && !dto.getLoginPwd().isBlank()) {
            dto.setLoginPwd(passwordEncoder.encode(dto.getLoginPwd()));
        } else {
            dto.setLoginPwd(null);
        }
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
