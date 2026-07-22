<template>
  <div class="vehicle-search-bar management-list-toolbar">
    <input
      v-model="carNo"
      class="vehicle-search-input management-car-search-input"
      placeholder="차량번호 검색"
      @keyup.enter="search"
    >

    <button class="search-action-btn management-search-button" @click="search">검색</button>
    <button class="search-action-btn show-all-action-btn management-reset-button" type="button" @click="showAll">
      초기화
    </button>
    <select
      v-model="filterType"
      class="vehicle-filter-select"
      @change="applyFilter"
    >
      <option value="all">전체 차량</option>
      <option value="normal">입주민 차량</option>
      <option value="visit">방문 차량</option>
      <option value="expired">만기 차량</option>
    </select>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useVehicleStore } from '../vehicleStore'

const vehicleStore = useVehicleStore()

const emit = defineEmits(['clear-initial-filter'])

const filterType = defineModel('filterType', { default: 'all' })

const carNo = ref('')

async function search() {

  emit('clear-initial-filter')

  if (carNo.value.trim() === '') {
    await vehicleStore.loadVehicleList()
    return
  }

  await vehicleStore.searchVehicle(carNo.value)
}

async function applyFilter() {

  emit('clear-initial-filter')

  carNo.value = ''

  await vehicleStore.loadVehicleList()
}

async function showAll() {
  carNo.value = ''
  filterType.value = 'all'
  emit('clear-initial-filter')
  await vehicleStore.loadVehicleList()
}

</script>

<style scoped>
.vehicle-search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.vehicle-search-input {
  width: 170px;
  height: 36px;
  box-sizing: border-box;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
}

.vehicle-search-input:focus {
  border-color: #2563eb;
  outline: 2px solid rgba(37, 99, 235, 0.14);
}

.search-action-btn {
  width: 56px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  padding: 0;
  line-height: 1;
  text-align: center;
  white-space: nowrap;
}

.show-all-action-btn {
  width: 68px;
  white-space: nowrap;
}

.vehicle-filter-select {
  width: 130px;
  height: 36px;
  box-sizing: border-box;
  padding: 0 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  text-align: center;
  white-space: nowrap;
}

.vehicle-filter-select:focus {
  border-color: #2563eb;
  outline: 2px solid rgba(37, 99, 235, 0.14);
}
</style>
