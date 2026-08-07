<template>
  <main class="admin-inquiry-page management-list-page">
    <template v-if="mode === 'list'">
      <header class="page-header">
        <h2>1:1 문의 관리</h2>

        <div class="header-actions">
          <div class="status-tabs">
            <button
              v-for="option in statusOptions"
              :key="option.value"
              type="button"
              :class="{ active: selectedStatus === option.value }"
              @click="changeStatus(option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <button type="button" class="secondary-button" @click="goFaqs">
            FAQ 관리
          </button>
        </div>
      </header>

      <p v-if="store.loading" class="page-state">문의사항을 불러오는 중입니다.</p>
      <p v-else-if="store.errorMessage" class="page-state error">
        {{ store.errorMessage }}
      </p>

      <template v-else>
        <div class="table-wrap management-list-table">
          <table>
            <thead>
              <tr>
                <th>번호</th>
                <th>분류</th>
                <th>제목</th>
                <th>작성 회원</th>
                <th>상태</th>
                <th>작성일</th>
                <th>답변일</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(item, index) in paginatedInquiries"
                :key="item.inquiryNo"
                class="clickable-row"
                @click="goDetail(item.inquiryNo)"
              >
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td>{{ categoryText(item.category) }}</td>
                <td class="title-cell">
                  <span v-if="item.rootInquiryNo !== null">RE: </span>{{ item.title }}
                </td>
                <td>{{ item.memberNo }}</td>
                <td>
                  <span class="status" :class="statusClass(item.status)">
                    {{ statusText(item.status) }}
                  </span>
                </td>
                <td>{{ dateText(item.createdAt) }}</td>
                <td>{{ dateText(item.answeredAt) }}</td>
              </tr>
              <tr v-if="store.adminInquiries.length === 0">
                <td colspan="7" class="empty-cell">
                  {{ selectedStatus === "WAITING"
                    ? "답변을 기다리는 문의가 없습니다."
                    : "답변 완료된 문의가 없습니다." }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <Pagination
          v-if="store.adminInquiries.length"
          :current-page="currentPage"
          :total-pages="totalPages"
          :page-numbers="pageNumbers"
          @change-page="setPage"
        />
      </template>
    </template>

    <template v-else>
      <header class="page-header">
        <h2>1:1 문의 상세</h2>
        <button type="button" class="secondary-button" @click="goList">목록</button>
      </header>

      <p v-if="store.loading" class="page-state">문의사항을 불러오는 중입니다.</p>
      <article v-else-if="store.inquiry" class="detail-card">
        <div class="detail-heading">
          <div>
            <span class="category-badge">{{ categoryText(store.inquiry.category) }}</span>
            <h3>
              <span v-if="store.inquiry.rootInquiryNo !== null">RE: </span>{{ store.inquiry.title }}
            </h3>
          </div>
          <span class="status" :class="statusClass(store.inquiry.status)">
            {{ statusText(store.inquiry.status) }}
          </span>
        </div>

        <dl class="detail-meta">
          <div><dt>문의 번호</dt><dd>{{ store.inquiry.inquiryNo }}</dd></div>
          <div><dt>작성 회원</dt><dd>{{ store.inquiry.memberNo }}</dd></div>
          <div><dt>작성일</dt><dd>{{ dateText(store.inquiry.createdAt) }}</dd></div>
          <div><dt>답변일</dt><dd>{{ dateText(store.inquiry.answeredAt) }}</dd></div>
        </dl>

        <section class="content-box">
          <h4>문의 내용</h4>
          <p>{{ store.inquiry.content }}</p>
        </section>

        <section v-if="store.inquiry.status === 'ANSWERED'" class="answer-box">
          <h4>답변 내용</h4>
          <p>{{ store.inquiry.answerContent }}</p>
        </section>

        <form v-else class="answer-form" @submit.prevent="submitAnswer">
          <label for="answer-content">답변 내용</label>
          <textarea
            id="answer-content"
            v-model="answerContent"
            rows="8"
            placeholder="답변 내용을 입력해 주세요"
          />
          <p v-if="store.errorMessage" class="form-error">
            {{ store.errorMessage }}
          </p>
          <button type="submit" :disabled="store.saving">
            {{ store.saving ? "등록 중" : "답변 등록" }}
          </button>
        </form>

        <p v-if="successMessage" class="success-message">
          {{ successMessage }}
        </p>
      </article>

      <p v-else-if="store.errorMessage" class="page-state error">
        {{ store.errorMessage }}
      </p>
    </template>
  </main>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";
import Pagination from "@/shared/pagination/Pagination.vue";
import { usePagination } from "@/shared/pagination/usePagination";
import { useInquiryStore } from "./inquiryStore";

const route = useRoute();
const router = useRouter();
const store = useInquiryStore();
const { adminInquiries } = storeToRefs(store);
const answerContent = ref("");
const successMessage = ref("");
const pageSize = 10;

const statusOptions = [
  { value: "WAITING", label: "답변 대기" },
  { value: "ANSWERED", label: "답변 완료" },
];

const mode = computed(() =>
  route.name === "AdminInquiryDetail" ? "detail" : "list"
);
const inquiryNo = computed(() => Number(route.params.inquiryNo));
const selectedStatus = computed(() =>
  route.query.status === "ANSWERED" ? "ANSWERED" : "WAITING"
);

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems: paginatedInquiries,
  setPage,
} = usePagination(adminInquiries, pageSize);

