<template>
  <main class="board-admin-page management-list-page">
    <template v-if="mode === 'list'">
      <header class="board-page-header">
        <div>
          <h2>공지사항 관리</h2>
        </div>
        <button class="primary-button" type="button" @click="goCreate">
          + 공지사항 등록
        </button>
      </header>

      <p v-if="store.loading" class="board-state">공지사항을 불러오는 중입니다.</p>
      <p v-else-if="store.errorMessage" class="board-state error">{{ store.errorMessage }}</p>

      <div v-else class="board-table-wrap">
        <table class="board-table">
          <thead>
            <tr>
              <th>번호</th>
              <th>제목</th>
              <th>게시기간</th>
              <th>노출여부</th>
              <th>작성자</th>
              <th>작성일</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in paginatedBoards" :key="item.boardNo">
              <td>{{ item.listNo }}</td>
              <td>
                <a
                  class="title-button"
                  :href="`/admin/boards/${item.boardNo}/detail`"
                  @click.prevent="goDetail(item.boardNo)"
                >
                  {{ item.title }}
                </a>
              </td>
              <td>
                <span class="period-text">{{ periodText(item) }}</span>
                <b class="period-badge" :class="periodClass(item.periodStatus)">
                  {{ item.periodStatus }}
                </b>
              </td>
              <td>
                <span class="active-badge" :class="{ inactive: !item.active }">
                  {{ item.active ? "노출" : "숨김" }}
                </span>
              </td>
              <td>{{ item.createdBy || "-" }}</td>
              <td>{{ dateTimeText(item.createdAt) }}</td>
              <td class="action-cell">
                <button type="button" @click="goEdit(item.boardNo)">수정</button>
                <button class="danger-button" type="button" @click="remove(item)">삭제</button>
              </td>
            </tr>
            <tr v-if="store.list.length === 0">
              <td colspan="7" class="empty-cell">등록된 공지사항이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <Pagination
        v-if="store.list.length"
        :current-page="currentPage"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        @change-page="setPage"
      />
    </template>

    <template v-else-if="mode === 'detail'">
      <header class="board-page-header">
        <div>
          <h2>공지사항 상세</h2>
        </div>
        <div class="detail-header-actions">
          <button class="secondary-button" type="button" @click="goList">목록</button>
          <button
            v-if="store.board"
            class="primary-button"
            type="button"
            @click="goEdit(store.board.boardNo)"
          >
            수정
          </button>
          <button
            v-if="store.board"
            class="danger-button"
            type="button"
            @click="remove(store.board)"
          >
            삭제
          </button>
        </div>
      </header>

      <p v-if="store.loading" class="board-state">공지사항을 불러오는 중입니다.</p>
      <p v-else-if="store.errorMessage" class="board-state error">{{ store.errorMessage }}</p>

      <article v-else-if="store.board" class="board-detail-card">
        <header class="detail-title">
          <div>
            <span class="period-badge" :class="periodClass(store.board.periodStatus)">
              {{ store.board.periodStatus }}
            </span>
            <h3>{{ store.board.title }}</h3>
          </div>
          <span>{{ store.board.active ? "노출" : "숨김" }}</span>
        </header>

        <dl class="detail-meta">
          <div><dt>게시기간</dt><dd>{{ periodText(store.board) }}</dd></div>
          <div><dt>작성자</dt><dd>{{ store.board.createdBy || "-" }}</dd></div>
          <div><dt>작성일</dt><dd>{{ dateTimeText(store.board.createdAt) }}</dd></div>
          <div><dt>수정일</dt><dd>{{ dateTimeText(store.board.updatedAt) }}</dd></div>
        </dl>

        <div class="detail-content">
          <div class="detail-copy">{{ store.board.content }}</div>
          <div v-if="detailImageUrl" class="detail-image-wrap">
            <img :src="detailImageUrl" :alt="store.board.title" />
          </div>
        </div>
      </article>
    </template>

    <template v-else>
      <header class="board-page-header">
        <div>
          <h2>{{ mode === "create" ? "공지사항 등록" : "공지사항 수정" }}</h2>
        </div>
        <button class="secondary-button" type="button" @click="cancelForm">뒤로</button>
      </header>

      <form class="board-form" @submit.prevent="submit">
        <label class="wide-field">
          <span>제목</span>
          <input v-model.trim="form.title" type="text" maxlength="150" required />
        </label>

        <label class="wide-field">
          <span>내용</span>
          <textarea v-model.trim="form.content" rows="9" required></textarea>
        </label>

        <label>
          <span>게시 시작</span>
          <input v-model="form.startAt" type="datetime-local" required />
        </label>

        <label>
          <span>게시 종료</span>
          <input v-model="form.endAt" type="datetime-local" />
        </label>

        <label>
          <span>노출 여부</span>
          <select v-model="form.active">
            <option :value="true">노출</option>
            <option :value="false">숨김</option>
          </select>
        </label>

        <label class="wide-field">
          <span>공지 이미지</span>
          <input
            ref="imageInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            @change="selectImage"
          />
          <small>JPG, PNG, WEBP 형식의 5MB 이하 이미지를 등록할 수 있습니다.</small>
        </label>

        <div v-if="previewUrl" class="form-image-preview wide-field">
          <img :src="previewUrl" alt="공지 이미지 미리보기" />
        </div>

        <footer class="form-actions wide-field">
          <button class="secondary-button" type="button" @click="cancelForm">취소</button>
          <button class="primary-button" type="submit" :disabled="store.saving">
            {{ store.saving ? "저장 중..." : mode === "create" ? "등록" : "수정완료" }}
          </button>
        </footer>
      </form>
    </template>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import Pagination from "@/shared/pagination/Pagination.vue";
