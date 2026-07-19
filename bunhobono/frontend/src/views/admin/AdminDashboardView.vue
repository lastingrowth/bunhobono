<template>
  <section class="admin-dashboard">
    <div class="dashboard-header">
      <div>
        <h2>관리자 대시보드</h2>
        <p>주차 관리 현황을 한눈에 확인하세요.</p>
      </div>

      <div class="dashboard-header-actions">
        <button
          type="button"
          class="dashboard-maintenance"
          @click="startMaintenanceMode">
          점검 시작
        </button>

        <button
          type="button"
          class="dashboard-refresh"
          @click="loadDashboard">
          새로고침
        </button>
      </div>
    </div>

    <!-- 데이터 조회 상태 -->
    <p v-if="loading" class="dashboard-message">
      현황을 불러오는 중입니다.
    </p>

    <p v-else-if="errorMessage" class="dashboard-error">
      {{ errorMessage }}
    </p>

    <!-- 상단 현황 카드 -->
    <div class="dashboard-grid">
      <!-- 미처리 알림 -->
      <button
        type="button"
        class="dashboard-card notice-card"
        @click="router.push('/admin/notice')">

        <div class="card-heading">
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
        class="dashboard-card vehicle-card"
        @click="router.push('/admin/vehicles?mode=approve')">

        <div class="card-heading">
          <span>차량 현황</span>
        </div>

        <div class="card-vehicle-summary">
          <div class="vehicle-summary-item">
            <span>승인 대기</span>
            <strong>
              {{ waitingVehicleCount }}
              <small>대</small>
            </strong>
          </div>

          <div class="vehicle-summary-item">
            <span>오늘 방문</span>
            <strong>
              {{ todayVisitVehicleCount }}
              <small>대</small>
            </strong>
          </div>
        </div>
      </button>

      <!-- 실시간 카메라 영상 / OCR 현황 -->
      <article class="dashboard-card parking-overview-card">
        <div class="card-heading">
          <span>실시간 카메라 영상</span>
        </div>

        <div
          v-if="parkingStatusWithOcr.length > 0"
          class="parking-overview-content">

          <!-- A/B/C/D 주차장 사용률 -->
          <div class="parking-status-strip">
            <button
              v-for="parking in parkingStatusWithOcr"
              :key="`status-${parking.parkingNo}`"
              type="button"
              class="parking-zone-status"
              @click="goCameraDataList(parking.parkingNo)">

              <strong class="parking-zone-name">
                {{ parking.parkingName }}
              </strong>

              <div
                class="parking-zone-donut"
                :style="{ '--parking-rate': `${parking.usageRate}%` }">
                <strong>{{ parking.usageRate }}%</strong>
              </div>

              <span class="parking-zone-count">
                {{ parking.occupied }} / {{ parking.total }}면
              </span>

              <span class="parking-zone-available">
                주차 가능 {{ parking.available }}면
              </span>
            </button>
          </div>

          <!-- A/B/C/D 주차장별 최신 OCR 사진 -->
          <div class="parking-ocr-row">
            <article
              v-for="parking in parkingStatusWithOcr"
              :key="`ocr-${parking.parkingNo}`"
              class="parking-ocr-preview">

              <button
                type="button"
                class="parking-ocr-frame"
                :disabled="!parking.ocr.cameraDataNo"
                @click="parking.ocr.cameraDataNo && goCameraDataList(parking.parkingNo)">

                <img
                  v-if="parking.ocr.imageUrl"
                  :src="parking.ocr.imageUrl"
                  :alt="`${parking.parkingName} ${parking.ocr.carNoText} 차량 사진`">

                <span v-else>
                  사진 없음
                </span>
              </button>

              <div class="parking-ocr-info">
                <strong>{{ parking.parkingName }}</strong>
                <span>{{ parking.ocr.carNoText }}</span>
                <span>{{ parking.ocr.movementText }}</span>
                <span>인식률 {{ parking.ocr.confidenceText }}</span>
              </div>

              <!-- 미등록/수동 처리 대상 OCR 데이터가 있을 때 관리자 승인용으로 사용 -->
              <button
                type="button"
                class="camera-gate-button"
                :disabled="!parking.ocr.cameraDataNo || openingCameraDataNo !== null"
                @click="confirmOcrGateOpen(parking.ocr)">
                {{ openingCameraDataNo === parking.ocr.cameraDataNo ? '처리 중...' : '게이트 열기' }}
              </button>
            </article>
          </div>
        </div>

        <p v-else class="parking-empty">
          등록된 주차장 정보가 없습니다.
        </p>
      </article>

      <!-- CCTV 스트림 영역 -->
      <div class="cctv-stream-row">
        <article
          v-for="camera in cctvCameras"
          :key="camera.cameraNo"
          class="cctv-stream-card">

          <div class="cctv-stream-header">
            <strong>{{ camera.title }}</strong>

            <div class="cctv-stream-actions">
              <span>{{ cctvPaused[camera.cameraNo] ? 'PAUSED' : 'LIVE' }}</span>

              <button
                type="button"
                :disabled="cctvLoading[camera.cameraNo] || cctvPaused[camera.cameraNo]"
                @click="pauseCctv(camera.cameraNo)">
                {{ cctvLoading[camera.cameraNo] ? '처리 중...' : '일시정지' }}
              </button>

              <button
                type="button"
                :disabled="cctvLoading[camera.cameraNo] || !cctvPaused[camera.cameraNo]"
                @click="resumeCctv(camera.cameraNo)">
                재생
              </button>
            </div>
          </div>

          <div class="cctv-stream-frame">
            <img
              :src="getCctvStreamUrl(camera.cameraNo)"
              :alt="camera.title">
          </div>
        </article>
      </div>
    </div>

    <!-- 주간 입차 그래프와 입출차 목록 -->
    <article class="recent-carlogs">
      <div class="section-heading">
        <h3>입출차 현황</h3>

        <button
          type="button"
          @click="router.push('/admin/carlogs')">
          전체보기
        </button>
      </div>

      <div class="carlog-dashboard-content">
        <!-- 페이지네이션 입출차 목록 -->
        <section class="carlog-list-panel">
          <div class="carlog-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>차량번호</th>
                  <th>차량종류</th>
                  <th>주차상태</th>
                  <th>입차시간</th>
                  <th>출차시간</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="log in paginatedCarlogs"
                  :key="log.carLogNo ?? log.displayNo">

                  <td>{{ log.carNo || '미인식' }}</td>
                  <td>{{ log.carKindText || log.carKind || '-' }}</td>

                  <td>
                    <span
                      class="carlog-state-badge"
                      :class="{
                        parking: log.parkingState === 'PARKING',
                        out: log.parkingState === 'OUT'
                      }">
                      {{ log.parkingStateText || log.parkingState || '-' }}
                    </span>
                  </td>

                  <td>{{ log.inTimeText || log.inTime || '-' }}</td>
                  <td>{{ log.outTime ? log.outTimeText || log.outTime : '-' }}</td>
                </tr>

                <tr v-if="paginatedCarlogs.length === 0">
                  <td colspan="5">
                    조회된 입출차 기록이 없습니다.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 입출차 목록 페이지 번호 -->
          <div class="carlog-pagination">
            <button
              v-if="currentCarlogPage > 1"
              type="button"
              @click="setCarlogPage(carlogPageNumbers[0] - 1)">
              이전
            </button>

            <button
              v-for="page in carlogPageNumbers"
              :key="page"
              type="button"
              :class="{ active: currentCarlogPage === page }"
              @click="setCarlogPage(page)">
              {{ page }}
            </button>

            <button
              v-if="currentCarlogPage < carlogTotalPages"
              type="button"
              @click="setCarlogPage(carlogPageNumbers[carlogPageNumbers.length - 1] + 1)">
              다음
            </button>
          </div>
        </section>

        <!-- 최근 7일 입차 막대그래프 -->
        <section class="weekly-entry-panel">
          <div class="weekly-entry-header">
            <h4 class="weekly-entry-title">
              최근 7일 입차 기록
            </h4>

            <div class="weekly-entry-legend">
              <span class="resident">입주민</span>
              <span class="visit">방문객</span>
            </div>
          </div>

          <div class="weekly-entry-chart">
            <div
              v-for="day in weeklyEntryStats"
              :key="day.dateKey"
              class="weekly-entry-item">

              <span class="weekly-entry-count">
                {{ day.residentCount + day.visitCount }}건
              </span>

              <div class="weekly-entry-track grouped">
                <div
                  class="weekly-entry-bar resident"
                  :style="{ '--bar-height': `${day.residentBarHeight}%` }" />

                <div
                  class="weekly-entry-bar visit"
                  :style="{ '--bar-height': `${day.visitBarHeight}%` }" />
              </div>

              <span class="weekly-entry-date">
                {{ day.dateLabel }}
              </span>

              <span class="weekly-entry-day">
                {{ day.dayLabel }}
              </span>
            </div>
          </div>
        </section>
      </div>
    </article>
  </section>
