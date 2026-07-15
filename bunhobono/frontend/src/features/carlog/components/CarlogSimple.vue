<template>
  <table border="">
    <thead>
      <tr>
        <th>차량번호</th>
        <th>주차상태</th>
        <th>차량종류</th>
        <th>주차위치</th>
        <th>관리</th>
      </tr>
    </thead>

    <tbody>
      <tr v-for="log in logs" :key="log.carLogNo">
        <td>{{ log.carNo || '미인식' }}</td>
        <td>{{ log.parkingStateText }}</td>
        <td>{{ log.carKindText }}</td>
        <td>{{ log.parkingName || '-' }}</td>
        <td>
          <button type="button" @click="carlogStore.remove(log.carLogNo)">삭제</button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script setup>
import { useCarlogStore } from '../carlogStore'

const carlogStore = useCarlogStore()

defineProps({
  logs: Array
})
</script>