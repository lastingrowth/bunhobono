<template>
  <section>
    <header>
      <h2>입출차 로그</h2>
    </header>

    <CarlogStats />

    <CarlogFilter />

    <CarlogDetail :logs="carlogStore.carLogs" />
  </section>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useCarlogStore } from './carlogStore'
import CarlogStats from './components/CarlogStats.vue'
import CarlogFilter from './components/CarlogFilter.vue'
import CarlogDetail from './components/CarlogDetail.vue'

const carlogStore = useCarlogStore()

let timer = null

onMounted(() => {
  carlogStore.loadCarLogs()

  timer = setInterval(() => {
    carlogStore.loadCarLogs()
  }, 5000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>