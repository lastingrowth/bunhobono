<template>
  <main class="notice-detail-page">
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
    <section class="notice-detail-dialog">
    <div class="detail-header">
      <h1>알림 상세</h1>

      <div class="detail-actions">
        <button
          type="button"
          :disabled="!prevNotice"
          @click="moveNotice(prevNotice)"
        >
          이전
        </button>

        <button
          type="button"
          :disabled="!nextNotice"
          @click="moveNotice(nextNotice)"
        >
          다음
        </button>

        <button type="button" @click="goNoticeList">
          목록
        </button>

        <button type="button" @click="deleteCurrentNotice">
          삭제
        </button>
      </div>
    </div>

    <p v-if="loading">
      불러오는 중...
    </p>

    <p v-else-if="errorMessage">
      {{ errorMessage }}
    </p>

    <template v-else-if="notice">
      <p v-if="feedbackMessage" class="notice-feedback-message">
        {{ feedbackMessage }}
      </p>

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

    <p v-else>
      조회할 알림이 없습니다.
    </p>
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
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { getNoteList } from "@/features/notice/noticeApi";
import { useNoticeStore } from "./noticeStore";
import NoticeDeleteConfirm from "./NoticeDeleteConfirm.vue";

const route = useRoute();
const router = useRouter();
const noticeStore = useNoticeStore();

const { notice, notices: noticeList } = storeToRefs(noticeStore);

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const feedbackMessage = ref("");
const deleteConfirmOpen = ref(false);
const deleting = ref(false);

const currentCarNo = computed(() => {
  return notice.value?.registeredCarNo
    ?? notice.value?.registered_car_no
    ?? notice.value?.capturedCarNo
    ?? notice.value?.captured_car_no
    ?? "";
});

const statusOptions = {
  Unresolved: "미확인",
  Checked: "확인",
  Resolved: "처리 완료",
};

const noticeNo = computed(() => {
  return route.params.noticeNo;
});

const currentNoticeNo = computed(() => {
  return Number(noticeNo.value);
});

// 상세 화면에서는 현재 알림과 처리 상태가 같은 항목끼리만 이동한다.
// 예: 확인 상태에서는 확인 알림만, 처리 완료에서는 처리 완료 알림만 표시한다.
const sameStatusNoticeList = computed(() => {
  const currentStatus = notice.value?.alertStat ?? notice.value?.alert_stat;

  if (!currentStatus) {
    return [];
  }

  return noticeList.value.filter((item) => {
    const itemStatus = item.alertStat ?? item.alert_stat;
    return itemStatus === currentStatus;
  });
});

