<template>
  <main class="notice-detail-page">
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

        <button type="button" @click="router.push('/admin/notice')">
          목록
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
      <table class="detail-table" border="1">
        <tbody>
          <tr v-for="row in detailRows" :key="row.label">
            <th>{{ row.label }}</th>
            <td>{{ formatValue(row.value) }}</td>
          </tr>
        </tbody>
      </table>

      <section class="status-panel">
        <button
          type="button"
          :disabled="!canResolveNotice"
          @click="resolveNotice"
        >
          {{ saving ? "처리 중" : "처리 완료" }}
        </button>
      </section>
    </template>

    <p v-else>
      조회할 알림이 없습니다.
    </p>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { getNoteList, updateNoticeStatus } from "@/features/notice/noticeApi";
import { useNoticeStore } from "./noticeStore";

const route = useRoute();
const router = useRouter();
const noticeStore = useNoticeStore();

const { notice, notices: noticeList } = storeToRefs(noticeStore);

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");

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

// 현재 알림이 목록에서 몇 번째인지 확인
const currentIndex = computed(() => {
  return noticeList.value.findIndex((item) => {
    const itemNo = item.noticeNo ?? item.notice_no;

    return Number(itemNo) === currentNoticeNo.value;
  });
});

// 이전 알림
const prevNotice = computed(() => {
  if (currentIndex.value <= 0) {
    return null;
  }

  return noticeList.value[currentIndex.value - 1];
});

// 다음 알림
const nextNotice = computed(() => {
  if (currentIndex.value < 0 || currentIndex.value >= noticeList.value.length - 1) {
    return null;
  }

  return noticeList.value[currentIndex.value + 1];
});

const canResolveNotice = computed(() => {
  return Boolean(notice.value) && notice.value.alertStat !== "Resolved" && !saving.value;
});

// 날짜 표시 형식
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

// 값이 없을 때 하이픈 표시
const formatValue = (value) => {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return value;
};

// 차량 구분 표시
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

// 백엔드 NoticeDTO에 맞춘 상세 항목
const detailRows = computed(() => {
  if (!notice.value) {
    return [];
  }

  return [
    { label: "알림 번호", value: notice.value.noticeNo },
    { label: "입출차 기록 번호", value: notice.value.carLogNo },
    { label: "등록 차량번호", value: notice.value.registeredCarNo },
    { label: "촬영 차량번호", value: notice.value.capturedCarNo },
    { label: "차량 구분", value: formatCarKind(notice.value.carKind) },
    { label: "주차장", value: notice.value.parkingName },
    { label: "입차 일시", value: formatDate(notice.value.inTime) },
    { label: "감지 일시", value: formatDate(notice.value.detectAt) },
    { label: "주차 일수", value: notice.value.stayDays },
    { label: "처리 상태", value: statusOptions[notice.value.alertStat] ?? notice.value.alertStat },
    { label: "처리 관리자 번호", value: notice.value.handledByMemberNo },
    { label: "처리 관리자", value: notice.value.handledByMemberName },
    { label: "처리 일시", value: formatDate(notice.value.handledAt) },
  ];
});

// 목록에서 현재 알림을 찾아 상세 정보로 사용
const loadDetail = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    await noticeStore.loadNotice(noticeNo.value);

    if (!notice.value) {
      return;
    }

    if (notice.value.alertStat === "Unresolved") {
      try {
        await updateNoticeStatus(notice.value.noticeNo, "Checked");
        notice.value.alertStat = "Checked";
      } catch (error) {
        console.error(error);
      }
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = "알림 상세 정보를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

// 이전·다음 알림 이동에 사용할 목록 조회
const loadNoticeList = async () => {
  try {
    const response = await getNoteList();

    noticeList.value = Array.isArray(response.data) ? response.data : [];
  } catch (error) {
    console.error(error);
  }
};

// 이전 또는 다음 알림으로 이동
const moveNotice = (targetNotice) => {
  const targetNoticeNo = targetNotice?.noticeNo ?? targetNotice?.notice_no;

  if (!targetNoticeNo) {
    return;
  }

  router.push(`/admin/notice/${targetNoticeNo}`);
};

const resolveNotice = async () => {
  if (!canResolveNotice.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";

  try {
    await updateNoticeStatus(notice.value.noticeNo, "Resolved");
    notice.value.alertStat = "Resolved";

    const target = noticeList.value.find((item) => {
      const itemNo = item.noticeNo ?? item.notice_no;

      return Number(itemNo) === Number(notice.value.noticeNo);
    });

    if (target) {
      target.alertStat = "Resolved";
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = "처리 완료 변경에 실패했습니다.";
  } finally {
    saving.value = false;
  }
};

onMounted(async () => {
  await loadNoticeList();
  await loadDetail();
});

// 이전 또는 다음 알림으로 이동하면 상세 정보를 다시 조회
watch(noticeNo, async () => {
  await loadDetail();
});
</script>
