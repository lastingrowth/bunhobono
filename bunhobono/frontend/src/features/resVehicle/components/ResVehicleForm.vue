<template>
  <div>
    <h3>{{ isEdit ? '차량 수정 신청' : '차량 등록 신청' }}</h3>

    <form @submit.prevent="submit">
      <table border="">
        <tbody>
          <tr>
            <th>차량번호</th>
            <td>
              <input
                v-model="form.carNo"
                placeholder="예: 12가3456"
                required
              >
            </td>
          </tr>

          <tr>
            <th>차량종류</th>
            <td>
              <!-- 관리 화면에서 선택한 차량 종류를 고정해 다른 목록에 잘못 등록되지 않게 한다. -->
              {{ form.vehicleType === 'visit' ? '방문차량' : '입주민차량' }}
            </td>
          </tr>

          <!-- 방문 차량만 주차 허용 시작·종료 기간을 입력한다. -->
          <template v-if="form.vehicleType === 'visit'">
            <tr>
              <th>방문 시작</th>
              <td>
                <input v-model="form.startDate" type="datetime-local" required>
              </td>
            </tr>
            <tr>
              <th>방문 종료</th>
              <td>
                <input v-model="form.endDate" type="datetime-local" required>
              </td>
            </tr>
          </template>

          <tr>
            <td colspan="2" align="right">
              <button type="submit">
                {{ isEdit ? '수정 신청' : '등록 신청' }}
              </button>
              <button type="button" @click="$emit('cancel')">
                취소
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </form>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from "vue";

const props = defineProps({
  vehicle: {
    type: Object,
    default: null
  },
  defaultVehicleType: {
    type: String,
    default: "normal"
  }
});

const emit = defineEmits(["submit", "cancel"]);

const form = reactive({
  carNo: "",
  vehicleType: "normal",
  startDate: "",
  endDate: ""
});

const isEdit = computed(() => !!props.vehicle);

watch(
  [() => props.vehicle, () => props.defaultVehicleType],
  ([vehicle, defaultVehicleType]) => {
    if (vehicle) {
      form.carNo = vehicle.carNo || "";
      form.vehicleType = vehicle.vehicleType || "normal";
      form.startDate = toDateTimeLocal(vehicle.startDate);
      form.endDate = toDateTimeLocal(vehicle.endDate);
      return;
    }

    form.carNo = "";
    // 대시보드에서 선택한 관리 종류를 신규 등록 차량의 기본값으로 사용한다.
    form.vehicleType = defaultVehicleType === "visit" ? "visit" : "normal";
    form.startDate = "";
    form.endDate = "";
  },
  { immediate: true }
);

function submit() {
  const isVisit = form.vehicleType === "visit";

  // 방문 종료는 반드시 방문 시작 이후여야 한다.
  if (isVisit && new Date(form.endDate) <= new Date(form.startDate)) {
    alert("방문 종료일은 시작일 이후여야 합니다.");
    return;
  }

  emit("submit", {
    carNo: form.carNo,
    vehicleType: form.vehicleType,
    startDate: isVisit ? form.startDate : null,
    endDate: isVisit ? form.endDate : null
  });
}

function toDateTimeLocal(value) {
  return value ? String(value).slice(0, 16) : "";
}
</script>
