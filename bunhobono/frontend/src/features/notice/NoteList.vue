<template>
  <main class="notice-page">
    <div class="notice-header">
      <h1>알림 리스트</h1>

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

    <div v-else class="notice-table-wrap">
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
            v-for="(notice, index) in sortedNotices"
            :key="notice.noticeNo ?? notice.notice_no"
            class="notice-row"
            @click="goDetail(notice)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              :class="column.className"
              :title="formatValue(getValue(notice, column), column)"
            >
              <span v-if="column.key === 'noticeNo'">
                {{ index + 1 }}
              </span>

              <span v-else-if="column.key === 'alertStat'">
                {{ getStatusLabel(getAlertStat(notice)) }}
              </span>

              <template v-else>
                {{ formatValue(getValue(notice, column), column) }}
              </template>
            </td>
            <td class="col-action">
              <button
                type="button"
                @click.stop="noticeStore.remove(getNoticeNo(notice))"
              >
                삭제
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useNoticeStore } from "./noticeStore";
import { storeToRefs } from "pinia";

const router = useRouter();
const noticeStore = useNoticeStore();

const { notices } = storeToRefs(noticeStore);

const loading = ref(false);
const errorMessage = ref("");
const sortOrder = ref("desc");
const selectedStatus = ref("Unresolved");

const statusOptions = [
  { value: "Unresolved", label: "미확인" },
  { value: "Checked", label: "확인" },
  { value: "Resolved", label: "처리완료" },
];

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

const filteredNotices = computed(() => {
  return notices.value.filter((notice) => {
    return getAlertStat(notice) === selectedStatus.value;
  });
});

const sortedNotices = computed(() => {
  return [...filteredNotices.value].sort((a, b) => {
    const aNo = Number(a.noticeNo ?? a.notice_no ?? 0);
    const bNo = Number(b.noticeNo ?? b.notice_no ?? 0);

    return sortOrder.value === "desc" ? bNo - aNo : aNo - bNo;
  });
});

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

// 화면에서 목록 조회를 처리하는 함수
const handleLoadNotices = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    // Store의 목록 조회 함수 호출
    await noticeStore.loadNotices();
  } catch (error) {
    console.error(error);
    errorMessage.value = "알림 목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const goDetail = (notice) => {
  const noticeNo = getNoticeNo(notice);

  if (!noticeNo) {
    return;
  }

  router.push(`/admin/notice/${noticeNo}`);
};

onMounted(handleLoadNotices);
</script>
