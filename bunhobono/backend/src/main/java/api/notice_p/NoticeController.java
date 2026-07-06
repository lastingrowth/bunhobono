package api.notice_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Resource
    NoticeService noticeService;

    @GetMapping("")
    public List<NoticeDTO> list(NoticeDTO dto) {
        List<NoticeDTO> list = noticeService.listservice(dto);
        System.out.println("알림정보확인: "+list);
        return list;
    }

    @GetMapping("/{noticeNo}")
    public NoticeDTO detail(@PathVariable int noticeNo) {
        return noticeService.detail(noticeNo);
    }

    @PutMapping("/{noticeNo}/status")
    public void status(@PathVariable int noticeNo, @RequestBody NoticeDTO dto) {
        dto.setNoticeNo(noticeNo);
        noticeService.status(dto);
    }
}
