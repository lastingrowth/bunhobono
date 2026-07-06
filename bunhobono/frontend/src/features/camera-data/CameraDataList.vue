<template>
  <div>
    <h2>카메라 데이터 목록</h2>
        <div>
          <input v-model="keyword" placeholder="차량번호 검색" />
          <button @click="searchGo">검색</button>
          <button @click="resetList">전체보기</button>
        </div>
    <table border="1">
      <thead>
        <tr>
          <th>display No</th>
          <th>Car No</th>
          <th>Capture Time</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in displayList" :key="d.cameraDataNo">
          <td>{{ d.displayNo }}</td>
          <td>{{ d.carNo }}</td>
          <td>{{ d.captureTime }}</td>
          <td>
            <router-link :to="{ name: 'cameraDatadetail', params: { id: d.cameraDataNo } }">
            <button>상세보기</button>
            </router-link>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useCameraDataStore } from './cameraDataStore';

const dStore = useCameraDataStore();
const keyword = ref("");

const searchGo = async () => {
  await dStore.searchByCarNo(keyword.value);
};

const resetList = async () => {
  keyword.value = "";
  await dStore.loadList();
  dStore.searchResults = [];
};

const displayList = computed(() =>
  dStore.searchResults.length > 0 ? dStore.searchResults : dStore.list
);

onMounted(() => {
  dStore.loadList();
});
</script>
