<template>
  <div class="resident-vehicle-management">
    <ManagementFeedbackToast
      :message="feedbackMessage"
      :type="feedbackType"
    />

    <div
      v-if="mode !== 'form'"
      class="resident-vehicle-header"
      :class="{
        'notification-mode': mode === 'notification',
        'list-mode': mode === 'list'
      }"
    >
      <h2>{{ mode.startsWith('notification') ? '알림' : '차량관리' }}</h2>

      <div class="resident-vehicle-header-actions">
        <button
          v-if="mode === 'list'"
          type="button"
          @click="openNotifications"
        >
          차량 알림
        </button>


        <button
          type="button"
          class="back-to-list-button"
          @click="goDashboard"
        >
          홈으로 돌아가기
        </button>
      </div>
    </div>

    <div v-if="mode === 'list'" class="resident-vehicle-member">
      {{ resVehicleStore.member.memName }}
      ({{ resVehicleStore.member.loginId }})
      {{ resVehicleStore.member.dong }}동
      {{ resVehicleStore.member.ho }}호
    </div>

    <template v-if="mode === 'list'">
      <section class="vehicle-management-section">
        <h3>본인 차량</h3>

        <ResVehicleList
          :vehicles="resVehicleStore.normalVehicles"
          empty-message="관리실에서 등록이 가능합니다."
          empty-action-label="문의"
          :show-manage="false"
          :show-extend="true"
          @extend-normal="openNormalExtension"
          @empty-action="scrollToResidentContact"
        />
      </section>

      <section
        class="vehicle-management-section
               vehicle-management-visit-section"
      >
        <div class="vehicle-management-section-header">
          <h3>방문 차량</h3>

          <div class="visit-application-actions">
            <small>(방문차량 조회는 3개월까지만 가능합니다.)</small>
            <button @click="openInsert">
              방문차량 신청
            </button>
          </div>
        </div>

        <ResVehicleList
          :vehicles="visibleVisitVehicles"
          empty-message="신청한 방문차량이 없습니다."
          :show-manage="false"
          :show-cancel="true"
          @edit-visit-time="openVisitTimeEdit"
          @cancel-visit="cancelVisitVehicle"
        />
      </section>
    </template>

    <section
      v-else-if="mode === 'notification'"
      class="vehicle-management-section notification-list-section"
    >
      <ResMemNotice
        :notifications="resVehicleStore.notifications"
        @open-detail="openNotificationDetail"
        @delete="deleteNotification"
      />
    </section>

    <section
      v-else-if="mode === 'notification-detail'"
      class="vehicle-management-section"
    >
      <ResMemNoticeDetail
        :notification="selectedNotification"
        @back="openNotifications"
        @delete="deleteDetailNotification"
        @pay="openResidentBillPayment"
      />
    </section>

    <ResNormalVehicleExtendForm
      v-else-if="mode === 'extend-normal'"
      :vehicle="extendingNormalVehicle"
      @submit="submitNormalExtension"
      @cancel="openList"
    />

    <ResVehicleForm
      v-else
      :edit-vehicle="editingVisitVehicle"
      @submit="submitVisitVehicle"
      @cancel="openList"
    />

    <ManagementConfirm
      :open="cancelConfirmOpen"
      title="방문차량 등록 취소"
      :item-name="cancelTarget?.carNo || ''"
      message="방문차량 등록을 취소하시겠습니까?"
      caution="입차 전 차량만 등록 취소할 수 있습니다."
      :processing="cancelProcessing"
      confirm-text="등록 취소"
      processing-text="취소 중"
      @cancel="closeCancelConfirm"
      @confirm="confirmCancelVisit"
    />

    <div
      v-if="paymentRequiredOpen"
      class="resident-notification-backdrop"
      @click.self="closePaymentRequired"
    >
      <section
        class="resident-notification-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="visit-credit-notification-title"
      >
        <div class="resident-notification-heading">
          <span aria-hidden="true"></span>
          <h2 id="visit-credit-notification-title">방문차량 추가 등록</h2>
        </div>

        <p>
          이번 달 사용 가능한 방문차량 등록 횟수를 모두 사용했습니다.
          추가 등록을 위해 횟수를 충전해 주세요.
        </p>
        <small>결제가 완료되면 결제한 수량만큼 추가 등록할 수 있습니다.</small>

        <div class="resident-notification-actions">
          <button type="button" @click="closePaymentRequired">나중에</button>
          <button
            type="button"
            class="resident-notification-primary"
            @click="openVisitCreditCharge"
          >
            횟수 충전
          </button>
        </div>
      </section>
    </div>
  </div>


