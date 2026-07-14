package api.notice_p;

import api.trash_p.TrashService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private NoticeService noticeService;

    @Resource
    private TrashService trashService;

    @GetMapping("")
    public List<NoticeDTO> list() {
        return noticeService.list();
    }

    @GetMapping("/{noticeNo}")
    public NoticeDTO detail(@PathVariable int noticeNo) {
        return noticeService.detail(noticeNo);
    }

    @PutMapping("/{noticeNo}/status")
    public int status(@PathVariable int noticeNo, @RequestBody NoticeDTO dto) {
        dto.setNoticeNo(noticeNo);

        return noticeService.status(dto);
    }

    @DeleteMapping("/{noticeNo}/delete")
    public int delete(@PathVariable int noticeNo) {
        trashService.moveNotice(noticeNo, "MANUAL");
        return 1;
    }
}
