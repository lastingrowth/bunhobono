<template>
  <div>
    <h2>카메라 목록</h2>
    <table border="1">
      <tr>
        <td>Camera No</td>
        <td>Parking No</td>
        <td>Gate No</td>
        <td>Camera Name</td>
        <td>Camera Type</td>
        <td>Install Date</td>
      </tr>
      <tr v-for="c in cStore.list" :key="c.cameraNo">
        <td>{{ c.cameraNo }}</td>
        <td>{{ c.parkingNo }}</td>
        <td>{{ c.gateNo }}</td>
        <td>{{ c.cameraName }}</td>
        <td>{{ c.cameraType }}</td>
        <td>{{ c.installDate }}</td>
      </tr>
    </table>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useCameraStore } from './cameraStore';

const cStore = useCameraStore();

onMounted(() => {
  cStore.loadList();
});
</script>