</template>

<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from "vue";
import { useRoute, useRouter } from "vue-router";

import { useResVehicleStore } from "./resVehicleStore";
import { getMonthlyVisitRegistration } from "./resVehicleApi";
import ResVehicleForm from "./components/ResVehicleForm.vue";
import ResVehicleList from "./components/ResVehicleList.vue";
import ResMemNotice from "./components/ResMemNotice.vue";
import ResMemNoticeDetail from "./components/ResMemNoticeDetail.vue";
import ResNormalVehicleExtendForm from "./components/ResNormalVehicleExtendForm.vue";
import ManagementConfirm from "@/shared/components/ManagementConfirm.vue";
import ManagementFeedbackToast from "@/shared/components/ManagementFeedbackToast.vue";

const resVehicleStore = useResVehicleStore();
const route = useRoute();
const router = useRouter();

let refreshTimer;
let feedbackTimer;

const cancelTarget = ref(null);
const cancelConfirmOpen = ref(false);
const cancelProcessing = ref(false);
const paymentRequiredOpen = ref(false);
const feedbackMessage = ref("");
const feedbackType = ref("success");

const mode = computed(() => {
  if (route.query.mode === "form") {
    return "form";
  }

  if (route.query.mode === "edit-time") {
    return "edit-time";
  }

  if (route.query.mode === "extend-normal") {
    return "extend-normal";
  }

  if (route.query.mode === "notification") {
    return "notification";
  }

  if (route.query.mode === "notification-detail") {
    return "notification-detail";
  }

  return "list";
});

const visibleVisitVehicles = computed(() => {
  const cutoff = new Date();
  cutoff.setMonth(cutoff.getMonth() - 3);

  return resVehicleStore.visitVehicles.filter((vehicle) => {
    const referenceTime = vehicle.outTime
      || vehicle.realEndDate
      || vehicle.endDate
      || vehicle.startDate
      || vehicle.approvedAt;

    if (!referenceTime) {
      return true;
    }

    const referenceDate = new Date(referenceTime);
    return !Number.isNaN(referenceDate.getTime()) && referenceDate >= cutoff;
  });
});

const editingVisitVehicle = computed(() => {
  if (mode.value !== "edit-time") {
    return null;
  }

  return resVehicleStore.visitVehicles.find((vehicle) => {
    return Number(vehicle.vehicleCarNo) === Number(route.query.vehicleCarNo);
  }) || null;
});

const extendingNormalVehicle = computed(() => {
  if (mode.value !== "extend-normal") {
    return null;
  }

  return resVehicleStore.normalVehicles.find((vehicle) => {
    return Number(vehicle.vehicleCarNo) === Number(route.query.vehicleCarNo);
  }) || null;
});

const selectedNotification = computed(() => {
  if (mode.value !== "notification-detail") return null;
  return resVehicleStore.notifications.find((item) => {
    return Number(item.memNoticeNo) === Number(route.query.memNoticeNo);
  }) || null;
});

onMounted(async () => {
  await resVehicleStore.loadMyInfo();
  await refreshData();

  refreshTimer = window.setInterval(() => {
    refreshData();
  }, 30000);
});

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer);
  window.clearTimeout(feedbackTimer);
});

// 차량관리 화면 안에서 알림 화면으로 이동했을 때 읽음 처리
watch(mode, async (newMode) => {
  if (newMode === "notification") {
    await resVehicleStore.loadNotifications();
  }
});

async function refreshData() {
  await Promise.all([
    resVehicleStore.loadVehicleList(),
    resVehicleStore.loadNotifications()
  ]);

}

function openList() {
  router.replace("/resident/vehicles");
}

