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

    <dialog ref="gateDialog" class="gate-open-dialog">
      <div class="gate-dialog-header">
        <div>
          <h3>수동 입차 승인</h3>
          <p>
            {{ selectedGateParkingName }}게이트를 열 차량을 확인해주세요.
          </p>
        </div>
        <button type="button" class="gate-dialog-close" @click="closeGateDialog">
          닫기
        </button>
      </div>

      <div class="gate-dialog-table-wrap">
        <table>
          <thead>
            <tr>
              <th>주차장</th>
              <th>게이트</th>
              <th>차량번호</th>
              <th>차량상태</th>
              <th>촬영시각</th>
              <th>처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="vehicle in dialogApprovalVehicles" :key="vehicle.cameraDataNo">
              <td>{{ vehicle.parkingName || '-' }}</td>
              <td>
                {{ vehicle.gateName || '-' }}
                <small v-if="vehicle.gateNo" class="gate-number">
                  #{{ vehicle.gateNo }}
                </small>
              </td>
              <td>{{ vehicle.carNo || '미인식' }}</td>
              <td>{{ getManualVehicleStatusText(vehicle) }}</td>
              <td>{{ formatGateRequestTime(vehicle.captureTime) }}</td>
              <td>
                <button
                  type="button"
                  class="gate-approve-button"
                  :disabled="openingCameraDataNo !== null"
                  @click="confirmGateOpen(vehicle)"
                >
                  {{ openingCameraDataNo === vehicle.cameraDataNo ? '처리 중...' : '입차 승인' }}
                </button>
              </td>
            </tr>
            <tr v-if="dialogApprovalVehicles.length === 0">
              <td colspan="6">수동 승인 대기 차량이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </dialog>

    <!-- 데이터 조회 상태 -->
    <p v-if="loading"
      class="dashboard-message">
      현황을 불러오는 중입니다.
    </p>

    <p v-else-if="errorMessage"
      class="dashboard-error" >
      {{ errorMessage }}
    </p>

    <!-- 상단 현황 카드 -->
    <div class="dashboard-grid">
      <!-- 미처리 알림 -->
      <button
        type="button"
        class="dashboard-card notice-card"
        @click="router.push('/admin/notice')"
      >
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
        @click="router.push('/admin/vehicles?mode=approve')"
      >
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

      <!-- 주차장별 현황 -->
      <article class="dashboard-card parking-overview-card">
        <div class="card-heading">
          <span>주차장 현황</span>
        </div>

        <div
          v-if="parkingStatusWithOcr.length > 0"
          class="parking-overview-content">
          <!-- A/B/C/D 주차장 사용률 -->
          <div
            class="parking-status-strip"
          >

            <button
              v-for="parking in parkingStatusWithOcr"
              :key="`status-${parking.parkingNo}`"
              type="button"
              class="parking-zone-status"
              :class="{ 'has-gate-request': hasManualRequest(parking.parkingNo) }"
              :disabled="!hasManualRequest(parking.parkingNo)"
              @click="openGateDialog(parking.parkingNo)"
            >

              <strong class="parking-zone-name">
                {{ parking.parkingName }}
              </strong>

              <div
                class="parking-zone-donut"
                :style="{
                  '--parking-rate': `${parking.usageRate}%`
                }"
              >
                <strong>{{ parking.usageRate }}%</strong>
              </div>

              <span class="parking-zone-count">
                {{ parking.occupied }} / {{ parking.total }}면
              </span>

              <span class="parking-zone-available">
                주차 가능 {{ parking.available }}면
              </span>

              <span
                v-if="manualRequestCount(parking.parkingNo) > 0"
                class="parking-gate-request-count"
              >
                게이트 요청 {{ manualRequestCount(parking.parkingNo) }}건
              </span>
            </button>
          </div>

          <!-- A/B/C/D 주차장별 최신 OCR 사진 -->
          <div class="parking-ocr-row">
            <button
              v-for="parking in parkingStatusWithOcr"
              :key="`ocr-${parking.parkingNo}`"
              type="button"
              class="parking-ocr-preview"
              :disabled="!parking.ocr.cameraDataNo"
              @click="parking.ocr.cameraDataNo && goCameraDataList(parking.parkingNo)"
            >
              <div class="parking-ocr-frame">
                <img
                  v-if="parking.ocr.imageUrl"
                  :src="parking.ocr.imageUrl"
                  :alt="`${parking.parkingName} ${parking.ocr.carNoText} 차량 사진`"
                >

                <span v-else>사진 없음</span>
              </div>

              <span class="parking-ocr-zone">{{ parking.parkingName }}</span>
              <strong>{{ parking.ocr.carNoText }}</strong>
              <span class="parking-ocr-movement">
                {{ parking.ocr.movementText }}
              </span>
              <span class="parking-ocr-rate">인식률 {{ parking.ocr.confidenceText }}</span>
            </button>
          </div>
        </div>

        <p v-else
          class="parking-empty" >
          등록된 주차장이 없습니다.
        </p>
      </article>
    </div>

    <!-- 주간 입차 그래프와 입출차 목록 -->
    <article class="recent-carlogs">
      <div class="section-heading">
        <h3>입출차 현황</h3>

        <button
          type="button"
          @click="router.push('/admin/carlogs')" >
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
                  :key="log.carLogNo ?? log.displayNo"
                >
                  <td>{{ log.carNo || '미인식' }}</td>

                  <td>
                    {{ log.carKindText || log.carKind || '-' }}
                  </td>

                  <td>
                    <!-- 주차 상태를 색상 배지로 표시 -->
                    <span class="carlog-state-badge"
                          :class="{ parking : log.parkingState === 'PARKING',
                                    out : log.parkingState === 'OUT' }">
                      {{ log.parkingStateText || log.parkingState || '-' }}
                    </span>
                  </td>

                  <td>
                    {{ log.inTimeText || log.inTime || '-' }}
                  </td>

                  <td>
                    {{ log.outTime ? log.outTimeText || log.outTime : '-' }}
                  </td>
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
              type="button"
              :disabled="currentCarlogPage === 1"
              @click="setCarlogPage(carlogPageNumbers[0] - 1)"
            >
              이전
            </button>

            <button
              v-for="page in carlogPageNumbers"
              :key="page"
              type="button"
              :class="{ active: currentCarlogPage === page }"
              @click="setCarlogPage(page)"
            >
              {{ page }}
            </button>

            <button
              type="button"
              :disabled="currentCarlogPage === carlogTotalPages"
              @click="setCarlogPage(carlogPageNumbers[carlogPageNumbers.length -1] + 1)"
            >
              다음
            </button>
          </div>
        </section>

        <!-- 최근 7일 입차 막대그래프 -->
        <section class="weekly-entry-panel">
          <!-- 그래프 제목과 막대 색상 설명 -->
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
              class="weekly-entry-item"
            >
          
              <!-- 입주민과 방문객 입차 합계 -->
              <span class="weekly-entry-count">
                {{ day.residentCount + day.visitCount }}건
              </span>

              <!-- 요일별 입주민/방문객 입차 막대 -->
              <div class="weekly-entry-track grouped">
                <div
                  class="weekly-entry-bar resident"
                  :style="{'--bar-height': `${day.residentBarHeight}%`}" />

                <div
                  class="weekly-entry-bar visit"
                  :style="{'--bar-height': `${day.visitBarHeight}%`}" />
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
import { openGateForCameraData } from '@/features/camera-data/cameraDataApi'
import { storeToRefs } from 'pinia'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

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
  manualApprovalVehicles,
  weeklyEntryStats,
  currentCarlogPage,
  carlogTotalPages,
  carlogPageNumbers,
  paginatedCarlogs
} = storeToRefs(dashboardStore)

