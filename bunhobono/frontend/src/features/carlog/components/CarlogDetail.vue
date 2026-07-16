<template>
  <table border="">
    <thead>
      <tr>
        <th>번호</th>
        <th>차량번호</th>
        <th>상태</th>
        <th>차량구분</th>
        <th>입차시간</th>
        <th>출차시간</th>
        <th>주차시간</th>
        <th>입차게이트</th>
        <th>출차게이트</th>
        <th>주차장</th>
        <th>관리</th>
      </tr>
    </thead>

    <tbody>
      <tr v-for="log in paginatedItems" :key="log.carLogNo">
        <td>{{ log.displayNo }}</td>
        <td>{{ log.carNo || '미인식' }}</td>
        <td>{{ log.parkingStateText }}</td>
        <td>{{ log.carKindText }}</td>
        <td>{{ log.inTimeText }}</td>
        <td>{{ log.outTimeText }}</td>
        <td>{{ log.parkingTimeText }}</td>
        <td>{{ log.inGateText }}</td>
        <td>{{ log.outGateText }}</td>
        <td>{{ log.parkingName || '-' }}</td>
        <td>
          <button type="button" @click="carlogStore.remove(log.carLogNo)">
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

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(logList, pageSize)


</script>