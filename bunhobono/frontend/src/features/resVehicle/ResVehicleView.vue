<template>
  <div>
    <h2>{{ pageTitle }}</h2>

    <div>
      {{ resVehicleStore.member.memName }}
      ({{ resVehicleStore.member.loginId }})
      {{ resVehicleStore.member.memDong }}동
      {{ resVehicleStore.member.memHo }}호
    </div>

    <div>
      <button @click="openList">차량 목록</button>
      <button @click="openInsert">차량 등록</button>
    </div>

    <ResVehicleList
      v-if="mode === 'list'"
      :vehicles="filteredVehicles"
      @edit="openEdit"
      @remove="removeVehicle"
    />

    <ResVehicleForm
      v-if="mode === 'form'"
      :vehicle="selectedVehicle"
      :default-vehicle-type="selectedVehicleType"
      @submit="submitVehicle"
      @cancel="openList"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useResVehicleStore } from "./resVehicleStore";
import ResVehicleList from "./components/ResVehicleList.vue";
import ResVehicleForm from "./components/ResVehicleForm.vue";

const resVehicleStore = useResVehicleStore();
const route = useRoute();

const mode = ref("list");
const selectedVehicle = ref(null);

// URL의 type 값으로 일반 차량과 방문 차량 관리 화면을 구분한다.
const selectedVehicleType = computed(() => {
  return route.query.type === "visit" ? "visit" : "normal";
});

const pageTitle = computed(() => {
  return selectedVehicleType.value === "visit"
    ? "방문 차량 관리"
    : "내 차량 관리";
});

// 서버가 반환한 본인 차량 중 현재 관리 화면의 차량 종류만 표시한다.
const filteredVehicles = computed(() => {
  return resVehicleStore.vehicleList.filter((vehicle) => {
    return vehicle.vehicleType === selectedVehicleType.value;
  });
});

onMounted(async () => {
  await resVehicleStore.loadMyInfo();
  await resVehicleStore.loadVehicleList();
});

function openList() {
  mode.value = "list";
  selectedVehicle.value = null;
}

function openInsert() {
  mode.value = "form";
  selectedVehicle.value = null;
}

function openEdit(vehicle) {
  mode.value = "form";
  selectedVehicle.value = vehicle;
}

async function submitVehicle(data) {
  if (selectedVehicle.value) {
    await resVehicleStore.editVehicle(
      selectedVehicle.value.vehicleCarNo,
      data
    );
  } else {
    await resVehicleStore.addVehicle(data);
  }

  openList();
}

async function removeVehicle(vehicleCarNo) {
  if (!confirm("삭제할까요?")) {
    return;
  }

  await resVehicleStore.removeVehicle(vehicleCarNo);
}
</script>
