<template>
  <div class="carlog-table-wrap">
  <table class="carlog-table" border="">
    <colgroup>
      <col class="col-number">
      <col class="col-car-number">
      <col class="col-state">
      <col class="col-kind">
      <col class="col-in-time">
      <col class="col-out-time">
      <col class="col-parking-time">
      <col class="col-in-gate">
      <col class="col-out-gate">
      <col class="col-parking">
      <col class="col-manage">
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
        <td :class="{ 'short-text': isShortText(log.inTimeText) }">
          <span
            v-for="line in timeLines(log.inTimeText)"
            :key="line"
            class="time-line"
          >{{ line }}</span>
        </td>
        <td
          class="out-time-column"
          :class="{ 'short-text': isShortText(log.outTimeText) }"
        >
          <span
            v-for="line in timeLines(log.outTimeText)"
            :key="line"
            class="time-line"
          >{{ line }}</span>
        </td>
        <td :class="{ 'short-text': isShortText(log.parkingTimeText) }">{{ log.parkingTimeText }}</td>
        <td :class="{ 'short-text': isShortText(log.inGateText) }">{{ log.inGateText }}</td>
        <td :class="{ 'short-text': isShortText(log.outGateText) }">{{ log.outGateText }}</td>
        <td :class="{ 'short-text': isShortText(log.parkingName || '-') }">{{ log.parkingName || '-' }}</td>
        <td class="manage-column">
          <button
            class="delete-btn"
            type="button"
            @click="carlogStore.remove(log.carLogNo)"
          >
            삭제
          </button>
        </td>
      </tr>

      <tr v-if="logs.length === 0">
        <td colspan="11" align="center">
          조회된 로그가 없습니다.
        </td>
      </tr>
    </tbody>
  </table>
  </div>
  <Pagination
    :current-page="currentPage"
    :total-pages="totalPages"
    :page-numbers="pageNumbers"
    @change-page="setPage"/>
</template>

<script setup>
import { computed } from 'vue';
import { useCarlogStore } from '../carlogStore'
import { usePagination } from '@/shared/pagination/usePagination';
import Pagination from '@/shared/pagination/Pagination.vue';


const carlogStore = useCarlogStore()

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

const isShortText = (value) => String(value ?? '').length <= 6
const timeLines = (value) => String(value ?? '-').split(' ')

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(logList, pageSize)


</script>

<style scoped>
.carlog-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.carlog-table {
  width: 100%;
  min-width: 760px;
  table-layout: fixed;
}

.carlog-table th,
.carlog-table td {
  padding: 10px 6px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.35;
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

.time-line {
  display: block;
  white-space: nowrap;
}

.col-number {
  width: 4%;
}

.col-car-number {
  width: 10%;
}

.col-state {
  width: 8%;
}

.col-kind {
  width: 9%;
}

.col-in-time,
.col-out-time {
  width: 11%;
}

.col-parking-time {
  width: 11%;
}

.col-in-gate,
.col-out-gate {
  width: 9%;
}

.col-parking {
  width: 10%;
}

.col-manage {
  width: 8%;
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
  width: 56px;
  height: 30px;
  box-sizing: border-box;
  padding: 0;
  border: 1px solid #111827;
  border-radius: 5px;
  background: #fff;
  color: #111827;
  font-size: 13px;
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
    min-width: 720px;
  }

  .carlog-table th,
  .carlog-table td {
    padding-right: 4px;
    padding-left: 4px;
    font-size: 12px;
  }

  .carlog-table .delete-btn {
    width: 50px;
    height: 28px;
    font-size: 12px;
  }
}

@media (max-width: 700px) {
  .carlog-table {
    min-width: 680px;
  }

  .carlog-table th,
  .carlog-table td {
    padding: 8px 3px;
    font-size: 11px;
  }

  .carlog-table .delete-btn {
    width: 44px;
    height: 26px;
    font-size: 11px;
  }
}
</style>
