<template>
  <div>
    <h3>승인 대기 차량</h3>

    <table border="">
      <thead>
        <tr>
          <th>차량번호</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>등록기간</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="vehicle in vehicles" :key="vehicle.vehicleCarNo">
          <td>{{ vehicle.carNo }}</td>
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>

          <td>
            <select
              v-if="vehicle.vehicleType === 'normal'"
              v-model.number="vehicle.periodMonths"
              style="width: 120px;"
            >
              <option :value="1">1개월</option>
              <option :value="3">3개월</option>
              <option :value="6">6개월</option>
              <option :value="12">12개월</option>
            </select>
          
            <span v-if="vehicle.vehicleType === 'visit'">
              <input
                type="number"
                min="1"
                v-model.number="vehicle.periodHours"
                placeholder="시간"
                style="width: 60px;"
              >
              시간
            </span>
          </td>

          <td>
            <button @click="approve(vehicle)">승인</button>
            <button @click="reject(vehicle)">반려</button>
            <button @click="expire(vehicle)">만료</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { useVehicleStore } from '../vehicleStore'

defineProps({
  vehicles: Array
})

const vehicleStore = useVehicleStore()

async function approve(vehicle) {
  const data = {
    vehicleStatus: 'APPROVED',
    vehicleType: vehicle.vehicleType
  }

  if (vehicle.vehicleType === 'normal') {
    data.periodMonths = vehicle.periodMonths || 3
  }

  if (vehicle.vehicleType === 'visit') {
    if (!vehicle.periodHours || vehicle.periodHours < 1) {
      alert('방문차량 등록시간을 입력하세요')
      return
    }

    data.periodHours = vehicle.periodHours
  }

  await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, data)
  await vehicleStore.loadVehicleList()
}

async function reject(vehicle) {
  await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, {
    vehicleStatus: 'UNKNOWN',
    vehicleType: vehicle.vehicleType
  })

  await vehicleStore.loadVehicleList()
}

async function expire(vehicle) {
  await vehicleStore.changeVehicleApproveStatus(vehicle.vehicleCarNo, {
    vehicleStatus: 'EXPIRED',
    vehicleType: vehicle.vehicleType
  })

  await vehicleStore.loadVehicleList()
}
</script>