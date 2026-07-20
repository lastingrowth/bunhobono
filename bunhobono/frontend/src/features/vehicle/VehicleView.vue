<template>
  <section>
    <h2>차량 관리</h2>

    <div>
      <button @click="openVehicleList">차량 목록</button>
      <button @click="openVehicleForm">차량 등록</button>
      <button @click="openVehicleApprove">승인 대기</button>
    </div>

    <VehicleSearch
      v-if="viewMode === 'list'"
    />

    <VehicleList
      v-if="viewMode === 'list'"
      :vehicles="vehicleStore.vehicleList"
    />

    <VehicleForm
      v-if="viewMode === 'form'"
    />

    <VehicleApprove
      v-if="viewMode === 'approve'"
      :vehicles="vehicleStore.approveList"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useVehicleStore } from './vehicleStore'

import VehicleSearch from './components/VehicleSearch.vue'
import VehicleForm from './components/VehicleForm.vue'
import VehicleList from './components/VehicleList.vue'
import VehicleApprove from './components/VehicleApprove.vue'

const vehicleStore = useVehicleStore()
const route = useRoute()

const viewMode = ref('list')

function openVehicleList() {
  viewMode.value = 'list'
  vehicleStore.loadVehicleList()
}

function openVehicleForm() {
  viewMode.value = 'form'
}

function openVehicleApprove() {
  viewMode.value = 'approve'
  vehicleStore.loadVehicleApproveList()
}

onMounted(() => {
  if (route.query.mode === 'approve') {
    viewMode.value = 'approve'
    vehicleStore.loadVehicleApproveList()
    return
  }

  viewMode.value = 'list'
  vehicleStore.loadVehicleList()
})
</script>