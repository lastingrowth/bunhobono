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

async function add() {
  if (carNo.value.trim() === '') {
    alert('차량번호를 입력하세요')
    return
  }

  try {
    await vehicleStore.addVehicle({
      carNo: carNo.value.trim(),
      vehicleType: vehicleType.value,
      vehicleStatus: 'WAITING'
    })

    alert('차량이 등록되었습니다')
    reset()
    await vehicleStore.loadVehicleApproveList()
  } catch (error) {
    if (error.response?.status === 409) {
      alert(error.response.data?.message || '이미 등록 또는 승인 대기 중인 차량번호입니다.')
      return
    }

    alert('차량 등록 중 오류가 발생했습니다.')
  }
}

function reset() {
  carNo.value = ''
  vehicleType.value = 'normal'
}
</script>
