package api.faq_p;

import jakarta.annotation.Resource;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    @Resource
    private FaqService faqService;

    // 자주하는 질문 목록 조회
    @GetMapping
    public List<FaqDTO> list() {
        return faqService.list();
    }

    // 자주하는 질문 등록
    @PostMapping("/signup")
    public int insert(@RequestBody FaqDTO dto, Authentication authentication) {
        return faqService.insert(dto, authentication.getName());
    }

    // 자주하는 질문 수정
    @PutMapping("/edit/{faqNo}")
    public int update(
            @PathVariable int faqNo,
            @RequestBody FaqDTO dto,
            Authentication authentication
    ) {
        return faqService.update(faqNo, dto, authentication.getName());
    }

    // 자주하는 질문 삭제
    @DeleteMapping("/delete/{faqNo}")
    public int delete(
            @PathVariable int faqNo,
            Authentication authentication
    ) {
        return faqService.delete(faqNo, authentication.getName());
    }
}
