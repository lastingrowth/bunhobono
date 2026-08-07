<template>
  <main class="resident-inquiry-page">
    <header class="page-header">
      <h2>{{ pageTitle }}</h2>

      <div class="header-actions">
        <button
          v-if="mode === 'list'"
          type="button"
          @click="goWrite"
        >
          문의 작성
        </button>

        <button
          v-else
          type="button"
          class="secondary"
          @click="goList"
        >
          문의 목록
        </button>
      </div>
    </header>

    <p v-if="store.loading" class="page-state">
      문의사항을 불러오는 중입니다.
    </p>

    <p
      v-else-if="store.errorMessage"
      class="page-state error"
    >
      {{ store.errorMessage }}
    </p>

    <template v-if="!store.loading">
    <!-- 입주민 문의 목록 -->
    <template v-if="mode === 'list'">
      <section
        v-if="store.residentInquiries.length"
        class="inquiry-list"
      >
        <button
          v-for="item in store.residentInquiries"
          :key="item.inquiryNo"
          type="button"
          class="inquiry-row"
          @click="goDetail(item.inquiryNo)"
        >
          <span class="category">
            {{ categoryText(item.category) }}
          </span>

          <span class="title">
            <span v-if="item.rootInquiryNo !== null">
              RE:
            </span>
            {{ item.title }}
          </span>

          <span
            class="status"
            :class="item.status.toLowerCase()"
          >
            {{ statusText(item.status) }}
          </span>

          <time>{{ dateText(item.createdAt) }}</time>
        </button>
      </section>

      <p v-else-if="!store.errorMessage" class="page-state">
        등록한 문의사항이 없습니다.
      </p>
    </template>

    <!-- 일반 문의 작성 -->
    <form
      v-else-if="mode === 'write'"
      class="inquiry-form"
      @submit.prevent="submitInquiry"
    >
      <label>
        문의 분류

        <select v-model="form.category">
          <option value="">선택해 주세요</option>
          <option value="PARKING">주차</option>
          <option value="VISIT">방문</option>
          <option value="PAYMENT">결제</option>
          <option value="ETC">기타</option>
        </select>
      </label>

      <label>
        제목

        <input
          v-model="form.title"
          type="text"
          maxlength="200"
          placeholder="제목을 입력해 주세요"
        />
      </label>

      <label>
        문의 내용

        <textarea
          v-model="form.content"
          rows="10"
          placeholder="문의 내용을 입력해 주세요"
        />
      </label>

      <button
        type="submit"
        :disabled="store.saving"
      >
        {{ store.saving ? "등록 중" : "문의 등록" }}
      </button>
    </form>

    <!-- 문의 상세 -->
    <article
      v-else-if="mode === 'detail' && store.inquiry"
      class="inquiry-detail"
    >
      <div class="detail-heading">
        <span class="category">
          {{ categoryText(store.inquiry.category) }}
        </span>

        <span
          class="status"
          :class="store.inquiry.status.toLowerCase()"
        >
          {{ statusText(store.inquiry.status) }}
        </span>
      </div>

      <h3>
        <span v-if="store.inquiry.rootInquiryNo !== null">
          RE:
        </span>
        {{ store.inquiry.title }}
      </h3>

      <time>{{ dateText(store.inquiry.createdAt) }}</time>

      <section class="content-box">
        <h4>문의 내용</h4>
        <p>{{ store.inquiry.content }}</p>
      </section>

      <section
        v-if="store.inquiry.status === 'ANSWERED'"
        class="answer-box"
      >
        <h4>답변 내용</h4>
        <p>{{ store.inquiry.answerContent }}</p>
        <time>
          답변일: {{ dateText(store.inquiry.answeredAt) }}
        </time>
      </section>

      <section v-else class="waiting-box">
        관리자의 답변을 기다리고 있습니다.
      </section>

      <button
        v-if="store.inquiry.status === 'ANSWERED'"
        type="button"
        @click="goReInquiry"
      >
        재문의
      </button>
    </article>

    <!-- 재문의 작성 -->
    <form
      v-else-if="mode === 'reInquiry' && 'store.inquiry'"
      class="inquiry-form"
      @submit.prevent="submitReInquiry"
    >
      <div v-if="store.inquiry" class="original-inquiry">
        <strong>기존 문의</strong>

        <p>
          {{ store.inquiry.title }}
        </p>
      </div>

      <label>
        재문의 내용

        <textarea
          v-model="reInquiryContent"
          rows="10"
          placeholder="추가 문의 내용을 입력해 주세요"
        />
      </label>

      <button
        type="submit"
        :disabled="store.saving"
      >
        {{ store.saving ? "등록 중" : "재문의 등록" }}
      </button>
    </form>
    </template>
  </main>