import { usePagination } from "@/shared/pagination/usePagination";
import { useDialog } from "@/shared/alert/useDialog";
import { useBoardStore } from "./boardStore";

const route = useRoute();
const router = useRouter();
const store = useBoardStore();
const { alertDialog, confirmDialog } = useDialog();
const { list } = storeToRefs(store);
const selectedImage = ref(null);
const localPreviewUrl = ref("");
const imageInput = ref(null);
const allowedImageTypes = ["image/jpeg", "image/png", "image/webp"];
const maxImageSize = 5 * 1024 * 1024;

const emptyForm = () => ({
  title: "",
  content: "",
  startAt: toInputDate(new Date()),
  endAt: "",
  active: true,
  removeImage: false,
});

const form = reactive(emptyForm());
const mode = computed(() => {
  if (route.name === "BoardCreate") return "create";
  if (route.name === "BoardEdit") return "edit";
  if (route.name === "BoardDetail") return "detail";
  return "list";
});
const boardNo = computed(() => Number(route.params.boardNo));
const detailImageUrl = computed(() =>
  Number(store.board?.boardNo) === boardNo.value
    ? store.imageUrls[store.board.boardNo] || ""
    : ""
);
const previewUrl = computed(() =>
  mode.value === "create"
    ? localPreviewUrl.value
    : localPreviewUrl.value || detailImageUrl.value
);

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems: paginatedBoards,
  setPage,
} = usePagination(list, 10);

function toInputDate(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

const dateTimeText = (value) => value
  ? new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(value))
  : "-";

const periodText = (item) =>
  `${dateTimeText(item.startAt)} ~ ${item.endAt ? dateTimeText(item.endAt) : "계속 게시"}`;

const periodClass = (status) => ({
  upcoming: status === "게시예정",
  expired: status === "기간만료",
});

const showBoardDialog = (message, type = "info", title = "공지사항 안내") =>
  alertDialog({
    theme: "admin",
    type,
    title,
    message,
  });

const resetForm = () => {
  Object.assign(form, emptyForm());
  selectedImage.value = null;
  if (imageInput.value) imageInput.value.value = "";
  revokePreview();
};

const fillForm = (item) => {
  Object.assign(form, {
    title: item.title || "",
    content: item.content || "",
    startAt: toInputDate(item.startAt),
    endAt: toInputDate(item.endAt),
    active: item.active !== false,
    removeImage: false,
  });
};

const loadDetail = async () => {
  if (mode.value === "edit") resetForm();

  const item = await store.loadDetail(boardNo.value);
  if (item?.hasImage) {
    await store.loadImage(item.boardNo);
  }
  if (mode.value === "edit") fillForm(item);
};

const loadPage = async () => {
  try {
    if (mode.value === "list") {
      await store.loadList();
    } else if (mode.value === "create") {
      resetForm();
    } else {
      await loadDetail();
    }
  } catch {
    await showBoardDialog(
      store.errorMessage,
      "error",
      "공지사항 조회 실패"
    );
  }
};

