<template>
  <div>
    <h2>{{ pageTitle }}</h2>

    <div>
      {{ memberStore.member.memName }}
      ({{ memberStore.member.loginId }})
      {{ memberStore.member.memDong }}동
      {{ memberStore.member.memHo }}호
    </div>

    <div>
      <button @click="openList">차량 목록</button>
      <button @click="openInsert">차량 등록</button>
    </div>

    <div v-if="mode === 'list'">
      <h3>{{ selectedVehicleType === "visit" ? "방문 차량 목록" : "본인 차량 목록" }}</h3>

      <table border="">
        <thead>
          <tr>
            <th>차량번호</th>
            <th>차량종류</th>
            <th>승인상태</th>
            <th>승인일</th>
            <th v-if="showPeriodColumns">등록기간</th>
            <th v-if="showPeriodColumns">만기일</th>
            <th v-if="showPeriodColumns">남은기간</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="vehicle in filteredVehicles" :key="vehicle.vehicleCarNo">
            <td>{{ vehicle.carNo }}</td>
            <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
            <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>
            <td>{{ vehicle.approvedAtText || "-" }}</td>
            <td v-if="showPeriodColumns">{{ vehicle.periodText || "-" }}</td>
            <td v-if="showPeriodColumns">{{ vehicle.endDateText || "-" }}</td>
            <td v-if="showPeriodColumns">{{ vehicle.remainingTimeText || "-" }}</td>
            <td>
              <button @click="openEdit(vehicle)">수정</button>
              <button @click="removeVehicle(vehicle.vehicleCarNo)">삭제</button>
            </td>
          </tr>

          <tr v-if="filteredVehicles.length === 0">
            <td :colspan="showPeriodColumns ? 8 : 5" align="center">
              등록된 차량이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 기존 ResVehicleForm 내용을 이 View에서 직접 처리한다. -->
    <div v-if="mode === 'form'">
      <h3>{{ selectedVehicle ? "차량 수정 신청" : "차량 등록 신청" }}</h3>

      <form @submit.prevent="submitVehicle">
        <table border="">
          <tbody>
            <tr>
              <th>차량번호</th>
              <td>
                <input
                  v-model="vehicleForm.carNo"
                  placeholder="예: 12가3456"
                  required
                >
              </td>
            </tr>
            <tr>
              <th>차량종류</th>
              <td>{{ vehicleForm.vehicleType === "visit" ? "방문차량" : "입주민차량" }}</td>
            </tr>

            <!-- 방문 차량만 방문 시작일과 종료일을 입력한다. -->
            <template v-if="vehicleForm.vehicleType === 'visit'">
              <tr>
                <th>방문 시작</th>
                <td>
                  <input v-model="vehicleForm.startDate" type="datetime-local" required>
                </td>
              </tr>
              <tr>
                <th>방문 종료</th>
                <td>
                  <input v-model="vehicleForm.endDate" type="datetime-local" required>
                </td>
              </tr>
            </template>

            <tr>
              <td colspan="2" align="right">
                <button type="submit">
                  {{ selectedVehicle ? "수정 신청" : "등록 신청" }}
                </button>
                <button type="button" @click="openList">취소</button>
              </td>
            </tr>
          </tbody>
        </table>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useMemStore } from "./memStore";

const memberStore = useMemStore();
const route = useRoute();

const mode = ref("list");
const selectedVehicle = ref(null);
const vehicleForm = reactive({
  carNo: "",
  vehicleType: "normal",
  startDate: "",
  endDate: ""
});

// URL의 type 값으로 일반 차량과 방문 차량 관리 화면을 구분한다.
const selectedVehicleType = computed(() => {
  return route.query.type === "visit" ? "visit" : "normal";
});

const pageTitle = computed(() => {
  return selectedVehicleType.value === "visit"
    ? "방문 차량 관리"
    : "내 차량 관리";
});

const showPeriodColumns = computed(() => selectedVehicleType.value === "visit");

// 서버가 반환한 본인 차량 중 현재 관리 화면의 차량 종류만 표시한다.
const filteredVehicles = computed(() => {
  return memberStore.vehicleList.filter((vehicle) => {
    return vehicle.vehicleType === selectedVehicleType.value;
  });
});

onMounted(async () => {
  await memberStore.loadMypage();
  await memberStore.loadVehicleList();
});

watch(selectedVehicleType, () => {
  openList();
});

function openList() {
  mode.value = "list";
  selectedVehicle.value = null;
  resetVehicleForm();
}

function openInsert() {
  selectedVehicle.value = null;
  resetVehicleForm();
  mode.value = "form";
}

function openEdit(vehicle) {
  selectedVehicle.value = vehicle;
  vehicleForm.carNo = vehicle.carNo || "";
  vehicleForm.vehicleType = vehicle.vehicleType || selectedVehicleType.value;
  vehicleForm.startDate = toDateTimeLocal(vehicle.startDate);
  vehicleForm.endDate = toDateTimeLocal(vehicle.endDate);
  mode.value = "form";
}

async function submitVehicle() {
  const isVisit = vehicleForm.vehicleType === "visit";

  if (isVisit && new Date(vehicleForm.endDate) <= new Date(vehicleForm.startDate)) {
    alert("방문 종료일은 시작일 이후여야 합니다.");
    return;
  }

  const data = {
    carNo: vehicleForm.carNo,
    vehicleType: vehicleForm.vehicleType,
    startDate: isVisit ? vehicleForm.startDate : null,
    endDate: isVisit ? vehicleForm.endDate : null
  };

  if (selectedVehicle.value) {
    await memberStore.editVehicle(selectedVehicle.value.vehicleCarNo, data);
  } else {
    await memberStore.addVehicle(data);
  }

  openList();
}

async function removeVehicle(vehicleCarNo) {
  if (!confirm("삭제할까요?")) return;

  await memberStore.removeVehicle(vehicleCarNo);
}

function resetVehicleForm() {
  vehicleForm.carNo = "";
  vehicleForm.vehicleType = selectedVehicleType.value;
  vehicleForm.startDate = "";
  vehicleForm.endDate = "";
}

function toDateTimeLocal(value) {
  return value ? String(value).slice(0, 16) : "";
}
</script>
