<template>
  <main class="notice-detail-page">
    <ManagementFeedbackToast
      :message="feedbackMessage || noticeStore.feedbackMessage"
      :type="feedbackMessage ? feedbackType : noticeStore.feedbackType"
    />

    <section class="notice-detail-dialog">
      <div class="detail-header">
        <h1>알림 상세</h1>

        <div class="detail-actions">
          <button type="button" :disabled="!prevNotice" @click="moveNotice(prevNotice)">
            이전
          </button>
          <button type="button" :disabled="!nextNotice" @click="moveNotice(nextNotice)">
            다음
          </button>
          <button type="button" @click="goNoticeList">목록</button>
          <button type="button" @click="deleteCurrentNotice">삭제</button>
        </div>
      </div>

      <p v-if="loading">불러오는 중...</p>
      <p v-else-if="errorMessage">{{ errorMessage }}</p>

      <template v-else-if="notice">
        <table class="detail-table" border="1">
          <tbody>
            <tr v-for="row in detailRows" :key="row.label">
              <th>{{ row.label }}</th>
              <td>{{ formatValue(row.value) }}</td>
            </tr>
          </tbody>
        </table>

        <section class="notice-detail-actions">
          <button
            type="button"
            :disabled="!canCompleteNotice"
            @click="completeNotice"
          >
            {{ saving ? "처리 중" : "처리 완료" }}
          </button>
        </section>
      </template>

      <p v-else>조회할 알림이 없습니다.</p>
    </section>

    <NoticeDeleteConfirm
      :open="deleteConfirmOpen"
      :car-no="currentCarNo"
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";

import ManagementFeedbackToast
  from "@/shared/components/ManagementFeedbackToast.vue";

import { getNoteList } from "./noticeApi";
import NoticeDeleteConfirm from "./NoticeDeleteConfirm.vue";
import { useNoticeStore } from "./noticeStore";

const route = useRoute();
const router = useRouter();
const noticeStore = useNoticeStore();

const {
  notice,
  notices: noticeList
} = storeToRefs(noticeStore);

const loading = ref(false);
const saving = ref(false);
const deleting = ref(false);
const deleteConfirmOpen = ref(false);
const errorMessage = ref("");
const feedbackMessage = ref("");
const feedbackType = ref("success");

let feedbackTimer;

const statusLabels = {
  Unresolved: "미처리",
  Resolved: "처리 완료"
};

const noticeTypeLabels = {
  EXIT_WITHOUT_ENTRY: "입차기록 없는 출차 시도",
  VISIT_OVERDUE: "방문차량 등록시간 초과",
  UNKNOWN_OVERSTAY: "미등록차량 24시간 초과",
  OCR_REVIEW: "OCR 확인 필요"
};

const overstayNoticeTypes = [
  "VISIT_OVERDUE",
  "UNKNOWN_OVERSTAY"
];

const noticeNo = computed(() => route.params.noticeNo);
const currentNoticeNo = computed(() => Number(noticeNo.value));

const getField = (target, camelName, snakeName) => {
  return target?.[camelName] ?? target?.[snakeName] ?? null;
};

const getNoticeNo = (target) => {
  return getField(target, "noticeNo", "notice_no");
};

const getNoticeType = (target) => {
  return getField(target, "noticeType", "notice_type") ?? "";
};

const getAlertStat = (target) => {
  return getField(target, "alertStat", "alert_stat") ?? "Unresolved";
};

const getOutTime = (target) => {
  return getField(target, "outTime", "out_time");
};

const currentCarNo = computed(() => {
  return getField(
    notice.value,
    "registeredCarNo",
    "registered_car_no"
  ) ?? getField(
    notice.value,
    "capturedCarNo",
    "captured_car_no"
  ) ?? "";
});

const sameStatusNoticeList = computed(() => {
  const currentStatus = getAlertStat(notice.value);

  return noticeList.value.filter((item) => {
    return getAlertStat(item) === currentStatus;
  });
});

