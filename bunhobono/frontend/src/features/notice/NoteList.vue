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
      <h2 class="management-list-title">
        {{ pageTitle }}
      </h2>

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

        <button
          type="button"
          @click="handleLoadNotices"
        >
          새로고침
        </button>
      </div>
    </div>

    <form
      class="notice-search management-list-toolbar"
      @submit.prevent="handleSearch"
    >
      <input
        v-model="carNoKeyword"
        class="notice-search-input management-car-search-input"
        type="search"
        placeholder="차량번호 검색"
        aria-label="차량번호 검색"
      >

      <button
        class="notice-search-button management-search-button"
        type="submit"
        :disabled="loading"
      >
        {{ loading && searchApplied ? "검색 중..." : "검색" }}
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

    <p v-if="loading">
      불러오는 중...
    </p>

    <p v-else-if="errorMessage">
      {{ errorMessage }}
    </p>

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

              <th class="col-action">
                관리
              </th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="(notice, index) in paginatedItems"
              :key="getNoticeNo(notice)"
              class="notice-row"
              @click="goDetail(notice)"
            >
              <td
                v-for="column in columns"
                :key="column.key"
                :class="column.className"
                :data-label="column.label"
              >
                <span v-if="column.key === 'noticeNo'">
                  {{ (currentPage - 1) * pageSize + index + 1 }}
                </span>

                <template v-else-if="column.key === 'noticeType'">
                  {{ getNoticeTypeLabel(getNoticeType(notice)) }}
                </template>

                <template v-else-if="column.key === 'alertStat'">
                  {{ getStatusLabel(getAlertStat(notice)) }}
                </template>

                <template v-else>
                  {{ formatValue(getValue(notice, column), column) }}
                </template>
              </td>

              <td
                class="col-action"
                data-label="관리"
              >
                <button
                  type="button"
                  @click.stop="removeNotice(notice)"
                >
                  삭제
                </button>
              </td>
            </tr>

            <tr
              v-if="paginatedItems.length === 0"
              class="notice-empty-row"
            >
              <td :colspan="columns.length + 1">
                선택한 처리상태의 알림이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-action-row admin-pagination-area">
        <Pagination
          :current-page="currentPage"
          :total-pages="totalPages"
          :page-numbers="pageNumbers"
          @change-page="setPage"
        />

        <button
          v-if="fromStatistics"
          type="button"
          class="back-button statistics-back-button"
          @click="backToStatistics"
        >
          통계로 돌아가기
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
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";

import Pagination from "@/shared/pagination/Pagination.vue";
import { usePagination } from "@/shared/pagination/usePagination";

import { searchNoticesByCarNo } from "./noticeApi";
import NoticeDeleteConfirm from "./NoticeDeleteConfirm.vue";
import { useNoticeStore } from "./noticeStore";

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
  {
    value: "Unresolved",
    label: "미처리"
  },
  {
    value: "Resolved",
    label: "처리완료"
  }
];

const validStatuses = statusOptions.map((option) => {
  return option.value;
});

const getRouteStatus = () => {
  const status = route.query.status;

  if (validStatuses.includes(status)) {
    return status;
  }

  return "Unresolved";
};

const selectedStatus = ref(getRouteStatus());

const getRouteNoticeType = () => {
  if (route.name === "NoticeVisitLongStay") {
    return "VISIT_OVERDUE";
  }

  if (route.name === "NoticeUnknownLongStay") {
    return "UNKNOWN_OVERSTAY";
  }

  return "";
};

const selectedNoticeType = ref(getRouteNoticeType());

const fromStatistics = computed(() => {
  return (
    route.name === "NoticeVisitLongStay"
    || route.name === "NoticeUnknownLongStay"
  );
});

const pageTitle = computed(() => {
  if (selectedNoticeType.value === "VISIT_OVERDUE") {
    return "방문차량 장기주차 알림";
  }

  if (selectedNoticeType.value === "UNKNOWN_OVERSTAY") {
    return "미등록차량 장기주차 알림";
  }

  return "알림 관리";
});

const columns = [
  {
    key: "noticeNo",
    fallbackKey: "notice_no",
    label: "번호",
    className: "col-xs"
  },
  {
    key: "noticeType",
    fallbackKey: "notice_type",
    label: "알림 종류",
    className: "col-type"
  },
  {
    key: "registeredCarNo",
    fallbackKey: "registered_car_no",
    label: "등록 차량번호",
    className: "col-sm"
  },
  {
    key: "capturedCarNo",
    fallbackKey: "captured_car_no",
    label: "촬영 차량번호",
    className: "col-sm"
  },
  {
    key: "detectAt",
    fallbackKey: "detect_at",
    label: "감지 일시",
    className: "col-date",
    type: "date"
  },
  {
    key: "stayDays",
    fallbackKey: "stay_days",
    label: "경과 일수",
    className: "col-days"
  },
  {
    key: "alertStat",
    fallbackKey: "alert_stat",
    label: "처리 상태",
    className: "col-status"
  }
];

