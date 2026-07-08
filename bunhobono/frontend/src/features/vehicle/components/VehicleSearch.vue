<template>
  <div>
    <input
      v-model="carNo"
      placeholder="차량번호 검색"
      @keyup.enter="search"
    >

    <button @click="search">검색</button>
    <button @click="reset">전체목록</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useVehicleStore } from '../vehicleStore'

const vehicleStore = useVehicleStore()

const carNo = ref('')

async function search() {
  if (carNo.value.trim() === '') {
    await vehicleStore.loadVehicleList()
    return
  }

  await vehicleStore.searchVehicle(carNo.value)
}

async function reset() {
  carNo.value = ''
  await vehicleStore.loadVehicleList()
}
</script>