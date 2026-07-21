<template>
  <div class="carlog-filter">
    <div class="carlog-heading-row">
      <h2>입출차 기록</h2>

      <select
        v-model="carlogStore.search.sort"
        class="filter-select sort-select"
        @change="loadCarLogs"
      >
        <option value="latest">최신순</option>
        <option value="oldest">오래된순</option>
      </select>
    </div>

    <div class="carlog-filter-bar">
      <input
        v-model="carNoKeyword"
        class="carlog-search-input"
        placeholder="차량번호 검색"
        @keyup.enter="searchLogs"
      >

      <label class="in-time-filter">
        <input
          v-model="inTimeFromInput"
          class="in-time-input"
          type="datetime-local"
          @keyup.enter="searchLogs"
        >
        <span>~</span>
        <input
          v-model="inTimeToInput"
          class="in-time-input"
          type="datetime-local"
          @keyup.enter="searchLogs"
        >
      </label>

      <select
        v-model="carKindInput"
        class="filter-select kind-select"
      >
        <option value="">전체차량</option>
        <option value="REGISTERED">등록차량</option>
        <option value="VISIT">방문차량</option>
        <option value="UNKNOWN">미등록차량</option>
      </select>

      <button class="search-btn" @click="searchLogs">
        검색
      </button>

      <button class="search-btn reset-btn" @click="resetAll">
        초기화
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useCarlogStore } from '../carlogStore'

const carlogStore = useCarlogStore()

const emit = defineEmits(['apply-in-time'])

const carNoKeyword = ref(carlogStore.search.carNo || '')
const inTimeFromInput = ref('')
const inTimeToInput = ref('')
const carKindInput = ref(carlogStore.search.carKind || '')

async function searchLogs() {
  carlogStore.search.carNo = carNoKeyword.value.trim()
  carlogStore.search.carKind = carKindInput.value
  emit('apply-in-time', {
    from: inTimeFromInput.value,
    to: inTimeToInput.value
  })
  await carlogStore.loadCarLogs()
}

async function resetAll() {
  carNoKeyword.value = ''
  inTimeFromInput.value = ''
  inTimeToInput.value = ''
  carKindInput.value = ''
  emit('apply-in-time', { from: '', to: '' })

  carlogStore.search.carNo = ''
  carlogStore.search.parkingState = ''
  carlogStore.search.carKind = ''
  carlogStore.search.parkingNo = null
  carlogStore.search.gateNo = null
  carlogStore.search.sort = 'latest'

  await carlogStore.loadCarLogs()
}

async function loadCarLogs() {
  await carlogStore.loadCarLogs()
}
</script>

<style scoped>
.carlog-filter {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  margin: 12px 0;
}

.carlog-search-input {
  flex: 1 1 150px;
  width: auto;
  min-width: 130px;
  height: 36px;
  box-sizing: border-box;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
}

.search-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 34px;
  box-sizing: border-box;
  padding: 0;
  border: 1px solid #2563eb;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.reset-btn {
  width: 52px;
  border-color: #d1d5db;
  background: #fff;
  color: #4b5563;
}

.search-btn:hover {
  background: #1d4ed8;
}

.reset-btn:hover {
  background: #f3f4f6;
}

.carlog-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.carlog-heading-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  margin-bottom: 8px;
}

.carlog-heading-row h2 {
  margin: 0 auto 0 0;
}

.in-time-filter {
  display: flex;
  flex: 2 1 340px;
  flex-wrap: nowrap;
  gap: 6px;
  align-items: center;
  max-width: 100%;
  height: 36px;
  white-space: nowrap;
}

.in-time-input {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  height: 36px;
  box-sizing: border-box;
  padding: 0 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
}

.filter-select {
  flex: 0 1 120px;
  width: 120px;
  height: 36px;
  box-sizing: border-box;
  padding: 0 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  text-align: center;
  white-space: nowrap;
}

.carlog-search-input:focus,
.in-time-input:focus,
.filter-select:focus {
  border-color: #2563eb;
  outline: 2px solid rgba(37, 99, 235, 0.14);
  outline-offset: 0;
}

.kind-select {
  flex-basis: 105px;
  width: 105px;
}

.sort-select {
  flex-basis: 100px;
  width: 100px;
}

/* 관리자 관제 테마: scoped 기본 스타일보다 명확하게 우선 적용 */
:global(.admin-layout) .carlog-filter {
  margin-top: 8px;
}

:global(.admin-layout) .carlog-heading-row h2 {
  color: #f1f3f5;
}

:global(.admin-layout) .carlog-filter-bar {
  border-color: #505960;
  background: #2b3035;
  box-shadow: none;
}

:global(.admin-layout) .carlog-search-input,
:global(.admin-layout) .in-time-input,
:global(.admin-layout) .filter-select {
  border-color: #596168;
  color: #f1f3f5;
  background: #343a40;
}

:global(.admin-layout) .carlog-search-input::placeholder,
:global(.admin-layout) .in-time-input::placeholder {
  color: #9da6ad;
  opacity: 1;
}

:global(.admin-layout) .in-time-filter {
  color: #9da6ad;
}

:global(.admin-layout) .carlog-search-input:focus,
:global(.admin-layout) .in-time-input:focus,
:global(.admin-layout) .filter-select:focus {
  border-color: #ffc928;
  outline: 2px solid rgba(255, 201, 40, 0.16);
}

:global(.admin-layout) .search-btn,
:global(.admin-layout) .reset-btn {
  border-color: #69737b;
  color: #ffffff;
  background: #3a4147;
}

:global(.admin-layout) .search-btn:hover,
:global(.admin-layout) .reset-btn:hover {
  border-color: #8d969d;
  color: #ffffff;
  background: #4a5259;
}

@media (max-width: 700px) {
  .carlog-search-input {
    flex: 1 1 180px;
    min-width: 0;
  }

  .in-time-filter {
    width: 100%;
  }

  .in-time-input {
    flex: 1 1 0;
    min-width: 0;
  }

  .filter-select {
    flex: 1 1 140px;
  }

  .carlog-heading-row .sort-select {
    flex: 0 0 100px;
  }
}
</style>
