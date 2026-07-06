<template>
  <section>
    <header>
      <h2>Car-log</h2>
    </header>

    <CarlogStats />

    <CarlogFilter />

    <div>
      <button @click="changeViewMode('simple')">기본 로그</button>
      <button @click="changeViewMode('detail')">상세 로그</button>
    </div>

    <CarlogSimple v-if="viewMode === 'simple'" :logs="carlogStore.carLogs" />
    <CarlogDetail v-if="viewMode === 'detail'" :logs="carlogStore.carLogs" />

  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useCarlogStore } from './carlogStore'
import CarlogStats from './components/CarlogStats.vue'
import CarlogFilter from './components/CarlogFilter.vue'
import CarlogSimple from './components/CarlogSimple.vue'
import CarlogDetail from './components/CarlogDetail.vue'

const carlogStore = useCarlogStore()

const viewMode = ref(localStorage.getItem('carlogViewMode') || 'simple')

let timer = null

function changeViewMode(mode) {
  viewMode.value = mode
  localStorage.setItem('carlogViewMode', mode)
}

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