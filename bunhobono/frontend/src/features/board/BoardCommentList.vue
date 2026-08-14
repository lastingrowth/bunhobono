<template>
  <section class="comment-section" :class="{ 'admin-comments': adminMode }">
    <h3>댓글 <span>{{ totalCount }}</span></h3>

    <form class="write-form" @submit.prevent="submitRoot">
      <div class="writer-name">{{ writerName }}</div>
      <textarea v-model="rootContent" maxlength="1000" rows="3" placeholder="댓글을 입력해 주세요."></textarea>
      <div class="write-footer">
        <span>{{ rootContent.length }}/1000</span>
        <button type="submit" :disabled="saving">등록</button>
      </div>
    </form>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="store.commentsLoading" class="state">댓글을 불러오는 중입니다.</p>
    <p v-else-if="!store.comments.length" class="state">첫 댓글을 작성해 보세요.</p>

    <div v-else>
      <article v-for="comment in store.comments" :key="comment.commentNo" class="comment">
        <CommentContent :comment="comment" />

        <div v-for="reply in comment.replies" :key="reply.commentNo" class="comment depth-2">
          <CommentContent :comment="reply" />

          <div v-for="nested in reply.replies" :key="nested.commentNo" class="comment depth-3">
            <CommentContent :comment="nested" :can-reply="false" />
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref, watch } from "vue";
import { useDialog } from "@/shared/alert/useDialog";
import { getCommentWriterName } from "./boardApi";
import { useBoardStore } from "./boardStore";

const props = defineProps({
  boardNo: { type: Number, required: true },
  adminMode: { type: Boolean, default: false },
});
const store = useBoardStore();
const { confirmDialog } = useDialog();
const rootContent = ref("");
const replyContent = ref("");
const editContent = ref("");
const replyingNo = ref(null);
const editingNo = ref(null);
const saving = ref(false);
const errorMessage = ref("");
const writerName = ref("");
const openWriterInfoNo = ref(null);

const dateText = (value) => value
  ? new Intl.DateTimeFormat("ko-KR", {
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit", hour12: false,
    }).format(new Date(value))
  : "-";

const isEdited = (comment) => {
  if (!comment.commentCreatedAt || !comment.commentUpdatedAt) return false;
  return new Date(comment.commentUpdatedAt).getTime()
    > new Date(comment.commentCreatedAt).getTime();
};

const writerRoleText = (comment) =>
  comment.commentWriterRole === "ADMIN" ? "관리자" : "입주민";

const writerUnitText = (comment) => {
  if (comment.commentWriterRole === "ADMIN") return "관리실";
  if (comment.commentWriterDong == null || comment.commentWriterHo == null) return "동·호수 미등록";
  return `${comment.commentWriterDong}동 ${comment.commentWriterHo}호`;
};

const countComments = (comments) => comments.reduce(
  (count, comment) => count + 1 + countComments(comment.replies || []), 0
);
const totalCount = computed(() => countComments(store.comments));

const run = async (action) => {
  errorMessage.value = "";
  saving.value = true;
  try {
    await action();
  } catch (error) {
    const statusMessage = {
      400: "댓글 내용을 확인해 주세요.",
      403: "댓글을 변경하거나 삭제할 권한이 없습니다.",
      404: "댓글 정보를 찾을 수 없습니다.",
    }[error?.response?.status];

    errorMessage.value = error?.response?.data?.message
      || error?.response?.data?.detail
      || statusMessage
      || error?.message
      || "댓글 요청을 처리하지 못했습니다.";
  } finally {
    saving.value = false;
  }
};

const cancelReply = () => {
  replyingNo.value = null;
  replyContent.value = "";
};
const cancelEdit = () => {
  editingNo.value = null;
  editContent.value = "";
};
const openReply = (commentNo) => {
  cancelEdit();
  replyingNo.value = commentNo;
  replyContent.value = "";
};
const openEdit = (comment) => {
  cancelReply();
  editingNo.value = comment.commentNo;
  editContent.value = comment.commentContent;
};

