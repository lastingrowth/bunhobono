package api.inquiry_p;

import api.a_security_config.AuthService;
import api.a_security_config.LoginDTO;
import api.mem_notice_p.MemNoticeDTO;
import api.mem_notice_p.MemNoticeService;
import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service

public class InquiryService {

    // 문의 등록에 허용할 분류
    private static final List<String> CATEGORIES =
            List.of(
              "PARKING",
              "VISIT",
              "PAYMENT",
              "ETC"
            );

    @Resource
    private InquiryMapper inquiryMapper;

    @Resource
    private AuthService authService;

    @Resource
    private TrashService trashService;

    @Resource
    private MemNoticeService memNoticeService;


    // 입주민 문의 등록
    public int insert(InquiryDTO dto, String loginId) {

        LoginDTO member = resident(loginId);

        validateInquiry(dto);

        // 로그인한 입주민의 실제 회원 번호를 문의 작성자로 설정
        dto.setMemberNo(member.getMemberNo());

        // 일반 문의이므로 최초 문의 연결 번호 없음
        dto.setRootInquiryNo(null);

        return inquiryMapper.insert(dto);
    }

    // 입주민 본인 문의 목록 조회
    public List<InquiryDTO> listByMember(String loginId) {
        LoginDTO member = resident(loginId);

        return inquiryMapper.listByMemberNo(member.getMemberNo());
    }

