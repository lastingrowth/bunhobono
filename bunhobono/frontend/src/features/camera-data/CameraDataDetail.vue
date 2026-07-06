<template>
  <div v-if="dStore.detail">
    <h2>카메라 데이터 상세</h2>
    <p>Camera Data No: {{ dStore.detail.cameraDataNo }}</p>
    <p>Car No: {{ dStore.detail.carNo }}</p>
    <p>Capture Time: {{ dStore.detail.captureTime }}</p>
    <p>Image Path: {{ dStore.detail.imagePath }}</p>
    <p>Confidence Score: {{ dStore.detail.confidenceScore }}</p>
  </div>
  <div v-else>
    <p>데이터를 불러오는 중...</p>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { useRoute } from "vue-router";
import { useCameraDataStore } from "./cameraDataStore";

const route = useRoute();
const dStore = useCameraDataStore();

onMounted(async () => {
  const id = route.params.id;  
  await dStore.loadDetail(id);  
});
</script>
