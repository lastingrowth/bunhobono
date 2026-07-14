<template>
  <div>
    <h3>방문차량 신청</h3>

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
            <td>방문차량</td>
          </tr>

          <tr>
            <th>방문 시작</th>
            <td>
              <input
                v-model="form.startDate"
                type="datetime-local"
                required
              >
            </td>
          </tr>

          <tr>
            <th>방문 시간</th>
            <td>
              <select v-model.number="form.periodHours" required>
                <option :value="2">2시간</option>
                <option :value="4">4시간</option>
                <option :value="6">6시간</option>
                <option :value="8">8시간</option>
                <option :value="12">12시간</option>
              </select>
            </td>
          </tr>

          <tr>
            <th>예상 만료</th>
            <td>{{ expectedEndText || '-' }}</td>
          </tr>

          <tr>
            <td colspan="2" align="right">
              <button type="submit">신청</button>
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
import { computed, reactive } from "vue";

const emit = defineEmits(["submit", "cancel"]);

const form = reactive({
  carNo: "",
  startDate: "",
  periodHours: 2
});

const expectedEndText = computed(() => {
  if (!form.startDate || !form.periodHours) {
    return "";
  }

  const date = new Date(form.startDate);
  date.setHours(date.getHours() + Number(form.periodHours));

  return formatDateTimeLocal(date);
});

function submit() {
  emit("submit", {
    carNo: form.carNo,
    startDate: form.startDate,
    periodHours: form.periodHours
  });
}

function formatDateTimeLocal(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
}
</script>