<template>
  <div>
    <h2>카메라 데이터 목록</h2>

    <div class="camera-data-toolbar">
      <div class="camera-data-search">
        <input
          v-model="keyword"
          type="search"
          placeholder="차량번호를 입력하세요"
          aria-label="차량번호 검색"
          @keyup.enter="searchGo"
        >
        <button type="button" :disabled="isSearching" @click="searchGo">
          {{ isSearching ? '검색 중...' : '검색' }}
        </button>
        <button type="button" :disabled="isSearching" @click="resetList">
          전체보기
        </button>
      </div>

      <div class="status-filters" aria-label="주차장 카메라 로그 필터">
      <label class="status-filter">
        <input
          type="radio"
          name="cameraDataParking"
          :checked="!selectedParkingNo"
          @change="selectParking(null)"
        >
        <span>전체</span>
      </label>

      <label
        v-for="parking in parkingButtons"
        :key="parking.parkingNo"
        class="status-filter"
      >
        <input
          type="radio"
          name="cameraDataParking"
          :value="parking.parkingNo"
          :checked="selectedParkingNo === parking.parkingNo"
          @change="selectParking(parking.parkingNo)"
        >
        <span>{{ parking.label }} 주차장</span>
      </label>
      </div>
    </div>

    <p v-if="searchError" class="search-error">{{ searchError }}</p>

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
        <tr v-for="(d, index) in paginatedItems" :key="d.cameraDataNo">
          <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
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
    <pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @change-page="setPage"/>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useCameraDataStore } from './cameraDataStore';
import { usePagination } from '@/shared/pagination/usePagination';
import Pagination from '@/shared/pagination/Pagination.vue';

const dStore = useCameraDataStore();
const route = useRoute();
const router = useRouter();

const keyword = ref("");
const isSearching = ref(false);
const searchError = ref("");
const parkingButtons = [
  { parkingNo: 1, label: 'A' },
  { parkingNo: 2, label: 'B' },
  { parkingNo: 3, label: 'C' },
  { parkingNo: 4, label: 'D' }
];

const selectedParkingNo = computed(() => {
  const parkingNo = Number(route.query.parkingNo);

  return Number.isInteger(parkingNo) && parkingNo > 0 ? parkingNo : null;
});

const filteredCameraDataList = computed(() => {
  if (!selectedParkingNo.value) return dStore.displayList;

  return dStore.displayList.filter((data) => {
    return Number(data.parkingNo) === selectedParkingNo.value;
  });
});

const pageSize = 10;

// usePagination(리스트, 한 화면에 보여줄 목록 수)
const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(filteredCameraDataList, pageSize);

const formatParkingName = (value) => {
  if (!value) return '-';

  const match = String(value).match(/[A-Za-z]+/);

  return match ? match[0].toUpperCase() : value;
};

const searchGo = async () => {
  const carNo = keyword.value.trim();

  if (!carNo) {
    await resetList();
    return;
  }

  isSearching.value = true;
  searchError.value = "";

  try {
    // 백엔드 차량번호 검색 결과가 주차장 필터에 가려지지 않도록 전체로 전환
    await router.replace({ name: 'CameraDataList' });
    await dStore.searchByCarNo(carNo);
  } catch (error) {
    console.error('카메라 데이터 검색 실패', error);
    searchError.value = '검색 결과를 불러오지 못했습니다.';
  } finally {
    isSearching.value = false;
  }
};

const selectParking = async (parkingNo) => {
  await router.replace({
    name: 'CameraDataList',
    query: parkingNo ? { parkingNo } : {}
  });
};

const resetList = async () => {
  keyword.value = "";
  searchError.value = "";
  await router.replace({ name: 'CameraDataList' });
  await dStore.loadList();
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
});
</script>

<style scoped>
.camera-data-toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.camera-data-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.camera-data-search input {
  width: 280px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  outline: none;
  color: var(--text-color);
  background: var(--bg-header);
}

.camera-data-search input:focus {
  border-color: var(--primary);
}

.search-error {
  margin-bottom: 16px;
  color: var(--text-color);
}

@media (max-width: 760px) {
  .camera-data-toolbar,
  .camera-data-search {
    align-items: stretch;
    flex-direction: column;
  }

  .camera-data-search input {
    width: 100%;
  }
}
</style>
