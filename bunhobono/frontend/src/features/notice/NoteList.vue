<template>
  <main class="notice-page">
    <div class="notice-header">
      <h1>{{ pageTitle }}</h1>

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

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="errorMessage">{{ errorMessage }}</p>
    <p v-else-if="sortedNotices.length === 0">선택한 처리상태의 알림이 없습니다.</p>

    <template v-else>
    <div class="notice-table-wrap">
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
            <th class="col-action"></th>
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
            <td class="col-action" data-label="관리"><button type="button" @click.stop="noticeStore.remove(getNoticeNo(notice))">삭제</button></td>
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
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useNoticeStore } from "./noticeStore";
import { storeToRefs } from "pinia";
import { usePagination } from "@/shared/pagination/usePagination";
import Pagination from "@/shared/pagination/Pagination.vue";

const route = useRoute();
const router = useRouter();
const noticeStore = useNoticeStore();

const { notices } = storeToRefs(noticeStore);

const loading = ref(false);
const errorMessage = ref("");
const sortOrder = ref("desc");

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
<<<<<<< HEAD
=======

<style scoped>
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
>>>>>>> jeongmin