const currentIndex = computed(() => {
  return sameStatusNoticeList.value.findIndex((item) => {
    return Number(getNoticeNo(item)) === currentNoticeNo.value;
  });
});

const prevNotice = computed(() => {
  if (currentIndex.value <= 0) {
    return null;
  }

  return sameStatusNoticeList.value[currentIndex.value - 1];
});

const nextNotice = computed(() => {
  if (
    currentIndex.value < 0
    || currentIndex.value >= sameStatusNoticeList.value.length - 1
  ) {
    return null;
  }

  return sameStatusNoticeList.value[currentIndex.value + 1];
});

const canCompleteNotice = computed(() => {
  return Boolean(notice.value)
    && getAlertStat(notice.value) === "Unresolved"
    && !saving.value;
});

const detailRows = computed(() => {
  if (!notice.value) {
    return [];
  }

  const target = notice.value;
  const type = getNoticeType(target);
  const status = getAlertStat(target);
  const isOverstay = overstayNoticeTypes.includes(type);

  const rows = [
    {
      label: "알림 번호",
      value: getNoticeNo(target)
    },
    {
      label: "알림 종류",
      value: noticeTypeLabels[type] ?? type
    },
    {
      label: "등록 차량번호",
      value: getField(
        target,
        "registeredCarNo",
        "registered_car_no"
      )
    },
    {
      label: "촬영 차량번호",
      value: getField(
        target,
        "capturedCarNo",
        "captured_car_no"
      )
    },
    {
      label: "차량 구분",
      value: formatCarKind(
        getField(target, "carKind", "car_kind")
      )
    },
    {
      label: "주차장",
      value: getField(
        target,
        "parkingName",
        "parking_name"
      )
    }
  ];

  if (isOverstay) {
    rows.push(
      {
        label: "입차 일시",
        value: formatDate(
          getField(target, "inTime", "in_time")
        )
      },
      {
        label: "초과 기준 일시",
        value: formatDate(
          getField(target, "dueAt", "due_at")
          ?? getField(
            target,
            "expectedOutTime",
            "expected_out_time"
          )
        )
      },
      {
        label: "출차 일시",
        value: formatDate(getOutTime(target))
      }
    );
  }

  rows.push(
    {
      label: type === "EXIT_WITHOUT_ENTRY"
        ? "출차 시도 일시"
        : type === "OCR_REVIEW"
          ? "촬영·알림 발생 일시"
          : "알림 발생 일시",
      value: formatDate(
        getField(target, "detectAt", "detect_at")
      )
    },
    {
      label: "알림 발생 후 경과 일수",
      value: getField(target, "stayDays", "stay_days")
    },
    {
      label: "처리 상태",
      value: statusLabels[status] ?? status
    },
    {
      label: "처리 관리자",
      value: status === "Resolved"
        ? getField(
          target,
          "handledByMemberName",
          "handled_by_member_name"
        )
        : "-"
    }
  );

  if (status === "Resolved") {
    rows.push({
      label: "처리 완료 일시",
      value: formatDate(
        getField(target, "handledAt", "handled_at")
      )
    });
  }

  return rows;
});

function formatDate(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return String(value).replace("T", " ");
  }

  return date.toLocaleString("ko-KR");
}

function formatValue(value) {
  if (
    value === null
    || value === undefined
    || value === ""
  ) {
    return "-";
  }

  return value;
}

function formatCarKind(value) {
  const carKind = String(value ?? "").trim().toUpperCase();

  const labels = {
    NORMAL: "입주민 차량",
    REGISTERED: "입주민 차량",
    VISIT: "방문 차량",
    UNREGISTERED: "미등록 차량",
    UNKNOWN: "미등록 차량"
  };

  return labels[carKind] ?? value ?? "-";
}

function showFeedback(message, type = "success") {
  feedbackMessage.value = message;
  feedbackType.value = type;

  window.clearTimeout(feedbackTimer);

  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = "";
  }, 2500);
}

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";
  feedbackMessage.value = "";

  try {
    await noticeStore.loadNotice(noticeNo.value);
  } catch (error) {
    console.error(error);
    errorMessage.value =
      "알림 상세 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function loadNoticeList() {
  try {
    const response = await getNoteList();

    noticeList.value = Array.isArray(response.data)
      ? response.data
      : [];
  } catch (error) {
    console.error(error);
  }
}

