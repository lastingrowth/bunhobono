<template>
  <main class="trash-page management-list-page trash-list-page">
    <TrashFeedbackToast
      :message="trashStore.feedbackMessage"
      :type="trashStore.feedbackType"
    />
    <div class="trash-header">
      <div class="trash-heading-row management-list-header">
        <h2 class="management-list-title">지난기록</h2>
      </div>

      <div class="trash-search management-list-toolbar">
        <div class="trash-filter">
          <select
            id="dataType"
            :value="trashStore.selectedDataType"
            @change="changeDataType"
          >
            <option value="">전체</option>
            <option value="CAMERA_DATA">카메라 데이터</option>
            <option value="CAR_LOG">입출차 기록</option>
            <option value="NOTICE">알림</option>
          </select>
        </div>

        <input
          v-model="trashStore.searchCarNo"
          class="management-car-search-input"
          type="search"
          placeholder="차량번호 검색"
          @keyup.enter="trashStore.searchByCarNo"
        />

        <button
          class="management-search-button"
          type="button"
          @click="trashStore.searchByCarNo"
        >
          검색
        </button>

        <button
          class="management-reset-button"
          type="button"
          @click="trashStore.resetTrashList"
        >
          초기화
        </button>
      </div>
    </div>

    <div class="trash-table-wrap management-list-table">
      <table class="trash-table">
        <colgroup>
          <col class="trash-number-col">
          <col class="trash-type-col">
          <col class="trash-car-col">
          <col class="trash-delete-type-col">
          <col class="trash-date-col">
          <col class="trash-manage-col">
        </colgroup>
        <thead>
          <tr>
            <th>지난기록 번호</th>
            <th>데이터 종류</th>
            <th>차량번호</th>
            <th>삭제 방식</th>
            <th>삭제일시</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(item, index) in paginatedItems"
            :key="item.trashNo"
          >
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>

            <td>
              {{ getDataTypeText(item.dataType) }}
            </td>

            <td>{{ getTrashCarNo(item) }}</td>

            <td>
              {{ getDeleteTypeText(item.deleteType) }}
            </td>

            <td>
              {{ formatDate(item.deletedAt) }}
            </td>

            <td>
              <div class="trash-actions">
                <button type="button" @click="goDetail(item.trashNo)">상세보기</button>
                <button type="button" class="restore-button" :disabled="restoringTrashNo === item.trashNo" @click="requestRestore(item)">
                  {{ restoringTrashNo === item.trashNo ? '복원 중...' : '복원' }}
                </button>
              </div>
            </td>

          </tr>

          <tr v-if="trashList.length === 0">
            <td colspan="6" class="empty-message">
              지난기록에 데이터가 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="admin-pagination-area">
      <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        @change-page="setPage" />
    </div>
    <TrashRestoreConfirm
      :open="Boolean(pendingRestoreItem)"
      :car-no="getTrashCarNo(pendingRestoreItem)"
      :restoring="Boolean(restoringTrashNo)"
      @cancel="cancelRestore"
      @confirm="confirmRestore"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { useTrashStore } from "./trashStore";

import {
  getDataTypeText,
  getDeleteTypeText,
  formatDate,
} from "./trashFormat";
import Pagination from "@/shared/pagination/Pagination.vue";
import { usePagination } from "@/shared/pagination/usePagination";
import TrashRestoreConfirm from "./TrashRestoreConfirm.vue";
import TrashFeedbackToast from "./TrashFeedbackToast.vue";

const router = useRouter();
const trashStore = useTrashStore();
const restoringTrashNo = ref(null);
const pendingRestoreItem = ref(null);

const pageSize = 10;

const trashList = computed(() => {
  return trashStore.trashList;
})

const {
  currentPage,
  totalPages,
  pageNumbers,
  paginatedItems,
  setPage
} = usePagination(trashList, pageSize);

const getTrashCarNo = (item) => {
  if (!item) {
    return "";
  }

  if (item.carNo) {
    return item.carNo;
  }

  try {
    const data = typeof item.dataJson === "string"
      ? JSON.parse(item.dataJson)
      : item.dataJson;

    return data?.captured_car_no
      || data?.car_no
      || data?.snapshot_car_no
      || data?.snapshot_captured_car_no
      || "-";
  } catch {
    return "-";
  }
};

const changeDataType = async (event) => {
  await trashStore.changeDataType(event.target.value);
};

const goDetail = (trashNo) => {
  router.push(`/admin/trash/${trashNo}`);
};

const requestRestore = (item) => {
  pendingRestoreItem.value = item;
};

const cancelRestore = () => {
  if (!restoringTrashNo.value) {
    pendingRestoreItem.value = null;
  }
};

const confirmRestore = async () => {
  if (!pendingRestoreItem.value || restoringTrashNo.value) {
    return;
  }

  const trashNo = pendingRestoreItem.value.trashNo;
  restoringTrashNo.value = trashNo;

  try {
    const restored = await trashStore.restoreTrashItem(trashNo);
    if (restored) {
      pendingRestoreItem.value = null;
    }
  } finally {
    restoringTrashNo.value = null;
  }
};

onMounted(async () => {
  await trashStore.loadTrashList();
});
</script>

<style scoped>
.trash-table {
  width: 100%;
  table-layout: fixed;
}

.trash-number-col { width: 8%; }
.trash-type-col { width: 20%; }
.trash-car-col { width: 17%; }
.trash-delete-type-col { width: 15%; }
.trash-date-col { width: 25%; }
.trash-manage-col { width: 15%; }

.trash-table th,
.trash-table td {
  box-sizing: border-box;
  height: 30px !important;
  padding: 4px 7px !important;
  font-size: 13px;
  line-height: 1.3;
  text-align: center !important;
  vertical-align: middle;
}

.trash-actions {
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.trash-actions button {
  box-sizing: border-box;
  width: auto;
  min-width: 52px;
  height: 22px !important;
  min-height: 0 !important;
  padding: 2px 8px !important;
  line-height: 16px;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 1000px) {
  .trash-table th,
  .trash-table td,
  .trash-actions button { font-size: 12px; }
}

@media (max-width: 700px) {
  .trash-table th,
  .trash-table td,
  .trash-actions button { font-size: 11px; }
}

.restore-button {
  background: #2563eb;
  color: #fff;
}

.restore-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
</style>
