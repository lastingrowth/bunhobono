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
              <select v-model="form.vehicleType">
                <option value="normal">입주민차량</option>
                <option value="visit">방문차량</option>
              </select>
            </td>
          </tr>

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
  }
});

const emit = defineEmits(["submit", "cancel"]);

const form = reactive({
  carNo: "",
  vehicleType: "normal"
});

const isEdit = computed(() => !!props.vehicle);

watch(
  () => props.vehicle,
  (vehicle) => {
    if (vehicle) {
      form.carNo = vehicle.carNo || "";
      form.vehicleType = vehicle.vehicleType || "normal";
      return;
    }

    form.carNo = "";
    form.vehicleType = "normal";
  },
  { immediate: true }
);

function submit() {
  emit("submit", {
    carNo: form.carNo,
    vehicleType: form.vehicleType
  });
}
</script>