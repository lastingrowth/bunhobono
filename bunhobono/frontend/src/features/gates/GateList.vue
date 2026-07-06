<template>
  <div>
    <h2>게이트 목록</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Gate No</th>
          <th>Parking No</th>
          <th>Gate Name</th>
          <th>Gate Type</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="g in gStore.list" :key="g.gateNo">
          <td>{{ g.gateNo }}</td>
          <td>{{ g.parkingNo }}</td>
          <td>{{ g.gateName }}</td>
          <td>{{ g.gateType }}</td>
          <td>
            <button @click="goDetail(g.gateNo)">상세보기</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script setup>
import { onMounted } from 'vue';
import { useGateStore } from './gateStore';
import { useRouter } from 'vue-router';

const gStore = useGateStore()
const router = useRouter()

onMounted(() => {
  gStore.loadList();
})

const goDetail = (gateNo) => {
  router.push(`/gates/${gateNo}/detail`);
};
</script>