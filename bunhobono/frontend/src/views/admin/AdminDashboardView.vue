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

      <!-- 실시간 카메라 영상 -->
      <article class="dashboard-card parking-overview-card">
        <div class="card-heading">
          <span>실시간 카메라 영상</span>
        </div>

        <div class="parking-overview-content">
          <!-- 
            주차장 선택 버튼
            gateStore.list 안에 들어있는 parkingName 값과 같은 이름을 사용
            ex) A 주차장, B 주차장, C 주차장, D 주차장
          -->
          <div class="parking-camera-tabs">
            <button
              v-for="parking in cameraParkingTabs"
              :key="parking.name"
              type="button"
              :class="{ active : selectedParkingName === parking.name }"
              @click="selectedParkingName = parking.name">
              {{ parking.label }}
            </button>
          </div>

          <!-- 
            선택한 주차장의 입차/출차 영상 영역
            실제 영상이 준비되면 camera-video-placeholder 안쪽에
            video, img, iframe 등을 넣으면 된다
          -->
          <div class="dashboard-camera-grid">
            <section
              v-for="panel in cameraPanels"
              :key="panel.type" 
              class="dashboard-camera-panel">
              
              <div class="camera-video-placeholder">
                <img
                  v-if="isCameraPlaying(panel.cameraNo)"
                  class="camera-stream-image"
                  :src="`http://127.0.0.1:8000/cctv/${panel.cameraNo}/stream`"
                  :alt="`CCTV ${panel.cameraNo}`"
                />
                <div v-else class="camera-stream-paused">
                  일시정지
                </div>
                <div class="camera-stream-title">
                  {{ selectedParkingName }}
                  {{ panel.type === 'IN' ? '입차' : '출차' }}
                  · CCTV {{ panel.cameraNo }}
                </div>
                <button
                  type="button"
                  class="camera-stream-toggle"
                  @click="toggleCamera(panel.cameraNo)"
                >
                  {{ isCameraPlaying(panel.cameraNo) ? '일시정지' : '재생' }}
                </button>
                <span class="camera-video-label">
                  {{ panel.cameraLabel }}
                </span>
                <strong>{{ panel.title }}</strong>
              
                <p v-if="panel.gate">
                  {{ panel.gate.gateName }}
                  상태:
                  {{ panel.gate.gateStatus === 1 ? '열림' : '닫힘' }}
                </p>
                <p v-else>연결된 게이트 없음</p>

                <!-- 
                  해당 주차장의 IN/OUT 게이트가 있으면 게이트 열기 가능
                  gate 정보가 없으면 버튼을 비활성화
                -->
                <div class="camera-gate-controls">
                  <span
                    class="camera-gate-status"
                    :class="{ open: panel.gate?.gateStatus === 1 }"
                  >
                    {{ panel.gate
                      ? (panel.gate.gateStatus === 1 ? '게이트 열림' : '게이트 닫힘')
                      : '게이트 미연결' }}
                  </span>

                <button
                  type="button"
                  class="camera-gate-button"
                  :disabled="!panel.gate"
                  @click="openManualGate(panel)">
                  게이트 열기
                </button>
                </div>
              </div>

            </section>
          </div>
        </div>

      

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
import { useGateStore } from '@/features/gates/gateStore'
import { useAdminDashboardStore } from '@/stores/adminDashboard'
import { storeToRefs } from 'pinia'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const dashboardStore = useAdminDashboardStore()
const gateStore = useGateStore();

// store의 상태와 계산 결과를 반응성을 유지한 상태로 사용
const {
  loading,
  errorMessage,
  unresolvedNoticeCount,
  waitingVehicleCount,
  todayVisitVehicleCount,
  weeklyEntryStats,
  currentCarlogPage,
  carlogTotalPages,
  carlogPageNumbers,
  paginatedCarlogs
} = storeToRefs(dashboardStore)

let ocrRefreshTimer = null
let ocrRefreshing = false

// 새로고침 버튼과 최초 화면 진입 시 사용
const loadDashboard = async () => {
  await dashboardStore.loadDashboard()
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

// 버튼은 A/B/C/D 만 보여주고
// 실제 게이트 연결은 백엔드에서 넘어오는 pakingName 값으로 찾는다
const cameraParkingTabs = [
  {
    label : 'A',
    name : 'A 주차장'
  },
  {
    label : 'B',
    name : 'B 주차장'
  },
  {
    label : 'C',
    name : 'C 주차장'
  },
  {
    label : 'D',
    name : 'D 주차장'
  }
]

// 처음 대시보드에 들어왔을 때 기본으로 보여줄 주차장
const selectedParkingName = ref('A 주차장')

// 선택한 주차장 이름과 게이트 타입으로 게이트를 찾는다
// gateType이 IN 이면 입차 게이트, OUT 이면 출차 게이트를 찾는다
const playingCameraNos = ref(new Set())

const isCameraPlaying = (cameraNo) => {
  return playingCameraNos.value.has(cameraNo)
}

const toggleCamera = (cameraNo) => {
  const next = new Set(playingCameraNos.value)

  if (next.has(cameraNo)) {
    next.delete(cameraNo)
  } else {
    next.add(cameraNo)
  }

  playingCameraNos.value = next
}

watch(selectedParkingName, () => {
  playingCameraNos.value = new Set()
})

const findGateByParking = (gateType) => {
  return gateStore.list.find((gate) => {
    return gate.parkingName === selectedParkingName.value
      && String(gate.gateType).toUpperCase() === gateType
  })
}

// 현재 선택한 주차장에 맞춰 입차/출차 영상 영역을 만든다
// 실제 영상은 아직 없으므로 지금은 영상 자리와 게이트 버튼만 준비한다
const cameraPanels = computed(() => {
  const parkingIndex = cameraParkingTabs.findIndex(
    (parking) => parking.name === selectedParkingName.value
  )
  const inCameraNo = parkingIndex >= 0 ? parkingIndex * 2 + 1 : 1

  return [
    {
      type : 'IN',
      cameraNo : inCameraNo,
      cameraLabel : '입차 영상 연결 예정',
      title : `${selectedParkingName.value} 입차 게이트`,
      gate : findGateByParking('IN')
    },
    {
      type : 'OUT',
      cameraNo : inCameraNo + 1,
      cameraLabel : '출차 영상 연결 예정',
      title : `${selectedParkingName.value} 출차 게이트`,
      gate : findGateByParking('OUT')
    },
  ]
})

// 관리자가 직접 게이트를 여는 함수
const openManualGate = async (panel) => {
  const gate = panel.gate

  if (!gate) {
    alert('연결된 게이트가 없습니다')
    return
  }

  const opened = await gateStore.open(gate.gateNo)

  if (!opened) {
    return
  }

  const response = await fetch(
    `http://127.0.0.1:8000/cctv/${panel.cameraNo}/resume`,
    { method: 'POST' }
  )

  if (!response.ok) {
    alert('게이트는 열렸지만 CCTV 영상을 재생하지 못했습니다.')
    return
  }
  alert(`${gate.parkingName} ${gate.gateName} 게이트를 열었습니다`)
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

  // 게이트 목록  조회
  await gateStore.loadList()

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
