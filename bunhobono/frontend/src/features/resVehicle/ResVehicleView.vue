<template>
  <div class="resident-vehicle-management">
    <h2 v-if="mode === 'list'">차량관리</h2>

    <div v-if="mode === 'list'" class="resident-vehicle-member">
      {{ resVehicleStore.member.memName }}
      ({{ resVehicleStore.member.loginId }})
      {{ resVehicleStore.member.memDong }}동
      {{ resVehicleStore.member.memHo }}호
    </div>

    <!-- 본인 등록 차량: 조회만 가능 -->
    <section v-if="mode === 'list'" class="vehicle-management-section">
      <h3>본인 차량</h3>

      <ResVehicleList
        :vehicles="resVehicleStore.normalVehicles"
        empty-message="등록된 본인 차량이 없습니다."
        :show-manage="false"
      />
    </section>

    <!-- 방문 차량: 신청 가능 -->
    <section v-if="mode === 'list'" class="vehicle-management-section vehicle-management-visit-section">
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
        승인 대기 또는 사용 중인 방문차량이 있어 추가 신청할 수 없습니다.
      </div>

      <ResVehicleList
        :vehicles="resVehicleStore.visitVehicles"
        empty-message="신청한 방문차량이 없습니다."
        :show-manage="false"
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
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useResVehicleStore } from "./resVehicleStore";
import ResVehicleList from "./components/ResVehicleList.vue";
import ResVehicleForm from "./components/ResVehicleForm.vue";

const resVehicleStore = useResVehicleStore();
const route = useRoute();

// 초기화면 -> '방문차량등록'버튼 눌렀을 때, 차량등록 폼만 나오도록 함
const mode = ref(route.query.mode === "form" ? "form" : "list");

onMounted(async () => {
  await resVehicleStore.loadMyInfo();
  await resVehicleStore.loadVehicleList();
});

function openList() {
  mode.value = "list";
}

function openInsert() {
  mode.value = "form";
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

.resident-vehicle-member {
  color: var(--text-muted);
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
</style>
