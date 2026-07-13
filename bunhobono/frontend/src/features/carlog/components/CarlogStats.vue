<template>
  <div>
    <div>전체 {{ carlogStore.totalCount }}</div>
    <div>주차중 {{ carlogStore.parkingCount }}</div>
    <div>출차완료 {{ carlogStore.outCount }}</div>
    <div>방문차량 {{ carlogStore.visitCount }}</div>
  </div>
</template>

<script setup>
import { useCarlogStore } from '../carlogStore'

const carlogStore = useCarlogStore()
</script>