</template>

<script setup>
import {
  computed,
  reactive,
  ref,
  watch
} from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

import { useInquiryStore } from "./inquiryStore";

const route = useRoute();
const router = useRouter();
const store = useInquiryStore();

const form = reactive({
  category: "",
  title: "",
  content: ""
});

const reInquiryContent = ref("");

const inquiryNo = computed(() =>
  Number(route.params.inquiryNo)
);

const mode = computed(() => {
  if (route.name === "ResidentInquiryWrite") {
    return "write";
  }

  if (route.name === "ResidentReInquiry") {
    return "reInquiry";
  }

  if (route.name === "ResidentInquiryDetail") {
    return "detail";
  }

  return "list";
});

const pageTitle = computed(() => {
  if (mode.value === "write") {
    return "1:1 문의 작성";
  }

  if (mode.value === "reInquiry") {
    return "재문의 작성";
  }

  if (mode.value === "detail") {
    return "1:1 문의 상세";
  }

  return "1:1 문의";
});

const categoryText = (category) => {
  const categories = {
    PARKING: "주차",
    VISIT: "방문",
    PAYMENT: "결제",
    ETC: "기타"
  };

  return categories[category] ?? category;
};

const statusText = (status) =>
  status === "ANSWERED"
    ? "답변 완료"
    : "답변 대기";

const dateText = (value) => {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).format(new Date(value));
};

const loadPage = async () => {
  store.errorMessage = "";

  if (mode.value === "list") {
    await store.loadResidentInquiries();
    return;
  }

  if (
    mode.value === "detail"
    || mode.value === "reInquiry"
  ) {
    await store.loadResidentInquiry(inquiryNo.value);
  }
};

const submitInquiry = async () => {
  await store.addInquiry({
    category: form.category,
    title: form.title,
    content: form.content
  });

  goList();
};

const submitReInquiry = async () => {
  await store.addReInquiry(
    inquiryNo.value,
    reInquiryContent.value
  );

  goList();
};

const goList = () =>
  router.push("/resident/inquiries");

const goWrite = () =>
  router.push("/resident/inquiries/write");

const goDetail = (number) =>
  router.push(`/resident/inquiries/${number}/detail`);

const goReInquiry = () =>
  router.push(
    `/resident/inquiries/${inquiryNo.value}/re-inquiry`
  );

watch(
  () => route.fullPath,
  () => {
    loadPage().catch(() => {});
  },
  { immediate: true }
);
</script>

<style scoped>
.resident-inquiry-page {
  width: min(1000px, calc(100% - 48px));
  margin: 28px auto;
  padding: 28px;
  border: 1px solid #d9e5ee;
  border-radius: 16px;
  background: #fff;
}

.page-header,
.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

button {
  padding: 10px 16px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #2f83d5;
  cursor: pointer;
}

button.secondary {
  color: #315c86;
  background: #eaf3fa;
}

button:disabled {
  opacity: 0.6;
  cursor: default;
}

.inquiry-list {
  display: flex;
  flex-direction: column;
  border-top: 2px solid #31485c;
}

.inquiry-row {
  display: grid;
  grid-template-columns: 90px 1fr 100px 160px;
  gap: 12px;
  align-items: center;
  width: 100%;
  border-bottom: 1px solid #d9e5ee;
  border-radius: 0;
  color: #263d50;
  background: #fff;
  text-align: left;
}

.inquiry-row:hover {
  background: #f5f9fc;
}

.title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  font-weight: 700;
}

.status.waiting {
  color: #d17b18;
}

.status.answered {
  color: #23804c;
}

.inquiry-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.inquiry-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-weight: 700;
}

.inquiry-form input,
.inquiry-form select,
.inquiry-form textarea {
  padding: 12px;
  border: 1px solid #ccd9e3;
  border-radius: 8px;
  font: inherit;
}

.inquiry-form textarea {
  resize: vertical;
}

.inquiry-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.content-box,
.answer-box,
.waiting-box,
.original-inquiry {
  padding: 20px;
  border-radius: 10px;
  background: #f5f8fb;
}

.answer-box {
  border-left: 5px solid #2f83d5;
}

.content-box p,
.answer-box p {
  white-space: pre-wrap;
}

.page-state {
  padding: 50px 12px;
  text-align: center;
}

.page-state.error {
  color: #be4242;
}

@media (max-width: 760px) {
  .resident-inquiry-page {
    width: calc(100% - 24px);
    padding: 18px;
  }

  .inquiry-row {
    grid-template-columns: 70px 1fr;
  }

  .inquiry-row time {
    grid-column: 2;
  }
}
</style>
