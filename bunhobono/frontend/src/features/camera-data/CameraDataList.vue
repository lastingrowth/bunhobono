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
          <th>관리</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="(d, index) in visibleCameraDataList"
          :key="d.cameraDataNo"
          :class="{ 'has-camera-note': hasCameraNote(d) }"
        >
          <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td>{{ formatParkingName(d.parkingName) }}</td>
          <td>{{ d.vehicleCarNo ? '등록 차량' : '미등록 차량' }}</td>
          <td>
            <router-link
              :to="{
                name: 'CameraDataDetail',
                params: { cameraDataNo: d.cameraDataNo },
                query: { ...route.query, page: currentPage }
              }"
            >
              {{ d.carNo || '미인식' }}
            </router-link>
          </td>
          <td>{{ formatDate(d.captureTime) }}</td>
          <td>{{ d.movementTypeText }}</td>
          <td>{{ formatConfidence(d.confidenceScore) }}</td>

          <td class="camera-data-action"><button class="list-delete-text" type="button" @click="requestDelete(d)">삭제</button></td>
        </tr>

        <tr v-if="visibleCameraDataList.length === 0">
          <td colspan="8">조회된 카메라 데이터가 없습니다.</td>
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

const visibleCameraDataList = computed(() => dStore.displayList);
const currentPage = computed(() => dStore.currentPage);
const totalPages = computed(() => dStore.totalPages);
const pageSize = computed(() => dStore.pageSize);
const pageNumbers = computed(() => {
  const start = Math.floor((currentPage.value - 1) / 5) * 5 + 1;
  const end = Math.min(start + 4, totalPages.value);
  return Array.from({ length: Math.max(0, end - start + 1) }, (_, index) => start + index);
});

const loadPage = async (page = 1) => {
  const carNo = keyword.value.trim();

  if (carNo) {
    await dStore.searchByCarNo(carNo, page, selectedParkingNo.value);
  } else {
    await dStore.loadList(page, selectedParkingNo.value);
  }
};

const setPage = async (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return;
  await router.replace({ query: { ...route.query, page } });
  await loadPage(page);
};

const formatParkingName = (value) => {
  if (!value) return '-';

  const parkingName = String(value).trim();

  if (/지하\s*1\s*층|\bB1\b/i.test(parkingName)) return 'B1';
  if (/지하\s*2\s*층|\bB2\b/i.test(parkingName)) return 'B2';
  if (/지상|\b1F\b|1\s*층/i.test(parkingName)) return '1F';

  return parkingName;
};

const searchGo = async () => {
  if (!keyword.value.trim()) {
    await resetList();
    return;
  }

  isSearching.value = true;
  searchError.value = "";

  try {
    await router.replace({
      query: { ...route.query, keyword: keyword.value.trim(), page: 1 }
    });
    await loadPage(1);
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
    query: {
      ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
      ...(parkingNo ? { parkingNo } : {}),
      page: 1,
    }
  });
  await loadPage(1);
};

const resetList = async () => {
  keyword.value = "";
  searchError.value = "";
  await router.replace({ name: 'CameraDataList' });
  await loadPage(1);
};

const formatDate = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR');
};

const hasCameraNote = (cameraData) => {
  return Boolean(String(cameraData?.camNote ?? "").trim());
};

const formatConfidence = (value) => {
  if (value === null || value === undefined) return '-';
  return `${Number(value).toFixed(1)}%`;
};

const requestDelete = (cameraData) => {
  pendingDeleteData.value = cameraData;
};

const cancelDelete = () => {
  if (!deleting.value) pendingDeleteData.value = null;
};

const confirmDelete = async () => {
  if (!pendingDeleteData.value || deleting.value) return;

  deleting.value = true;
  const removed = await dStore.remove(pendingDeleteData.value.cameraDataNo);

  if (removed) {
    const targetPage = visibleCameraDataList.value.length <= 1 && currentPage.value > 1
      ? currentPage.value - 1
      : currentPage.value;
    await loadPage(targetPage);
  }

  deleting.value = false;
  pendingDeleteData.value = null;
};

onMounted(async () => {
  keyword.value = String(route.query.keyword ?? '');
  const routePage = Math.max(1, Number(route.query.page) || 1);
  await loadPage(routePage);
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

.camera-data-table tbody tr.has-camera-note td {
  border-color: #a8c8e8;
  background: #eaf4ff;
}

.camera-data-table tbody tr.has-camera-note:hover td {
  background: #dceeff;
}

.camera-data-table tbody tr.has-camera-note td:first-child {
  box-shadow: inset 3px 0 0 #2f80c9;
}
.camera-data-action {
  height: 30px !important;
  line-height: 1;
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

.camera-data-search .management-search-button {
  min-width: 88px;
  flex: 0 0 88px;
  white-space: nowrap;
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
