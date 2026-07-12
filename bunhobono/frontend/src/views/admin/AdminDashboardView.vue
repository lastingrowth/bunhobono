<template>
  <section class="admin-dashboard">
    <div class="dashboard-header">
      <div>
        <h2>관리자 대시보드</h2>
        <p>주차 관리 현황을 한눈에 확인하세요.</p>
      </div>

      <button
        type="button"
        class="dashboard-refresh"
        @click="loadDashboard"
      >
        새로고침
      </button>
    </div>

    <p
      v-if="loading"
      class="dashboard-message"
    >
      현황을 불러오는 중입니다.
    </p>

    <p
      v-else-if="errorMessage"
      class="dashboard-error"
    >
      {{ errorMessage }}
    </p>

    <div class="dashboard-grid">
      <!-- 미처리 알림 -->
      <button
        type="button"
        class="dashboard-card notice-card"
        @click="router.push('/admin/notice')"
      >
        <div class="card-heading">
          <span class="card-icon">🔔</span>
          <span>미처리 알림</span>
        </div>

        <strong class="card-value">
          {{ unresolvedNoticeCount }}
          <small>건</small>
        </strong>

        <span class="card-description">
          확인이 필요한 알림
        </span>
      </button>

      <!-- 차량 현황 -->
      <button
        type="button"
        class="dashboard-card"
        @click="router.push('/admin/vehicles')"
      >
        <div class="card-heading">
          <span class="card-icon">🚗</span>
          <span>차량 현황</span>
        </div>

        <strong class="card-value">
          {{ registeredVehicleCount }}
          <small>대</small>
        </strong>

        <span class="card-description">
          전체 등록 차량
        </span>
      </button>

      <!-- 주차장 현황 -->
      <button
        type="button"
        class="dashboard-card parking-card"
        @click="router.push('/admin/parkings')"
      >
        <div class="card-heading">
          <span class="card-icon">🅿️</span>
          <span>주차장 현황</span>
        </div>

        <div class="parking-content">
          <div
            class="parking-donut"
            :style="{
              '--parking-rate': `${parkingUsageRate}%`
            }"
          >
            <strong>{{ parkingUsageRate }}%</strong>
            <span>사용률</span>
          </div>

          <div class="parking-detail">
            <span>
              전체
              <strong>{{ totalParkingCount }}면</strong>
            </span>

            <span>
              사용
              <strong>{{ occupiedParkingCount }}면</strong>
            </span>

            <span>
              가능
              <strong>{{ availableParkingCount }}면</strong>
            </span>
          </div>
        </div>
      </button>

      <!-- 입출차 현황 -->
      <button
        type="button"
        class="dashboard-card"
        @click="router.push('/admin/carlogs')"
      >
        <div class="card-heading">
          <span class="card-icon">🚙</span>
          <span>입출차 현황</span>
        </div>

        <div class="carlog-summary">
          <div>
            <span>입차</span>

            <strong>
              {{ carlogStore.parkingCount }}건
            </strong>
          </div>

          <div>
            <span>출차</span>

            <strong>
              {{ carlogStore.outCount }}건
            </strong>
          </div>
        </div>

        <span class="card-description">
          전체 기록 {{ carlogStore.totalCount }}건
        </span>
      </button>

      <!-- OCR 성공률 -->
      <article class="dashboard-card ocr-card">
        <div class="card-heading">
          <span class="card-icon">📷</span>
          <span>OCR 성공률</span>
        </div>

        <div class="ocr-content">
          <div
            class="ocr-circle"
            :style="{
              '--ocr-rate': `${ocrSuccessRate}%`
            }"
          >
            <strong>{{ ocrSuccessRate }}%</strong>
          </div>

          <div class="ocr-detail">
            <span>
              성공
              <strong>{{ ocrSuccessCount }}건</strong>
            </span>

            <span>
              실패
              <strong>{{ ocrFailCount }}건</strong>
            </span>

            <span>
              전체
              <strong>{{ ocrTotalCount }}건</strong>
            </span>
          </div>
        </div>
      </article>
    </div>

    <!-- 최근 입출차 기록 -->
    <article class="recent-carlogs">
      <div class="section-heading">
        <h3>최근 입출차 기록</h3>

        <button
          type="button"
          @click="router.push('/admin/carlogs')"
        >
          전체보기
        </button>
      </div>

      <table>
        <thead>
          <tr>
            <th>차량번호</th>
            <th>차량종류</th>
            <th>주차상태</th>
            <th>입출차시간</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="log in recentCarlogs"
            :key="log.carlogNo ?? log.carLogNo"
          >
            <td>{{ log.carNo || '미인식' }}</td>
            <td>{{ log.carKind || '-' }}</td>
            <td>{{ log.parkingState || '-' }}</td>

            <td>
              {{
                log.entryAt
                || log.exitAt
                || log.createdAt
                || '-'
              }}
            </td>
          </tr>

          <tr v-if="recentCarlogs.length === 0">
            <td colspan="4">
              조회된 입출차 기록이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useNoticeStore } from '@/features/notice/noticeStore'
