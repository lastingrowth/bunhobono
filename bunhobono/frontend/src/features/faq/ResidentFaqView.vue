<template>
  <main class="resident-faq-page resident-standard-page">
    <header class="page-header resident-standard-header">
      <div>
        <h2>1:1 문의</h2>

        <p class="page-description">
          문의하기 전에 자주 묻는 질문을 확인해 보세요.
        </p>
      </div>

      <div class="header-actions">
        <button type="button" class="resident-home-button" title="홈으로 돌아가기" aria-label="홈으로 돌아가기" @click="router.push('/resident/dashboard')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11.2 12 4l9 7.2"/><path d="M5.5 10.2V20h13v-9.8"/><path d="M9.5 20v-6h5v6"/></svg></button>

        <button
          type="button"
          class="secondary"
          @click="goMyInquiries"
        >
          내 문의
        </button>

        <button
          type="button"
          @click="goWrite"
        >
          문의하기
        </button>
      </div>
    </header>

    <p v-if="store.loading" class="page-state">
      자주하는 질문을 불러오는 중입니다.
    </p>

    <p
      v-else-if="store.errorMessage"
      class="page-state error"
    >
      {{ store.errorMessage }}
    </p>

    <!-- 자주하는 질문 목록 -->
    <section
      v-else-if="store.faqs.length"
      class="faq-section"
    >
      <h3>자주하는 질문</h3>

      <div class="faq-list">
        <article
          v-for="faq in store.faqs"
          :key="faq.faqNo"
          class="faq-item"
        >
          <!-- 질문을 누르면 답변 열기 또는 닫기 -->
          <button
            type="button"
            class="faq-question"
            @click="toggleFaq(faq.faqNo)"
          >
            <span class="question-mark">
              Q
            </span>

            <span class="category">
              {{ categoryText(faq.category) }}
            </span>

            <strong>
              {{ faq.question }}
            </strong>

            <span class="toggle-mark">
              {{
                openFaqNo === faq.faqNo
                  ? "▲"
                  : "▼"
              }}
            </span>
          </button>

          <!-- 선택한 질문의 답변 -->
          <div
            v-if="openFaqNo === faq.faqNo"
            class="faq-answer"
          >
            <span class="answer-mark">
              A
            </span>

            <p>{{ faq.answer }}</p>
          </div>
        </article>
      </div>
    </section>

    <p v-else class="page-state">
      등록된 자주하는 질문이 없습니다.
    </p>
  </main>
</template>

<script setup>
import {
  onMounted,
  ref
} from "vue";

import { useRouter } from "vue-router";
import { useFaqStore } from "./faqStore";

const router = useRouter();
const store = useFaqStore();

// 현재 열려 있는 질문 번호
const openFaqNo = ref(null);

// 질문을 다시 누르면 답변 닫기
const toggleFaq = (faqNo) => {
  openFaqNo.value =
    openFaqNo.value === faqNo
      ? null
      : faqNo;
};

// FAQ 분류 한글 표시
const categoryText = (category) => {
  const categories = {
    PARKING: "주차",
    VISIT: "방문",
    PAYMENT: "결제",
    ETC: "기타"
  };

  return categories[category] ?? category;
};

// 내 문의 목록으로 이동
const goMyInquiries = () =>
  router.push("/resident/inquiries/my");

// 문의 작성 화면으로 이동
const goWrite = () =>
  router.push("/resident/inquiries/write");

// 화면을 열 때 FAQ 목록 조회
onMounted(() => {
  store.loadFaqs().catch(() => {});
});
</script>

<style scoped>
.resident-faq-page {
  width: min(1000px, calc(100% - 48px));
  margin: 28px auto;
  padding: 28px;
  border: 1px solid #d9e5ee;
  border-radius: 16px;
  background: #fff;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 32px;
}

.page-header h2 {
  margin: 0;
}

.page-description {
  margin: 8px 0 0;
  color: #607588;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-actions button {
  padding: 10px 16px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #2f83d5;
  cursor: pointer;
}

.header-actions button.secondary {
  color: #315c86;
  background: #eaf3fa;
}

.faq-section h3 {
  margin: 0 0 16px;
}

.faq-list {
  border-top: 2px solid #31485c;
}

.faq-item {
  border-bottom: 1px solid #d9e5ee;
}

.faq-question {
  display: grid;
  grid-template-columns: 34px 70px 1fr 24px;
  gap: 12px;
  align-items: center;
  width: 100%;
  padding: 18px 16px;
  border: 0;
  color: #263d50;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.faq-question:hover {
  background: #f5f9fc;
}

.question-mark,
.answer-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: #2674be;
  background: #eaf3fa;
  font-weight: 700;
}

.category {
  color: #587087;
  font-size: 14px;
}

.toggle-mark {
  color: #587087;
  font-size: 12px;
  text-align: center;
}

.faq-answer {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  align-items: flex-start;
  padding: 20px 16px;
  border-top: 1px solid #e4edf4;
  background: #f7fafc;
}

.faq-answer p {
  margin: 4px 0 0;
  color: #344b5f;
  line-height: 1.7;
  white-space: pre-wrap;
}

.page-state {
  padding: 50px 12px;
  text-align: center;
}

.page-state.error {
  color: #be4242;
}

@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
  .resident-faq-page {
    width: calc(100% - 24px);
    padding: 18px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .faq-question {
    grid-template-columns: 34px 1fr 24px;
  }

  .faq-question .category {
    display: none;
  }
}
</style>
