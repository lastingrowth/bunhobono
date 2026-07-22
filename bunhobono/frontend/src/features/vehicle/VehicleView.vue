<template>
  <section>
    <div class="vehicle-page-header">
      <h2 class="vehicle-page-title">차량 관리</h2>

      <nav class="vehicle-tabs" aria-label="차량 관리 메뉴">
        <button
          type="button"
          class="vehicle-tab register-tab"
          :class="{ active: viewMode === 'form' }"
          :aria-pressed="viewMode === 'form'"
          @click="openVehicleForm"
        >
          차량 등록
        </button>
        <button
          v-if="viewMode === 'form'"
          type="button"
          class="vehicle-tab list-tab"
          @click="openVehicleList"
        >
          목록으로
        </button>
        <button
          type="button"
          class="vehicle-tab approve-tab"
          :class="{ active: viewMode === 'approve' }"
          :aria-pressed="viewMode === 'approve'"
          @click="openVehicleApprove"
        >
          승인 대기
        </button>
        <button
          v-if="viewMode === 'approve'"
          type="button"
          class="vehicle-tab list-tab"
          @click="openVehicleList"
        >
          목록으로
        </button>
      </nav>

      <select
        v-if="viewMode === 'list'"
        v-model="sortMode"
        class="sort-select"
      >
        <option value="latest">최신순</option>
        <option value="oldest">오래된순</option>
      </select>
    </div>

    <div v-if="viewMode === 'list'" class="vehicle-list-toolbar">
      <VehicleSearch
        v-model:filter-type="filterType"
        @clear-initial-filter="vehicleInitialFilter = 'all'"
      />
    </div>

    <VehicleList
      v-if="viewMode === 'list'"
      :vehicles="vehicleStore.vehicleList"
      :initial-filter="vehicleInitialFilter"
      :filter-type="filterType"
      :sort-mode="sortMode"
    >
      <template v-if="fromStatistics" #pagination-action>
        <button
          class="back-button statistics-back-button"
          type="button"
          @click="backToStatistics"
        >
          ← 통계로 돌아가기
        </button>
      </template>
    </VehicleList>

    <VehicleForm
      v-if="viewMode === 'form'"
      @back="openVehicleList"
    />

    <VehicleApprove
      v-if="viewMode === 'approve'"
      :vehicles="vehicleStore.approveList"
      @back="openVehicleList"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useVehicleStore } from './vehicleStore'

import VehicleSearch from './components/VehicleSearch.vue'
import VehicleForm from './components/VehicleForm.vue'
import VehicleList from './components/VehicleList.vue'
import VehicleApprove from './components/VehicleApprove.vue'

const vehicleStore = useVehicleStore()
const route = useRoute()
const router = useRouter()

const viewMode = ref('list')
const filterType = ref('all')
const sortMode = ref('latest')

// 통계 화면에서 '주차중 방문 만료 차량'을 눌러 들어온 경우
// 차량 목록 화면은 그대로 쓰되, 처음 필터만 전용 필터로 열어준다.
const vehicleInitialFilter = ref('all')

// 통계 페이지에서 들어온 경우에만 뒤로가기 버튼을 보여준다.
const fromStatistics = computed(() => {
  return route.name === 'ParkedExpiredVehicleList'
})

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

// 통계 페이지로 돌아간다.
function backToStatistics() {
  router.push('/admin/statistics')
}

onMounted(() => {
  if (route.query.mode === 'approve') {
    viewMode.value = 'approve'
    vehicleStore.loadVehicleApproveList()
    return
  }

  if (route.name === 'ParkedExpiredVehicleList') {
    vehicleInitialFilter.value = 'parkedExpired'
  }

  viewMode.value = 'list'
  vehicleStore.loadVehicleList()
})
</script>

<style scoped>
.vehicle-page-title {
  margin: 0;
  white-space: nowrap;
}

.vehicle-page-header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 12px;
}

.sort-select {
  width: 100px;
  height: 36px;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  text-align: center;
}

.vehicle-tabs {
  display: inline-flex;
  margin-left: auto;
  gap: 8px;
}

.vehicle-tab {
  min-width: 100px;
  height: 36px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.vehicle-tab:hover {
  background: #e5e7eb;
  color: #111827;
}

.vehicle-tab.active {
  color: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.18);
}

.register-tab {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}

.register-tab:hover,
.register-tab.active {
  border-color: #1d4ed8;
  background: #1d4ed8;
  color: #fff;
}

.approve-tab {
  border-color: #d97706;
  background: #f59e0b;
  color: #fff;
}

.approve-tab:hover,
.approve-tab.active {
  border-color: #b45309;
  background: #d97706;
  color: #fff;
}

.vehicle-list-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0;
}

.back-button {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  cursor: pointer;
}

.back-button:hover {
  background: #f3f4f6;
}

@media (max-width: 520px) {
  .vehicle-tabs {
    display: flex;
    width: 100%;
  }

  .vehicle-tab {
    flex: 1;
    min-width: 0;
    padding: 0 8px;
  }
}
</style>
