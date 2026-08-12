<template>
  <main class="admin-faq-page management-list-page">
    <header class="page-header">
      <h2>자주하는 질문 관리</h2>

      <div class="header-actions">
        <button type="button" class="secondary-button" @click="goInquiries">
          문의 관리
        </button>
        <button type="button" class="primary-button" @click="openCreateForm">
          + FAQ 등록
        </button>
      </div>
    </header>

    <!-- 자주하는 질문 등록 또는 수정 -->
    <form v-if="formOpen" class="faq-form" @submit.prevent="submit">
      <h3>{{ editingFaqNo === null ? "자주하는 질문 등록" : "자주하는 질문 수정" }}</h3>

      <label>
        분류
        <select v-model="form.category" required>
          <option value="">분류를 선택해 주세요</option>
          <option value="PARKING">주차</option>
          <option value="VISIT">방문</option>
          <option value="PAYMENT">결제</option>
          <option value="ETC">기타</option>
        </select>
      </label>

      <label>
        질문
        <input
          v-model="form.question"
          type="text"
          maxlength="200"
          placeholder="질문을 입력해 주세요"
          required
        />
      </label>

      <label>
        답변
        <textarea
          v-model="form.answer"
          rows="8"
          placeholder="답변을 입력해 주세요"
          required
        />
      </label>

      <div class="form-actions">
        <button type="button" class="secondary-button" @click="closeForm">
          취소
        </button>
        <button type="submit" class="primary-button" :disabled="store.saving">
          {{
            store.saving ? "저장 중" : editingFaqNo === null ? "등록" : "수정"
          }}
        </button>
      </div>
    </form>

    <p v-if="store.loading" class="page-state">
      자주하는 질문을 불러오는 중입니다.
    </p>

    <p v-else-if="store.errorMessage" class="page-state error">
      {{ store.errorMessage }}
    </p>

    <!-- 자주하는 질문 목록 -->
    <div v-else class="table-wrap management-list-table">
      <table>
        <thead>
          <tr>
            <th>번호</th>
            <th>분류</th>
            <th>질문</th>
            <th>답변</th>
            <th>등록일</th>
            <th>수정일</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="(faq, index) in store.faqs" :key="faq.faqNo">
            <td>{{ index + 1 }}</td>
            <td>
              <span class="category-badge">
                {{ categoryText(faq.category) }}
              </span>
            </td>
            <td class="question-cell">{{ faq.question }}</td>
            <td class="answer-cell">{{ faq.answer }}</td>
            <td>{{ dateText(faq.createdAt) }}</td>
            <td>{{ dateText(faq.updatedAt) }}</td>
            <td>
              <div class="row-actions">
                <button type="button" class="edit-button" @click="openEditForm(faq)">
                  수정
                </button>
                <button
                  type="button"
                  class="delete-button"
                  :disabled="store.saving"
                  @click="remove(faq)"
                >
                  삭제
                </button>
              </div>
            </td>
          </tr>

          <tr v-if="store.faqs.length === 0">
            <td colspan="7" class="empty-cell">
              등록된 자주하는 질문이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useDialog } from "@/shared/alert/useDialog";
import { useFaqStore } from "./faqStore";

const router = useRouter();
const store = useFaqStore();
const { alertDialog, confirmDialog } = useDialog();

const formOpen = ref(false);
const editingFaqNo = ref(null);

const form = reactive({
  category: "",
  question: "",
  answer: ""
});

// 관리자 알림창 표시
const showFaqDialog = (message, type = "info", title = "자주하는 질문 안내") =>
  alertDialog({
    theme: "admin",
    type,
    title,
    message
  });

// 입력값 초기화
const resetForm = () => {
  form.category = "";
  form.question = "";
  form.answer = "";
};

// 등록 폼 열기
const openCreateForm = () => {
  editingFaqNo.value = null;
  resetForm();
  formOpen.value = true;
};

// 수정 폼 열기
const openEditForm = (faq) => {
  editingFaqNo.value = faq.faqNo;
  form.category = faq.category;
  form.question = faq.question;
  form.answer = faq.answer;
  formOpen.value = true;
};

// 입력 폼 닫기
const closeForm = () => {
  formOpen.value = false;
  editingFaqNo.value = null;
  resetForm();
};