const submitRoot = () => run(async () => {
  if (!rootContent.value.trim()) throw new Error("댓글 내용을 입력해 주세요.");
  await store.addComment(props.boardNo, rootContent.value.trim());
  rootContent.value = "";
});

const submitReply = (parentCommentNo) => run(async () => {
  if (!replyContent.value.trim()) throw new Error("답글 내용을 입력해 주세요.");
  await store.addComment(props.boardNo, replyContent.value.trim(), parentCommentNo);
  cancelReply();
});

const saveEdit = (commentNo) => run(async () => {
  if (!editContent.value.trim()) throw new Error("댓글 내용을 입력해 주세요.");
  await store.editComment(props.boardNo, commentNo, editContent.value.trim());
  cancelEdit();
});

const removeComment = (commentNo) => run(async () => {
  const confirmed = await confirmDialog({
    type: "warning",
    title: "댓글 삭제",
    message: "댓글을 삭제하시겠습니까?",
    caution: "이 댓글에 작성된 모든 답글도 함께 삭제되며 복구할 수 없습니다.",
    confirmText: "삭제",
    cancelText: "취소",
  });

  if (!confirmed) return;
  await store.removeComment(props.boardNo, commentNo);
});

const CommentContent = defineComponent({
  props: {
    comment: { type: Object, required: true },
    canReply: { type: Boolean, default: true },
  },
  setup(componentProps) {
    return () => {
      const comment = componentProps.comment;
      const editing = editingNo.value === comment.commentNo;
      const replying = replyingNo.value === comment.commentNo;

      return h("div", { class: "comment-content" }, [
        h("div", { class: "comment-meta" }, [
          h("strong", comment.commentWriterName),
          componentProps.comment && props.adminMode ? h("button", {
            type: "button",
            class: "writer-toggle",
            onClick: () => {
              openWriterInfoNo.value = openWriterInfoNo.value === comment.commentNo
                ? null
                : comment.commentNo;
            },
          }) : null,
          h("span", dateText(comment.commentCreatedAt)),
          isEdited(comment)
            ? h("span", { class: "edited" }, `수정됨 ${dateText(comment.commentUpdatedAt)}`)
            : null,
        ]),
        props.adminMode && openWriterInfoNo.value === comment.commentNo
          ? h("div", { class: "writer-info" }, [
              h("span", `회원 구분: ${writerRoleText(comment)}`),
              h("span", `거주 정보: ${writerUnitText(comment)}`),
            ])
          : null,
        editing
          ? h("textarea", {
              value: editContent.value,
              maxlength: 1000,
              rows: 3,
              onInput: (event) => { editContent.value = event.target.value; },
            })
          : h("p", comment.commentContent),
        h("div", { class: "actions" }, [
          componentProps.canReply && !editing
            ? h("button", { type: "button", onClick: () => openReply(comment.commentNo) }, "답글") : null,
          comment.myComment && !editing
            ? h("button", { type: "button", onClick: () => openEdit(comment) }, "수정") : null,
          (comment.myComment || props.adminMode) && !editing
            ? h("button", { type: "button", class: "delete", onClick: () => removeComment(comment.commentNo) }, "삭제") : null,
          editing
            ? h("button", { type: "button", onClick: () => saveEdit(comment.commentNo) }, "저장") : null,
          editing
            ? h("button", { type: "button", onClick: cancelEdit }, "취소") : null,
        ]),
        replying
          ? h("form", {
              class: "reply-form",
              onSubmit: (event) => { event.preventDefault(); submitReply(comment.commentNo); },
            }, [
              h("textarea", {
                value: replyContent.value,
                maxlength: 1000,
                rows: 2,
                placeholder: "답글을 입력해 주세요.",
                onInput: (event) => { replyContent.value = event.target.value; },
              }),
              h("div", [
                h("button", { type: "submit", disabled: saving.value }, "등록"),
                h("button", { type: "button", onClick: cancelReply }, "취소"),
              ]),
            ])
          : null,
      ]);
    };
  },
});

const loadComments = () => store.loadComments(props.boardNo).catch((error) => {
  errorMessage.value = error?.response?.data?.message || "댓글을 불러오지 못했습니다.";
});

