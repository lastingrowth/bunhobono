<template>
  <section>
    <h2>차량 관리</h2>

    <div>
      <button @click="openVehicleList">차량 목록</button>
      <button @click="toggleVehicleForm">차량 등록</button>
      <button @click="viewMode = 'approve'">승인 대기</button>
    </div>

    <VehicleSearch />

    <VehicleForm
      v-if="viewMode === 'list' && showVehicleForm"
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
import { useRoute } from 'vue-router'

const vehicleStore = useVehicleStore()
const route = useRoute()

const viewMode = ref('list')
const showVehicleForm = ref(false)

const openVehicleList = () => {
  viewMode.value = 'list'
}

const toggleVehicleForm = () => {
  viewMode.value = 'list'
  showVehicleForm.value = !showVehicleForm.value
}

onMounted(() => {
  if (route.query.mode === 'approve') {
    viewMode.value = 'approve'
  }
  vehicleStore.loadVehicleList()
  vehicleStore.loadVehicleApproveList()
})
</script>
