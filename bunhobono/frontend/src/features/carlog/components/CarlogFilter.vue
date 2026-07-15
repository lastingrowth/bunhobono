<template>
  <div class="carlog-filter">
    <div class="carlog-search-row">
      <input
        v-model="carNoKeyword"
        class="carlog-search-input"
        placeholder="차량번호 검색"
        @keyup.enter="searchCarNo"
      >

      <button class="search-btn" @click="searchCarNo">
        검색
      </button>

      <button class="search-btn" @click="resetAll">
        초기화
      </button>
    </div>

    <div class="carlog-filter-bar">
      <button class="filter-btn" @click="showAllLogs">
        전체로그
      </button>

      <button class="filter-btn" @click="toggleParkingState">
        {{ parkingStateButtonText }}
      </button>

      <button class="filter-btn" @click="toggleCarKind">
        {{ carKindButtonText }}
      </button>

      <button class="filter-btn" @click="toggleParking">
        {{ parkingButtonText }}
      </button>

      <button class="filter-btn" @click="toggleSort">
        {{ sortButtonText }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useCarlogStore } from '../carlogStore'

const carlogStore = useCarlogStore()

const carNoKeyword = ref(carlogStore.search.carNo || '')

const parkingStateStep = ref(0)
const carKindStep = ref(0)
const parkingStep = ref(0)

const parkingStateButtonText = computed(() => {
  if (parkingStateStep.value === 1) {
    return '주차중'
  }

  if (parkingStateStep.value === 2) {
    return '출차완료'
  }

  return '전체상태'
})

const carKindButtonText = computed(() => {
  if (carKindStep.value === 1) {
    return '입주민'
  }

  if (carKindStep.value === 2) {
    return '방문차량'
  }

  if (carKindStep.value === 3) {
    return '미등록차량'
  }

  return '전체차량'
})

const parkingButtonText = computed(() => {
  if (parkingStep.value === 1) {
    return '주차장 A'
  }

  if (parkingStep.value === 2) {
    return '주차장 B'
  }

  if (parkingStep.value === 3) {
    return '주차장 C'
  }

  if (parkingStep.value === 4) {
    return '주차장 D'
  }

  return '전체주차장'
})

const sortButtonText = computed(() => {
  if (carlogStore.search.sort === 'oldest') {
    return '오래된순'
  }

  return '최신순'
})

async function searchCarNo() {
  carlogStore.search.carNo = carNoKeyword.value.trim()
  await carlogStore.loadCarLogs()
}

async function showAllLogs() {
  carNoKeyword.value = ''

  carlogStore.search.carNo = ''
  carlogStore.search.parkingState = ''
  carlogStore.search.carKind = ''
  carlogStore.search.parkingNo = null
  carlogStore.search.gateNo = null
  carlogStore.search.sort = 'latest'

  parkingStateStep.value = 0
  carKindStep.value = 0
  parkingStep.value = 0

  await carlogStore.loadCarLogs()
}

async function toggleParkingState() {
  parkingStateStep.value += 1

  if (parkingStateStep.value > 2) {
    parkingStateStep.value = 0
  }

  if (parkingStateStep.value === 0) {
    carlogStore.search.parkingState = ''
  }

  if (parkingStateStep.value === 1) {
    carlogStore.search.parkingState = 'PARKING'
  }

  if (parkingStateStep.value === 2) {
    carlogStore.search.parkingState = 'OUT'
  }

  await carlogStore.loadCarLogs()
}

async function toggleCarKind() {
  carKindStep.value += 1

  if (carKindStep.value > 3) {
    carKindStep.value = 0
  }

  if (carKindStep.value === 0) {
    carlogStore.search.carKind = ''
  }

  if (carKindStep.value === 1) {
    carlogStore.search.carKind = 'REGISTERED'
  }

  if (carKindStep.value === 2) {
    carlogStore.search.carKind = 'VISIT'
  }

  if (carKindStep.value === 3) {
    carlogStore.search.carKind = 'UNKNOWN'
  }

  await carlogStore.loadCarLogs()
}

async function toggleParking() {
  parkingStep.value += 1

  if (parkingStep.value > 4) {
    parkingStep.value = 0
  }

  if (parkingStep.value === 0) {
    carlogStore.search.parkingNo = null
  } else {
    carlogStore.search.parkingNo = parkingStep.value
  }

  await carlogStore.loadCarLogs()
}

async function toggleSort() {
  if (carlogStore.search.sort === 'oldest') {
    carlogStore.search.sort = 'latest'
  } else {
    carlogStore.search.sort = 'oldest'
  }

  await carlogStore.loadCarLogs()
}

async function resetAll() {
  await showAllLogs()
}
</script>

<style scoped>
.carlog-filter {
  width: 760px;
  min-width: 760px;
  max-width: 760px;
  margin: 12px 0;
}

.carlog-search-row {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 760px;
  margin-bottom: 10px;
}

.carlog-search-input {
  width: 300px;
  height: 36px;
  box-sizing: border-box;
}

.search-btn {
  width: 80px;
  height: 36px;
  white-space: nowrap;
}

.carlog-filter-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 760px;
  min-width: 760px;
  max-width: 760px;
}

.filter-btn {
  width: 140px;
  height: 36px;
  text-align: center;
  white-space: nowrap;
}
</style>