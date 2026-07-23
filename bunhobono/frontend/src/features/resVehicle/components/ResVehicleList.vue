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
  showManage: {
    type: Boolean,
    default: false
  }
});

defineEmits(["edit", "remove"]);
</script>