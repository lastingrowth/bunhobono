<template>
  <table border="">
    <thead>
      <tr>
        <th>상태</th>
        <th>차량번호</th> 
        <th>차량구분</th> 
        <th>입차시간</th> 
        <th>출차시간</th>
        <th>주차시간</th>
        <th>입차게이트</th>
        <th>출차게이트</th>
        <th>주차장</th>        
        <th>관리</th>
      </tr>
    </thead>

    <tbody>
      <tr v-for="log in logs" :key="log.carLogNo">
        <td>{{ log.parkingStateText }}</td>
        <td>{{ log.carNo || '미인식' }}</td>
        <td>{{ log.carKindText }}</td>
        <td>{{ log.inTimeText }}</td>
        <td>{{ log.outTimeText }}</td>
        <td>{{ log.parkingTimeText }}</td>
        <td>{{ log.inGateText }}</td>
        <td>{{ log.outGateText }}</td>
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