const getValue = (notice, column) => {
  return notice[column.key] ?? notice[column.fallbackKey];
};

const getNoticeNo = (notice) => {
  return notice.noticeNo ?? notice.notice_no;
};

const getNoticeType = (notice) => {
  return notice.noticeType ?? notice.notice_type ?? "";
};

const getAlertStat = (notice) => {
  return notice.alertStat
    ?? notice.alert_stat
    ?? "Unresolved";
};

const getNoticeTypeLabel = (value) => {
  const labels = {
    EXIT_WITHOUT_ENTRY: "입차기록 없는 출차",
    VISIT_OVERDUE: "방문차량 시간 초과",
    UNKNOWN_OVERSTAY: "미등록차량 장기주차",
    OCR_REVIEW: "OCR 확인 필요"
  };

  return labels[value] ?? value ?? "-";
};

const getStatusLabel = (value) => {
  return statusOptions.find((option) => {
    return option.value === value;
  })?.label ?? value ?? "-";
};

const filteredNotices = computed(() => {
  return notices.value.filter((notice) => {
    const statusMatched =
      getAlertStat(notice) === selectedStatus.value;

    if (!statusMatched) {
      return false;
    }

    if (!selectedNoticeType.value) {
      return true;
    }

    return (
      getNoticeType(notice)
      === selectedNoticeType.value
    );
  });
});

const sortedNotices = computed(() => {
  return [...filteredNotices.value].sort((a, b) => {
    const aNo = Number(
      a.noticeNo
      ?? a.notice_no
      ?? 0
    );

    const bNo = Number(
      b.noticeNo
      ?? b.notice_no
      ?? 0
    );

    return sortOrder.value === "desc"
      ? bNo - aNo
      : aNo - bNo;
  });
});

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

  const month = String(
    date.getMonth() + 1
  ).padStart(2, "0");

  const day = String(
    date.getDate()
  ).padStart(2, "0");

  const hour = String(
    date.getHours()
  ).padStart(2, "0");

  const minute = String(
    date.getMinutes()
  ).padStart(2, "0");

  return `${month}-${day} ${hour}:${minute}`;
};

const formatValue = (value, column) => {
  if (
    value === null
    || value === undefined
    || value === ""
  ) {
    return "-";
  }

  if (column?.type === "date") {
    return formatDate(value);
  }

  return value;
};

const handleLoadNotices = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    await noticeStore.loadNotices();
  } catch (error) {
    console.error(error);

    errorMessage.value =
      "알림 목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const backToStatistics = () => {
  router.push("/admin/statistics");
};

const handleStatusChange = () => {
  currentPage.value = 1;
};

const goDetail = (notice) => {
  const noticeNo = getNoticeNo(notice);

  if (!noticeNo) {
    return;
  }

  router.push(`/admin/notice/${noticeNo}`);
};

const handleSearch = async () => {
  const carNo =
    carNoKeyword.value.replace(/\s+/g, "");

  if (!carNo) {
    await resetSearch();
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  searchApplied.value = true;

  try {
    const response =
      await searchNoticesByCarNo(carNo);

    notices.value = Array.isArray(response.data)
      ? response.data
      : [];

    currentPage.value = 1;
  } catch (error) {
    console.error(error);

    errorMessage.value =
      "차량번호 검색 결과를 불러오지 못했습니다.";
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
  if (
    !pendingDeleteNotice.value
    || deleting.value
  ) {
    return;
  }

  deleting.value = true;

  await noticeStore.remove(
    getNoticeNo(pendingDeleteNotice.value)
  );

  deleting.value = false;
  pendingDeleteNotice.value = null;
};

onMounted(handleLoadNotices);

watch(
  () => route.query.status,
  () => {
    selectedStatus.value = getRouteStatus();
    currentPage.value = 1;
  }
);

watch(
  () => route.name,
  () => {
    selectedNoticeType.value =
      getRouteNoticeType();

    currentPage.value = 1;
  }
);
</script>

<style scoped>
.notice-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.notice-table {
  width: 100%;
  min-width: 900px;
  table-layout: fixed;
}

.notice-table .col-xs { width: 6%; }
.notice-table .col-type { width: 17%; }
.notice-table .col-sm { width: 15%; }
.notice-table .col-date { width: 18%; }
.notice-table .col-days { width: 10%; }
.notice-table .col-status { width: 11%; }
.notice-table .col-action { width: 8%; }

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
.notice-toast-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.notice-toast-enter-from,
.notice-toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
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
  box-sizing: border-box;
  width: 170px;
  height: 36px !important;
  min-height: 36px !important;
  max-height: 36px;
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

@media (max-width: 1000px) {
  .notice-table th,
  .notice-table td,
  .notice-table .col-action button {
    font-size: 12px;
  }
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

@media (max-width: 700px) {
  .notice-table th,
  .notice-table td,
  .notice-table .col-action button {
    font-size: 11px;
  }
}
</style>