import { useVehicleStore } from '@/features/vehicle/vehicleStore'
import { useParkingsStore } from '@/features/parking/parkingsStore'
import { useCarlogStore } from '@/features/carlog/carlogStore'
import { useCameraDataStore } from '@/features/camera-data/cameraDataStore'

const router = useRouter()

const noticeStore = useNoticeStore()
const vehicleStore = useVehicleStore()
const parkingStore = useParkingsStore()
const carlogStore = useCarlogStore()
const cameraDataStore = useCameraDataStore()

const loading = ref(false)
const errorMessage = ref('')

// 미처리 알림 수
const unresolvedNoticeCount = computed(() => {
  return noticeStore.notices.filter(notice => {
    const status = notice.alertStat ?? notice.alert_stat

    return status === 'Unresolved'
  }).length
})

// 전체 등록 차량 수
const registeredVehicleCount = computed(() => {
  return vehicleStore.vehicleList.length
})

// 전체 주차면 수
const totalParkingCount = computed(() => {
  return parkingStore.list.reduce((total, parking) => {
    return total + Number(parking.parkingSpaces ?? 0)
  }, 0)
})

// 주차 가능한 면 수
const availableParkingCount = computed(() => {
  return parkingStore.list.reduce((total, parking) => {
    return total + Number(parking.availableSpaces ?? 0)
  }, 0)
})

// 현재 사용 중인 주차면 수
const occupiedParkingCount = computed(() => {
  return Math.max(
    totalParkingCount.value - availableParkingCount.value,
    0
  )
})

// 전체 주차면 대비 사용률
const parkingUsageRate = computed(() => {
  if (totalParkingCount.value === 0) {
    return 0
  }

  return Math.round(
    occupiedParkingCount.value
      / totalParkingCount.value
      * 100
  )
})

// 전체 OCR 데이터 수
const ocrTotalCount = computed(() => {
  return cameraDataStore.list.length
})

// OCR 성공 수
const ocrSuccessCount = computed(() => {
  return cameraDataStore.list.filter(data => {
    if (typeof data.recognitionState === 'boolean') {
      return data.recognitionState
    }

    return Boolean(data.carNo)
  }).length
})

// OCR 실패 수
const ocrFailCount = computed(() => {
  return ocrTotalCount.value - ocrSuccessCount.value
})

// OCR 성공률
const ocrSuccessRate = computed(() => {
  if (ocrTotalCount.value === 0) {
    return 0
  }

  return Math.round(
    ocrSuccessCount.value
      / ocrTotalCount.value
      * 100
  )
})

// 최근 입출차 기록 5건
const recentCarlogs = computed(() => {
  return carlogStore.carLogs.slice(0, 5)
})

// 대시보드 데이터 조회
const loadDashboard = async () => {
  loading.value = true
  errorMessage.value = ''

  const results = await Promise.allSettled([
    noticeStore.loadNotices(),
    vehicleStore.loadVehicleList(),
    parkingStore.loadList(),
    carlogStore.loadCarLogs(),
    cameraDataStore.loadList()
  ])

  const failed = results.some(result => {
    return result.status === 'rejected'
  })

  if (failed) {
    errorMessage.value = '일부 현황을 불러오지 못했습니다.'
  }

  loading.value = false
}

onMounted(loadDashboard)
</script>