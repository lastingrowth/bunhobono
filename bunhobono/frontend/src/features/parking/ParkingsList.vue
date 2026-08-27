<template>
  <div class="resident-parking-list-page">
    <header class="resident-parking-header">
      <h2>주차장 목록</h2>
      <button type="button" class="resident-home-button" title="홈으로 돌아가기" aria-label="홈으로 돌아가기" @click="router.push('/resident/dashboard')"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11.2 12 4l9 7.2"/><path d="M5.5 10.2V20h13v-9.8"/><path d="M9.5 20v-6h5v6"/></svg></button>
    </header>
    <table border="1">
      <thead>
        <tr>
          <th>No</th>
          <th>Parking Name</th>
          <th>Parking Spaces</th>
          <th>Parking Location</th>
          <th>Available Spaces</th>
          <th>주차중</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in pStore.list" :key="p.parkingNo">
         <td>{{ p.displayNo }}</td>
         <td>{{ p.parkingName }}</td>
         <td>{{ p.parkingSpaces }}</td>
         <td>{{ p.parkingLocation }}</td>
         <td>{{ p.availableSpaces }}</td>
         <td>
            {{ p.parkingSpaces - p.availableSpaces }}/{{ p.parkingSpaces }}
         </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useParkingsStore } from './parkingsStore';

const pStore = useParkingsStore();
const router = useRouter();

onMounted(() => {
     pStore.loadList();
});
</script>

<style scoped>
.resident-parking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.resident-parking-header h2 {
  margin: 0;
}
</style>
