<template>
  <div class="resident-vehicle-management">
    <div v-if="mode !== 'form'" class="resident-vehicle-header">
      <h2>차량관리</h2>

      <div class="resident-vehicle-header-actions">
        <button
          v-if="mode === 'list'"
          type="button"
          @click="openNotifications"
        >
          차량 알림
        </button>

        <button
          v-if="mode === 'notification'"
          type="button"
          @click="openList"
        >
          차량 목록
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
      {{ resVehicleStore.member.memDong }}동
      {{ resVehicleStore.member.memHo }}호
    </div>

    <template v-if="mode === 'list'">
      <section class="vehicle-management-section">
        <h3>본인 차량</h3>

        <ResVehicleList
          :vehicles="resVehicleStore.normalVehicles"
          empty-message="관리실에서 등록이 가능합니다. 문의"
          :show-manage="false"
        />
      </section>

      <section
        class="vehicle-management-section
               vehicle-management-visit-section"
      >
        <div class="vehicle-management-section-header">
          <h3>방문 차량</h3>

          <button
            :disabled="resVehicleStore.hasActiveVisitVehicle"
            @click="openInsert"
          >
            방문차량 신청
          </button>
        </div>

        <div v-if="resVehicleStore.hasActiveVisitVehicle">
          승인 대기 또는 사용 중인 방문차량이 있어
          추가 신청할 수 없습니다.
        </div>

        <ResVehicleList
          :vehicles="resVehicleStore.visitVehicles"
          empty-message="신청한 방문차량이 없습니다."
          :show-manage="false"
        />
      </section>
    </template>

    <section
      v-else-if="mode === 'notification'"
      class="vehicle-management-section"
    >
      <ResVehicleNt
        :notifications="resVehicleStore.notifications"
      />
    </section>

    <ResVehicleForm
      v-else
      @submit="submitVisitVehicle"
      @cancel="openList"
    />
  </div>
</template>

<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  watch
} from "vue";
import { useRoute, useRouter } from "vue-router";

import { useResVehicleStore } from "./resVehicleStore";
import ResVehicleForm from "./components/ResVehicleForm.vue";
import ResVehicleList from "./components/ResVehicleList.vue";
import ResVehicleNt from "./components/ResVehicleNt.vue";

const resVehicleStore = useResVehicleStore();
const route = useRoute();
const router = useRouter();

let refreshTimer;

const mode = computed(() => {
  if (route.query.mode === "form") {
    return "form";
  }

  if (route.query.mode === "notification") {
    return "notification";
  }

  return "list";
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
});

// 차량관리 화면 안에서 알림 화면으로 이동했을 때 읽음 처리
watch(mode, async (newMode) => {
  if (newMode === "notification") {
    await resVehicleStore.loadNotifications();
    await resVehicleStore.markAllNotificationsRead();
  }
});

async function refreshData() {
  await Promise.all([
    resVehicleStore.loadVehicleList(),
    resVehicleStore.loadNotifications()
  ]);

  if (mode.value === "form" && resVehicleStore.hasActiveVisitVehicle) {
    openList();
    return;
  }

  if (mode.value === "notification") {
    await resVehicleStore.markAllNotificationsRead();
  }
}

function openList() {
  router.replace("/resident/vehicles");
}

function openInsert() {
  router.replace({
    path: "/resident/vehicles",
    query: { mode: "form" }
  });
}

function openNotifications() {
  router.replace({
    path: "/resident/vehicles",
    query: { mode: "notification" }
  });
}

function goDashboard() {
  router.push("/resident/dashboard");
}

async function submitVisitVehicle(data) {
  await resVehicleStore.addVisitVehicle(data);
  openList();
}
</script>

<style scoped>
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
  color: #287fd5;
}

.vehicle-management-visit-section {
  border-color: #bfe8cf;
}

.vehicle-management-visit-section h3 {
  color: #2ca66a;
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

@media (max-width: 600px) {
  .vehicle-management-section {
    padding: 18px;
  }

  .vehicle-management-section-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

.resident-vehicle-header-actions { display: flex; align-items: center; gap: 8px; }
</style>
