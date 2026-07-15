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
          <th>기록 번호</th>
          <th>주차장</th>
          <th>등록 상태</th>
          <th>차량 번호</th>
          <th>촬영 시각</th>
          <th>입출차 구분</th>
          <th>인식률</th>
          <th>상세보기</th>
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="(d, index) in filteredCameraDataList" :key="d.cameraDataNo">
          <td>{{ index + 1 }}</td>
          <td>{{ formatParkingName(d.parkingName) }}</td>
          <td>{{ d.vehicleCarNo ? '등록 차량' : '미등록 차량' }}</td>
          <td>{{ d.carNo || '미인식' }}</td>
          <td>{{ formatDate(d.captureTime) }}</td>
          <td>{{ d.movementTypeText }}</td>
          <td>{{ formatConfidence(d.confidenceScore) }}</td>
          <td>
            <router-link :to="{ name: 'CameraDataDetail', params: { cameraDataNo: d.cameraDataNo } }">
              <button>이미지보기</button>
            </router-link>
          </td>
          <td>
            <button type="button" @click="dStore.remove(d.cameraDataNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="filteredCameraDataList.length === 0">
          <td colspan="9">조회된 카메라 데이터가 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useCameraDataStore } from './cameraDataStore';

const dStore = useCameraDataStore();

const keyword = ref("");
const isSearchMode = ref(false);

let listRefreshTimer = null;
let listRefreshing = false;

const filteredCameraDataList = computed(() => {
  return dStore.displayList;
});

const formatParkingName = (value) => {
  if (!value) return '-';

  const match = String(value).match(/[A-Za-z]+/);

  return match ? match[0].toUpperCase() : value;
};

const searchGo = async () => {
  const carNo = keyword.value.trim();

  if (!carNo) {
    isSearchMode.value = false;
    await dStore.loadList();
    return;
  }

  isSearchMode.value = true;
  await dStore.searchByCarNo(carNo);
};

const resetList = async () => {
  keyword.value = "";
  isSearchMode.value = false;
  await dStore.loadList();
};

const refreshCameraDataList = async () => {
  if (listRefreshing || isSearchMode.value) return;

  listRefreshing = true;

  try {
    await dStore.loadList();
  } finally {
    listRefreshing = false;
  }
};

const formatDate = (value) => {
  if (!value) return '-';

  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR');
};

const formatConfidence = (value) => {
  if (value === null || value === undefined) return '-';

  return `${Number(value).toFixed(1)}%`;
};

onMounted(async () => {
  await dStore.loadList();

  listRefreshTimer = window.setInterval(refreshCameraDataList, 3000);
});

onUnmounted(() => {
  if (listRefreshTimer) {
    window.clearInterval(listRefreshTimer);
  }
});
</script>