async function openInsert() {
  try {
    const response = await getMonthlyVisitRegistration();

    if (response.data.remainingCount <= 0) {
      paymentRequiredOpen.value = true;
      return;
    }

    router.replace({
      path: "/resident/vehicles",
      query: { mode: "form" }
    });
  } catch (error) {
    showFeedback(
      error.response?.data?.message
      || "방문차량 등록 가능 횟수를 확인하지 못했습니다.",
      "error"
    );
  }
}

function openNotifications() {
  router.replace({
    path: "/resident/vehicles",
    query: { mode: "notification" }
  });
}

function openVisitTimeEdit(vehicle) {
  router.replace({
    path: "/resident/vehicles",
    query: {
      mode: "edit-time",
      vehicleCarNo: vehicle.vehicleCarNo
    }
  });
}

function openNormalExtension(vehicle) {
  router.replace({
    path: "/resident/vehicles",
    query: {
      mode: "extend-normal",
      vehicleCarNo: vehicle.vehicleCarNo
    }
  });
}

async function submitNormalExtension(endDate) {
  if (!extendingNormalVehicle.value) return;

  const vehicle = extendingNormalVehicle.value;

  try {
    await resVehicleStore.extendNormalVehicle(vehicle.vehicleCarNo, endDate);
    openList();
    showFeedback(`${vehicle.carNo} 등록기간을 연장했습니다.`);
  } catch (error) {
    showFeedback(
      error.response?.data?.message || "차량 등록기간을 연장하지 못했습니다.",
      "error"
    );
  }
}

function goDashboard() {
  router.push("/resident/dashboard");
}

function scrollToResidentContact() {
  document.getElementById("resident-contact")?.scrollIntoView({
    behavior: "smooth",
    block: "center"
  });
}

async function submitVisitVehicle(data) {
  try {
    if (mode.value === "edit-time" && editingVisitVehicle.value) {
      await resVehicleStore.updateVisitVehicleTime(
        editingVisitVehicle.value.vehicleCarNo,
        data
      );
      showFeedback(`${editingVisitVehicle.value.carNo} 방문시간을 변경했습니다.`);
    } else {
      await resVehicleStore.addVisitVehicle(data);
    }
    openList();
  } catch (error) {
    if (error.response?.status === 402) {
      paymentRequiredOpen.value = true;
      return;
    }

    showFeedback(
      error.response?.data?.message
      || (mode.value === "edit-time"
        ? "방문시간을 변경하지 못했습니다."
        : "방문차량을 등록하지 못했습니다."),
      "error"
    );
  }
}

function closePaymentRequired() {
  paymentRequiredOpen.value = false;
}

function openVisitCreditCharge() {
  paymentRequiredOpen.value = false;
  router.push({ name: "MemPurchase" });
}

function cancelVisitVehicle(vehicle) {
  cancelTarget.value = vehicle;
  cancelConfirmOpen.value = true;
}

function closeCancelConfirm() {
  if (cancelProcessing.value) {
    return;
  }

  cancelConfirmOpen.value = false;
  cancelTarget.value = null;
}

function showFeedback(message, type = "success") {
  feedbackMessage.value = message;
  feedbackType.value = type;
  window.clearTimeout(feedbackTimer);
  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = "";
  }, 2500);
}

async function confirmCancelVisit() {
  if (!cancelTarget.value || cancelProcessing.value) {
    return;
  }

  const vehicle = cancelTarget.value;
  cancelProcessing.value = true;

  try {
    await resVehicleStore.cancelVisitVehicle(vehicle.vehicleCarNo);
    cancelConfirmOpen.value = false;
    cancelTarget.value = null;
    showFeedback(`${vehicle.carNo} 방문차량 등록을 취소했습니다.`);
  } catch (error) {
    showFeedback(
      error.response?.data?.message
      || "방문차량 등록을 취소하지 못했습니다.",
      "error"
    );
  } finally {
    cancelProcessing.value = false;
  }
}

async function readNotification(notification) {
  await resVehicleStore.readNotification(notification)
}

async function openNotificationDetail(notification) {
  await readNotification(notification);
  router.replace({
    path: "/resident/vehicles",
    query: {
      mode: "notification-detail",
      memNoticeNo: notification.memNoticeNo
    }
  });
}

