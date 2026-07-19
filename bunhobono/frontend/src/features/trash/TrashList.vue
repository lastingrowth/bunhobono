<template>
  <main class="trash-page">
    <div class="trash-header">
      

      <h2>지난기록</h2>

      <div class="trash-filter">
        <label for="dataType">데이터 종류</label>
        <br/>
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
      <div class="trash-search">
        <input
          v-model="trashStore.searchCarNo"
          type="text"
          placeholder="차량번호를 입력하세요"
          @keyup.enter="trashStore.searchByCarNo"
        />

        <button
          type="button"
          @click="trashStore.searchByCarNo"
        >
          검색
        </button>

        <button
          type="button"
          @click="trashStore.resetTrashList"
        >
          목록 초기화
        </button>
      </div>
    </div>

    <div class="trash-table-wrap">
      <table class="trash-table">
        <thead>
          <tr>
            <th>지난기록 번호</th>
            <th>데이터 종류</th>
            <th>차량번호</th>
            <th>삭제 방식</th>
            <th>삭제일시</th>
            <th>상세</th>
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
              <button
                type="button"
                @click="goDetail(item.trashNo)"
              >
                상세보기
              </button>
            </td>
          </tr>

          <tr v-if="trashList.length === 0">
            <td colspan="6" class="empty-message">
              지난기록에 데이터가 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination 
        :current-page="currentPage"
        :total-pages="totalPages"
        :page-numbers="pageNumbers"
        @change-page="setPage" />
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";

import { useTrashStore } from "./trashStore";

import {
  getDataTypeText,
  getDeleteTypeText,
  formatDate,
} from "./trashFormat";
import Pagination from "@/shared/pagination/Pagination.vue";
import { usePagination } from "@/shared/pagination/usePagination";

const router = useRouter();
const trashStore = useTrashStore();

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

onMounted(async () => {
  await trashStore.loadTrashList();
});
</script>
