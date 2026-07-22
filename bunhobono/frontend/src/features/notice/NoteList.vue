<template>
  <main class="notice-page management-list-page">
    <Transition name="notice-toast">
      <div
        v-if="noticeStore.feedbackMessage"
        class="notice-feedback-toast"
        :class="noticeStore.feedbackType"
        role="status"
      >
        {{ noticeStore.feedbackMessage }}
      </div>
    </Transition>
    <div class="notice-header management-list-header">
      <h2 class="management-list-title">{{ pageTitle }}</h2>

      <div class="notice-actions">
        <div class="status-filters">
          <label
            v-for="option in statusOptions"
            :key="option.value"
            class="status-filter"
          >
            <input
              v-model="selectedStatus"
              type="radio"
              name="noticeStatus"
              :value="option.value"
              @change="handleStatusChange"
            >
            <span>{{ option.label }}</span>
          </label>
        </div>

        <select v-model="sortOrder">
          <option value="desc">최신순</option>
          <option value="asc">오래된순</option>
        </select>

        <button type="button" @click="handleLoadNotices">새로고침</button>
      </div>
    </div>

    <form class="notice-search management-list-toolbar" @submit.prevent="handleSearch">
      <input
        v-model="carNoKeyword"
        class="notice-search-input management-car-search-input"
        type="search"
        placeholder="차량번호 검색"
        aria-label="차량번호 검색"
      >
      <button class="notice-search-button management-search-button" type="submit" :disabled="loading">
        {{ loading && searchApplied ? '검색 중...' : '검색' }}
      </button>
      <button
        class="notice-reset-button management-reset-button"
        type="button"
        :disabled="loading"
        @click="resetSearch"
      >
        초기화
      </button>
    </form>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="errorMessage">{{ errorMessage }}</p>

    <template v-else>
    <div class="notice-table-wrap management-list-table">
      <table class="notice-table" border="1">
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              :class="column.className"
            >
              {{ column.label }}
            </th>
            <th class="col-action">관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(notice, index) in paginatedItems"
            :key="notice.noticeNo ?? notice.notice_no"
            class="notice-row"
            @click="goDetail(notice)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              :class="column.className"
              :data-label="column.label"
              :title="formatValue(getValue(notice, column), column)"
            >
              <span v-if="column.key === 'noticeNo'">
                {{ (currentPage - 1) * pageSize + index + 1 }}
              </span>

              <template v-else-if="column.key === 'alertStat'">
                {{ getStatusLabel(getAlertStat(notice)) }}
              </template>

              <template v-else>
                {{ formatValue(getValue(notice, column), column) }}
              </template>
            </td>
            <td class="col-action" data-label="관리"><button type="button" @click.stop="removeNotice(notice)">삭제</button></td>
          </tr>
          <tr v-if="paginatedItems.length === 0" class="notice-empty-row">
            <td :colspan="columns.length + 1">선택한 처리상태의 알림이 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination-action-row admin-pagination-area">
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :page-numbers="pageNumbers"
          @change-page="setPage"/>
        <button
          v-if="fromStatistics"
          type="button"
          class="back-button statistics-back-button"
          @click="backToStatistics"
        >
          ← 통계로 돌아가기
        </button>
    </div>
    </template>
    <NoticeDeleteConfirm
      :open="Boolean(pendingDeleteNotice)"
      :car-no="getDeleteCarNo(pendingDeleteNotice)"
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useNoticeStore } from "./noticeStore";
import { storeToRefs } from "pinia";
import { usePagination } from "@/shared/pagination/usePagination";
import Pagination from "@/shared/pagination/Pagination.vue";
import { searchNoticesByCarNo } from "./noticeApi";
import NoticeDeleteConfirm from "./NoticeDeleteConfirm.vue";

const route = useRoute();
const router = useRouter();
const noticeStore = useNoticeStore();

const { notices } = storeToRefs(noticeStore);

const loading = ref(false);
const errorMessage = ref("");
const sortOrder = ref("desc");
const carNoKeyword = ref("");
const searchApplied = ref(false);
const pendingDeleteNotice = ref(null);
const deleting = ref(false);

const statusOptions = [
  { value: "Unresolved", label: "미확인" },
  { value: "Checked", label: "확인" },
  { value: "Resolved", label: "처리완료" },
];

const validStatuses = statusOptions.map((option) => option.value);

// 통계 페이지에서 들어온 장기주차 알림 화면은
// 미확인(Unresolved)과 확인(Checked)을 함께 보여준다.
// 처리완료(Resolved)만 제외해야 대시보드 숫자와 목록 숫자가 맞는다.
const getRouteStatus = () => {
  const status = route.query.status

  if (validStatuses.includes(status)) {
    return status
  }

  if (
    route.name === 'NoticeVisitLongStay'
    || route.name === 'NoticeUnknownLongStay'
  ) {
    return ''
  }

  return 'Unresolved'
}