const selectImage = async (event) => {
  const file = event.target.files?.[0] || null;

  if (file && !allowedImageTypes.includes(file.type)) {
    event.target.value = "";
    selectedImage.value = null;
    form.removeImage = false;
    revokePreview();
    await showBoardDialog(
      "JPG, PNG, WEBP 이미지만 등록할 수 있습니다.",
      "warning",
      "이미지 형식 확인"
    );
    return;
  }

  if (file && file.size > maxImageSize) {
    event.target.value = "";
    selectedImage.value = null;
    form.removeImage = false;
    revokePreview();
    await showBoardDialog(
      "이미지는 5MB 이하만 등록할 수 있습니다.",
      "warning",
      "이미지 용량 확인"
    );
    return;
  }

  selectedImage.value = file;
  form.removeImage = false;
  revokePreview();
  if (file) localPreviewUrl.value = URL.createObjectURL(file);
};

const submit = async () => {
  const title = form.title.trim();
  const content = form.content.trim();

  if (!title || title.length > 150) {
    await showBoardDialog(
      "제목은 1~150자로 입력해 주세요.",
      "warning",
      "제목 확인"
    );
    return;
  }

  if (!content) {
    await showBoardDialog(
      "공지 내용을 입력해 주세요.",
      "warning",
      "내용 확인"
    );
    return;
  }

  if (!form.startAt) {
    await showBoardDialog(
      "게시 시작일을 입력해 주세요.",
      "warning",
      "게시기간 확인"
    );
    return;
  }

  if (form.endAt && new Date(form.endAt) < new Date(form.startAt)) {
    await showBoardDialog(
      "게시 종료일은 시작일보다 빠를 수 없습니다.",
      "warning",
      "게시기간 확인"
    );
    return;
  }

  const data = {
    title,
    content,
    startAt: form.startAt,
    endAt: form.endAt || null,
    active: form.active,
    removeImage: form.removeImage,
  };

  try {
    const saved = mode.value === "create"
      ? await store.add(data, selectedImage.value)
      : await store.edit(boardNo.value, data, selectedImage.value);

    await showBoardDialog(
      mode.value === "create"
        ? "공지사항을 등록했습니다."
        : "공지사항을 수정했습니다.",
      "success",
      mode.value === "create" ? "공지사항 등록 완료" : "공지사항 수정 완료"
    );
    router.push(`/admin/boards/${saved.boardNo}/detail`);
  } catch (error) {
    await showBoardDialog(
      error?.response?.data?.message || "공지사항을 저장하지 못했습니다.",
      "error",
      "공지사항 저장 실패"
    );
  }
};

const remove = async (item) => {
  const confirmed = await confirmDialog({
    theme: "admin",
    type: "warning",
    title: "공지사항 삭제",
    message: `'${item.title}' 공지사항을 삭제하시겠습니까?`,
    caution: "삭제한 공지사항과 이미지는 복구할 수 없습니다.",
    confirmText: "삭제",
    cancelText: "취소",
  });

  if (!confirmed) return;

  try {
    await store.remove(item.boardNo);
    await store.loadList();
    await showBoardDialog(
      "공지사항을 삭제했습니다.",
      "success",
      "공지사항 삭제 완료"
    );
    router.push("/admin/boards");
  } catch (error) {
    await showBoardDialog(
      error?.response?.data?.message || "공지사항을 삭제하지 못했습니다.",
      "error",
      "공지사항 삭제 실패"
    );
  }
};

const goList = () => router.push("/admin/boards");
const goCreate = () => router.push("/admin/boards/signUp");
const goDetail = (no) => router.push(`/admin/boards/${no}/detail`);
const goEdit = (no) => router.push(`/admin/boards/${no}/edit`);
const cancelForm = () => mode.value === "edit" ? goDetail(boardNo.value) : goList();

const revokePreview = () => {
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value);
    localPreviewUrl.value = "";
  }
};

watch(() => route.fullPath, loadPage, { immediate: true });

onBeforeUnmount(() => {
  revokePreview();
});
</script>

