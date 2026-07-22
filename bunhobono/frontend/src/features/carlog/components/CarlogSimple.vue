<template>
  <div
    v-if="carlogStore.feedbackMessage"
    class="carlog-feedback-toast"
    :class="carlogStore.feedbackType"
    role="status"
  >
    {{ carlogStore.feedbackMessage }}
  </div>
  <table border="">
    <thead>
      <tr>
        <th>차량번호</th>
        <th>주차상태</th>
        <th>차량종류</th>
        <th>주차위치</th>
        <th>관리</th>
      </tr>
    </thead>

    <tbody>
      <tr v-for="log in logs" :key="log.carLogNo">
        <td>{{ log.carNo || '미인식' }}</td>
        <td>{{ log.parkingStateText }}</td>
        <td>{{ log.carKindText }}</td>
        <td>{{ log.parkingName || '-' }}</td>
        <td>
          <button type="button" @click="requestDelete(log)">삭제</button>
        </td>
      </tr>
    </tbody>
  </table>
  <CarlogDeleteConfirm
    :open="Boolean(pendingDeleteLog)"
    :car-no="pendingDeleteLog?.carNo || ''"
    :deleting="deleting"
    @cancel="cancelDelete"
    @confirm="confirmDelete"
  />
</template>

<script setup>
import { ref } from 'vue'
import { useCarlogStore } from '../carlogStore'
import CarlogDeleteConfirm from './CarlogDeleteConfirm.vue'

const carlogStore = useCarlogStore()
const pendingDeleteLog = ref(null)
const deleting = ref(false)

const requestDelete = (log) => { pendingDeleteLog.value = log }
const cancelDelete = () => {
  if (!deleting.value) pendingDeleteLog.value = null
}
const confirmDelete = async () => {
  if (!pendingDeleteLog.value || deleting.value) return
  deleting.value = true
  await carlogStore.remove(pendingDeleteLog.value.carLogNo)
  deleting.value = false
  pendingDeleteLog.value = null
}

defineProps({
  logs: Array
})
</script>

<style scoped>
.carlog-feedback-toast {
  position: fixed;
  z-index: 1200;
  top: 24px;
  right: 24px;
  padding: 11px 16px;
  border: 1px solid #9fcfb0;
  border-radius: 8px;
  color: #1f6840;
  background: #ecf8f0;
  box-shadow: 0 8px 24px rgba(23, 45, 34, .18);
  font-size: 13px;
  font-weight: 800;
}
.carlog-feedback-toast.error { border-color: #e3adad; color: #9f2f2f; background: #fff0f0; }
</style>