const selectedStatus = ref(getRouteStatus());

// 통계 페이지에서 들어온 전용 주소에 따라 알림 종류를 나눈다.
// 기존 NoteList 화면은 그대로 쓰고, route name만 보고 필터링한다.
const getRouteCarKind = () => {
  if (route.name === 'NoticeVisitLongStay') {
    return 'VISIT'
  }

  if (route.name === 'NoticeUnknownLongStay') {
    return 'UNKNOWN'
  }

  return ''
}

const selectedCarKind = ref(getRouteCarKind())

// 통계 화면에서 들어온 전용 화면인지 확인한다.
const fromStatistics = computed(() => {
  return (
    route.name === 'NoticeVisitLongStay'
    || route.name === 'NoticeUnknownLongStay'
  )
})

const pageTitle = computed(() => {
  if (selectedCarKind.value === 'VISIT') {
    return '방문 장기주차 알림'
  }

  if (selectedCarKind.value === 'UNKNOWN') {
    return '비등록 장기주차 알림'
  }

  return '알림 관리'
})

const columns = [
  { key: "noticeNo", fallbackKey: "notice_no", label: "번호", className: "col-xs" },
  { key: "registeredCarNo", fallbackKey: "registered_car_no", label: "등록 차량번호", className: "col-sm" },
  { key: "capturedCarNo", fallbackKey: "captured_car_no", label: "촬영 차량번호", className: "col-sm" },
  { key: "detectAt", fallbackKey: "detect_at", label: "감지 일시", className: "col-date", type: "date" },
  { key: "stayDays", fallbackKey: "stay_days", label: "주차 일수", className: "col-xs" },
  { key: "alertStat", fallbackKey: "alert_stat", label: "처리 상태", className: "col-status" },
];

const getValue = (notice, column) => {
  return notice[column.key] ?? notice[column.fallbackKey];
};

const getAlertStat = (notice) => {
  return notice.alertStat ?? notice.alert_stat ?? "Unresolved";
};

const getCarKind = (notice) => {
  return notice.carKind ?? notice.car_kind ?? "";
};

const filteredNotices = computed(() => {
  return notices.value.filter((notice) => {
    // selectedStatus가 비어 있으면 처리완료를 제외한 알림을 보여준다.
    // 통계 카드의 장기주차 알림 수와 목록 수를 맞추기 위한 조건이다.
    const statusMatched = selectedStatus.value
      ? getAlertStat(notice) === selectedStatus.value
      : getAlertStat(notice) !== 'Resolved'

    if (!selectedCarKind.value) {
      return statusMatched
    }

    return statusMatched && getCarKind(notice) === selectedCarKind.value
  })
})

const sortedNotices = computed(() => {
  return [...filteredNotices.value].sort((a, b) => {
    const aNo = Number(a.noticeNo ?? a.notice_no ?? 0);
    const bNo = Number(b.noticeNo ?? b.notice_no ?? 0);

    return sortOrder.value === "desc" ? bNo - aNo : aNo - bNo;
  });
});

// 한 페이지에 10개씩
const pageSize = 10;

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(sortedNotices, pageSize);


const formatDate = (value) => {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");

  return `${month}-${day} ${hour}:${minute}`;
};

const formatValue = (value, column) => {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  if (column?.type === "date") {
    return formatDate(value);
  }

  if (column?.type === "money") {
    return Number(value).toLocaleString();
  }

  return value;
};

const getNoticeNo = (notice) => {
  return notice.noticeNo ?? notice.notice_no;
};

const getStatusLabel = (value) => {
  return statusOptions.find((option) => option.value === value)?.label ?? value ?? "-";
};

