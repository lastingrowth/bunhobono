<template>
  <div>
    <h3>본인 차량 기간 연장</h3>

    <form @submit.prevent="submit">
      <table border="">
        <tbody>
          <tr>
            <th>차량번호</th>
            <td>{{ vehicle?.carNo || '-' }}</td>
          </tr>
          <tr>
            <th>현재 만기일</th>
            <td>{{ formatDateTime(vehicle?.endDate) }}</td>
          </tr>
          <tr>
            <th>연장 기간</th>
            <td>
              <select v-model.number="extensionMonths" required>
                <option :value="1">1개월</option>
                <option :value="3">3개월</option>
                <option :value="6">6개월</option>
                <option :value="12">12개월</option>
              </select>
            </td>
          </tr>
          <tr>
            <th>새 만기일</th>
            <td>{{ formatDateTime(newEndDate) }}</td>
          </tr>
          <tr>
            <td colspan="2" class="visit-form-actions">
              <div class="normal-extension-actions">
                <button type="submit">연장</button>
                <button type="button" @click="emit('cancel')">돌아가기</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </form>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  vehicle: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['submit', 'cancel'])
const extensionMonths = ref(1)

const newEndDate = computed(() => {
  const currentEndDate = new Date(props.vehicle?.endDate)
  const now = new Date()
  const baseDate = !Number.isNaN(currentEndDate.getTime()) && currentEndDate > now
    ? currentEndDate
    : now
  const result = new Date(baseDate)

  result.setMonth(result.getMonth() + Number(extensionMonths.value))
  return result
})

function submit() {
  emit('submit', formatDateTimeValue(newEndDate.value))
}

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatDateTimeValue(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function pad(value) {
  return String(value).padStart(2, '0')
}
</script>

<style scoped>
.normal-extension-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
