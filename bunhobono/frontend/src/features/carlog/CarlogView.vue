<template>
  <section class="management-list-page carlog-list-page">
    <CarlogFilter @apply-in-time="applyInTimeFilter" />

    <CarlogDetail :logs="filteredCarLogs" />
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useCarlogStore } from './carlogStore'
import CarlogFilter from './components/CarlogFilter.vue'
import CarlogDetail from './components/CarlogDetail.vue'

const carlogStore = useCarlogStore()
const inTimeFrom = ref('')
const inTimeTo = ref('')

const applyInTimeFilter = ({ from, to }) => {
  inTimeFrom.value = from
  inTimeTo.value = to
}

const filteredCarLogs = computed(() => {
  if (!inTimeFrom.value && !inTimeTo.value) {
    return carlogStore.carLogs
  }

  const from = inTimeFrom.value ? new Date(inTimeFrom.value).getTime() : null
  const to = inTimeTo.value
    ? new Date(inTimeTo.value).getTime() + 59_999
    : null

  return carlogStore.carLogs.filter((log) => {
    if (!log.inTime) {
      return false
    }

    const inTime = new Date(log.inTime).getTime()

    if (from !== null && inTime < from) {
      return false
    }

    if (to !== null && inTime > to) {
      return false
    }

    return true
  })
})

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