const currentIndex = computed(() => {
  return sameStatusNoticeList.value.findIndex((item) => {
    const itemNo = item.noticeNo ?? item.notice_no;

    return Number(itemNo) === currentNoticeNo.value;
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
  return Boolean(notice.value) && notice.value.alertStat !== "Resolved" && !saving.value;
});

const formatDate = (value) => {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("ko-KR");
};

const formatValue = (value) => {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return value;
};

const formatCarKind = (value) => {
  if (value === "NORMAL") {
    return "입주민 차량";
  }

  if (value === "VISIT") {
    return "방문 차량";
  }

  if (value === "UNREGISTERED") {
    return "미등록 차량";
  }

  return value;
};

const detailRows = computed(() => {
  if (!notice.value) {
    return [];
  }

  return [
    { label: "알림 번호", value: notice.value.noticeNo },
    { label: "등록 차량번호", value: notice.value.registeredCarNo },
    { label: "촬영 차량번호", value: notice.value.capturedCarNo },
    { label: "차량 구분", value: formatCarKind(notice.value.carKind) },
    { label: "주차장", value: notice.value.parkingName },
    { label: "입차 일시", value: formatDate(notice.value.inTime) },
    { label: "출차 일시", value: formatDate(notice.value.outTime) },
    { label: "감지 일시", value: formatDate(notice.value.detectAt) },
    { label: "주차 일수", value: notice.value.stayDays },
    { label: "처리 상태", value: statusOptions[notice.value.alertStat] ?? notice.value.alertStat },
    { label: "처리 관리자", value: notice.value.handledByMemberName },
  ];
});

const loadDetail = async () => {
  loading.value = true;
  errorMessage.value = "";
  feedbackMessage.value = "";

  try {
    await noticeStore.loadNotice(noticeNo.value);

    if (!notice.value) {
      return;
    }

    if (notice.value.alertStat === "Unresolved") {
      await noticeStore.changeNoticeStatus(notice.value.noticeNo, "Checked");
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = "알림 상세 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const loadNoticeList = async () => {
  try {
    const response = await getNoteList();

    noticeList.value = Array.isArray(response.data) ? response.data : [];
  } catch (error) {
    console.error(error);
  }
};

const moveNotice = (targetNotice) => {
  const targetNoticeNo = targetNotice?.noticeNo ?? targetNotice?.notice_no;

  if (!targetNoticeNo) {
    return;
  }

  router.push(`/admin/notice/${targetNoticeNo}`);
};

const getCurrentNoticeStatus = () => {
  return notice.value?.alertStat ?? notice.value?.alert_stat ?? "Unresolved";
};

const goNoticeList = () => {
  router.push({
    name: "NoticeList",
    query: {
      status: getCurrentNoticeStatus(),
    },
  });
};

const completeNotice = async () => {
  if (!canCompleteNotice.value) {
    return;
  }

  if (!notice.value.outTime) {
    feedbackMessage.value = "해당 차량이 여전히 주차 중이어서 처리 완료할 수 없습니다.";
    return;
  }

  saving.value = true;
  errorMessage.value = "";
  feedbackMessage.value = "";

  try {
    await noticeStore.changeNoticeStatus(notice.value.noticeNo, "Resolved");
    feedbackMessage.value = "처리 완료되었습니다.";
  } catch (error) {
    console.error(error);
    errorMessage.value = "처리 완료 변경에 실패했습니다.";
  } finally {
    saving.value = false;
  }
};

const deleteCurrentNotice = () => {
  if (!notice.value) {
    return;
  }

  deleteConfirmOpen.value = true;
};

const cancelDelete = () => {
  if (!deleting.value) {
    deleteConfirmOpen.value = false;
  }
};

const confirmDelete = async () => {
  if (!notice.value || deleting.value) {
    return;
  }

  const targetNoticeNo = notice.value.noticeNo ?? notice.value.notice_no;
  const targetStatus = getCurrentNoticeStatus();
  deleting.value = true;

  try {
    const removed = await noticeStore.remove(targetNoticeNo);

    if (removed) {
      deleteConfirmOpen.value = false;
      await router.push({ name: "NoticeList", query: { status: targetStatus } });
    }
  } finally {
    deleting.value = false;
  }
};

onMounted(async () => {
  await loadNoticeList();
  await loadDetail();
});

watch(noticeNo, async () => {
  await loadDetail();
});
</script>

<style scoped>
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

.notice-detail-page {
  width: 100%;
  max-width: none;
  min-height: calc(100dvh - 120px);
  margin: 0 auto;
  padding: 20px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.notice-detail-dialog {
  width: min(100%, 760px);
  overflow: hidden;
  text-align: center;
  border-radius: 10px;
  background: var(--bg-header);
  box-shadow: 0 20px 48px rgba(35, 52, 66, 0.18);
}

.detail-header {
  width: 100%;
  margin: 0;
  padding: 22px 24px;
  border-bottom: 1px solid var(--border-color);
}

.detail-table {
  width: 100%;
  max-width: none;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.detail-table th {
  width: 190px;
}

.detail-table th,
.detail-table td {
  text-align: center;
}

.notice-detail-actions {
  padding: 18px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-color);
  background: #f8fafb;
}

.notice-detail-dialog > p {
  margin: 0;
  padding: 32px 24px;
  text-align: center;
}

.notice-detail-dialog > .notice-feedback-message {
  padding: 10px 18px;
  color: #315c45;
  font-size: 13px;
  font-weight: 800;
  background: #eaf6ef;
  border-bottom: 1px solid #cde6d7;
}

@media (max-width: 760px) {
  .notice-detail-page {
    min-height: 0;
    padding: 12px 0;
    align-items: flex-start;
  }

  .detail-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .detail-table th {
    width: 140px;
  }
}
</style>
