<template>
  <div class="management-list-page camera-data-list-page">
    <Transition name="camera-data-toast">
      <div
        v-if="dStore.feedbackMessage"
        class="camera-data-feedback-toast"
        :class="dStore.feedbackType"
        role="status"
      >
        {{ dStore.feedbackMessage }}
      </div>
    </Transition>
    <div class="management-list-header camera-data-heading-row">
      <h2 class="management-list-title">카메라 기록 관리</h2>
    </div>

    <div class="camera-data-toolbar management-list-toolbar">
      <div class="camera-data-search">
        <input
          v-model="keyword"
          class="management-car-search-input"
          type="search"
          placeholder="차량번호 검색"
          aria-label="차량번호 검색"
          @keyup.enter="searchGo"
        >
        <button class="management-search-button" type="button" :disabled="isSearching" @click="searchGo">
          {{ isSearching ? '검색 중...' : '검색' }}
        </button>
        <button class="management-reset-button" type="button" :disabled="isSearching" @click="resetList">
          초기화
        </button>
      </div>

      <div class="status-filters" aria-label="주차장 카메라 로그 필터">
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

    <div class="admin-table-scroll management-list-table">
    <table class="camera-data-table" border="1">
      <thead>
        <tr>
          <th>번호</th>
          <th>주차장</th>
          <th>등록 상태</th>
          <th>차량 번호</th>
          <th>촬영 시각</th>
          <th>입출차 구분</th>
          <th>인식 신뢰도</th>
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
          <td class="camera-data-action"><router-link :to="{ name: 'CameraDataDetail', params: { cameraDataNo: d.cameraDataNo } }"><button>이미지보기</button></router-link></td>
          <td class="camera-data-action"><button type="button" @click="requestDelete(d)">삭제</button></td>
        </tr>

        <tr v-if="filteredCameraDataList.length === 0">
          <td colspan="9">조회된 카메라 데이터가 없습니다.</td>
        </tr>
      </tbody>
    </table>
    </div>
    <div class="admin-pagination-area">
    <pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-numbers="pageNumbers"
      @change-page="setPage"/>
    </div>
    <CameraDataDeleteConfirm
      :open="Boolean(pendingDeleteData)"
      :car-no="pendingDeleteData?.carNo || ''"
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useCameraDataStore } from './cameraDataStore';
import { usePagination } from '@/shared/pagination/usePagination';
import Pagination from '@/shared/pagination/Pagination.vue';
import CameraDataDeleteConfirm from './CameraDataDeleteConfirm.vue';

const dStore = useCameraDataStore();
const route = useRoute();
const router = useRouter();

const keyword = ref("");
const isSearching = ref(false);
const searchError = ref("");
const pendingDeleteData = ref(null);
const deleting = ref(false);
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

const requestDelete = (cameraData) => {
  pendingDeleteData.value = cameraData;
};

const cancelDelete = () => {
  if (!deleting.value) {
    pendingDeleteData.value = null;
  }
};

const confirmDelete = async () => {
  if (!pendingDeleteData.value || deleting.value) {
    return;
  }

  deleting.value = true;
  await dStore.remove(pendingDeleteData.value.cameraDataNo);
  deleting.value = false;
  pendingDeleteData.value = null;
};

onMounted(async () => {
  await dStore.loadList();
});
</script>

<style scoped>
.camera-data-table th,
.camera-data-table td {
  box-sizing: border-box;
  height: 30px !important;
  padding: 4px 7px !important;
  font-size: 13px;
  line-height: 1.3;
  text-align: center !important;
  vertical-align: middle;
}

.camera-data-table tbody tr {
  height: 30px !important;
}

.camera-data-action {
  height: 30px !important;
  line-height: 1;
}

.camera-data-action a {
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  vertical-align: middle;
  text-decoration: none;
}

.camera-data-action button {
  box-sizing: border-box;
  width: auto;
  min-width: 52px;
  height: 22px !important;
  min-height: 0 !important;
  padding: 2px 8px !important;
  line-height: 16px;
  font-size: 12px;
  vertical-align: middle;
  white-space: nowrap;
}

.camera-data-feedback-toast {
  position: fixed;
  z-index: 1200;
  top: 24px;
  right: 24px;
  padding: 11px 16px;
  border: 1px solid #9fcfb0;
  border-radius: 8px;
  color: #1f6840;
  background: #ecf8f0;
  box-shadow: 0 8px 24px rgba(23, 45, 34, 0.18);
  font-size: 13px;
  font-weight: 800;
}

.camera-data-feedback-toast.error {
  border-color: #e3adad;
  color: #9f2f2f;
  background: #fff0f0;
}

.camera-data-toast-enter-active,
.camera-data-toast-leave-active { transition: opacity .18s ease, transform .18s ease; }
.camera-data-toast-enter-from,
.camera-data-toast-leave-to { opacity: 0; transform: translateY(-8px); }

@media (max-width: 1000px) {
  .camera-data-table th,
  .camera-data-table td,
  .camera-data-action button { font-size: 12px; }
}

@media (max-width: 700px) {
  .camera-data-table th,
  .camera-data-table td,
  .camera-data-action button { font-size: 11px; }
}

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