const categoryText = (category) => ({
  PARKING: "주차",
  VISIT: "방문",
  PAYMENT: "결제",
  ETC: "기타",
})[category] ?? category ?? "-";

const statusText = (status) =>
  status === "ANSWERED" ? "답변 완료" : "답변 대기";

const statusClass = (status) =>
  status === "ANSWERED" ? "answered" : "waiting";

const dateText = (value) => value
  ? new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(value))
  : "-";

const loadPage = async () => {
  successMessage.value = "";
  answerContent.value = "";

  if (mode.value === "detail") {
    await store.loadAdminInquiry(inquiryNo.value);
    return;
  }

  await store.loadAdminInquiries(selectedStatus.value);
};

const changeStatus = (status) => {
  router.push({
    path: "/admin/inquiries",
    query: status === "ANSWERED" ? { status } : {},
  });
};

const goDetail = (number) => router.push({
  path: `/admin/inquiries/${number}/detail`,
  query: { status: selectedStatus.value },
});

// FAQ 관리로 이동
const goFaqs = () => router.push("/admin/inquiries/faqs");

const goList = () => router.push({
  path: "/admin/inquiries",
  query: route.query.status === "ANSWERED" ? { status: "ANSWERED" } : {},
});

const submitAnswer = async () => {
  const content = answerContent.value.trim();

  if (!content) {
    store.errorMessage = "답변 내용을 입력해 주세요.";
    return;
  }

  await store.submitAnswer(inquiryNo.value, content);
  successMessage.value = "답변을 등록했습니다.";
};

watch(
  () => route.fullPath,
  () => loadPage().catch(() => {}),
  { immediate: true }
);
</script>

<style scoped>
.admin-inquiry-page { padding: 24px; color: #22364a; }
.page-header,.detail-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.page-header h2,.detail-heading h3 { margin: 0; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.status-tabs { display: flex; gap: 8px; }
.status-tabs button,.secondary-button,.answer-form button { min-height: 38px; padding: 8px 15px; border: 1px solid #c9d5df; border-radius: 7px; background: #fff; cursor: pointer; font-weight: 700; }
.status-tabs button.active,.answer-form button { border-color: #168bd2; color: #fff; background: #168bd2; }
.table-wrap { width: 100%; overflow-x: auto; }
table { width: 100%; min-width: 850px; border-collapse: collapse; background: #fff; }
th,td { padding: 11px 10px; border: 1px solid #dce5ec; text-align: center; }
th { background: #f2f6f9; }
.clickable-row { cursor: pointer; }
.clickable-row:hover { background: #f4f9fd; }
.title-cell { text-align: left; }
.status,.category-badge { display: inline-flex; padding: 4px 9px; border-radius: 999px; font-size: 12px; font-weight: 800; }
.status.waiting { color: #9a5b0a; background: #fff1d6; }
.status.answered { color: #157344; background: #e4f7eb; }
.category-badge { margin-bottom: 9px; color: #176fba; background: #e8f3fc; }
.empty-cell,.page-state { padding: 36px; text-align: center; }
.page-state.error { color: #c73d3d; }
.detail-card { padding: 24px; border: 1px solid #d7e2eb; border-radius: 12px; background: #fff; box-shadow: 0 8px 24px rgba(35,70,100,.08); }
.detail-meta { display: grid; grid-template-columns: repeat(2,1fr); margin: 20px 0; border: 1px solid #dde6ed; }
.detail-meta div { display: grid; grid-template-columns: 100px 1fr; padding: 12px; border-bottom: 1px solid #e5ebf0; }
.detail-meta dt { color: #77899a; font-weight: 700; }
.detail-meta dd { margin: 0; }
.content-box,.answer-box { min-height: 130px; margin-top: 16px; padding: 20px; background: #f8fafc; }
.answer-box { border-left: 5px solid #168bd2; }
.content-box p,.answer-box p { white-space: pre-wrap; }
.answer-form { display: grid; gap: 10px; margin-top: 18px; }
.answer-form label { font-weight: 800; }
.answer-form textarea { box-sizing: border-box; width: 100%; padding: 12px; border: 1px solid #cbd8e2; border-radius: 7px; resize: vertical; font: inherit; }
.answer-form button { justify-self: end; }
.form-error { margin: 0; color: #c73d3d; }
.success-message { color: #157344; }
button:disabled { cursor: not-allowed; opacity: .6; }
@media (max-width: 760px) {
  .admin-inquiry-page { padding: 14px; }
  .page-header,.detail-heading { align-items: stretch; flex-direction: column; }
  .detail-meta { grid-template-columns: 1fr; }
}
</style>
