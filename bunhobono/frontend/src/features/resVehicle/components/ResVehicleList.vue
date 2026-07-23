<template>
  <div>
    <table border="">
      <thead>
        <tr>
          <th>차량번호</th>
          <th>차량종류</th>
          <th>승인상태</th>
          <th>승인일</th>
          <th>등록기간</th>
          <th>만기일</th>
          <th>남은기간</th>
          <th v-if="showManage">관리</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="vehicle in vehicles" :key="vehicle.vehicleCarNo">
          <td>{{ vehicle.carNo }}</td>
          <td>{{ vehicle.vehicleTypeText || vehicle.vehicleType }}</td>
          <td>{{ vehicle.vehicleStatusText || vehicle.vehicleStatus }}</td>
          <td>{{ vehicle.approvedAtText || '-' }}</td>
          <td>{{ vehicle.periodText || '-' }}</td>
          <td>{{ vehicle.endDateText || '-' }}</td>
          <td>{{ vehicle.remainingTimeText || '-' }}</td>

          <td v-if="showManage">
            <button @click="$emit('edit', vehicle)">수정</button>
            <button @click="$emit('remove', vehicle.vehicleCarNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="vehicles.length === 0">
          <td :colspan="showManage ? 8 : 7" align="center">
            {{ emptyMessage }}
            <a
              v-if="emptyActionLabel"
              href="#resident-contact"
              class="empty-action-link"
              @click.prevent="$emit('empty-action')"
            >
              {{ emptyActionLabel }}
            </a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  vehicles: {
    type: Array,
    default: () => []
  },
  emptyMessage: {
    type: String,
    default: "조회된 차량이 없습니다."
  },
  emptyActionLabel: {
    type: String,
    default: ""
  },
  showManage: {
    type: Boolean,
    default: false
  }
});

defineEmits(["edit", "remove", "empty-action"]);
</script>

<style scoped>
.empty-action-link {
  margin-left: 5px;
  color: #287fd5;
  font-size: inherit;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.empty-action-link:hover {
  color: #175fa9;
}
</style>
