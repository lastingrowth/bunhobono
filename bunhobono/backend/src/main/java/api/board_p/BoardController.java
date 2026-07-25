package api.board_p;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService service;

    public BoardController(BoardService service) {
        this.service = service;
    }

    // 공지사항 등록
    @PostMapping(
            value = "/signUp",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public BoardDTO create(
            @ModelAttribute BoardDTO dto,
            @RequestParam(value = "image", required = false)
            MultipartFile image,
            Authentication authentication
    ) {
        requireRole(authentication, "ADMIN");

        String loginId = authentication.getName();

        return service.create(dto, image, loginId);
    }

    // 공지사항 목록 조회 (관리자는 전체 공지, 입주민은 게시 중인 공지를 반환)
    @GetMapping
    public List<BoardDTO> list(Authentication authentication) {
        requireRole(authentication, "ADMIN", "RESIDENT");

        return service.list(!hasRole(authentication, "ADMIN"));
    }

    // 공지사항 상세 조회 (관리자는 전체 공지, 입주민은 게시 중인 공지를 반환)
    @GetMapping("/{boardNo}/detail")
    public BoardDTO detail(
            @PathVariable int boardNo,
            Authentication authentication
    ) {
        requireRole(authentication, "ADMIN", "RESIDENT");

        return service.detail(boardNo, !hasRole(authentication, "ADMIN"));
    }

    // 이미지 조회 (서버에 저장된 이미지 파일을 프론트로 전달)
    @GetMapping("/{boardNo}/image")
    public ResponseEntity<Resource> image(
            @PathVariable int boardNo,
            Authentication authentication
    ) {
        requireRole(authentication, "ADMIN", "RESIDENT");
        BoardService.BoardImage image = service.image(
                boardNo,
                !hasRole(authentication, "ADMIN")
        );

        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(image.resource());
    }

    // 수정
    @PutMapping(
            value = "/{boardNo}/edit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public BoardDTO update(
            @PathVariable int boardNo,
            @ModelAttribute BoardDTO dto,
            @RequestParam(value = "image", required = false)
            MultipartFile image,
            Authentication authentication
    ) {
        requireRole(authentication, "ADMIN");
        return service.update(boardNo, dto, image);
    }

    // 삭제 (선택한 공지 번호를 Service로 전달해 공지와 이미지를 삭제)
    @DeleteMapping("/{boardNo}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable int boardNo,
            Authentication authentication
    ) {
        requireRole(authentication, "ADMIN");
        service.delete(boardNo);
    }

    // 권한 확인 (로그인 사용자의 관리자·입주민 권한을 검사)
    private void requireRole(
            Authentication authentication,
            String... roles
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        boolean allowed = Arrays.stream(roles)
                .anyMatch(role -> hasRole(authentication, role));

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 권한이 없습니다."
            );
        }
    }

    private boolean hasRole(
            Authentication authentication,
            String role
    ) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equalsIgnoreCase(
                        authority.getAuthority()
                ));
    }
}
