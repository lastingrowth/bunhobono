<template>
  <div>
    <h2>내 차량 관리</h2>

    <div>
      {{ resVehicleStore.member.memName }}
      ({{ resVehicleStore.member.loginId }})
      {{ resVehicleStore.member.memDong }}동
      {{ resVehicleStore.member.memHo }}호
    </div>

    <!-- 본인 등록 차량: 조회만 가능 -->
    <section>
      <h3>본인 차량</h3>

      <ResVehicleList
        :vehicles="resVehicleStore.normalVehicles"
        empty-message="등록된 본인 차량이 없습니다."
        :show-manage="false"
      />
    </section>

    <hr>

    <!-- 방문 차량: 신청 가능 -->
    <section>
      <div>
        <h3>방문 차량</h3>

        <button
          v-if="mode === 'list'"
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
        v-if="mode === 'list'"
        :vehicles="resVehicleStore.visitVehicles"
        empty-message="신청한 방문차량이 없습니다."
        :show-manage="false"
      />

      <ResVehicleForm
        v-if="mode === 'form'"
        @submit="submitVisitVehicle"
        @cancel="openList"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useResVehicleStore } from "./resVehicleStore";
import ResVehicleList from "./components/ResVehicleList.vue";
import ResVehicleForm from "./components/ResVehicleForm.vue";

const resVehicleStore = useResVehicleStore();

const mode = ref("list");

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