<template>
    <section class="carlog-page">
        <header class="page-head">
            <h2>Car-log</h2>
            <button @click="carlogStore.loadCarLogs">새로고침</button>
        </header>

        <div class="summary">
            <div class="summary-box">전체 {{ carlogStore.totalCount }}</div>
            <div class="summary-box">주차중 {{ carlogStore.parkingCount }}</div>
            <div class="summary-box">출차완료 {{ carlogStore.outCount }}</div>
            <div class="summary-box">방문차량 {{ carlogStore.visitCount }}</div>
            </div>

            <div class="filter-bar">
            <select v-model="carlogStore.search.parkingState" @change="carlogStore.loadCarLogs">
                <option value="">전체상태</option>
                <option value="PARKING">주차중</option>
                <option value="OUT">출차완료</option>
            </select>

            <select v-model="carlogStore.search.carKind" @change="carlogStore.loadCarLogs">
                <option value="">전체차량</option>
                <option value="REGISTERED">등록차량</option>
                <option value="VISIT">방문차량</option>
                <option value="UNKNOWN">미등록차량</option>
            </select>

            <select v-model="carlogStore.search.sort" @change="carlogStore.loadCarLogs">
                <option value="latest">최신순</option>
                <option value="oldest">오래된순</option>
            </select>

            <input
                v-model="carlogStore.search.carNo"
                placeholder="차량번호 검색"
                @keyup.enter="carlogStore.loadCarLogs"
            >

            <button @click="carlogStore.loadCarLogs">검색</button>
            <button @click="carlogStore.resetSearch">초기화</button>
        </div>

        <table class="log-table">
            <thead>
                <tr>
                <th>번호</th>
                <th>입차게이트</th>
                <th>차량번호</th>
                <th>차량구분</th>
                <th>승인상태</th>
                <th>입차시간</th>
                <th>출차시간</th>
                <th>주차장</th>
                </tr>
            </thead>

            <tbody>
                <tr v-for="log in carlogStore.carLogs" :key="log.carLogNo">
                <td>{{ log.carLogNo }}</td>
                <td>{{ log.inGateName || log.inGateNo }}</td>
                <td>{{ log.carNo || '미인식' }}</td>
                <td>
                    <span :class="['badge', log.carKind]">
                    {{ log.carKind }}
                    </span>
                </td>
                <td>{{ log.vehicleStatus || '-' }}</td>
                <td>{{ log.inTime }}</td>
                <td>{{ log.outTime || '주차중' }}</td>
                <td>{{ log.parkingName || '-' }}</td>
                </tr>
            </tbody>
        </table>
  </section>
</template>

<script setup>
import { onMounted } from 'vue';
import { useCarlogStore } from './carlogStore';

const carlogStore = useCarlogStore();

onMounted(() => {
    carlogStore.loadCarLogs();
});
</script>