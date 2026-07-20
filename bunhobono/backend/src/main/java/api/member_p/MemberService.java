package api.member_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MemberService {

    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 5;
    private static final int CAPTCHA_EXPIRE_MINUTES = 3;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, CaptchaData> captchaStore = new ConcurrentHashMap<>();

    private record CaptchaData(String answer, LocalDateTime expiresAt) {}


    @Resource MemberMapper mapper;

    @Resource
    PasswordEncoder passwordEncoder;

    // 전출 신청된 회원을 복원한다.
// member_archive로 이동하기 전 단계에서 탈퇴 신청을 취소하는 기능이다.
    @Transactional
    public void restoreWithdrawnMember(int memberNo) {
        MemberDTO savedMember = mapper.detail(memberNo);

        if (savedMember == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회원을 찾을 수 없습니다."
            );
        }

        int restored = mapper.restoreWithdrawnMember(memberNo);

        if (restored == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "복원할 수 없는 회원입니다."
            );
        }
    }

    // 전출 신청된 회원을 확정 처리한다.
    // 회원 정보는 member_archive에 복사하고, member 원본은 동/호수 자리만 남기도록 초기화한다.
    @Transactional
    public void confirmWithdrawnMember(int memberNo) {
        MemberDTO savedMember = mapper.detail(memberNo);

        if (savedMember == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "회원을 찾을 수 없습니다."
            );
        }

        mapper.saveMemberArchive(memberNo);

        mapper.deleteVehiclesByMemberNo(memberNo);

        int updated = mapper.delete(memberNo);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "전출 확정 처리에 실패했습니다."
            );
        }
    }
    // 선택한 탈퇴 신청 회원들을 다시 거주 상태로 복원한다.
    @Transactional
    public int restoreWithdrawnMembers(List<Long> memberNos) {
        if (memberNos == null || memberNos.isEmpty()) {
            throw new IllegalArgumentException("복원할 회원을 선택해 주세요.");
        }

        for (Long memberNo : memberNos) {
            restoreWithdrawnMember(memberNo.intValue());
        }

        return memberNos.size();
    }

    // 선택한 탈퇴 신청 회원들을 전출 확정 처리한다.
    // 실제 member 삭제가 아니라 archive 보관 후 member 원본을 미등록 상태로 비운다.
    @Transactional
    public int permanentlyDeleteWithdrawnMembers(List<Long> memberNos) {
        if (memberNos == null || memberNos.isEmpty()) {
            throw new IllegalArgumentException("전출 확정할 회원을 선택해 주세요.");
        }

        for (Long memberNo : memberNos) {
            confirmWithdrawnMember(memberNo.intValue());
        }

        return memberNos.size();
    }

    // 회원가입
    @Transactional
    public void signup(MemberDTO dto) {
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

    // 회원을 탈퇴 신청 상태로 변경한다.
    // 이 단계에서는 archive 저장, 차량 삭제, 회원 정보 초기화를 하지 않는다.
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

        int updated = mapper.requestWithdrawnMember(memberNo);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "전출 신청 처리할 회원이 없습니다."
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

    // 보안문자 정답은 서버에만 저장하고 화면에는 PNG 이미지만 전달한다.
    public Map<String, String> issueSecurityChallenge() {
        String challengeId = UUID.randomUUID().toString();
        String answer = createCaptchaAnswer();
        captchaStore.put(challengeId, new CaptchaData(
                answer,
                LocalDateTime.now().plusMinutes(CAPTCHA_EXPIRE_MINUTES)
        ));

        return Map.of(
                "challengeId", challengeId,
                "imageData", createCaptchaImage(answer),
                "expiresIn", "180"
        );
    }

    private String createCaptchaAnswer() {
        StringBuilder answer = new StringBuilder();
        for (int index = 0; index < CAPTCHA_LENGTH; index++) {
            answer.append(CAPTCHA_CHARS.charAt(secureRandom.nextInt(CAPTCHA_CHARS.length())));
        }
        return answer.toString();
    }

    private String createCaptchaImage(String answer) {
        try {
            int width = 180;
            int height = 60;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(new Color(245, 248, 252));
            graphics.fillRect(0, 0, width, height);

            for (int index = 0; index < 8; index++) {
                graphics.setColor(new Color(
                        secureRandom.nextInt(180),
                        secureRandom.nextInt(180),
                        secureRandom.nextInt(180)
                ));
                graphics.drawLine(
                        secureRandom.nextInt(width), secureRandom.nextInt(height),
                        secureRandom.nextInt(width), secureRandom.nextInt(height)
                );
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
            for (int index = 0; index < answer.length(); index++) {
                graphics.setColor(new Color(
                        secureRandom.nextInt(100),
                        secureRandom.nextInt(100),
                        secureRandom.nextInt(150)
                ));
                graphics.drawString(
                        String.valueOf(answer.charAt(index)),
                        20 + index * 30,
                        40 + secureRandom.nextInt(10) - 5
                );
            }
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "보안문자 생성에 실패했습니다."
            );
        }
    }

    private void verifySecurityChallenge(String challengeId, String challengeAnswer) {
        verifySecurityChallenge(challengeId, challengeAnswer, true);
    }

    private void verifySecurityChallenge(String challengeId, String challengeAnswer, boolean consume) {
        if (challengeId == null || challengeAnswer == null || challengeAnswer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보안문자를 입력해주세요.");
        }

        CaptchaData captcha = consume
                ? captchaStore.remove(challengeId)
                : captchaStore.get(challengeId);
        if (captcha == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "보안문자가 존재하지 않거나 이미 사용되었습니다."
            );
        }
        if (LocalDateTime.now().isAfter(captcha.expiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보안문자가 만료되었습니다.");
        }
        if (!captcha.answer().equalsIgnoreCase(challengeAnswer.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보안문자가 일치하지 않습니다.");
        }
    }

    // 실제 탈퇴 전에 비밀번호와 보안문자만 확인하고 회원 상태는 변경하지 않는다.
    public void verifyResidentWithdrawal(
            String loginId,
            String currentPassword,
            String challengeId,
            String challengeAnswer
    ) {
        verifySecurityChallenge(challengeId, challengeAnswer, false);
        verifyResidentPassword(loginId, currentPassword);
    }

    private String verifyResidentPassword(String loginId, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호를 입력해주세요.");
        }

        String savedPassword = mapper.findResPw(loginId);
        if (savedPassword == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "입주민 계정을 찾을 수 없습니다.");
        }
        if (!passwordEncoder.matches(currentPassword, savedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        return savedPassword;
    }

    @Transactional
    public void residentDelete(
            String loginId,
            String currentPassword,
            String challengeId,
            String challengeAnswer
    ) {
        verifySecurityChallenge(challengeId, challengeAnswer);
        verifyResidentPassword(loginId, currentPassword);
        if (mapper.residentDelete(loginId) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원탈퇴에 실패했습니다.");
        }
    }

    @Transactional
    public void changeResidentPassword(
            String loginId,
            String currentPassword,
            String newPassword,
            String challengeId,
            String challengeAnswer
    ) {
        verifySecurityChallenge(challengeId, challengeAnswer);
        String savedPassword = verifyResidentPassword(loginId, currentPassword);
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력하세요.");
        }
        if (passwordEncoder.matches(newPassword, savedPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 비밀번호를 입력하세요.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        if (mapper.changeResidentPassword(loginId, encodedPassword) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호 변경에 실패했습니다.");
        }
    }

    // 아이디 중복확인
    public boolean checkLoginId(String loginId){
        // exists = true(아이디 있다) 가 되면 사용 거부
        // exists = false(아이디 없다) 가 되면 사용 가능
        return  mapper.checkLoginId(loginId);
    }

}
