<template>
  <div>
    <h2>카메라 목록</h2>
    <table border="1">
      <tr>
        <td>카메라 번호</td>
        <td>출입구 번호</td>
        <td>카메라 이름</td>
        <td>카메라 종류</td>
        <td>설치 날짜</td>
      </tr>
      <tr v-for="c in cStore.list" :key="c.cameraNo">
        <td>{{ c.cameraNo }}</td>
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
