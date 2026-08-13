import { defineStore } from "pinia";
import { reactive, ref } from "vue";
import {
  createBoard,
  deleteBoard,
  getBoard,
  getBoardImage,
  getBoards,
  updateBoard,
  getBoardComments,
  createBoardComment,
  updateBoardComment,
  deleteBoardComment,
} from "./boardApi";

export const useBoardStore = defineStore("board", () => {
  const list = ref([]);
  const board = ref(null);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");
  const imageUrls = reactive({});
  const comments = ref([]);
  const commentsLoading = ref(false);

  const message = (error, fallback) =>
    error?.response?.data?.message
    || error?.response?.data?.detail
    || fallback;

  // 공지사항을 최신 등록일순으로 정렬한다.
  const sortByNewest = (boards) => [...boards].sort((left, right) => {
    const createdAtDifference =
      new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();

    return createdAtDifference
      || Number(right.boardNo) - Number(left.boardNo);
  });

  // 목록 조회
  const loadList = async () => {
    loading.value = true;
    errorMessage.value = "";

    try {
      const response = await getBoards();
      const boards = Array.isArray(response.data) ? response.data : [];
      list.value = sortByNewest(boards);
      return list.value;
    } catch (error) {
      errorMessage.value = message(error, "공지사항을 불러오지 못했습니다.");
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 상세 조회
  const loadDetail = async (boardNo) => {
    loading.value = true;
    errorMessage.value = "";

    try {
      const response = await getBoard(boardNo);
      board.value = response.data;
      return board.value;
    } catch (error) {
      board.value = null;
      errorMessage.value = message(error, "공지사항을 불러오지 못했습니다.");
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 이미지 조회
  const loadImage = async (boardNo) => {
    if (!boardNo || imageUrls[boardNo]) {
      return imageUrls[boardNo] || "";
    }

    try {
      const response = await getBoardImage(boardNo);
      imageUrls[boardNo] = URL.createObjectURL(response.data);
      return imageUrls[boardNo];
    } catch {
      return "";
    }
  };

  // 등록
  const add = async (data, image) => {
    saving.value = true;

    try {
      const response = await createBoard(data, image);
      board.value = response.data;
      return board.value;
    } finally {
      saving.value = false;
    }
  };

  // 수정
  const edit = async (boardNo, data, image) => {
    saving.value = true;

    try {
      const response = await updateBoard(boardNo, data, image);
      revokeImage(boardNo);
      board.value = response.data;
      return board.value;
    } finally {
      saving.value = false;
    }
  };

  // 삭제
  const remove = async (boardNo) => {
    await deleteBoard(boardNo);
    list.value = list.value.filter(
      (item) => Number(item.boardNo) !== Number(boardNo)
    );
    revokeImage(boardNo);
    if (Number(board.value?.boardNo) === Number(boardNo)) {
      board.value = null;
    }
  };

  const loadComments = async (boardNo) => {
    commentsLoading.value = true;
    try {
      const response = await getBoardComments(boardNo);
      comments.value = Array.isArray(response.data) ? response.data : [];
      return comments.value;
    } finally {
      commentsLoading.value = false;
    }
  };

  const addComment = async (boardNo, commentContent, parentCommentNo = null) => {
    await createBoardComment(boardNo, { commentContent, parentCommentNo });
    return loadComments(boardNo);
  };

  const editComment = async (boardNo, commentNo, commentContent) => {
    await updateBoardComment(boardNo, commentNo, { commentContent });
    return loadComments(boardNo);
  };

  const removeComment = async (boardNo, commentNo) => {
    await deleteBoardComment(boardNo, commentNo);
    return loadComments(boardNo);
  };

  const revokeImage = (boardNo) => {
    if (imageUrls[boardNo]) {
      URL.revokeObjectURL(imageUrls[boardNo]);
      delete imageUrls[boardNo];
    }
  };

  return {
    list,
    board,
    loading,
    saving,
    errorMessage,
    imageUrls,
    comments,
    commentsLoading,
    loadList,
    loadDetail,
    loadImage,
    add,
    edit,
    remove,
    loadComments,
    addComment,
    editComment,
    removeComment,
  };
});