// 자주하는 질문 등록 또는 수정
const submit = async () => {
  const data = {
    category: form.category,
    question: form.question.trim(),
    answer: form.answer.trim()
  };

  try {
    if (editingFaqNo.value === null) {
      await store.addFaq(data);
      await showFaqDialog(
        "자주하는 질문을 등록했습니다.",
        "success",
        "자주하는 질문 등록 완료"
      );
    } else {
      await store.editFaq(editingFaqNo.value, data);
      await showFaqDialog(
        "자주하는 질문을 수정했습니다.",
        "success",
        "자주하는 질문 수정 완료"
      );
    }

    closeForm();
  } catch {
    await showFaqDialog(
      store.errorMessage,
      "error",
      "자주하는 질문 저장 실패"
    );
  }
};

// 자주하는 질문 즉시 삭제
const remove = async (faq) => {
  const confirmed = await confirmDialog({
    theme: "admin",
    type: "warning",
    title: "자주하는 질문 삭제",
    message: `'${faq.question}' 질문을 삭제하시겠습니까?`,
    caution: "삭제한 자주하는 질문은 복구할 수 없습니다.",
    confirmText: "삭제",
    cancelText: "취소"
  });

  if (!confirmed) return;

  try {
    await store.removeFaq(faq.faqNo);

    if (editingFaqNo.value === faq.faqNo) {
      closeForm();
    }

    await showFaqDialog(
      "자주하는 질문을 삭제했습니다.",
      "success",
      "자주하는 질문 삭제 완료"
    );
  } catch {
    await showFaqDialog(
      store.errorMessage,
      "error",
      "자주하는 질문 삭제 실패"
    );
  }
};

// FAQ 분류 한글 표시
const categoryText = (category) => ({
  PARKING: "주차",
  VISIT: "방문",
  PAYMENT: "결제",
  ETC: "기타"
})[category] ?? category ?? "-";

// 날짜 표시
const dateText = (value) => value
  ? new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false
    }).format(new Date(value))
  : "-";

// 문의 관리로 이동
const goInquiries = () => router.push("/admin/inquiries");

// 화면을 열 때 FAQ 목록 조회
onMounted(() => {
  store.loadFaqs().catch(async () => {
    await showFaqDialog(
      store.errorMessage,
      "error",
      "자주하는 질문 조회 실패"
    );
  });
});
</script>

<style scoped>
.admin-faq-page { padding: 24px; color: #22364a; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.page-header h2,.faq-form h3 { margin: 0; }
.header-actions,.row-actions,.form-actions { display: flex; gap: 8px; }

.primary-button,.secondary-button,.edit-button,.delete-button {
  min-height: 38px;
  padding: 8px 15px;
  border: 1px solid #c9d5df;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
  font-weight: 700;
}

.primary-button { border-color: #168bd2; color: #fff; background: #168bd2; }
.edit-button { color: #176fba; background: #e8f3fc; }
.delete-button { border-color: #efc7c7; color: #b43b3b; background: #fff1f1; }

.faq-form {
  display: grid;
  gap: 20px;
  margin-bottom: 24px;
  padding: 24px;
  border: 1px solid #d7e2eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(35,70,100,.08);
}

.faq-form label { display: grid; gap: 8px; font-weight: 800; }

.faq-form input,.faq-form select,.faq-form textarea {
  box-sizing: border-box;
  width: 100%;
  padding: 12px;
  border: 1px solid #cbd8e2;
  border-radius: 7px;
  font: inherit;
}

.faq-form textarea { resize: vertical; }
.form-actions { justify-content: flex-end; }
.table-wrap { width: 100%; overflow-x: auto; }
table { width: 100%; min-width: 1050px; border-collapse: collapse; background: #fff; }
th,td { padding: 11px 10px; border: 1px solid #dce5ec; text-align: center; }
th { background: #f2f6f9; }

.question-cell,.answer-cell {
  max-width: 280px;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  color: #176fba;
  background: #e8f3fc;
  font-size: 12px;
  font-weight: 800;
}

.empty-cell,.page-state { padding: 36px; text-align: center; }
.page-state.error { color: #c73d3d; }
button:disabled { cursor: not-allowed; opacity: .6; }

@media (max-width: 760px) {
  .admin-faq-page { padding: 14px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions { justify-content: flex-end; }
}
</style>