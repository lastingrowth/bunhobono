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
          <th>차량 번호</th>
          <th>촬영 시각</th>
          <th>입출차 구분</th>
          <th>등록 상태</th>
          <th>등록 기간</th>
          <th>만료일</th>
          <th>남은 시간</th>
          <th>상세보기</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in dStore.displayList" :key="d.cameraDataNo">
          <td>{{ d.displayNo }}</td>
          <td>{{ d.carNo || '미인식' }}</td>
          <td>{{ formatDate(d.captureTime) }}</td>
          <td>{{ d.movementTypeText }}</td>
          <td>{{ d.vehicleCarNo ? '등록 차량' : '미등록 차량' }}</td>
          <td>{{ registrationPeriod(d.startDate, d.endDate, d.vehicleCarNo) }}</td>
          <td>{{ formatDate(d.endDate) }}</td>
          <td>{{ remainingTime(d.endDate, d.vehicleCarNo) }}</td>
          <td>
            <router-link :to="{ name: 'CameraDataDetail', params: { cameraDataNo : d.cameraDataNo } }">
            <button>상세보기</button>
            </router-link>
          </td>
        </tr>

        <tr v-if="dStore.displayList.length === 0">
          <td colspan="9">조회된 카메라 데이터가 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useCameraDataStore } from './cameraDataStore';

const dStore = useCameraDataStore();
const keyword = ref("");

// 차량번호 검색
const searchGo = async () => {
  await dStore.searchByCarNo(keyword.value);
};

// 검색을 해제하고 전체 목록 조회
const resetList = async () => {
  keyword.value = "";
  await dStore.loadList();
};

const formatDate = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR');
};

// 차량 등록 기간
const registrationPeriod = (startDate, endDate, vehicleCarNo) => {
  if (!vehicleCarNo) return '-';
  if (!endDate) return '기간 제한 없음';
  return `${formatDate(startDate)} ~ ${formatDate(endDate)}`;
};

// 차량 등록 만료일까지 남은 식ㄴ
const remainingTime = (endDate, vehicleCarNo) => {
  if (!vehicleCarNo) return '-';
  if (!endDate) return '기간 제한 없음';

  const minutes = Math.floor((new Date(endDate) - new Date()) / 60000);
  
  if (minutes <= 0) return '만료됨';

  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const remainMinutes = minutes % 60;
  
  return days > 0 ? `${days}일 ${hours}시간` : `${hours}시간 ${remainMinutes}분`;
};

// 화면에 들어오면 카메라 데이터와 carlog 조회
onMounted(() => {
  dStore.loadList();
});
</script>
