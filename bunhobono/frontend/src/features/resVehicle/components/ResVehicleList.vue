<template>
  <div>
    <h3>내 차량 목록</h3>

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
          <th>관리</th>
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
          <td>
            <button @click="$emit('edit', vehicle)">수정</button>
            <button @click="$emit('remove', vehicle.vehicleCarNo)">삭제</button>
          </td>
        </tr>

        <tr v-if="vehicles.length === 0">
          <td colspan="8" align="center">등록된 차량이 없습니다.</td>
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
  }
})

defineEmits(['edit', 'remove'])
</script>