    // 입주민 본인 문의 상세 조회
    public InquiryDTO detailByMember(int inquiryNo, String loginId) {
        LoginDTO member = resident(loginId);

        InquiryDTO inquiry = inquiryMapper.detailByMember(inquiryNo, member.getMemberNo());

        if (inquiry == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "문의사항이 없습니다."
            );
        }
        return inquiry;
    }

    // 관리자 문의 목록 조회
    public List<InquiryDTO> listByStatus(String status, String loginId) {

        admin(loginId);

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();

        if (!"WAITING".equals(normalizedStatus) && !"ANSWERED".equals(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "문의 상태가 올바르지 않습니다."
            );
        }

        return inquiryMapper.listByStatus(normalizedStatus);
    }

    // 관리자 문의 상세 조회
    public InquiryDTO detailByAdmin(int inquiryNo, String loginId) {
        admin(loginId);

        InquiryDTO inquiry = inquiryMapper.detail(inquiryNo);

        if (inquiry == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "문의사항이 없습니다."
            );
        }
        return inquiry;
    }

    // 관리자 답변 등록
    @Transactional
    public int answer(int inquiryNo, InquiryDTO dto, String loginId) {
        LoginDTO admin = admin(loginId);

        if (dto == null || dto.getAnswerContent() == null || dto.getAnswerContent().trim().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "답변 내용을 입력해 주세요."
            );
        }

        InquiryDTO inquiry = inquiryMapper.detail(inquiryNo);

        if (inquiry == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "문의사항이 없습니다."
            );
        }

        if (!"WAITING".equals(inquiry.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 답변이 등록된 문의입니다."
            );
        }

        dto.setInquiryNo(inquiryNo);
        dto.setAnswerContent(dto.getAnswerContent().trim());
        dto.setAnsweredBy(admin.getMemberNo());

        int answered = inquiryMapper.answer(dto);

        if (answered == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "답변을 등록할 수 없습니다."
            );
        }

        createInquiryAnsweredNotification(inquiry);

        return answered;
    }

    // 문의를 작성한 입주민에게 관리자 답변 등록 알림 생성
    private void createInquiryAnsweredNotification(
            InquiryDTO inquiry
    ) {
        MemNoticeDTO notice = new MemNoticeDTO();
        notice.setRecipientMemberNo(inquiry.getMemberNo());
        notice.setReferenceTable("inquiry");
        notice.setReferenceNo(inquiry.getInquiryNo());
        notice.setNoticeType("INQUIRY_ANSWERED");
        notice.setTitle("1:1 문의 답변 등록");
        notice.setMessage(
                "문의하신 ‘"
                        + inquiry.getTitle()
                        + "’에 답변이 등록되었습니다."
        );

        memNoticeService.createNotification(notice);
    }

    // 입주민 재문의 등록
    public int reInquiry(int inquiryNo, InquiryDTO dto, String loginId) {
        LoginDTO member = resident(loginId);

        // 재문의 대상이 본인의 문의인지 확인
        InquiryDTO previous = inquiryMapper.detailByMember(inquiryNo, member.getMemberNo());

        if (previous == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "재문의할 문의사항이 없습니다."
            );
        }

        // 답변완료된 문의에만 재문의 가능
        if (!"ANSWERED".equals(previous.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "답변완료된 문의만 재문의할 수 있습니다."
            );
        }

        // 최초 문의 번호 결정
        int rootInquiryNo =
                previous.getRootInquiryNo() == null
                        ? previous.getInquiryNo()
                        : previous.getRootInquiryNo();

        // 같은 문의 흐름에 답변대기 문의가 있으면 추가 재문의 차단
        if (inquiryMapper.countWaitingByRoot(
                rootInquiryNo
        ) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 답변을 기다리는 재문의가 있습니다."
            );
        }

        if (dto == null
                || dto.getContent() == null
                || dto.getContent().trim().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "재문의 내용을 입력해 주세요."
            );
        }

        dto.setMemberNo(member.getMemberNo());
        dto.setRootInquiryNo(rootInquiryNo);

        // 분류는 이전 문의에서 가져옴
        dto.setCategory(previous.getCategory());

        // 재문의 제목은 기존 문의 제목을 그대로 저장
        // RE: 표시는 화면에서 rootInquiryNo가 있을 때 추가
        dto.setTitle(previous.getTitle());

        dto.setContent(dto.getContent().trim());

        return inquiryMapper.insert(dto);
    }

    // 답변 완료 후 3개월이 지난 문의를 지난 기록으로 이동
    public void moveOldInquiriesToTrash() {
        List<Integer> inquiryNos = inquiryMapper.findInquiryNosForTrash();

        for (Integer inquiryNo : inquiryNos) {
            try {
                trashService.moveInquiry(inquiryNo, "SCHEDULED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 문의 입력값 검사
    private void validateInquiry(InquiryDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "문의 내용을 입력해 주세요."
            );
        }

        dto.setCategory(dto.getCategory() == null ? "" : dto.getCategory().trim());

        dto.setTitle(dto.getTitle() == null ? "" : dto.getTitle().trim());

        dto.setContent(dto.getContent() == null ? "" : dto.getContent().trim());

        if (!CATEGORIES.contains(dto.getCategory())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 문의 분류를 선택해 주세요."
            );
        }

        if (dto.getTitle().isBlank() || dto.getTitle().length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "제목은 1~200자로 입력해 주세요."
            );
        }

        if (dto.getContent().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "문의 내용을 입력해 주세요."
            );
        }
    }

    // 로그인한 입주민 정보 조회
    private LoginDTO resident(String loginId) {
        LoginDTO member = authService.getUserInfo(loginId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (!"RESIDENT".equals(member.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "입주민만 이용할 수 있습니다."
            );
        }
        return member;
    }

    // 로그인한 관리자 정보 조회
    private LoginDTO admin(String loginId) {
        LoginDTO member = authService.getUserInfo(loginId);

        if (member == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (!"ADMIN".equals(member.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "관리자만 이용할 수 있습니다."
            );
        }
        return member;
    }

    // 관리자가 문의 흐름 전체를 지난 기록으로 이동
    @Transactional
    public int deleteByAdmin(int inquiryNo, String loginId) {

        admin(loginId);

        // 이동할 문의 조회
        InquiryDTO inquiry = inquiryMapper.detail(inquiryNo);

        if (inquiry == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "삭제할 문의사항이 없습니다."
            );
        }

        // 최초 문의 번호 결정
        int rootInquiryNo =
                inquiry.getRootInquiryNo() == null
                        ? inquiry.getInquiryNo()
                        : inquiry.getRootInquiryNo();

        // 최초 문의와 연결된 재문의 조회
        List<Integer> inquiryNos =
                inquiryMapper.findInquiryNosByRoot(
                        rootInquiryNo,
                        inquiry.getMemberNo()
                );

        // 외래키를 참조하는 재문의부터 지난 기록으로 이동
        for (Integer targetInquiryNo : inquiryNos) {
            trashService.moveInquiry(
                    targetInquiryNo,
                    "MANUAL"
            );
        }

        return inquiryNos.size();
    }

    // 입주민 본인 문의 삭제
    @Transactional
    public int deleteByMember(int inquiryNo, String loginId) {

        LoginDTO member = resident(loginId);

        // 로그인한 입주민 본인의 문의인지 확인
        InquiryDTO inquiry =
                inquiryMapper.detailByMember(
                        inquiryNo,
                        member.getMemberNo()
                );

        if (inquiry == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "삭제할 문의사항이 없습니다."
            );
        }

        // 최초 문의 번호 결정
        int rootInquiryNo =
                inquiry.getRootInquiryNo() == null
                        ? inquiry.getInquiryNo()
                        : inquiry.getRootInquiryNo();

        // 최초 문의와 연결된 재문의 조회
        List<Integer> inquiryNos =
                inquiryMapper.findInquiryNosByRoot(
                        rootInquiryNo,
                        member.getMemberNo()
                );

        // 외래키를 참조하는 재문의부터 지난 기록으로 이동
        for (Integer targetInquiryNo : inquiryNos) {
            trashService.moveInquiry(
                    targetInquiryNo,
                    "MANUAL"
            );
        }

        return inquiryNos.size();
    }
}