</template>

<script setup>
import { useAdminDashboardStore } from '@/stores/adminDashboard'
import { storeToRefs } from 'pinia'
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import '@/assets/css/cctv.css'

const router = useRouter()
const dashboardStore = useAdminDashboardStore()

// store의 상태와 계산 결과를 반응성을 유지한 상태로 사용
const {
  loading,
  errorMessage,
  unresolvedNoticeCount,
  waitingVehicleCount,
  todayVisitVehicleCount,
  parkingStatusWithOcr,
  weeklyEntryStats,
  currentCarlogPage,
  carlogTotalPages,
  carlogPageNumbers,
  paginatedCarlogs
} = storeToRefs(dashboardStore)

const openingCameraDataNo = ref(null)

const CCTV_API_URL = 'http://localhost:8000'

const cctvCameras = [
  { cameraNo: 1, title: 'A 주차장 입차 카메라' },
  { cameraNo: 3, title: 'B 주차장 입차 카메라' },
  { cameraNo: 5, title: 'C 주차장 입차 카메라' },
  { cameraNo: 7, title: 'D 주차장 입차 카메라' },
  { cameraNo: 2, title: 'A 주차장 출차 카메라' },
  { cameraNo: 4, title: 'B 주차장 출차 카메라' },
  { cameraNo: 6, title: 'C 주차장 출차 카메라' },
  { cameraNo: 8, title: 'D 주차장 출차 카메라' }
]

