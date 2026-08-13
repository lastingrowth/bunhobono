package api.board_p;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BoardService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final String SEED_IMAGE_PREFIX = "classpath:";
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp"
    );

    @Resource
    private BoardMapper mapper;

    @Value("${file.board:${user.dir}/runtime/board}")
    private String uploadDir;

    // 공지사항과 첨부 이미지를 등록한다.
    @Transactional
    public BoardDTO create(
            BoardDTO dto,
            MultipartFile image,
            String createdBy
    ) {
        validate(dto);
        dto.setCreatedBy(createdBy);

        String savedPath = null;

        try {
            if (hasFile(image)) {
                savedPath = saveImage(dto, image);
            }

            mapper.insert(dto);
            return detail(dto.getBoardNo(), false);
        } catch (RuntimeException exception) {
            deleteImage(savedPath);
            throw exception;
        }
    }

    // 관리자 전체 목록 또는 입주민 게시 목록을 조회한다.
    public List<BoardDTO> list(boolean activeOnly) {
        List<BoardDTO> boards = activeOnly
                ? mapper.activeList()
                : mapper.list();

        // 최신 공지부터 화면용 번호를 1번으로 부여한다.
        for (int index = 0; index < boards.size(); index++) {
            boards.get(index).setListNo(index + 1);
        }

        return boards;
    }

    // 공지사항 전체 상세 또는 게시 중인 상세를 조회한다.
    public BoardDTO detail(int boardNo, boolean activeOnly) {
        BoardDTO board = activeOnly
                ? mapper.activeDetail(boardNo)
                : mapper.detail(boardNo);

        if (board == null) {
            String message = activeOnly
                    ? "게시 중인 공지사항이 아닙니다."
                    : "공지사항을 찾을 수 없습니다.";
            throw error(HttpStatus.NOT_FOUND, message);
        }

        return board;
    }

    // 공지사항에 저장된 이미지 파일을 반환한다.
    public BoardImage image(int boardNo, boolean activeOnly) {
        BoardDTO board = detail(boardNo, activeOnly);

        if (board.getImagePath() == null || board.getImagePath().isBlank()) {
            throw error(HttpStatus.NOT_FOUND, "등록된 이미지가 없습니다.");
        }

        ClassPathResource seedResource = seedResource(board.getImagePath());
        if (seedResource != null) {
            return seedImage(board, seedResource);
        }

        try {
            Path path = safeImagePath(board.getImagePath());

            if (!Files.isRegularFile(path)) {
                throw error(HttpStatus.NOT_FOUND, "이미지 파일을 찾을 수 없습니다.");
            }

            String imageType = board.getImageType();
            if (imageType == null || imageType.isBlank()) {
                imageType = Files.probeContentType(path);
            }

            MediaType mediaType = imageType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(imageType);

            return new BoardImage(new UrlResource(path.toUri()), mediaType);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    // 공지 내용과 첨부 이미지를 수정한다.
    @Transactional
    public BoardDTO update(
            int boardNo,
            BoardDTO dto,
            MultipartFile image
    ) {
        BoardDTO before = detail(boardNo, false);
        dto.setBoardNo(boardNo);
        validate(dto);

        String newPath = null;

        if (hasFile(image)) {
            newPath = saveImage(dto, image);
        } else {
            applyImage(dto, Boolean.TRUE.equals(dto.getRemoveImage()) ? null : before);
        }

        try {
            if (mapper.update(dto) == 0) {
                throw error(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
            }
        } catch (RuntimeException exception) {
            deleteImage(newPath);
            throw exception;
        }

        if (!Objects.equals(before.getImagePath(), dto.getImagePath())) {
            deleteImage(before.getImagePath());
        }

        return detail(boardNo, false);
    }

    // 공지사항과 서버에 저장된 첨부 이미지를 삭제한다.
    @Transactional
    public void delete(int boardNo) {
        BoardDTO board = detail(boardNo, false);

        // [댓글] 공지사항을 삭제하기 전에 연결된 댓글을 삭제한다.
        mapper.deleteCommentsByBoardNo(boardNo);

        if (mapper.delete(boardNo) == 0) {
            throw error(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
        }

        deleteImage(board.getImagePath());
    }

    // ================================
    // 댓글
    // ================================
    // 댓글 목록을 3단계 부모·자식 구조로 만든다.
    public List<BoardDTO> commentList(int boardNo, String loginId) {
        detail(boardNo, false);
        List<BoardDTO> comments = mapper.commentList(boardNo, loginId);
        Map<Integer, BoardDTO> byCommentNo = new LinkedHashMap<>();
        List<BoardDTO> roots = new ArrayList<>();

        for (BoardDTO comment : comments) {
            comment.setReplies(new ArrayList<>());
            byCommentNo.put(comment.getCommentNo(), comment);
        }

        for (BoardDTO comment : comments) {
            if (comment.getParentCommentNo() == null) {
                roots.add(comment);
                continue;
            }

            BoardDTO parent = byCommentNo.get(comment.getParentCommentNo());
            if (parent != null) {
                parent.getReplies().add(comment);
            }
        }

        return roots;
    }

    // 댓글 입력창에 표시할 로그인 사용자의 이름을 반환한다.
    public String commentWriterName(String loginId) {
        String name = mapper.findCommentWriterName(loginId);
        return name == null || name.isBlank() ? loginId : name;
    }

    // 로그인 사용자의 댓글 또는 대댓글을 등록한다.
    @Transactional
    public BoardDTO createComment(int boardNo, BoardDTO dto, String loginId) {
        detail(boardNo, false);
        normalizeComment(dto);

        if (dto.getParentCommentNo() != null) {
            BoardDTO parent = mapper.findComment(dto.getParentCommentNo());
            if (parent == null || !Objects.equals(parent.getBoardNo(), boardNo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            Integer parentDepth = mapper.findCommentDepth(dto.getParentCommentNo());
            if (parentDepth == null || parentDepth >= 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }

        if (mapper.insertComment(boardNo, loginId, dto) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return mapper.findComment(dto.getCommentNo());
    }

    // 작성자 본인의 댓글 내용만 수정한다.
    @Transactional
    public BoardDTO updateComment(
            int boardNo,
            int commentNo,
            BoardDTO dto,
            String loginId
    ) {
        normalizeComment(dto);

        if (mapper.updateComment(
                boardNo,
                commentNo,
                loginId,
                dto.getCommentContent()
        ) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return mapper.findComment(commentNo);
    }

    // 작성자 본인의 댓글과 모든 하위 댓글을 아래 단계부터 삭제한다.
    @Transactional
    public void deleteComment(
            int boardNo,
            int commentNo,
            String loginId,
            boolean admin
    ) {
        if (mapper.deleteCommentTree(boardNo, commentNo, loginId, admin) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    // [댓글] 저장할 댓글 내용의 공백만 정리한다.
    private void normalizeComment(BoardDTO dto) {
        if (dto == null || dto.getCommentContent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        dto.setCommentContent(dto.getCommentContent().trim());
        if (dto.getCommentContent().isEmpty()
                || dto.getCommentContent().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    // 공지 입력값과 게시기간을 검증하고 기본값을 적용한다.
    private void validate(BoardDTO dto) {
        if (dto == null) {
            throw error(HttpStatus.BAD_REQUEST, "공지 내용이 필요합니다.");
        }

        dto.setTitle(dto.getTitle() == null ? "" : dto.getTitle().trim());
        dto.setContent(dto.getContent() == null ? "" : dto.getContent().trim());

        if (dto.getTitle().isBlank() || dto.getTitle().length() > 150) {
            throw error(HttpStatus.BAD_REQUEST, "제목은 1~150자로 입력해 주세요.");
        }

        if (dto.getContent().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "공지 내용을 입력해 주세요.");
        }

        if (dto.getStartAt() == null) {
            dto.setStartAt(LocalDateTime.now());
        }

        if (dto.getEndAt() != null
                && dto.getEndAt().isBefore(dto.getStartAt())) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "종료일은 시작일보다 빠를 수 없습니다."
            );
        }

        if (dto.getActive() == null) {
            dto.setActive(true);
        }
    }

    // 이미지 형식과 크기를 확인한 뒤 서버 폴더에 저장한다.
    private String saveImage(BoardDTO dto, MultipartFile image) {
        String type = image.getContentType();
        String extension = IMAGE_EXTENSIONS.get(type);

        if (extension == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "JPG, PNG, WEBP 이미지만 등록할 수 있습니다."
            );
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "이미지는 5MB 이하만 등록할 수 있습니다."
            );
        }

        try {
            Path directory = uploadRoot();
            Files.createDirectories(directory);

            Path savedPath = directory
                    .resolve(UUID.randomUUID() + extension)
                    .normalize();

            Files.copy(
                    image.getInputStream(),
                    savedPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            dto.setImagePath(savedPath.toString());
            dto.setImageName(image.getOriginalFilename());
            dto.setImageType(type);
            dto.setHasImage(true);
            return savedPath.toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지를 저장하지 못했습니다.",
                    exception
            );
        }
    }

    // 기존 이미지 정보를 유지하거나 제거한다.
    private void applyImage(BoardDTO target, BoardDTO source) {
        target.setImagePath(source == null ? null : source.getImagePath());
        target.setImageName(source == null ? null : source.getImageName());
        target.setImageType(source == null ? null : source.getImageType());
        target.setHasImage(source != null && Boolean.TRUE.equals(source.getHasImage()));
    }

    // Git에 포함된 더미 포스터를 클래스패스에서 불러온다.
    private BoardImage seedImage(
            BoardDTO board,
            ClassPathResource resource
    ) {
        String imageType = board.getImageType();
        MediaType mediaType = imageType == null || imageType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(imageType);

        return new BoardImage(resource, mediaType);
    }

    // 절대경로와 관계없이 같은 이름의 더미 포스터를 찾는다.
    private ClassPathResource seedResource(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String resourcePath;

        if (imagePath.startsWith(SEED_IMAGE_PREFIX)) {
            resourcePath = imagePath.substring(SEED_IMAGE_PREFIX.length());
        } else {
            int separator = Math.max(
                    imagePath.lastIndexOf('/'),
                    imagePath.lastIndexOf('\\')
            );
            String fileName = imagePath.substring(separator + 1);
            resourcePath = "board-seed/" + fileName;
        }

        ClassPathResource resource = new ClassPathResource(resourcePath);
        return resource.exists() ? resource : null;
    }

    // 공지 이미지 저장 폴더의 절대경로를 반환한다.
    private Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // 공지 이미지 저장 폴더 내부의 안전한 경로만 반환한다.
    private Path safeImagePath(String imagePath) {
        Path root = uploadRoot();
        Path path = Paths.get(imagePath).toAbsolutePath().normalize();

        if (!path.startsWith(root)) {
            throw error(HttpStatus.BAD_REQUEST, "잘못된 이미지 경로입니다.");
        }

        return path;
    }

    // 서버에 저장된 공지 이미지를 삭제한다.
    private void deleteImage(String imagePath) {
        if (imagePath == null
                || imagePath.isBlank()
                || seedResource(imagePath) != null) {
            return;
        }

        try {
            Files.deleteIfExists(safeImagePath(imagePath));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "이미지를 삭제하지 못했습니다.",
                    exception
            );
        }
    }

    // 요청에 실제 이미지 파일이 포함됐는지 확인한다.
    private boolean hasFile(MultipartFile image) {
        return image != null && !image.isEmpty();
    }

    // HTTP 상태와 메시지를 가진 예외를 생성한다.
    private ResponseStatusException error(
            HttpStatus status,
            String message
    ) {
        return new ResponseStatusException(status, message);
    }

    // 이미지 파일과 응답 형식을 함께 전달한다.
    public record BoardImage(
            org.springframework.core.io.Resource resource,
            MediaType mediaType
    ) {
    }
}
