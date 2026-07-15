<template>
  <div>
    <h3>차량 목록</h3>

    <div class="vehicle-filter-bar">
      <button class="filter-btn" @click="filterType = 'all'">전체</button>
      <button class="filter-btn" @click="filterType = 'normal'">입주민만</button>
      <button class="filter-btn" @click="filterType = 'visit'">방문자만</button>
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
        <tr v-for="vehicle in sortedVehicles" :key="vehicle.vehicleCarNo">
          <td>{{ vehicle.displayNo }}</td>
          <td>{{ vehicle.carNo }}</td>
          <td>{{ getVehicleMember(vehicle)?.memDong ?? '-' }}</td>
          <td>{{ getVehicleMember(vehicle)?.memHo ?? '-' }}</td>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useMemStore } from '../../member/memStore'
import { useVehicleStore } from '../vehicleStore'

const props = defineProps({
  vehicles: Array
})

const vehicleStore = useVehicleStore()
const memberStore = useMemStore()

const filterType = ref('all')
const sortMode = ref('latest')

const filteredVehicles = computed(() => {
  if (filterType.value === 'all') {
    return props.vehicles
  }

  return props.vehicles.filter((vehicle) => {
    return vehicle.vehicleType === filterType.value
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

function getVehicleMember(vehicle) {
  return memberStore.memberList.find((member) => {
    return Number(member.memberNo) === Number(vehicle.memberNo)
  })
}

onMounted(async () => {
  if (memberStore.memberList.length === 0) {
    await memberStore.loadmemberList()
  }
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
  width: 600px;
  min-width: 600px;
  max-width: 600px;
  margin: 12px 0;
}

.filter-btn {
  width: 120px;
  height: 36px;
  white-space: nowrap;
  text-align: center;
}
</style>