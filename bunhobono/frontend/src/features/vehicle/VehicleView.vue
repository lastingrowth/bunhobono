<template>
  <section>
    <h2>차량 관리</h2>

    <div>
      <button @click="viewMode = 'list'">차량 목록</button>
      <button @click="viewMode = 'approve'">승인 대기</button>
      <button @click="viewMode = 'insert'">차량 등록</button>
    </div>

    <VehicleSearch />

    <VehicleForm
      v-if="viewMode === 'insert'"
    />

    <VehicleList
      v-if="viewMode === 'list'"
      :vehicles="vehicleStore.vehicleList"
    />

    <VehicleApprove
      v-if="viewMode === 'approve'"
      :vehicles="vehicleStore.approveList"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useVehicleStore } from './vehicleStore'

import VehicleSearch from './components/VehicleSearch.vue'
import VehicleForm from './components/VehicleForm.vue'
import VehicleList from './components/VehicleList.vue'
import VehicleApprove from './components/VehicleApprove.vue'

const vehicleStore = useVehicleStore()

const viewMode = ref('list')

onMounted(() => {
  vehicleStore.loadVehicleList()
  vehicleStore.loadVehicleApproveList()
})
</script>