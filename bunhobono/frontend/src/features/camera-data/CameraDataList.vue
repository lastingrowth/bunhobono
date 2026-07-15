<template>
  <div>
    <h2>카메라 데이터 목록</h2>
        <div>
          <input v-model="keyword" placeholder="차량번호 검색" />
          <button @click="searchGo">검색</button>
          <button @click="resetList">전체보기</button>
        </div>

        <div class="status-filters">
          <label class="status-filter">
            <input
              v-model="selectedParkingNo"
              type="radio"
              value="all"
            />
            <span>전체</span>
          </label>

          <label
            v-for="parking in parkingOptions"
            :key="parking.parkingNo"
            class="status-filter"
          >
            <input
              v-model="selectedParkingNo"
              type="radio"
              :value="String(parking.parkingNo)"
            />
            <span>{{ formatParkingName(parking.parkingName) }}</span>
          </label>
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
          <th>등록 기간</th>
          <th>만료일</th>
          <th>남은 시간</th>
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
          <td>{{ registrationPeriod(d.startDate, d.endDate, d.vehicleCarNo) }}</td>
          <td>{{ formatDate(d.endDate) }}</td>
          <td>{{ remainingTime(d.endDate, d.vehicleCarNo) }}</td>
          <td>
            <router-link :to="{ name: 'CameraDataDetail', params: { cameraDataNo : d.cameraDataNo } }">
            <button>이미지보기</button>
            </router-link>
          </td>
          <td>
            <button type="button" @click="dStore.remove(d.cameraDataNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="filteredCameraDataList.length === 0">
          <td colspan="12">조회된 카메라 데이터가 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useCameraDataStore } from './cameraDataStore';
import { useParkingsStore } from '../parking/parkingsStore';

const route = useRoute();
const router = useRouter();
const dStore = useCameraDataStore();
const parkingStore = useParkingsStore();
const keyword = ref("");
const selectedParkingNo = ref("all");
let listRefreshTimer = null;
let listRefreshing = false;

const parkingOptions = computed(() => {
  return [...parkingStore.list].sort((a, b) => {
    return Number(a.displayNo ?? a.parkingNo) - Number(b.displayNo ?? b.parkingNo);
  });
});

const filteredCameraDataList = computed(() => {
  const sortLatestFirst = (items) => {
    return [...items].sort((a, b) => {
      const aTime = new Date(a.captureTime ?? 0).getTime();
      const bTime = new Date(b.captureTime ?? 0).getTime();

      if (aTime !== bTime) {
        return bTime - aTime;
      }

      return Number(b.cameraDataNo ?? 0) - Number(a.cameraDataNo ?? 0);
    });
  };

  if (selectedParkingNo.value === "all") {
    return sortLatestFirst(dStore.displayList);
  }

  return sortLatestFirst(dStore.displayList.filter((data) => {
    return Number(data.parkingNo) === Number(selectedParkingNo.value);
  }));
});

const applyParkingQuery = () => {
  selectedParkingNo.value = route.query.parkingNo
    ? String(route.query.parkingNo)
    : "all";
};

const formatParkingName = (value) => {
  if (!value) return '-';

  const match = String(value).match(/[A-Za-z]+/);

  return match ? match[0].toUpperCase() : value;
};

// 차량번호 검색
const searchGo = async () => {
  await dStore.searchByCarNo(keyword.value);
};

// 검색을 해제하고 전체 목록 조회
const resetList = async () => {
  keyword.value = "";
  selectedParkingNo.value = "all";
  await router.replace({
    name: 'CameraDataList'
  });
  await dStore.loadList();
};

const refreshCameraDataList = async () => {
  if (listRefreshing) return;

  listRefreshing = true;

  try {
    const trimmedKeyword = keyword.value.trim();

    if (trimmedKeyword) {
      await dStore.searchByCarNo(trimmedKeyword);
    } else {
      await dStore.loadList();
    }
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
onMounted(async () => {
  applyParkingQuery();

  await Promise.all([
    dStore.loadList(),
    parkingStore.loadList()
  ]);

  listRefreshTimer = window.setInterval(refreshCameraDataList, 3000);
});

onUnmounted(() => {
  if (listRefreshTimer) {
    window.clearInterval(listRefreshTimer);
  }
});

watch(
  () => route.query.parkingNo,
  () => {
    applyParkingQuery();
  }
);
</script>