const loadWriterName = async () => {
  const response = await getCommentWriterName();
  writerName.value = response.data || "";
};

onMounted(() => {
  loadComments();
  loadWriterName().catch(() => { writerName.value = ""; });
});
watch(() => props.boardNo, () => {
  cancelReply();
  cancelEdit();
  loadComments();
});
</script>

<style scoped>
.comment-section { margin-top: 28px; padding: 22px; color: #f5f5f5; border: 1px solid #4c5258; border-radius: 12px; background: #17191c; }
.comment-section.admin-comments { padding: 0; border: 0; border-radius: 0; background: transparent; }
h3 { margin: 0 0 16px; color: #fff; font-size: 20px; }
h3 span { color: #c8cdd2; }
.write-form,.reply-form { display: grid; gap: 8px; }
.comment-section .write-form { position: static; box-sizing: border-box; width: 100%; max-width: none; margin-bottom: 40px; padding: 14px 16px 10px; gap: 5px; overflow: hidden; border: 1px solid #575d63; border-radius: 6px; background: #292d32; box-shadow: none; }
.comment-section.admin-comments .write-form { border: 1px solid #575d63 !important; outline: 0 !important; background: #292d32 !important; box-shadow: none !important; }
.comment-section .write-form > .writer-name { margin: 0; padding: 0; color: #fff; border: 0; font-size: 17px; font-weight: 700; background: transparent; }
textarea { box-sizing: border-box; width: 100%; resize: vertical; padding: 11px; color: #f5f5f5; border: 1px solid #575d63; border-radius: 7px; outline: none; background: #25282c; }
.comment-section .write-form > textarea { min-height: 38px; height: 38px; margin: 0; padding: 6px 0; resize: none; border: 0; border-radius: 0; font-size: 17px; line-height: 1.5; background: transparent; box-shadow: none; }
.comment-section .write-form > textarea:focus,
.comment-section .write-form > textarea:focus-visible { border: 0 !important; outline: 0 !important; box-shadow: none !important; }
textarea::placeholder { color: #9ca2a8; }
textarea:focus { border-color: #aeb4ba; }
button { width: fit-content; padding: 7px 13px; color: #e8eaec; border: 1px solid #666c72; border-radius: 6px; cursor: pointer; background: #292c30; }
button:hover { color: #fff; border-color: #aeb4ba; background: #383c41; }
button:disabled { cursor: default; opacity: .55; }
.write-footer { display: flex; align-items: center; justify-content: flex-end; gap: 12px; color: #9ca2a8; font-size: 12px; }
.write-footer > button { min-height: 38px; margin: 0; padding: 7px 13px; color: #17191c; border-color: #e3e5e7; font-size: 16px; font-weight: 700; background: #e3e5e7; }
.write-footer > button:hover { background: #fff; }
.state { padding: 24px; text-align: center; color: #a7adb3; }
.error { color: #ff9b9b; }
.comment-content :deep(.delete) { color: #111; }
.comment { margin-top: 14px; }
.comment-content { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; column-gap: 16px; padding: 15px; border: 1px solid #484d52; border-radius: 9px; background: #222529; }
.depth-2,.depth-3 { margin-left: 28px; }
.depth-2 > .comment-content { background: #292c30; }
.depth-3 > .comment-content { background: #303338; }
.comment-content :deep(.comment-meta) { grid-column: 1 / -1; display: flex; align-items: center; gap: 18px; color: #aab0b6; font-size: 15px; }
.comment-content :deep(.comment-meta strong) { color: #fff; font-size: 17px; }
.comment-content :deep(.comment-meta .edited) { color: #c9ced3; }
.comment-content :deep(.comment-meta strong + .writer-toggle) { margin-left: -12px; }
.comment-content :deep(.writer-toggle) { position: relative; width: 16px !important; min-width: 16px !important; height: 16px !important; min-height: 0 !important; padding: 0 !important; border: 0 !important; border-radius: 0 !important; outline: 0 !important; background: transparent !important; box-shadow: none !important; appearance: none; }
.comment-content :deep(.writer-toggle::before) { position: absolute; top: 4px; left: 1px; width: 0; height: 0; content: ""; border-right: 7px solid transparent; border-left: 7px solid transparent; border-top: 8px solid #b9bec4; }
.comment-content :deep(.writer-toggle:hover::before) { border-top-color: #fff; }
.comment-content :deep(.comment-meta:has(+ .writer-info) .writer-toggle::before) { top: 3px; border-top: 0; border-bottom: 8px solid #b9bec4; }
.comment-content :deep(.writer-toggle:hover),
.comment-content :deep(.writer-toggle:focus),
.comment-content :deep(.writer-toggle:focus-visible),
.comment-content :deep(.writer-toggle:active) { border: 0 !important; outline: 0 !important; background: transparent !important; box-shadow: none !important; }
.comment-content :deep(.writer-info) { grid-column: 1 / -1; display: flex; gap: 24px; margin-top: 10px; padding: 10px 12px; color: #c8cdd2; border: 1px solid #484d52; border-radius: 6px; font-size: 13px; background: #191b1e; }
.comment-content :deep(p) { grid-column: 1; min-width: 0; margin: 12px 0 0; overflow-wrap: anywhere; white-space: pre-wrap; font-size: 17px; line-height: 1.65; }
.comment-content :deep(.actions),.comment-content :deep(.reply-form > div) { display: flex; gap: 6px; }
.comment-content :deep(.actions) { grid-column: 2; align-self: end; justify-content: flex-end; white-space: nowrap; }
.comment-content :deep(.reply-form) { grid-column: 1 / -1; margin: 9px 0 0 28px; }

/* 입주민 화면은 밝은 댓글 테마를 사용한다. */
.comment-section:not(.admin-comments) { color: #263746; border-color: #dce6ee; background: #fff; }
.comment-section:not(.admin-comments) h3 { color: #263746; }
.comment-section:not(.admin-comments) h3 span { color: #168bd2; }
.comment-section:not(.admin-comments) .write-form { border: 1px solid #c5d0da; background: #eef3f6; }
.comment-section:not(.admin-comments) .write-form > .writer-name,
.comment-section:not(.admin-comments) .write-form > textarea { color: #263746; background: transparent; }
.comment-section:not(.admin-comments) textarea { color: #263746; border-color: #ccd9e3; background: #fff; }
.comment-section:not(.admin-comments) textarea::placeholder { color: #8293a3; }
.comment-section:not(.admin-comments) button { color: #263746; border-color: #cbd8e2; background: #fff; }
.comment-section:not(.admin-comments) button:hover { border-color: #8295a5; background: #f4f7f9; }
.comment-section:not(.admin-comments) .write-form > button { color: #fff; border-color: #263746; background: #263746; }
.comment-section:not(.admin-comments) .comment-content,
.comment-section:not(.admin-comments) .depth-2 > .comment-content,
.comment-section:not(.admin-comments) .depth-3 > .comment-content { color: #263746; border-color: #dfe7ed; background: #fbfcfd; }
.comment-section:not(.admin-comments) .comment-content :deep(.comment-meta) { color: #708295; }
.comment-section:not(.admin-comments) .comment-content :deep(.comment-meta strong) { color: #263746; }
.comment-section:not(.admin-comments) .comment-content :deep(.comment-meta .edited) { color: #557188; }
.comment-section:not(.admin-comments) .comment-content :deep(.delete) { color: #111; }
@media (max-width: 600px) {
  .comment-section { min-width: 0; padding: 14px 0; }
  .write-form { padding: 14px; }
  .write-form textarea { min-height: 76px; font-size: 16px; }
  .write-footer { align-items: center; }
  .write-footer button { min-height: 44px; }
  .comment { padding: 14px 12px; }
  .depth-2,.depth-3,.reply-form { margin-left: 10px; }
  .comment-header { align-items: flex-start; flex-wrap: wrap; gap: 6px; }
  .comment-actions { width: 100%; justify-content: flex-end; flex-wrap: wrap; }
  .comment-actions button { min-height: 40px; }
}
</style>