const handleLoadNotices = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    await noticeStore.loadNotices();
  } catch (error) {
    console.error(error);
    errorMessage.value = "알림 목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

// 통계 화면으로 돌아간다.
const backToStatistics = () => {
  router.push('/admin/statistics')
}

const handleStatusChange = () => {
  // 상태를 직접 선택하면 방문/미등록 필터를 해제
  selectedCarKind.value = ""

  // 첫 페이지부터 다시 보여준다
  currentPage.value = 1
}

const goDetail = (notice) => {
  const noticeNo = getNoticeNo(notice);

  if (!noticeNo) {
    return;
  }

  router.push(`/admin/notice/${noticeNo}`);
};

const handleSearch = async () => {
  const carNo = carNoKeyword.value.replace(/\s+/g, "");

  if (!carNo) {
    await resetSearch();
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  searchApplied.value = true;

  try {
    const response = await searchNoticesByCarNo(carNo);
    notices.value = Array.isArray(response.data) ? response.data : [];
    currentPage.value = 1;
  } catch (error) {
    console.error(error);
    errorMessage.value = "차량번호 검색 결과를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const resetSearch = async () => {
  carNoKeyword.value = "";
  searchApplied.value = false;
  currentPage.value = 1;
  await handleLoadNotices();
};

const getDeleteCarNo = (notice) => {
  return notice?.registeredCarNo
    ?? notice?.registered_car_no
    ?? notice?.capturedCarNo
    ?? notice?.captured_car_no
    ?? "";
};

const removeNotice = (notice) => {
  pendingDeleteNotice.value = notice;
};

const cancelDelete = () => {
  if (!deleting.value) {
    pendingDeleteNotice.value = null;
  }
};

const confirmDelete = async () => {
  if (!pendingDeleteNotice.value || deleting.value) {
    return;
  }

  deleting.value = true;
  await noticeStore.remove(getNoticeNo(pendingDeleteNotice.value));
  deleting.value = false;
  pendingDeleteNotice.value = null;
};

onMounted(handleLoadNotices);

watch(
  () => route.query.status,
  () => {
    selectedStatus.value = getRouteStatus();
  }
);

// 같은 NoteList를 쓰더라도 주소가 바뀌면 알림 필터를 다시 적용한다.
watch(
  () => route.name,
  () => {
    selectedCarKind.value = getRouteCarKind()
    currentPage.value = 1
  }
)

</script>

<style scoped>
.notice-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.notice-table {
  width: 100%;
  min-width: 760px;
  table-layout: fixed;
}

.notice-table .col-xs { width: 7%; }
.notice-table .col-sm { width: 20%; }
.notice-table .col-date { width: 25%; }
.notice-table .col-status { width: 14%; }
.notice-table .col-action { width: 7%; }

.notice-table th,
.notice-table td {
  box-sizing: border-box;
  height: 30px !important;
  padding: 4px 7px !important;
  font-size: 13px;
  line-height: 1.3;
  text-align: center !important;
  vertical-align: middle;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-table .col-action {
  text-align: center !important;
}

.notice-table .notice-empty-row td {
  height: 54px !important;
  color: #aeb6bd;
  text-align: center !important;
}

.notice-table .col-action button {
  box-sizing: border-box;
  width: auto;
  min-width: 52px;
  height: 22px !important;
  min-height: 0 !important;
  padding: 2px 8px !important;
  font-size: 12px;
  line-height: 16px;
  white-space: nowrap;
}

.notice-feedback-toast {
  position: fixed;
  z-index: 1200;
  top: 24px;
  right: 24px;
  padding: 11px 16px;
  border: 1px solid #9fcfb0;
  border-radius: 8px;
  color: #1f6840;
  background: #ecf8f0;
  box-shadow: 0 8px 24px rgba(23, 45, 34, 0.18);
  font-size: 13px;
  font-weight: 800;
}

.notice-feedback-toast.error {
  border-color: #e3adad;
  color: #9f2f2f;
  background: #fff0f0;
}

.notice-toast-enter-active,
.notice-toast-leave-active { transition: opacity .18s ease, transform .18s ease; }
.notice-toast-enter-from,
.notice-toast-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 1000px) {
  .notice-table th,
  .notice-table td,
  .notice-table .col-action button { font-size: 12px; }
}

@media (max-width: 700px) {
  .notice-table th,
  .notice-table td,
  .notice-table .col-action button { font-size: 11px; }
}

.notice-page form.notice-search {
  width: auto;
  max-width: none;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 0;
  border: 0 !important;
  border-radius: 0;
  background: transparent !important;
  box-shadow: none !important;
}

.notice-page form.notice-search .notice-search-input {
  width: 170px;
  height: 36px !important;
  min-height: 36px !important;
  max-height: 36px;
  box-sizing: border-box;
  padding: 0 10px !important;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
}

.notice-page form.notice-search .notice-search-input:focus {
  border-color: #2563eb;
  outline: 2px solid rgba(37, 99, 235, 0.14);
}

.notice-search-button,
.notice-reset-button {
  width: 56px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1;
  text-align: center;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .notice-page form.notice-search {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
  }

  .notice-search-input {
    width: 100%;
  }
}

.back-button {
  height: 36px;
  padding: 0 12px;
  margin-top: 8px;

  border: 1px solid #d1d5db;
  border-radius: 6px;

  background: #fff;
  cursor: pointer;
}

.back-button:hover {
  background: #f3f4f6;
}
</style>