function moveNotice(targetNotice) {
  const targetNoticeNo = getNoticeNo(targetNotice);

  if (!targetNoticeNo) {
    return;
  }

  router.push(`/admin/notice/${targetNoticeNo}`);
}

function goNoticeList() {
  router.push({
    name: "NoticeList",
    query: {
      status: getAlertStat(notice.value)
    }
  });
}

async function completeNotice() {
  if (!canCompleteNotice.value) {
    return;
  }

  const type = getNoticeType(notice.value);
  const outTime = getOutTime(notice.value);

  if (
    overstayNoticeTypes.includes(type)
    && !outTime
  ) {
    showFeedback(
      "해당 차량은 출차가 완료된 후 처리할 수 있습니다.",
      "error"
    );

    return;
  }

  saving.value = true;
  errorMessage.value = "";
  feedbackMessage.value = "";

  try {
    await noticeStore.changeNoticeStatus(
      getNoticeNo(notice.value),
      "Resolved"
    );

    showFeedback("처리 완료되었습니다.");
  } catch (error) {
    console.error(error);

    const message =
      error.response?.status === 409
        ? "현재 상태에서는 처리 완료할 수 없습니다."
        : "처리 완료 변경에 실패했습니다.";

    showFeedback(message, "error");
  } finally {
    saving.value = false;
  }
}

function deleteCurrentNotice() {
  if (notice.value) {
    deleteConfirmOpen.value = true;
  }
}

function cancelDelete() {
  if (!deleting.value) {
    deleteConfirmOpen.value = false;
  }
}

async function confirmDelete() {
  if (!notice.value || deleting.value) {
    return;
  }

  const targetNoticeNo = getNoticeNo(notice.value);
  const targetStatus = getAlertStat(notice.value);

  deleting.value = true;

  try {
    const removed = await noticeStore.remove(targetNoticeNo);

    if (removed) {
      deleteConfirmOpen.value = false;

      await router.push({
        name: "NoticeList",
        query: {
          status: targetStatus
        }
      });
    }
  } finally {
    deleting.value = false;
  }
}

onMounted(async () => {
  await loadNoticeList();
  await loadDetail();
});

onUnmounted(() => {
  window.clearTimeout(feedbackTimer);
});

watch(noticeNo, loadDetail);
</script>

<style scoped>
.notice-detail-page { box-sizing: border-box; width: 100%; min-height: calc(100dvh - 120px); margin: 0 auto; padding: 20px 0; display: flex; align-items: center; justify-content: center; }
.notice-detail-dialog { width: min(100%, 760px); overflow: hidden; text-align: center; border-radius: 10px; background: var(--bg-header); box-shadow: 0 20px 48px rgba(35, 52, 66, 0.18); }
.detail-header { width: 100%; margin: 0; padding: 22px 24px; display: flex; align-items: center; justify-content: space-between; gap: 16px; border-bottom: 1px solid var(--border-color); }
.detail-header h1 { margin: 0; }
.detail-actions { display: flex; align-items: center; gap: 8px; }
.detail-table { width: 100%; max-width: none; border: 0; border-radius: 0; border-collapse: collapse; box-shadow: none; }
.detail-table th { width: 190px; background: var(--bg-soft); }
.detail-table th, .detail-table td { padding: 10px 14px; text-align: center; }
.notice-detail-actions { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-color); background: #f8fafb; }
.notice-detail-dialog > p { margin: 0; padding: 32px 24px; text-align: center; }

@media (max-width: 760px) {
  .notice-detail-page { min-height: 0; padding: 12px 0; align-items: flex-start; }
  .detail-header { align-items: flex-start; flex-direction: column; }
  .detail-actions { width: 100%; flex-wrap: wrap; }
  .detail-table th { width: 140px; }
}
</style>