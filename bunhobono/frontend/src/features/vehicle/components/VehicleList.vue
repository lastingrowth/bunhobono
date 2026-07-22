<template>
  <div>
    <div class="admin-table-scroll">
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
        <tr
          v-for="(vehicle, index) in paginatedItems"
          :key="vehicle.vehicleCarNo"
        >
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
          <td><button @click="remove(vehicle.vehicleCarNo)">삭제</button></td>
        </tr>

        <tr v-if="sortedVehicles.length === 0">
          <td colspan="11">
            조회된 차량이 없습니다.
          </td>
        </tr>
      </tbody>
    </table>
    </div>
    <div class="pagination-action-row admin-pagination-area">
      <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        @change-page="setPage"/>
      <slot name="pagination-action" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { usePagination } from '@/shared/pagination/usePagination'
import Pagination from '@/shared/pagination/Pagination.vue'
import { useVehicleStore } from '../vehicleStore'

const props = defineProps({
  vehicles: {
    type: Array,
    default: () => []
  },
  initialFilter: {
    type: String,
    default: 'all'
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

// 통계 화면에서 전용 주소로 들어왔는지 구분
const isInitialMode = ref(
  props.initialFilter === 'parkedExpired'
)

const filteredVehicles = computed(() => {
  return props.vehicles.filter((vehicle) => {
    const expired = isExpiredVehicle(vehicle)

    // 통계 화면의 주차 중 방문 만기차량
    // 실제 입차 후 시간을 초과한 방문차량만 표시
    if (isInitialMode.value) {
      return isParkedExpiredVehicle(vehicle)
    }

    // 미입차 만기와 주차시간 만기를 모두 표시
    if (props.filterType === 'expired') {
      return expired
    }

    // 만기차량은 일반 차량목록에서 제외
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
    const left = Number(
      a.displayNo ?? a.vehicleCarNo
    )

    const right = Number(
      b.displayNo ?? b.vehicleCarNo
    )

    if (props.sortMode === 'oldest') {
      return left - right
    }

    return right - left
  })
})

const pageSize = 10

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(sortedVehicles, pageSize)

// 만기 여부는 백엔드의 expiryType만 사용
function isExpiredVehicle(vehicle) {
  return (
    vehicle.expiryType === 'NO_ENTRY'
    || vehicle.expiryType === 'OVERSTAY'
  )
}

// 통계 화면에서는 입차 후 주차시간을 초과한
// 방문차량만 표시한다.
function isParkedExpiredVehicle(vehicle) {
  return (
    vehicle.vehicleType === 'visit'
    && vehicle.expiryType === 'OVERSTAY'
  )
}

function memberDongText(vehicle) {
  if (
    vehicle.memDong === null
    || vehicle.memDong === undefined
  ) {
    return '-'
  }

  if (Number(vehicle.memDong) === 0) {
    return '관리동'
  }

  return vehicle.memDong
}

function memberHoText(vehicle) {
  if (
    vehicle.memHo === null
    || vehicle.memHo === undefined
  ) {
    return '-'
  }

  if (Number(vehicle.memHo) === 0) {
    return '관리실'
  }

  return vehicle.memHo
}

watch(
  () => props.initialFilter,
  (value) => {
    isInitialMode.value = value === 'parkedExpired'
    currentPage.value = 1
  }
)

watch(
  () => props.filterType,
  () => {
    isInitialMode.value = false
    currentPage.value = 1
  }
)

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