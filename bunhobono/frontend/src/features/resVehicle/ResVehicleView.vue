<template>
  <div>
    <h2>내 차량 목록</h2>

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
      :vehicles="resVehicleStore.vehicleList"
      @edit="openEdit"
      @remove="removeVehicle"
    />

    <ResVehicleForm
      v-if="mode === 'form'"
      :vehicle="selectedVehicle"
      @submit="submitVehicle"
      @cancel="openList"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useResVehicleStore } from "./resVehicleStore";
import ResVehicleList from "./components/ResVehicleList.vue";
import ResVehicleForm from "./components/ResVehicleForm.vue";

const resVehicleStore = useResVehicleStore();

const mode = ref("list");
const selectedVehicle = ref(null);

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

async function removeVehicle(vehicle) {
  if (!confirm("삭제할까요?")) {
    return;
  }

  await resVehicleStore.removeVehicle(vehicle.vehicleCarNo);
}
</script>