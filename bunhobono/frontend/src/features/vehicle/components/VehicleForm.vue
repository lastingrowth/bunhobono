<template>
  <div>
    <h3>차량 등록</h3>

    <div>
      <input
        v-model="carNo"
        placeholder="차량번호"
      >

      <select v-model="vehicleType">
        <option value="normal">등록차량</option>
        <option value="visit">방문차량</option>
      </select>

      <input
        type="date"
        v-model="approvedDate"
      >

      <input
        type="time"
        v-model="approvedTime"
      >

      <button @click="add">등록</button>
      <button @click="reset">초기화</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useVehicleStore } from '../vehicleStore'

const vehicleStore = useVehicleStore()

const carNo = ref('')
const vehicleType = ref('normal')
const approvedDate = ref('')
const approvedTime = ref('')

async function add() {
  if (carNo.value.trim() === '') {
    alert('차량번호를 입력하세요')
    return
  }

  if (approvedDate.value === '' || approvedTime.value === '') {
    alert('승인 날짜와 시간을 입력하세요')
    return
  }

  await vehicleStore.addVehicle({
    carNo: carNo.value,
    vehicleType: vehicleType.value,
    vehicleStatus: 'WAITING',
    approvedAt: `${approvedDate.value}T${approvedTime.value}:00`
  })

  alert('차량이 등록되었습니다')

  reset()

  await vehicleStore.loadVehicleList()
  await vehicleStore.loadVehicleApproveList()
}

function reset() {
  carNo.value = ''
  vehicleType.value = 'normal'
  approvedDate.value = ''
  approvedTime.value = ''
}
</script>