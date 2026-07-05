<template>
  <div>
    <h2>게이트 상세</h2>
    <div v-if="gateStore.detail">
      <p>게이트 이름: {{ gateStore.detail.gateName }}</p>
      <p>게이트 타입: {{ gateStore.detail.gateType }}</p>
      <p>주차장 이름: {{ gateStore.detail.parkingName }}</p>
      <p>주차장 위치: {{ gateStore.detail.parkingLocation }}</p>
      
    </div>
    <button @click="goBack">뒤로가기</button>
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useGateStore } from "./gateStore";


const gateStore = useGateStore();
const route = useRoute();
const router= useRouter();

onMounted(() => {
    gateStore.loadDetail(route.params.gateNo);
});

const goBack = () => {
  router.back(); 
};
</script>

