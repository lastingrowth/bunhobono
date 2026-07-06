<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getNoteDetail, getNoteList, updateNoticeStatus } from "@/features/notice/noticeApi";

const route = useRoute();
const router = useRouter();

const notice = ref(null);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const statusDraft = ref("Unresolved");
const noticeList = ref([]);

const statusOptions = {
  Unresolved: "미확인",
  Checked: "확인",
  Resolved: "처리완료",
};

const statusSelectOptions = [
  { value: "Unresolved", label: "미확인" },
  { value: "Checked", label: "확인" },
  { value: "Resolved", label: "처리완료" },
];

const noticeNo = computed(() => route.params.noticeNo);

const currentNoticeNo = computed(() => Number(noticeNo.value));

const currentIndex = computed(() => {
  return noticeList.value.findIndex((item) => {
    return Number(item.noticeNo ?? item.notice_no) === currentNoticeNo.value;
  });
});

const prevNotice = computed(() => {
  if (currentIndex.value <= 0) {
    return null;
  }

  return noticeList.value[currentIndex.value - 1];
});

const nextNotice = computed(() => {
  if (currentIndex.value < 0 || currentIndex.value >= noticeList.value.length - 1) {
    return null;
  }

  return noticeList.value[currentIndex.value + 1];
});

const canChangeStatus = computed(() => {
  return notice.value?.paymentStatus === "SUCCESS";
});

const isResolvedBlocked = computed(() => {
  return statusDraft.value === "Resolved" && !canChangeStatus.value;
});

const canSaveStatus = computed(() => {
  return Boolean(notice.value) && !saving.value && !isResolvedBlocked.value;
});

const isStatusOptionDisabled = (value) => {
  return value === "Resolved" && !canChangeStatus.value;
};

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

const detailRows = computed(() => {
  if (!notice.value) {
    return [];
  }

  return [
    { label: "알림번호", value: notice.value.noticeNo },
    { label: "주차장번호", value: notice.value.parkingNo },
    { label: "차량번호", value: notice.value.plateNo },
    { label: "입차일시", value: formatDate(notice.value.entryAt) },
    { label: "감지일시", value: formatDate(notice.value.detectAt) },
    { label: "주차일수", value: notice.value.stayDays },
    { label: "처리상태", value: statusOptions[notice.value.alertStat] ?? notice.value.alertStat },
    { label: "차량유형", value: notice.value.vehicleType },
    { label: "차량상태", value: notice.value.vehicleStatus },
    { label: "만료상태", value: notice.value.expireStatus },
    { label: "등록시작", value: formatDate(notice.value.startDate) },
    { label: "등록만료", value: formatDate(notice.value.endDate) },
    { label: "청구금액", value: formatMoney(notice.value.amount) },
    { label: "청구상태", value: notice.value.chargeStatus },
    { label: "결제상태", value: notice.value.paymentStatus },
    { label: "결제일시", value: formatDate(notice.value.paidAt) },
  ];
});

const formatMoney = (value) => {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return `${Number(value).toLocaleString()}원`;
};

const loadDetail = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getNoteDetail(noticeNo.value);
    notice.value = response.data;
    statusDraft.value = notice.value?.alertStat ?? "Unresolved";
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

const saveStatus = async () => {
  if (!canSaveStatus.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";

  try {
    await updateNoticeStatus(notice.value.noticeNo, statusDraft.value);
    notice.value.alertStat = statusDraft.value;

    if (statusDraft.value === "Resolved") {
      alert("처리완료 되었습니다.");
    } else if (statusDraft.value === "Unresolved") {
      alert("미확인 되었습니다.");
    } else {
      alert("확인되었습니다.");
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = "처리상태 변경에 실패했습니다.";
  } finally {
    saving.value = false;
  }
};

onMounted(async () => {
  await loadNoticeList();
  await loadDetail();
});

watch(noticeNo, () => {
  loadDetail();
});
</script>

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

        <button type="button" @click="router.push('/admin/notice')">목록</button>
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

      <section class="status-panel">
        <label for="alert-status">처리상태 변경</label>

        <select
          id="alert-status"
          v-model="statusDraft"
          :disabled="saving"
        >
          <option
            v-for="option in statusSelectOptions"
            :key="option.value"
            :value="option.value"
            :disabled="isStatusOptionDisabled(option.value)"
          >
            {{ option.label }}
          </option>
        </select>

        <button
          type="button"
          :disabled="!canSaveStatus"
          @click="saveStatus"
        >
          {{ saving ? "저장중" : "저장" }}
        </button>

        <p v-if="!canChangeStatus" class="status-help">
          결제가 완료되지 않은 알림은 처리완료로 변경할 수 없습니다.
        </p>
      </section>
    </template>

    <p v-else>조회된 알림이 없습니다.</p>
  </main>
</template>
