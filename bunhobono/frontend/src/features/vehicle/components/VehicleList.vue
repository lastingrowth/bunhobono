<template>
  <div>
    <h3>차량 목록</h3>

    <div>
      <button @click="filterType = 'all'">전체</button>
      <button @click="filterType = 'normal'">입주민차량만 보기</button>
      <button @click="filterType = 'visit'">방문차량만 보기</button>
    </div>

    <table border="">
      <thead>
        <tr>
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
        <tr v-for="vehicle in filteredVehicles" :key="vehicle.vehicleCarNo">
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

        <tr v-if="filteredVehicles.length === 0">
          <td colspan="10">조회된 차량이 없습니다.</td>
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

const filteredVehicles = computed(() => {
  if (filterType.value === 'all') {
    return props.vehicles
  }

  return props.vehicles.filter(vehicle => vehicle.vehicleType === filterType.value)
})

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
