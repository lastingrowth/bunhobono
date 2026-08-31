package api.board_p;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardMapper mapper;

    @InjectMocks
    private BoardService boardService;

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                boardService,
                "uploadDir",
                temporaryDirectory.toString()
        );
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-001 | 공지 입력값을 정리하고 기본값을 적용해 등록한다"
    )
    void create_normalizesAndAppliesDefaults() {
        BoardDTO request = validBoard();
        request.setTitle("  주차장 점검  ");
        request.setContent("  점검 안내입니다.  ");

        doAnswer(invocation -> {
            BoardDTO inserted = invocation.getArgument(0);
            inserted.setBoardNo(1);
            return 1;
        }).when(mapper).insert(request);

        BoardDTO saved = new BoardDTO();
        saved.setBoardNo(1);
        when(mapper.detail(1)).thenReturn(saved);

        BoardDTO result = boardService.create(
                request,
                null,
                "admin01"
        );

        assertSame(saved, result);
        assertEquals("주차장 점검", request.getTitle());
        assertEquals("점검 안내입니다.", request.getContent());
        assertEquals("admin01", request.getCreatedBy());
        assertTrue(request.getActive());
        assertNotNull(request.getStartAt());
        verify(mapper).insert(request);
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-002 | 제목이나 내용이 없는 공지 등록을 거부한다"
    )
    void create_rejectsBlankTitleOrContent() {
        BoardDTO request = validBoard();
        request.setTitle("   ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.create(request, null, "admin01")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-003 | 지원하는 이미지를 테스트 임시 폴더에 저장한다"
    )
    void create_savesSupportedImageInTemporaryDirectory()
            throws Exception {
        BoardDTO request = validBoard();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "notice.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3}
        );

        doAnswer(invocation -> {
            request.setBoardNo(1);
            return 1;
        }).when(mapper).insert(request);
        when(mapper.detail(1)).thenReturn(request);

        BoardDTO result = boardService.create(
                request,
                image,
                "admin01"
        );

        assertSame(request, result);
        assertEquals("notice.png", request.getImageName());
        assertEquals(MediaType.IMAGE_PNG_VALUE, request.getImageType());
        assertTrue(request.getHasImage());
        assertTrue(Files.isRegularFile(Path.of(request.getImagePath())));
        assertTrue(
                Path.of(request.getImagePath())
                        .startsWith(temporaryDirectory)
        );
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-004 | 지원하지 않는 이미지 형식을 거부한다"
    )
    void create_rejectsUnsupportedImageType() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "notice.gif",
                MediaType.IMAGE_GIF_VALUE,
                new byte[]{1, 2, 3}
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.create(
                        validBoard(),
                        image,
                        "admin01"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-005 | 관리자·입주민 목록을 구분하고 화면 번호를 부여한다"
    )
    void list_selectsVisibilityAndAssignsListNumbers() {
        BoardDTO first = new BoardDTO();
        BoardDTO second = new BoardDTO();
        when(mapper.list()).thenReturn(List.of(first, second));
        when(mapper.activeList()).thenReturn(List.of(first));

        List<BoardDTO> adminList = boardService.list(false);
        List<BoardDTO> residentList = boardService.list(true);

        assertEquals(2, adminList.size());
        assertEquals(1, first.getListNo());
        assertEquals(2, second.getListNo());
        assertEquals(List.of(first), residentList);
        verify(mapper).list();
        verify(mapper).activeList();
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-006 | 게시 여부에 맞는 상세 조회를 선택한다"
    )
    void detail_selectsAdminOrActiveQuery() {
        BoardDTO admin = new BoardDTO();
        BoardDTO active = new BoardDTO();
        when(mapper.detail(1)).thenReturn(admin);
        when(mapper.activeDetail(1)).thenReturn(active);

        assertSame(admin, boardService.detail(1, false));
        assertSame(active, boardService.detail(1, true));
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-007 | 존재하지 않는 공지 상세를 찾을 수 없음으로 처리한다"
    )
    void detail_rejectsMissingBoard() {
        when(mapper.detail(1)).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.detail(1, false)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-008 | 테스트 임시 폴더의 공지 이미지를 응답 객체로 반환한다"
    )
    void image_returnsLocalImageWithMediaType() throws Exception {
        Path imagePath = temporaryDirectory.resolve("notice.png");
        Files.write(imagePath, new byte[]{1, 2, 3});

        BoardDTO board = new BoardDTO();
        board.setImagePath(imagePath.toString());
        board.setImageType(MediaType.IMAGE_PNG_VALUE);
        when(mapper.detail(1)).thenReturn(board);

        BoardService.BoardImage result = boardService.image(1, false);

        assertEquals(MediaType.IMAGE_PNG, result.mediaType());
        assertTrue(result.resource().exists());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-009 | 수정 이미지가 없으면 기존 이미지 정보를 유지한다"
    )
    void update_keepsExistingImageWhenNoNewImageIsGiven() {
        BoardDTO before = validBoard();
        before.setBoardNo(1);
        before.setImagePath(temporaryDirectory.resolve("old.png").toString());
        before.setImageName("old.png");
        before.setImageType(MediaType.IMAGE_PNG_VALUE);
        before.setHasImage(true);

        BoardDTO request = validBoard();
        BoardDTO saved = validBoard();
        saved.setBoardNo(1);

        when(mapper.detail(1)).thenReturn(before, saved);
        when(mapper.update(request)).thenReturn(1);

        BoardDTO result = boardService.update(1, request, null);

        assertSame(saved, result);
        assertEquals(1, request.getBoardNo());
        assertEquals(before.getImagePath(), request.getImagePath());
        assertEquals("old.png", request.getImageName());
        assertTrue(request.getHasImage());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-010 | 공지를 삭제하기 전에 연결 댓글을 먼저 삭제한다"
    )
    void delete_removesCommentsBeforeBoard() {
        BoardDTO board = new BoardDTO();
        board.setBoardNo(1);
        when(mapper.detail(1)).thenReturn(board);
        when(mapper.delete(1)).thenReturn(1);

        boardService.delete(1);

        verify(mapper).deleteCommentsByBoardNo(1);
        verify(mapper).delete(1);
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-011 | 평면 댓글 목록을 부모·자식 계층으로 구성한다"
    )
    void commentList_buildsParentChildHierarchy() {
        when(mapper.detail(1)).thenReturn(new BoardDTO());

        BoardDTO root = comment(1, null);
        BoardDTO child = comment(2, 1);
        BoardDTO grandchild = comment(3, 2);
        when(mapper.commentList(1, "resident01"))
                .thenReturn(List.of(root, child, grandchild));

        List<BoardDTO> result =
                boardService.commentList(1, "resident01");

        assertEquals(List.of(root), result);
        assertEquals(List.of(child), root.getReplies());
        assertEquals(List.of(grandchild), child.getReplies());
        assertTrue(grandchild.getReplies().isEmpty());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-012 | 댓글 작성자 이름이 없으면 로그인 ID를 반환한다"
    )
    void commentWriterName_fallsBackToLoginId() {
        when(mapper.findCommentWriterName("resident01"))
                .thenReturn("   ");

        String result = boardService.commentWriterName("resident01");

        assertEquals("resident01", result);
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-013 | 댓글 내용을 정리해 등록하고 저장 결과를 반환한다"
    )
    void createComment_trimsAndReturnsSavedComment() {
        when(mapper.detail(1)).thenReturn(new BoardDTO());

        BoardDTO request = new BoardDTO();
        request.setCommentContent("  확인했습니다.  ");
        doAnswer(invocation -> {
            request.setCommentNo(10);
            return 1;
        }).when(mapper).insertComment(1, "resident01", request);

        BoardDTO saved = comment(10, null);
        when(mapper.findComment(10)).thenReturn(saved);

        BoardDTO result = boardService.createComment(
                1,
                request,
                "resident01"
        );

        assertSame(saved, result);
        assertEquals("확인했습니다.", request.getCommentContent());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-014 | 3단계 댓글 아래의 추가 대댓글을 거부한다"
    )
    void createComment_rejectsReplyDeeperThanThreeLevels() {
        when(mapper.detail(1)).thenReturn(new BoardDTO());

        BoardDTO parent = comment(10, 9);
        parent.setBoardNo(1);
        when(mapper.findComment(10)).thenReturn(parent);
        when(mapper.findCommentDepth(10)).thenReturn(3);

        BoardDTO request = new BoardDTO();
        request.setParentCommentNo(10);
        request.setCommentContent("대댓글");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.createComment(
                        1,
                        request,
                        "resident01"
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(mapper, never()).insertComment(any(Integer.class), any(), any());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-015 | 작성자 본인의 댓글 내용을 정리해 수정한다"
    )
    void updateComment_trimsContentAndReturnsSavedComment() {
        BoardDTO request = new BoardDTO();
        request.setCommentContent("  수정한 내용  ");
        when(mapper.updateComment(
                1,
                10,
                "resident01",
                "수정한 내용"
        )).thenReturn(1);

        BoardDTO saved = comment(10, null);
        when(mapper.findComment(10)).thenReturn(saved);

        BoardDTO result = boardService.updateComment(
                1,
                10,
                request,
                "resident01"
        );

        assertSame(saved, result);
        assertEquals("수정한 내용", request.getCommentContent());
    }

    @Test
    @DisplayName(
            "UT-BE-BOARD-016 | 댓글 삭제 권한이 없으면 요청을 거부한다"
    )
    void deleteComment_rejectsUnauthorizedDeletion() {
        when(mapper.deleteCommentTree(
                1,
                10,
                "resident01",
                false
        )).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> boardService.deleteComment(
                        1,
                        10,
                        "resident01",
                        false
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private static BoardDTO validBoard() {
        BoardDTO board = new BoardDTO();
        board.setTitle("공지 제목");
        board.setContent("공지 내용");
        board.setStartAt(LocalDateTime.now());
        board.setActive(true);
        return board;
    }

    private static BoardDTO comment(
            int commentNo,
            Integer parentCommentNo
    ) {
        BoardDTO comment = new BoardDTO();
        comment.setCommentNo(commentNo);
        comment.setParentCommentNo(parentCommentNo);
        return comment;
    }
}