async function deleteNotification(memNoticeNo) {
  await resVehicleStore.removeNotification(memNoticeNo)
}

async function deleteDetailNotification(memNoticeNo) {
  await deleteNotification(memNoticeNo);
  openNotifications();
}

function openResidentBillPayment(billNo) {
  router.push({
    name: "ResidentBillPay",
    params: { billNo }
  });
}
</script>

<style scoped>
:global(.resident-layout .content > .resident-vehicle-management) {
  width: min(1120px, calc(100% - 200px));
  max-width: 1120px;
}

.resident-vehicle-management {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.resident-vehicle-management h2,
.vehicle-management-section h3 {
  margin: 0;
}

.resident-vehicle-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.back-to-list-button {
  min-width: 88px;
}

.resident-vehicle-member {
  padding: 12px 16px;
  border-left: 5px solid #45bff2;
  color: #315c86;
  background: #eef9ff;
  font-weight: 700;
}

.vehicle-management-section {
  padding: 22px 24px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--bg-header);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.vehicle-management-section h3 {
  margin-bottom: 16px;
  color: var(--resident-accent);
}

.vehicle-management-visit-section {
  border-color: #bfe8cf;
}

.vehicle-management-visit-section h3 {
  color: var(--resident-accent);
}

.vehicle-management-section-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.vehicle-management-section-header h3 {
  margin-bottom: 0;
}

.visit-application-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.visit-application-actions small {
  color: #708698;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.vehicle-management-section-header button {
  border-color: #42d77d;
  color: #ffffff;
  background: #42d77d;
}

.vehicle-management-section-header button:hover:not(:disabled) {
  background: #2fc86b;
}

.vehicle-management-section-header button:disabled {
  border-color: #bdd8c8;
  color: #789487;
  background: #e7f2ec;
}

.vehicle-management-visit-section > div:nth-child(2) {
  margin-bottom: 14px;
  color: var(--text-muted);
}

.resident-notification-backdrop {
  position: fixed;
  z-index: 1200;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 16px;
  background: rgba(19, 35, 51, 0.48);
}

.resident-notification-dialog {
  width: min(430px, calc(100vw - 32px));
  padding: 26px;
  border: 1px solid #cbd8e5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 20px 55px rgba(20, 48, 74, 0.26);
}

.resident-notification-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.resident-notification-heading span {
  width: 5px;
  height: 24px;
  border-radius: 2px;
  background: #2387d9;
}

.resident-notification-heading h2 {
  margin: 0;
  color: #18344e;
  font-size: 21px;
}

.resident-notification-dialog p {
  margin: 22px 0 8px;
  color: #526b80;
  line-height: 1.7;
}

.resident-notification-dialog small {
  display: block;
  color: #708698;
  line-height: 1.6;
}

.resident-notification-actions {
  margin-top: 26px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.resident-notification-actions button {
  min-width: 86px;
  height: 38px;
  border: 1px solid #cad7e3;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.resident-notification-actions .resident-notification-primary {
  border-color: #2387d9;
  color: #fff;
  background: #2387d9;
}

@media (max-width: 600px) {
  :global(.resident-layout .content > .resident-vehicle-management) {
    width: calc(100% - 24px);
  }

  .resident-vehicle-header { align-items: flex-start; flex-direction: column; gap: 12px; }
  .resident-vehicle-header-actions { display: none; }

  .vehicle-management-section {
    padding: 16px 12px;
  }

  .notification-list-section {
    padding: 0;
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
  }

  .vehicle-management-section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .visit-application-actions { width: 100%; align-items: stretch; flex-direction: column-reverse; }
  .visit-application-actions button { width: 100%; min-height: 44px; }
}

.resident-vehicle-header-actions { display: flex; align-items: center; gap: 8px; }

@media (max-width: 760px) {
  .resident-vehicle-header-actions { display: none !important; }
  .resident-vehicle-header:is(.notification-mode,.list-mode) { display: none; }
  .resident-vehicle-member { display: none; }
}

@media (min-width: 761px) and (max-width: 900px) {
  :global(.resident-layout .content > .resident-vehicle-management) {
    width: calc(100% - 36px);
  }
}
</style>
