import api from "@/shared/api/apiClient";

const toFormData = (board, image) => {
  const formData = new FormData();

  Object.entries(board).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== "") {
      formData.append(key, value);
    }
  });

  if (image) {
    formData.append("image", image);
  }

  return formData;
};

const multipartConfig = {
  headers: {
    "Content-Type": "multipart/form-data",
  },
};

// 목록 조회
export const getBoards = () => api.get("/boards");

// 상세 조회
export const getBoard = (boardNo) =>
  api.get(`/boards/${boardNo}/detail`);

// 이미지 조회
export const getBoardImage = (boardNo) =>
  api.get(`/boards/${boardNo}/image`, {
    params: { v: Date.now() },
    responseType: "blob",
  });

// 등록
export const createBoard = (board, image) =>
  api.post(
    "/boards/signUp",
    toFormData(board, image),
    multipartConfig
  );

// 수정
export const updateBoard = (boardNo, board, image) =>
  api.put(
    `/boards/${boardNo}/edit`,
    toFormData(board, image),
    multipartConfig
  );

// 삭제
export const deleteBoard = (boardNo) =>
  api.delete(`/boards/${boardNo}/delete`);

// 댓글 목록 조회
export const getBoardComments = (boardNo) =>
  api.get(`/boards/${boardNo}/comments`);

export const getCommentWriterName = () =>
  api.get("/boards/comments/writer");

// 댓글 또는 대댓글 등록
export const createBoardComment = (boardNo, data) =>
  api.post(`/boards/${boardNo}/comments`, data);

// 댓글 수정
export const updateBoardComment = (boardNo, commentNo, data) =>
  api.put(`/boards/${boardNo}/comments/${commentNo}`, data);

// 댓글과 하위 댓글 삭제
export const deleteBoardComment = (boardNo, commentNo) =>
  api.delete(`/boards/${boardNo}/comments/${commentNo}`);
