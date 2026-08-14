package api.mem_notice_p;

import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mem-notices")
public class MemNoticeController {

    @Resource
    private MemNoticeService memNoticeService;

    // 로그인한 입주민 본인의 알림 목록 조회
    @GetMapping("/resident")
    public List<MemNoticeDTO> notificationList(
            Authentication authentication
    ) {
        return memNoticeService.notificationList(
                authentication.getName()
        );
    }

    // 로그인한 입주민이 선택한 알림 한 건 읽음 처리
    @PatchMapping("/resident/{memNoticeNo}/read")
    public int markRead(
            Authentication authentication,
            @PathVariable int memNoticeNo
    ) {
        return memNoticeService.markRead(
                authentication.getName(),
                memNoticeNo
        );
    }

    // 로그인한 입주민 본인의 알림 삭제
    @DeleteMapping("/resident/{memNoticeNo}")
    public int deleteNotification(
            Authentication authentication,
            @PathVariable int memNoticeNo
    ) {
        return memNoticeService.deleteNotification(
                authentication.getName(),
                memNoticeNo
        );
    }
}
