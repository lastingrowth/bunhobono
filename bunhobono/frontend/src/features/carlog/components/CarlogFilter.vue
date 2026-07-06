<template>
  <div>
    <input
      v-model="carlogStore.search.carNo"
      placeholder="차량번호 검색"
      @keyup.enter="carlogStore.loadCarLogs"
    >

    <select v-model="carlogStore.search.parkingNo">
      <option :value="null">전체 주차장</option>
      <option :value="1">1번 주차장</option>
      <option :value="2">2번 주차장</option>
      <option :value="3">3번 주차장</option>
      <option :value="4">4번 주차장</option>
    </select>

    <select v-model="carlogStore.search.gateNo">
      <option :value="null">전체 게이트</option>
      <option :value="1">1번 게이트</option>
      <option :value="2">2번 게이트</option>
      <option :value="3">3번 게이트</option>
      <option :value="4">4번 게이트</option>
    </select>

    <select v-model="carlogStore.search.carKind">
      <option value="">전체 차량종류</option>
      <option value="REGISTERED">등록차량</option>
      <option value="VISIT">방문차량</option>
      <option value="UNKNOWN">미등록차량</option>
    </select>

    <select v-model="carlogStore.search.parkingState">
      <option value="">전체 주차상태</option>
      <option value="PARKING">주차중</option>
      <option value="OUT">출차완료</option>
    </select>

    <select v-model="carlogStore.search.sort">
      <option value="latest">최신순</option>
      <option value="oldest">오래된순</option>
    </select>

    <button @click="carlogStore.loadCarLogs">검색</button>
    <button @click="carlogStore.resetSearch">초기화</button>
  </div>
</template>

<script setup>
import { useCarlogStore } from '../carlogStore'

const carlogStore = useCarlogStore()
</script>