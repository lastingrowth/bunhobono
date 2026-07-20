<template>
  <div>
    <h3>{{ listTitle }}</h3>

    <div class="vehicle-filter-bar">
      <button class="filter-btn" @click="filterType = 'all'">전체</button>
      <button class="filter-btn" @click="filterType = 'normal'">입주민만</button>
      <button class="filter-btn" @click="filterType = 'visit'">방문자만</button>
      <button class="filter-btn" @click="filterType = 'expired'">만기차량</button>
      <button class="filter-btn" @click="toggleSort">
        {{ sortButtonText }}
      </button>
    </div>

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
import { computed, ref, watch } from 'vue'
import { useVehicleStore } from '../vehicleStore'
import { usePagination } from '@/shared/pagination/usePagination'
import Pagination from '@/shared/pagination/Pagination.vue'

const props = defineProps({
  vehicles: {
    type: Array,
    default: () => []
  },
  initialFilter: {
    type: String,
    default: 'all'
  }
})

const vehicleStore = useVehicleStore()

const filterType = ref(props.initialFilter)
const sortMode = ref('latest')

// 통계 화면에서 들어왔을 때 제목도 전용 화면처럼 보이게 한다.
const listTitle = computed(() => {
  if (filterType.value === 'parkedExpired') {
    return '주차중 방문 만료 차량'
  }

  return '차량 목록'
})

const filteredVehicles = computed(() => {
  return props.vehicles.filter((vehicle) => {
    const expired = isExpiredVehicle(vehicle)
    const parking = isParkingVehicle(vehicle)

    // 통계 화면에서 들어온 '주차중 방문 만료 차량' 전용 필터다.
    // 방문차량이면서, 현재 주차중이고, 만료된 차량만 보여준다.
    if (filterType.value === 'parkedExpired') {
      return vehicle.vehicleType === 'visit'
        && parking
        && expired
    }

    if (filterType.value === 'expired') {
      return expired
    }

    if (expired) {
      return false
    }

    if (filterType.value === 'normal') {
      return vehicle.vehicleType === 'normal'
    }

    if (filterType.value === 'visit') {
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

    if (sortMode.value === 'oldest') {
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

const sortButtonText = computed(() => {
  if (sortMode.value === 'oldest') {
    return '오래된순'
  }

  return '최신순'
})

function toggleSort() {
  if (sortMode.value === 'latest') {
    sortMode.value = 'oldest'
  } else {
    sortMode.value = 'latest'
  }
}

function isExpiredVehicle(vehicle) {
  const realEndDate = vehicle.realEndDate ?? vehicle.endDate

  if (realEndDate) {
    return new Date(realEndDate) <= new Date()
  }

  return String(vehicle.remainingTimeText || '').startsWith('만기됨')
}

// 입차 시간이 있고 출차 시간이 없으면 현재 주차중으로 본다.
// 입차 X 차량은 여기서 false가 되기 때문에 전용 목록에서 빠진다.
function isParkingVehicle(vehicle) {
  return Boolean(vehicle.inTime) && !vehicle.outTime
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

// 부모 화면에서 넘긴 초기 필터가 바뀌면 목록 필터도 같이 바꾼다.
// /admin/vehicles/parked-expired로 들어왔을 때 전용 필터가 적용된다.
watch(
  () => props.initialFilter,
  (value) => {
    filterType.value = value
    currentPage.value = 1
  }
)

watch(filterType, () => {
  currentPage.value = 1
})

async function remove(vehicleNo) {
  if (!confirm('삭제할까요?')) {
    return
  }

  await vehicleStore.removeVehicle(vehicleNo)
}
</script>

<style scoped>
.vehicle-filter-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 740px;
  min-width: 740px;
  max-width: 740px;
  margin: 12px 0;
}

.filter-btn {
  width: 120px;
  height: 36px;
  white-space: nowrap;
  text-align: center;
}
</style>