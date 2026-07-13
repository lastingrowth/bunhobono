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
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>
          <td>{{ vehicle.approvedAtText || '-' }}</td>
          <td>{{ vehicle.periodText || '-' }}</td>
          <td>{{ vehicle.endDateText || '-' }}</td>
          <td>{{ vehicle.remainingTimeText || '-' }}</td>
          <td>
            <button @click="detail(vehicle.vehicleCarNo)">상세</button>
            <button @click="remove(vehicle.vehicleCarNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="filteredVehicles.length === 0">
          <td colspan="8">조회된 차량이 없습니다.</td>
        </tr>
      </tbody>
    </table>

    <!-- 선택한 차량 상세 정보 -->
    <div v-if="vehicleStore.vehicle.vehicleCarNo">
      <div class="detail-header">
        <h3>차량 상세</h3>
        <button @click="closeDetail">닫기</button>
      </div>

      <table class="detail-table">
        <tbody>
          <tr>
            <th>차량번호</th>
            <td>{{ vehicleStore.vehicle.carNo || '-' }}</td>
          </tr>

          <tr>
            <th>차량종류</th>
            <td>
              {{ vehicleStore.vehicle.vehicleTypeText || vehicleStore.vehicle.vehicleType || '-' }}
            </td>
          </tr>

          <tr>
            <th>승인상태</th>
            <td>
              {{ vehicleStore.vehicle.vehicleStatusText || vehicleStore.vehicle.vehicleStatus || '-' }}
            </td>
          </tr>

          <tr>
            <th>회원번호</th>
            <td>{{ vehicleStore.vehicle.memberNo ?? '-' }}</td>
          </tr>

          <tr>
            <th>승인 관리자</th>
            <td>{{ vehicleStore.vehicle.approvedMemberName || '-' }}</td>
          </tr>

          <tr>
            <th>승인일</th>
            <td>{{ vehicleStore.vehicle.approvedAtText || '-' }}</td>
          </tr>

          <tr>
            <th>등록기간</th>
            <td>{{ vehicleStore.vehicle.periodText || '-' }}</td>
          </tr>

          <tr>
            <th>만기일</th>
            <td>{{ vehicleStore.vehicle.endDateText || '-' }}</td>
          </tr>

          <tr>
            <th>남은기간</th>
            <td>{{ vehicleStore.vehicle.remainingTimeText || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useVehicleStore } from '../vehicleStore'

const props = defineProps({
  vehicles: Array
})

const vehicleStore = useVehicleStore()
const filterType = ref('all')

const filteredVehicles = computed(() => {
  if (filterType.value === 'all') {
    return props.vehicles
  }

  return props.vehicles.filter(vehicle => vehicle.vehicleType === filterType.value)
})

async function detail(vehicleNo) {
  await vehicleStore.loadVehicle(vehicleNo)
}

function closeDetail() {
  vehicleStore.vehicle = {}
}

async function remove(vehicleNo) {
  if (!confirm('삭제할까요?')) {
    return
  }

  await vehicleStore.removeVehicle(vehicleNo)
}
</script>