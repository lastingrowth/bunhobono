package api.faq_p;

import api.a_security_config.AuthService;
import api.a_security_config.LoginDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FaqService {

    // 자주하는 질문에 사용할 수 있는 분류
    private static final List<String> CATEGORIES =
            List.of(
                    "PARKING",
                    "VISIT",
                    "PAYMENT",
                    "ETC"
            );

    @Resource
    private FaqMapper faqMapper;

    @Resource
    private AuthService authService;

    // (관리자) 자주하는 질문 등록
    public int insert(FaqDTO dto, String loginId) {
        admin(loginId);

        validateFaq(dto);

        return faqMapper.insert(dto);
    }

    // 자주하는 질문 목록 조회
    public List<FaqDTO> list() {
        return faqMapper.list();
    }

    // (관리자) 자주하는 질문 수정
    public int update(int faqNo, FaqDTO dto, String loginId) {
        admin(loginId);

        validateFaq(dto);

        dto.setFaqNo(faqNo);

        int updated = faqMapper.update(dto);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "자주하는 질문이 없습니다."
            );
        }
        return updated;
    }

    // (관리자) 자주하는 질문 삭제
    public int delete(int faqNo, String loginId) {
        admin(loginId);

        int deleted = faqMapper.delete(faqNo);

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "자주하는 질문이 없습니다."
            );
        }
        return deleted;
    }

    // 입력값 검사
    private void validateFaq(FaqDTO dto) {
        if(dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자주하는 질문 내용을 입력해주세요."
            );
        }

        dto.setCategory(
                dto.getCategory() == null ? "" : dto.getCategory().trim()
        );

        dto.setQuestion(
                dto.getQuestion() == null ? "" : dto.getQuestion().trim()
        );

        dto.setAnswer(
                dto.getAnswer() == null ? "" : dto.getAnswer().trim()
        );

        if (!CATEGORIES.contains(dto.getCategory())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 분류를 선택해주세요."
            );
        }

        if(dto.getQuestion().isBlank() || dto.getQuestion().length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문은 1~200자로 입력해주세요."
            );
        }

        if (dto.getAnswer().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "답변 내용을 입력해주세요."
            );
        }
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

}
