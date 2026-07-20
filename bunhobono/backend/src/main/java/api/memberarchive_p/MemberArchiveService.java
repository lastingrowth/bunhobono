package api.memberarchive_p;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MemberArchiveService {

    @Resource
    MemberArchiveMapper mapper;

    // 전출 확정되어 보관된 회원 이력 목록을 조회한다.
    public List<MemberArchiveDTO> list() {
        return mapper.list();
    }

    // member_archive에 보관된 전출 회원 이력을 영구 삭제한다.
    // member 원본을 삭제하는 것이 아니라 archive 기록만 삭제한다.
    public void delete(long archiveNo) {
        int deleted = mapper.delete(archiveNo);

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "전출 회원 이력을 찾을 수 없습니다."
            );
        }
    }
}
