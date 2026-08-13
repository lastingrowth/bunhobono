<template>
  <main class="resident-board-list-page">
    <template v-if="!isDetail">
      <header class="page-header">
        <div>
          <h2>공지사항</h2>
        </div>
        <button type="button" @click="goDashboard">대시보드로 돌아가기</button>
      </header>

      <p v-if="store.loading" class="page-state">공지사항을 불러오는 중입니다.</p>
      <p v-else-if="store.errorMessage" class="page-state error">{{ store.errorMessage }}</p>

      <section v-else-if="store.list.length" class="board-card-list">
        <article
          v-for="item in paginatedBoards"
          :key="item.boardNo"
          class="board-card"
          tabindex="0"
          @click="goDetail(item.boardNo)"
          @keydown.enter="goDetail(item.boardNo)"
        >
          <header class="card-heading">
            <h3>{{ item.title }}</h3>
            <span class="period-badge">{{ item.periodStatus }}</span>
          </header>

          <div v-if="item.hasImage" class="card-image">
            <img
              v-if="store.imageUrls[item.boardNo]"
              :src="store.imageUrls[item.boardNo]"
              :alt="item.title"
            />
            <span v-else>이미지 불러오는 중</span>
          </div>
          <div v-else class="card-image card-image-empty">
            <span>등록된 공지 이미지가 없습니다.</span>
          </div>

          <div class="card-copy">
            <p>{{ item.content }}</p>
            <small>{{ periodText(item) }}</small>
          </div>
        </article>
      </section>
      <p v-else class="page-state">게시 중인 공지사항이 없습니다.</p>

      <Pagination
        v-if="store.list.length"
        :current-page="currentPage"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        @change-page="setPage"
      />
    </template>

    <template v-else>
      <header class="page-header">
        <div>
          <h2>공지사항 상세</h2>
        </div>
        <div class="detail-navigation">
          <button type="button" class="home-button" @click="goDashboard">홈으로 돌아가기</button>
          <button type="button" @click="goList">공지사항 전체 목록</button>
        </div>
      </header>

      <p v-if="store.loading" class="page-state">공지사항을 불러오는 중입니다.</p>
      <p v-else-if="store.errorMessage" class="page-state error">{{ store.errorMessage }}</p>

      <article v-else-if="store.board" class="board-detail">
        <h3>{{ store.board.title }}</h3>
        <div class="detail-content">
          <div class="detail-copy">{{ store.board.content }}</div>
          <div v-if="detailImageUrl" class="detail-image">
            <img :src="detailImageUrl" :alt="store.board.title" />
          </div>
        </div>
        <div class="detail-period">
          <span class="period-badge">{{ store.board.periodStatus }}</span>
          <small>{{ periodText(store.board) }}</small>
        </div>
      </article>

      <BoardCommentList
        v-if="store.board"
        :board-no="store.board.boardNo"
      />

      <Pagination
        v-if="store.board && store.list.length"
        :current-page="detailCurrentPage"
        :total-pages="detailTotalPages"
        :page-numbers="detailPageNumbers"
        @change-page="goDetailPage"
      />
    </template>
  </main>
</template>

<script setup>
import { computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import Pagination from "@/shared/pagination/Pagination.vue";
import { usePagination } from "@/shared/pagination/usePagination";
import BoardCommentList from "./BoardCommentList.vue";
import { useBoardStore } from "./boardStore";

const route = useRoute();
const router = useRouter();
const store = useBoardStore();
const { list } = storeToRefs(store);
const isDetail = computed(() => route.name === "ResidentBoardDetail");
const boardNo = computed(() => Number(route.params.boardNo));
const detailImageUrl = computed(() =>
  store.board?.boardNo ? store.imageUrls[store.board.boardNo] || "" : ""
);
const detailCurrentPage = computed(() => {
  const index = list.value.findIndex(
    (item) => Number(item.boardNo) === boardNo.value
  );
  return index >= 0 ? index + 1 : 1;
});
const detailTotalPages = computed(() => Math.max(list.value.length, 1));
const detailPageNumbers = computed(() => {
  const pageGroupSize = 5;
  const currentGroup = Math.ceil(detailCurrentPage.value / pageGroupSize);
  const startPage = (currentGroup - 1) * pageGroupSize + 1;
  const endPage = Math.min(
    startPage + pageGroupSize - 1,
    detailTotalPages.value
  );

  return Array.from(
    { length: endPage - startPage + 1 },
    (_, index) => startPage + index
  );
});

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems: paginatedBoards,
  setPage: setPaginationPage,
} = usePagination(list, 2);

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

