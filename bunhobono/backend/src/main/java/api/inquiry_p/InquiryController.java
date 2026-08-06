package api.inquiry_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    @Resource
    private InquiryService inquiryService;

    // 입주민 문의 등록
    @PostMapping
    public int insert(Authentication authentication, @RequestBody InquiryDTO dto) {
        return inquiryService.insert(dto, authentication.getName());
    }

    // 입주민 본인 문의 목록 조회
    @GetMapping("/resident")
    public List<InquiryDTO> listByMember(
            Authentication authentication
    ) {
        return inquiryService.listByMember(
                authentication.getName()
        );
    }

    // 입주민 본인 문의 상세 조회
    @GetMapping("/resident/{inquiryNo}")
    public InquiryDTO detailByMember(
            Authentication authentication,
            @PathVariable int inquiryNo
    ) {
        return inquiryService.detailByMember(
                inquiryNo,
                authentication.getName()
        );
    }

    // 입주민 재문의 등록
    @PostMapping("/resident/{inquiryNo}/re-inquiry")
    public int reInquiry(
            Authentication authentication,
            @PathVariable int inquiryNo,
            @RequestBody InquiryDTO dto
    ) {
        return inquiryService.reInquiry(
                inquiryNo,
                dto,
                authentication.getName()
        );
    }

    // 관리자 문의 목록 조회
    @GetMapping("/admin")
    public List<InquiryDTO> listByStatus(
            Authentication authentication,
            @RequestParam String status
    ) {
        return inquiryService.listByStatus(
                status,
                authentication.getName()
        );
    }

    // 관리자 문의 상세 조회
    @GetMapping("/admin/{inquiryNo}")
    public InquiryDTO detailByAdmin(
            Authentication authentication,
            @PathVariable int inquiryNo
    ) {
        return inquiryService.detailByAdmin(
                inquiryNo,
                authentication.getName()
        );
    }

    // 관리자 답변 등록
    @PatchMapping("/admin/{inquiryNo}/answer")
    public int answer(
            Authentication authentication,
            @PathVariable int inquiryNo,
            @RequestBody InquiryDTO dto
    ) {
        return inquiryService.answer(
                inquiryNo,
                dto,
                authentication.getName()
        );
    }
}