const cctvPaused = ref(
  Object.fromEntries(
    cctvCameras.map((camera) => [camera.cameraNo, true])
  )
)

const cctvLoading = ref(
  Object.fromEntries(
    cctvCameras.map((camera) => [camera.cameraNo, false])
  )
)

let ocrRefreshTimer = null
let ocrRefreshing = false

// 새로고침 버튼과 최초 화면 진입 시 사용
const loadDashboard = async () => {
  await dashboardStore.loadDashboard()
}

// 카메라 데이터 목록으로 이동
const goCameraDataList = (parkingNo) => {
  router.push({
    path: '/admin/camera-data',
    query: {
      parkingNo
    }
  })
}

// OCR 카드의 cameraDataNo를 기준으로 백엔드에 게이트 열기를 요청
const confirmOcrGateOpen = async (ocr) => {
  if (!ocr.cameraDataNo) {
    alert('처리할 OCR 데이터가 없습니다')
    return
  }

  const confirmed = window.confirm(
    `${ocr.carNoText || '미인식'} 차량의 게이트를 열겠습니까?`
  )

  if (!confirmed || openingCameraDataNo.value !== null) {
    return
  }

  openingCameraDataNo.value = ocr.cameraDataNo

  try {
    await dashboardStore.openGateByOcr(ocr.cameraDataNo)
    await dashboardStore.refreshOcrImages()
  } catch (error) {
    console.error(error)

    const status = error.response?.status

    if (status === 409) {
      alert('이미 처리됐거나 게이트가 열려 있습니다')
    } else {
      alert('게이트 열기 처리에 실패했습니다')
    }
  } finally {
    openingCameraDataNo.value = null
  }
}

// MJPEG 8개를 동일 호스트에 연결하면 브라우저 연결 제한에 걸릴 수 있어
// 카메라 1~4와 5~8의 스트림 호스트를 나누어 사용한다.
const getCctvStreamUrl = (cameraNo) => {
  const streamHost = cameraNo <= 4
    ? 'http://localhost:8000'
    : 'http://127.0.0.1:8000'

  return `${streamHost}/cctv/${cameraNo}/stream`
}

const changeCctvState = async (cameraNo, action) => {
  if (cctvLoading.value[cameraNo]) {
    return
  }

  cctvLoading.value[cameraNo] = true

  try {
    const response = await fetch(
      `${CCTV_API_URL}/cctv/${cameraNo}/${action}`,
      { method: 'POST' }
    )

    if (!response.ok) {
      throw new Error(`CCTV ${action} 실패: ${response.status}`)
    }

    cctvPaused.value[cameraNo] = action === 'pause'
  } catch (error) {
    console.error('CCTV 제어 오류', error)

    window.alert(
      action === 'pause'
        ? 'CCTV 영상을 일시정지하지 못했습니다'
        : 'CCTV 영상을 재생하지 못했습니다'
    )
  } finally {
    cctvLoading.value[cameraNo] = false
  }
}

const pauseCctv = (cameraNo) => {
  return changeCctvState(cameraNo, 'pause')
}

const resumeCctv = (cameraNo) => {
  return changeCctvState(cameraNo, 'resume')
}

// 관리자 대시보드에서 점검 화면을 테스트하기 위한 임시 트리거
// 백엔드 점검 API가 연결되면 window.startMaintenance() 대신 API 호출로 변경한다.
const startMaintenanceMode = () => {
  if (window.startMaintenance) {
    window.startMaintenance()
  }
}

// 입출차 목록 페이지 변경
const setCarlogPage = (page) => {
  dashboardStore.setCarlogPage(page)
}

// OCR 업로드 후 대시보드가 자동으로 바뀌도록 주기적으로 최신 데이터 조회
const refreshDashboardImages = async () => {
  if (ocrRefreshing) {
    return
  }

  ocrRefreshing = true

  try {
    await dashboardStore.refreshOcrImages()
  } catch (error) {
    console.error('OCR 대시보드 자동 갱신 실패', error)
  } finally {
    ocrRefreshing = false
  }
}

// 처음 진입했을 때 전체 대시보드를 조회하고 3초마다 OCR 자동 갱신 시작
onMounted(async () => {
  await loadDashboard()

  ocrRefreshTimer = window.setInterval(() => {
    refreshDashboardImages()
  }, 3000)
})

// 화면에서 나가면 자동 갱신 중지
onUnmounted(() => {
  if (ocrRefreshTimer) {
    window.clearInterval(ocrRefreshTimer)
    ocrRefreshTimer = null
  }
})
</script>