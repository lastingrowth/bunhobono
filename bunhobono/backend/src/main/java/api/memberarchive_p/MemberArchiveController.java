package api.memberarchive_p;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 전출 확정되어 member_archive에 보관된 회원 이력을 관리한다.
@RestController
@RequestMapping("/api/member-archive")
public class MemberArchiveController {

    @Resource
    MemberArchiveService service;

    // 전출 회원 이력 목록 조회
    @GetMapping("")
    public List<MemberArchiveDTO> list() {
        return service.list();
    }

    // 전출 회원 이력 영구 삭제
    // member 원본이 아니라 member_archive 기록만 삭제한다.
    @DeleteMapping("/{archiveNo}")
    public void delete(@PathVariable long archiveNo) {
        service.delete(archiveNo);
    }
}
