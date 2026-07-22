<template>
  <Transition name="carlog-toast">
    <div
      v-if="carlogStore.feedbackMessage"
      class="carlog-feedback-toast"
      :class="carlogStore.feedbackType"
      role="status"
    >
      {{ carlogStore.feedbackMessage }}
    </div>
  </Transition>
  <div class="carlog-table-wrap management-list-table">
  <table class="carlog-table" border="">
    <colgroup>
      <col class="number-col">
      <col class="car-number-col">
      <col class="state-col">
      <col class="kind-col">
      <col class="in-time-col">
      <col class="out-time-col">
      <col class="parking-time-col">
      <col class="gate-col">
      <col class="gate-col">
      <col class="parking-col">
      <col class="manage-col">
    </colgroup>
    <thead>
      <tr>
        <th>번호</th>
        <th>차량번호</th>
        <th>상태</th>
        <th>차량구분</th>
        <th>입차시간</th>
        <th class="out-time-column">출차시간</th>
        <th>주차시간</th>
        <th>입차게이트</th>
        <th>출차게이트</th>
        <th>주차장</th>
        <th class="manage-column">관리</th>
      </tr>
    </thead>

    <tbody>
      <tr v-for="(log, index) in paginatedItems" :key="log.carLogNo">
        <td class="short-text">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
        <td :class="{ 'short-text': isShortText(log.carNo || '미인식') }">{{ log.carNo || '미인식' }}</td>
        <td :class="{ 'short-text': isShortText(log.parkingStateText) }">{{ log.parkingStateText }}</td>
        <td :class="{ 'short-text': isShortText(log.carKindText) }">{{ log.carKindText }}</td>
        <td :class="{ 'short-text': isShortText(log.inTimeText) }">{{ log.inTimeText }}</td>
        <td
          class="out-time-column"
          :class="{ 'short-text': isShortText(log.outTimeText) }"
        >{{ log.outTimeText }}</td>
        <td :class="{ 'short-text': isShortText(log.parkingTimeText) }">{{ log.parkingTimeText }}</td>
        <td :class="{ 'short-text': isShortText(log.inGateText) }">{{ log.inGateText }}</td>
        <td :class="{ 'short-text': isShortText(log.outGateText) }">{{ log.outGateText }}</td>
        <td :class="{ 'short-text': isShortText(log.parkingName || '-') }">{{ log.parkingName || '-' }}</td>
        <td class="manage-column"><button class="delete-btn" type="button" @click="requestDelete(log)">삭제</button></td>
      </tr>

      <tr v-if="logs.length === 0">
        <td colspan="11" align="center">
          조회된 로그가 없습니다.
        </td>
      </tr>
    </tbody>
  </table>
  </div>
  <div class="admin-pagination-area">
  <Pagination
    :current-page="currentPage"
    :total-pages="totalPages"
    :page-numbers="pageNumbers"
    @change-page="setPage"/>
  </div>
  <CarlogDeleteConfirm
    :open="Boolean(pendingDeleteLog)"
    :car-no="pendingDeleteLog?.carNo || ''"
    :deleting="deleting"
    @cancel="cancelDelete"
    @confirm="confirmDelete"
  />
</template>

<script setup>
import { computed, ref } from 'vue';
import { useCarlogStore } from '../carlogStore'
import { usePagination } from '@/shared/pagination/usePagination';
import Pagination from '@/shared/pagination/Pagination.vue';
import CarlogDeleteConfirm from './CarlogDeleteConfirm.vue';


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

const props = defineProps({
  logs: {
    type: Array,
    default: () => []
  }
})

const logList = computed(() => {
  return props.logs
})

const pageSize = 10

const isShortText = (value) => String(value ?? '').length <= 5

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(logList, pageSize)


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
  box-shadow: 0 8px 24px rgba(23, 45, 34, 0.18);
  font-size: 13px;
  font-weight: 800;
}

.carlog-feedback-toast.error {
  border-color: #e3adad;
  color: #9f2f2f;
  background: #fff0f0;
}

.carlog-toast-enter-active,
.carlog-toast-leave-active { transition: opacity .18s ease, transform .18s ease; }
.carlog-toast-enter-from,
.carlog-toast-leave-to { opacity: 0; transform: translateY(-8px); }

.carlog-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.carlog-table {
  width: 100%;
  min-width: 1080px;
  table-layout: fixed;
}

.carlog-table .number-col { width: 5%; }
.carlog-table .car-number-col { width: 12%; }
.carlog-table .state-col { width: 7%; }
.carlog-table .kind-col { width: 8%; }
.carlog-table .in-time-col,
.carlog-table .out-time-col { width: 14%; }
.carlog-table .parking-time-col { width: 9%; }
.carlog-table .gate-col,
.carlog-table .parking-col { width: 8%; }
.carlog-table .manage-col { width: 7%; }

.carlog-table th,
.carlog-table td {
  box-sizing: border-box;
  height: 30px !important;
  padding: 4px 7px !important;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.3;
  text-align: center;
  text-overflow: ellipsis;
  white-space: normal;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.carlog-table th,
.carlog-table td.short-text {
  white-space: nowrap;
}

.carlog-table td {
  white-space: nowrap;
}

.out-time-column {
  font-size: 12px;
}

.manage-column {
  text-align: center;
}

.carlog-table .delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: auto;
  min-width: 52px;
  height: 22px !important;
  min-height: 0 !important;
  box-sizing: border-box;
  padding: 2px 8px !important;
  border: 1px solid #111827;
  border-radius: 5px;
  background: #fff;
  color: #111827;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
}

.carlog-table .delete-btn:hover {
  border-color: #111827;
  background: #111827;
  color: #fff;
}

@media (max-width: 1000px) {
  .carlog-table {
    min-width: 1080px;
  }

  .carlog-table th,
  .carlog-table td {
    padding: 4px 7px !important;
    font-size: 12px;
  }

  .carlog-table .delete-btn {
    width: auto;
    min-width: 52px;
    height: 22px !important;
    font-size: 12px;
  }
}

@media (max-width: 700px) {
  .carlog-table {
    min-width: 1080px;
  }

  .carlog-table th,
  .carlog-table td {
    padding: 4px 7px !important;
    font-size: 11px;
  }

  .carlog-table .delete-btn {
    width: auto;
    min-width: 52px;
    height: 22px !important;
    font-size: 11px;
  }
}
</style>
