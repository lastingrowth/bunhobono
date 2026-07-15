<template>
    <!-- 점검 중에는 화면 전체를 덮어서 뒤쪽 화면 조작을 막는다 -->
    <div v-if="isMaintenance" class="maintenance-overlay">
        <section class="maintenance-card">
            <span class="maintenance-label">
                SERVER MAINTENANCE
            </span>

            <h1>서버 점검 중입니다</h1>

            <p>
                안정적인 서비스 이용을 위해 잠시 점검을 진행하고 있습니다
                점검이 끝나면 자동으로 화면 이용이 가능합니다.
            </p>

            <div class="maintenance-time">
                <span>남은 시간</span>
                <strong>{{ remainingText }}</strong>
            </div>
        </section>
    </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';

const isMaintenance =  ref(false)
const remainingSeconds = ref(0)

let timer = null

// 새로고침해도 점검 종료 시간을 유지하기 위한 저장 키
const MAINENANCE_KEY = 'maintenanceEndAt'

// 현재는 프론트에서 임시로 10분 점검 시간을 사용한다.
const MAINTENANCE_MINUTES = 10

const remainingText = computed(() => {
    const minutes = Math.floor(remainingSeconds.value / 60)
    const seconds = remainingSeconds.value % 60

    return `${minutes}분 ${String(seconds).padStart(2, '0')}초`
})

// localStorage 에 저장된 점검 종료 시간을 기준으로 남은 시간을 계산한다.
const updateMaintenanceState = () => {
    const endAt = Number(localStorage.getItem(MAINENANCE_KEY))

    if (!endAt || Number.isNaN(endAt)) {
        isMaintenance.value = false
        remainingSeconds.value = 0
        return
    }

    const diff = Math.ceil((endAt - Date.now()) / 1000)

    if (diff <= 0) {
        localStorage.removeItem(MAINENANCE_KEY)
        isMaintenance.value = false
        remainingSeconds.value = 0
        return
    }

    isMaintenance.value = true
    remainingSeconds.value = diff
}

// 브라우저 Console에서 startMaintenance()를  실행하면 10분 점검 화면이 켜진다.
window.startMaintenance = () => {
    const endAt = Date.now() + MAINTENANCE_MINUTES * 60 * 1000

    localStorage.setItem(MAINENANCE_KEY, String(endAt))
    updateMaintenanceState()
}

// 브라우저 Console 에서 stopMaintenance()를 실행하면 점검 화면이 즉시 꺼진다.
window.stopMaintenance = () => {
    localStorage.removeItem(MAINENANCE_KEY)
    updateMaintenanceState()
}

onMounted(() => {
    updateMaintenanceState()

    timer = window.setInterval(() => {
        updateMaintenanceState()
    }, 1000)
})

onUnmounted(() => {
    if (timer) {
        window.clearInterval(timer)
    }
})
</script>

<style scoped>
.maintenance-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(5px);
}

.maintenance-card {
  width: min(460px, 100%);
  padding: 34px 32px;
  box-sizing: border-box;
  text-align: center;
  color: #172033;
  background: #ffffff;
  border-radius: 22px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.28);
}

.maintenance-label {
  color: #168bd1;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.maintenance-card h1 {
  margin: 14px 0 12px;
  font-size: 28px;
}

.maintenance-card p {
  margin: 0;
  color: #667085;
  font-size: 15px;
  line-height: 1.7;
}

.maintenance-time {
  margin-top: 26px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #f1f7ff;
  border: 1px solid #cfe3ff;
  border-radius: 14px;
}

.maintenance-time span {
  color: #667085;
  font-size: 13px;
  font-weight: 700;
}

.maintenance-time strong {
  color: #0f6fd1;
  font-size: 30px;
}
</style>