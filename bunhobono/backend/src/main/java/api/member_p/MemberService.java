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

    // =====================================================
    // 1. 회원가입 역할·세대·비밀번호를 검증하고 회원 정보를 저장한다.
    // =====================================================

    // 역할을 표준화하고 가입 유형에 맞는 저장 방식을 선택한다.
    @Transactional
    public void signup(MemberDTO dto) {
        // 역할 비교와 DB 저장 형식을 맞추기 위해 대문자로 통일한다.
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
        // ADMIN은 새 행으로 등록하고 RESIDENT·PENDING은 기존 전출 세대 행을 갱신한다.
        int savedCount = "ADMIN".equals(dto.getRole())
                ? mapper.signupAdmin(dto)
                : mapper.signup(dto);

        if (savedCount != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "회원으로 등록할 수 없는 동·호수입니다."
            );
        }
    }

    // 공개 회원가입에서 선택할 수 있는 빈 세대 목록을 반환한다.
    public List<MemberDTO> availableSignupUnits() {
        return mapper.availableSignupUnits();
    }

    // true이면 이미 사용 중이고 false이면 사용할 수 있다.
    public boolean checkLoginId(String loginId){
        return mapper.checkLoginId(loginId);
    }

    // 동·호수 형식과 동일 세대의 활성 회원 존재 여부를 확인한다.
    private void validateAvailableSignupUnit(int dong, int ho) {
        if (!Set.of(101, 102, 201, 202, 301, 302, 401, 402).contains(dong)
                || ho / 100 < 1 || ho / 100 > 10
                || ho % 100 < 1 || ho % 100 > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 동·호수입니다.");
        }

        // 같은 전출 세대의 동시 가입 요청을 순서대로 처리한다.
        Long withdrawnMemberNo = mapper.lockWithdrawnUnit(dong, ho);
        if (withdrawnMemberNo == null || mapper.countActiveMembersAtUnit(dong, ho) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 거주 중이거나 가입 신청이 접수된 세대입니다.");
        }
    }

    // =====================================================
    // 2. 관리자 회원 목록·상세·검색·수정·승인을 처리한다.
    // =====================================================

    // 관리자 회원관리 화면에 표시할 전체 회원을 조회한다.
    public List<MemberDTO> list(){
        return mapper.list();
    }

    // 회원 번호에 해당하는 상세 정보를 조회한다.
    public MemberDTO detail(int memberNo){
        return mapper.detail(memberNo);
    }

    // 이름, 역할 또는 동·호수 검색만 허용하고 그 외 조건은 전체 목록을 반환한다.
    public List<MemberDTO> search(String type, String keyword, Integer dong, Integer ho){
        if (!Set.of("name", "role", "dongHo").contains(type)) {
            return mapper.list();
        }
        return mapper.search(type, keyword, dong, ho);
    }

    // 수정 대상과 현재 관리자를 확인하고 허용된 회원 정보만 변경한다.
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

        // 입주민을 전출로 변경하면 일반 수정 대신 전출 신청 상태로 전환한다.
        if ("RESIDENT".equalsIgnoreCase(savedMember.getRole())
                && "전출".equals(dto.getMemStatus())) {

            delete((int) dto.getMemberNo(), currentLoginId);
            return;
        }

        // 전출 신청이 아니면 비밀번호 암호화 여부를 확인한 뒤 일반 수정한다.
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

    // 선택한 승인 대기 회원의 역할을 입주민으로 변경한다.
    public void approvePendingMembers(List<Long> memberNos) {
        if (memberNos == null || memberNos.isEmpty()) {
            throw new IllegalArgumentException("승인할 회원을 선택해 주세요.");
        }
        mapper.approvePendingMembers(memberNos);
    }

    // =====================================================
    // 2-1. 관리자가 회원을 전출 신청 상태로 변경한다.
    // =====================================================

    // 이력 보관·차량 삭제·회원 초기화 전 단계로 상태와 탈퇴일만 변경한다.
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

    // =====================================================
    // 3. 전출 신청 회원을 복원하거나 이력을 보관한 뒤 전출을 확정한다.
    // =====================================================

    // 아직 이력 보관 전인 전출 신청 회원을 다시 거주 상태로 복원한다.
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

    // 회원 이력과 동·호수 자리는 유지하면서 전출을 확정하고 연결 차량을 삭제한다.
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

    // 선택한 전출 신청 회원들을 한 명씩 거주 상태로 복원한다.
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

    // 선택한 전출 신청 회원들의 이력을 보관하고 원본 회원 행을 초기화한다.
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

    // =====================================================
    // 4. 입주민 마이페이지와 대시보드 데이터를 조회·집계한다.
    // =====================================================

    // 로그인 아이디에 해당하는 본인 정보를 조회하고 비밀번호는 응답에서 제거한다.
    public MemberDTO residentMypage(String loginId) {
        MemberDTO member = mapper.residentMypage(loginId);
        if (member != null) {
            // 암호화된 비밀번호도 클라이언트에 노출하지 않는다.
            member.setLoginPwd(null);
        }
        return member;
    }


    // 본인 정보, 등록 차량과 입출차 기록을 하나의 대시보드 응답으로 집계한다.
    public MemberDTO.ResidentDashboard residentDashboard(String loginId) {
        MemberDTO.ResidentDashboard dashboard = new MemberDTO.ResidentDashboard();
        dashboard.setMember(residentMypage(loginId));
        dashboard.setVehicles(mapper.residentVehicles(loginId));
        dashboard.setRecentCarLogs(mapper.residentCarLogs(loginId));
        return dashboard;
    }


    // 마이페이지에서 연락처를 수정하고 새 비밀번호가 있으면 암호화해 저장한다.
    public void residentMypageEdit(MemberDTO dto){
        // 새 비밀번호를 입력한 경우에만 암호화하고, 빈 값이면 기존 비밀번호를 유지한다.
        if (dto.getLoginPwd() != null && !dto.getLoginPwd().isBlank()) {
            dto.setLoginPwd(passwordEncoder.encode(dto.getLoginPwd()));
        } else {
            dto.setLoginPwd(null);
        }
        mapper.residentMypageEdit(dto);
    }

    // =====================================================
    // 5. 일회용 보안문자와 현재 비밀번호로 민감한 작업의 본인 여부를 검증한다.
    // =====================================================

    // 보안문자 정답은 서버에 저장하고 화면에는 ID, 이미지와 만료시간만 전달한다.
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

    // 혼동하기 쉬운 문자를 제외한 5자리 보안문자 정답을 생성한다.
    private String createCaptchaAnswer() {
        StringBuilder answer = new StringBuilder();
        for (int index = 0; index < CAPTCHA_LENGTH; index++) {
            answer.append(CAPTCHA_CHARS.charAt(secureRandom.nextInt(CAPTCHA_CHARS.length())));
        }
        return answer.toString();
    }

    // 정답 문자와 방해선을 그린 PNG 이미지를 Base64 데이터로 변환한다.
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

    // 검증에 성공하거나 실패한 보안문자를 저장소에서 제거한다.
    private void verifySecurityChallenge(String challengeId, String challengeAnswer) {
        verifySecurityChallenge(challengeId, challengeAnswer, true);
    }

    // consume 값에 따라 보안문자를 즉시 소모하거나 다음 요청까지 유지한다.
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

    // 저장된 암호화 비밀번호와 사용자가 입력한 현재 비밀번호를 비교한다.
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

    // 보안 검증 후 본인 회원 상태를 전출로 변경하고 탈퇴일을 기록한다.
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

    // 보안 검증 후 현재 비밀번호와 다른 새 비밀번호를 암호화해 저장한다.
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

}
