<template>
  <div>
    <table border="">
      <thead>
        <tr>
          <th>번호</th>
          <th>차량번호</th>
          <th>동</th>
          <th>호수</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>승인일</th>
          <th>등록기간</th>
          <th>만기일</th>
          <th>남은기간</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="(vehicle, index) in paginatedItems" :key="vehicle.vehicleCarNo">
          <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td>{{ vehicle.carNo }}</td>
          <td>{{ memberDongText(vehicle) }}</td>
          <td>{{ memberHoText(vehicle) }}</td>
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>
          <td>{{ vehicle.approvedAtText || '-' }}</td>
          <td>{{ vehicle.periodText || '-' }}</td>
          <td>{{ vehicle.endDateText || '-' }}</td>
          <td>{{ vehicle.remainingTimeText || '-' }}</td>
          <td>
            <button @click="remove(vehicle.vehicleCarNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="sortedVehicles.length === 0">
          <td colspan="11">조회된 차량이 없습니다.</td>
        </tr>
      </tbody>
    </table>
    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @change-page="setPage"/>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useVehicleStore } from '../vehicleStore'
import { usePagination } from '@/shared/pagination/usePagination'
import Pagination from '@/shared/pagination/Pagination.vue'

const props = defineProps({
  vehicles: {
    type: Array,
    default: () => []
  },
  filterType: {
    type: String,
    default: 'all'
  },
  sortMode: {
    type: String,
    default: 'latest'
  }
})

const vehicleStore = useVehicleStore()

const filteredVehicles = computed(() => {
  return props.vehicles.filter((vehicle) => {
    const expired = isExpiredVehicle(vehicle)

    if (props.filterType === 'expired') {
      return expired
    }

    if (expired) {
      return false
    }

    if (props.filterType === 'normal') {
      return vehicle.vehicleType === 'normal'
    }

    if (props.filterType === 'visit') {
      return vehicle.vehicleType === 'visit'
    }

    return true
  })
})

const sortedVehicles = computed(() => {
  const list = [...filteredVehicles.value]

  return list.sort((a, b) => {
    const left = Number(a.displayNo ?? a.vehicleCarNo)
    const right = Number(b.displayNo ?? b.vehicleCarNo)

    if (props.sortMode === 'oldest') {
      return left - right
    }

    return right - left
  })
})

// 한 페이지에 보여줄 목록 수
const pageSize = 10

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(sortedVehicles, pageSize)

function isExpiredVehicle(vehicle) {
  return String(vehicle.remainingTimeText || '').startsWith('만기됨')
}

function memberDongText(vehicle) {
  if (vehicle.memDong === null || vehicle.memDong === undefined) {
    return '-'
  }

  if (Number(vehicle.memDong) === 0) {
    return '관리동'
  }

  return vehicle.memDong
}

function memberHoText(vehicle) {
  if (vehicle.memHo === null || vehicle.memHo === undefined) {
    return '-'
  }

  if (Number(vehicle.memHo) === 0) {
    return '관리실'
  }

  return vehicle.memHo
}

async function remove(vehicleNo) {
  if (!confirm('삭제할까요?')) {
    return
  }

  await vehicleStore.removeVehicle(vehicleNo)
}
</script>

<style scoped>
table th,
table td {
  text-align: center;
}
</style>
