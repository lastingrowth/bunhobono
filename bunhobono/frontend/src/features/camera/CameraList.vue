<template>
  <div>
    <h2>카메라 목록</h2>
    <table border="1">
      <thead>
        <tr>
          <th>카메라 번호</th>
          <th>게이트 이름</th>
          <th>주차장 이름</th>
          <th>카메라 이름</th>
          <th>카메라 종류</th>
          <th>설치 날짜</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="c in cStore.list" :key="c.cameraNo">
          <td>{{ c.cameraNo }}</td>
          <td>{{ c.gateName ?? '-' }}</td>
          <td>{{ c.parkingName ?? '-' }}</td>
          <td>{{ c.cameraName }}</td>
          <td>{{ c.cameraType }}</td>
          <td>{{ c.installDate }}</td>
        </tr>
      </tbody>
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