<style scoped>
.board-admin-page { padding: 24px; color: var(--text-main, #22364a); }
.board-page-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.board-page-header h2 { margin: 0 0 6px; font-size: 28px; }
.board-page-header p { margin: 0; color: #7c8d9f; }
.detail-header-actions { display: flex; align-items: center; gap: 8px; }
.primary-button,.secondary-button,.danger-button,.action-cell button { min-height: 38px; padding: 8px 15px; border: 1px solid transparent; border-radius: 7px; cursor: pointer; font-weight: 700; }
.primary-button { color: #fff; background: #168bd2; }
.secondary-button,.action-cell button { color: #28465f; border-color: #c9d5df; background: #fff; }
.danger-button,.action-cell .danger-button { color: #fff; background: #dc4c4c; }
button:disabled { cursor: not-allowed; opacity: .6; }
.board-state { padding: 30px; text-align: center; border: 1px solid #dce5ec; background: #fff; }
.board-state.error { color: #c73d3d; }
.board-table-wrap { overflow-x: auto; border: 1px solid #d8e2ea; border-radius: 10px; background: #fff; }
.board-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.board-table th,.board-table td { padding: 13px 11px; border-bottom: 1px solid #e1e8ee; text-align: center; vertical-align: middle; }
.board-table th { color: #29465f; background: #eef4f8; font-size: 13px; }
.board-table th:nth-child(1) { width: 6%; }
.board-table th:nth-child(2) { width: 24%; }
.board-table th:nth-child(3) { width: 26%; }
.board-table th:nth-child(4) { width: 8%; }
.board-table th:nth-child(5) { width: 9%; }
.board-table th:nth-child(6) { width: 13%; }
.board-table th:nth-child(7) { width: 14%; }
.title-button {
  cursor: pointer;
  color: #176fba;
  font-weight: 800;
  text-decoration: none;
}
.period-text { display: block; margin-bottom: 5px; font-size: 12px; }
.period-badge,.active-badge { display: inline-flex; padding: 4px 9px; border-radius: 999px; color: #14783d; background: #e4f7eb; font-size: 11px; }
.period-badge.upcoming { color: #8b6600; background: #fff4ce; }
.period-badge.expired,.active-badge.inactive { color: #6c7782; background: #edf0f2; }
.action-cell { white-space: nowrap; }
.action-cell button { min-width: 44px; min-height: 30px; padding: 5px 7px; font-size: 12px; }
.action-cell button + button { margin-left: 4px; }
.empty-cell { padding: 36px !important; color: #8190a0; }
.board-detail-card,.board-form { padding: 24px; border: 1px solid #d7e2eb; border-radius: 12px; background: #fff; box-shadow: 0 8px 24px rgba(35,70,100,.08); }
.detail-image-wrap,.form-image-preview { overflow: hidden; border-radius: 10px; background: #eff4f7; }
.detail-image-wrap { margin-top: 22px; }
.form-image-preview { margin-bottom: 20px; }
.detail-image-wrap img,.form-image-preview img { display: block; width: 100%; max-height: 420px; object-fit: contain; }
.detail-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; padding-bottom: 18px; border-bottom: 1px solid #dfe6ec; }
.detail-title h3 { margin: 10px 0 0; color: #111 !important; font-size: 25px; }
.detail-meta { display: grid; grid-template-columns: repeat(2, 1fr); margin: 20px 0; border: 1px solid #dde6ed; }
.detail-meta div { display: grid; grid-template-columns: 100px 1fr; padding: 12px; border-bottom: 1px solid #e5ebf0; }
.detail-meta dt { color: #77899a; font-weight: 700; }
.detail-meta dd { margin: 0; }
.detail-content { min-height: 180px; padding: 20px; line-height: 1.8; background: #f8fafc; }
.detail-copy { white-space: pre-wrap; }
.form-actions { display: flex; justify-content: center; gap: 10px; margin-top: 24px; }
.board-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.board-form label { display: grid; gap: 8px; font-weight: 700; }
.board-form input,.board-form textarea,.board-form select { box-sizing: border-box; width: 100%; padding: 11px 12px; border: 1px solid #cbd8e2; border-radius: 7px; color: inherit; background: #fff; font: inherit; }
.board-form textarea { resize: vertical; }
.board-form small { color: #7f8d9a; font-weight: 400; }
.wide-field { grid-column: 1 / -1; }
@media (max-width: 760px) {
  .board-admin-page { padding: 14px; }
  .board-page-header { align-items: stretch; flex-direction: column; }
  .board-form,.detail-meta { grid-template-columns: 1fr; }
  .wide-field { grid-column: auto; }
}
</style>
