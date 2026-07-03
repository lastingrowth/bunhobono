<template>
  <div>
    <div>Vehicle 볼곳임</div>

    <div
      v-for="vehicle in vehicleStore.vehicles"
      :key="vehicle.vehicleCarNo"
    >
      <div>{{ vehicle.vehicleCarNo }}</div>
      <div>{{ vehicle.carNo }}</div>
      <div>{{ vehicle.vehicleType }}</div>
      <div>{{ vehicle.memberNo }}</div>
      <div>{{ vehicle.regDate }}</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useVehicleStore } from '@/stores/vehicleStore'

const vehicleStore = useVehicleStore()

onMounted(() => {
  vehicleStore.getVehicles()
})
</script>