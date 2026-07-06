<template>
  <div>
    <h2>주차장 목록</h2>
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
import { useParkingsStore } from './parkingsStore';

const pStore = useParkingsStore();

onMounted(() => {
     pStore.loadList();
});
</script>