const gateDialog = ref(null)
const openingCameraDataNo = ref(null)
const selectedGateParkingNo = ref(null)
let ocrRefreshTimer = null
let ocrRefreshing = false

// 새로고침 버튼과 최초 화면 진입 시 사용
const loadDashboard = async () => {
  await dashboardStore.loadDashboard()
}

const dialogApprovalVehicles = computed(() => {
  if (selectedGateParkingNo.value === null) {
    return manualApprovalVehicles.value
  }

  return manualApprovalVehicles.value.filter((vehicle) => {
    return Number(vehicle.parkingNo) === Number(selectedGateParkingNo.value)
  })
})

const selectedGateParkingName = computed(() => {
  if (selectedGateParkingNo.value === null) return ''

  const parking = parkingStatusWithOcr.value.find((item) => {
    return Number(item.parkingNo) === Number(selectedGateParkingNo.value)
  })

  return parking?.parkingName ? `${parking.parkingName} 주차장 ` : ''
})

const manualRequestCount = (parkingNo) => {
  return manualApprovalVehicles.value.filter((vehicle) => {
    return Number(vehicle.parkingNo) === Number(parkingNo)
  }).length
}

const hasManualRequest = (parkingNo) => {
  return manualRequestCount(parkingNo) > 0
}

const openGateDialog = (parkingNo = null) => {
  selectedGateParkingNo.value = parkingNo
  gateDialog.value?.showModal()
}

const closeGateDialog = () => {
  gateDialog.value?.close()
  selectedGateParkingNo.value = null
}

const getManualVehicleStatusText = (vehicle) => {
  if (!vehicle.vehicleCarNo) return '미등록'
  if (vehicle.vehicleStatus === 'WAITING') return '승인 대기'
  if (vehicle.vehicleStatus === 'EXPIRED') return '만료'
  if (vehicle.vehicleStatus === 'UNKNOWN') return '미확인'
  return vehicle.vehicleStatus || '-'
}

const formatGateRequestTime = (value) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const confirmGateOpen = async (vehicle) => {
  const confirmed = window.confirm(
    `${vehicle.carNo || '미인식'} 차량의 `
      + `${vehicle.gateName || '연결된 게이트'}를 열겠습니까?`
  )

  if (!confirmed || openingCameraDataNo.value !== null) return

  openingCameraDataNo.value = vehicle.cameraDataNo

  try {
    await openGateForCameraData(vehicle.cameraDataNo)
    window.alert('입차 승인이 완료되었습니다.')
    await dashboardStore.refreshOcrImages()

    if (dialogApprovalVehicles.value.length === 0) {
      closeGateDialog()
    }
  } catch (error) {
    const status = error.response?.status

    if (status === 409) {
      window.alert('이미 처리됐거나 게이트가 열려 있습니다.')
    } else {
      window.alert('게이트 열기 처리에 실패했습니다.')
    }
  } finally {
    openingCameraDataNo.value = null
  }
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

// OCR 사진 카드를 누르면 해당 주차장의 카메라 데이터 목록으로 이동
const goCameraDataList = (parkingNo) => {
  router.push({
    name: 'CameraDataList',
    query: {
      parkingNo
    }
  })
}


// OCR 업로드 후 대시보드가 자동으로 바뀌도록 주기적으로 최신 데이터 조회
const refreshDashboardImages = async () => {
  // 이전 갱신이 끝나지 않았으면 중복 요청하지 않음
  if (ocrRefreshing) {
    return
  }

  ocrRefreshing = true

  try {
    // 카메라 목록 갱신 후 카메라 번호별 OCR 이미지 갱신
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
