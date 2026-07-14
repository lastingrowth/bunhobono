<template>
  <main class="trash-page">
    <div class="trash-header">
      <h2>휴지통</h2>

      <div class="trash-filter">
        <label for="dataType">데이터 종류</label>

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
    </div>

    <div class="trash-table-wrap">
      <table class="trash-table">
        <thead>
          <tr>
            <th>휴지통 번호</th>
            <th>데이터 종류</th>
            <th>삭제 방식</th>
            <th>삭제 일시</th>
            <th>영구삭제 예정일</th>
            <th>상세</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="item in trashStore.trashList"
            :key="item.trashNo"
          >
            <td>{{ item.trashNo }}</td>

            <td>
              {{ getDataTypeText(item.dataType) }}
            </td>

            <td>
              {{ getDeleteTypeText(item.deleteType) }}
            </td>

            <td>
              {{ formatDate(item.deletedAt) }}
            </td>

            <td>
              {{ formatDate(item.purgeAt) }}
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

          <tr v-if="trashStore.trashList.length === 0">
            <td colspan="6" class="empty-message">
              휴지통에 데이터가 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </main>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";

import { useTrashStore } from "./trashStore";

import {
  getDataTypeText,
  getDeleteTypeText,
  formatDate,
} from "./trashFormat";

const router = useRouter();
const trashStore = useTrashStore();

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