const loadPage = async () => {
  if (isDetail.value) {
    await store.loadList();
    const item = await store.loadDetail(boardNo.value);
    if (item?.hasImage) await store.loadImage(item.boardNo);
    return;
  }

  await store.loadList();
  const requestedPage = Math.max(1, Number(route.query.page) || 1);
  setPaginationPage(requestedPage);
  await loadVisibleImages();
};

const loadVisibleImages = async () => {
  const items = paginatedBoards.value;
  await Promise.all(
    items
      .filter((item) => item.hasImage)
      .map((item) => store.loadImage(item.boardNo))
  );
};

const goDashboard = () => router.push("/resident/dashboard");
const goList = () => router.push("/resident/boards");
const goDetail = (no) => router.push(`/resident/boards/${no}/detail`);
const setPage = (page) => router.push({
  path: "/resident/boards",
  query: Number(page) > 1 ? { page: String(page) } : {},
});
const goDetailPage = (page) => {
  const item = list.value[page - 1];
  if (item) goDetail(item.boardNo);
};

watch(() => route.fullPath, () => {
  loadPage().catch(() => {});
}, { immediate: true });

watch(currentPage, () => {
  loadVisibleImages().catch(() => {});
});
</script>

<style scoped>
:global(.content:has(.resident-board-list-page)) {
  padding: 0;
}

.resident-board-list-page {
  box-sizing: border-box;
  width: min(1120px, calc(100% - 48px));
  min-height: calc(100vh - var(--header-height) - 90px);
  margin: 28px auto;
  padding: 28px;
  border: 1px solid #d9e5ee;
  border-radius: 16px;
  color: #203b54;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 12px 32px rgba(39, 79, 113, .12);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 7px;
  font-size: 27px;
}

.page-header h2::before {
  width: 5px;
  height: 25px;
  border-radius: 999px;
  background: #2f8ddd;
  content: "";
}

.page-header p {
  margin: 0;
  color: #7a8ea1;
}

.page-header button,
.board-detail button {
  padding: 10px 15px;
  border: 1px solid #2f83d5;
  border-radius: 9px;
  color: #fff;
  background: #2f83d5;
  font-weight: 800;
  cursor: pointer;
}

.detail-navigation {
  display: flex;
  align-items: center;
  gap: 9px;
}

.page-header .home-button {
  border-color: #a9c8df;
  color: #315c86;
  background: #fff;
}

.board-card-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.board-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 420px;
  border: 1px solid #d9e5ee;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color .2s ease, transform .2s ease, box-shadow .2s ease;
}

.board-card:hover,
.board-card:focus-visible {
  border-color: #69a9df;
  outline: 0;
  box-shadow: 0 8px 20px rgba(41, 92, 135, .13);
  transform: translateY(-2px);
}

.card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 70px;
  padding: 17px 17px 13px;
}

.card-heading h3 {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #23425d;
  font-size: 19px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-image {
  display: grid;
  place-items: center;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 4 / 3;
  color: #8194a6;
  background: #eef4f8;
  font-size: 11px;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.card-image-empty {
  color: #8b9bab;
  background:
    linear-gradient(135deg, rgba(229, 239, 247, .75), rgba(246, 249, 251, .95));
}

.card-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  padding: 16px;
}

.card-copy p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0 0 12px;
  color: #647b8f;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.card-copy small {
  margin-top: auto;
  color: #8294a4;
}

.board-detail small {
  color: #8294a4;
}

.period-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  color: #14783d;
  background: #e5f7eb;
  font-size: 11px;
  font-weight: 800;
}

.page-state {
  padding: 50px 12px;
  border: 1px solid #dce6ee;
  border-radius: 12px;
  color: #71879a;
  text-align: center;
  background: #f9fbfc;
}

.page-state.error {
  color: #be4242;
}

.board-detail {
  padding: 25px;
  border: 1px solid #d9e5ee;
  border-radius: 13px;
  background: #fff;
}

.detail-image {
  display: grid;
  place-items: center;
  overflow: hidden;
  margin-top: 22px;
  border-radius: 10px;
  background: #eef4f8;
}

.detail-image img {
  display: block;
  width: 100%;
  max-height: 480px;
  object-fit: contain;
}

.board-detail h3 {
  margin: 0 0 12px;
  color: #17212b;
  font-size: 26px;
}

.detail-period {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.detail-content {
  min-height: 210px;
  margin: 14px 0 0;
  padding: 22px;
  line-height: 1.8;
  background: #f7fafc;
}

.detail-copy {
  white-space: pre-wrap;
}

@media (max-width: 760px) {
  .resident-board-list-page {
    width: calc(100% - 24px);
    margin: 12px auto;
    padding: 18px;
  }

  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-navigation {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .board-card-list {
    grid-template-columns: 1fr;
  }

  .board-card {
    min-height: 0;
  }
}
</style>
