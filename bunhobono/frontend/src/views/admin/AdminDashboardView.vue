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
        @click="loadDashboard">
        새로고침
      </button>
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
        class="dashboard-card vehicle-card"
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

      <!-- 주차장별 현황 -->
      <button
        type="button"
        class="dashboard-card parking-overview-card"
        @click="router.push('/admin/parkings')" >
        <div class="card-heading">
          <span class="card-icon">🅿️</span>
          <span>주차장 현황</span>
        </div>

        <div
          v-if="parkingStatusList.length > 0"
          class="parking-zone-grid">
          
          <div
            v-for="parking in parkingStatusList"
            :key="parking.parkingNo"
            class="parking-zone"
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
          </div>
        </div>

        <p v-else
          class="parking-empty" >
          등록된 주차장이 없습니다.
        </p>
      </button>

      <!-- OCR 성공률 -->
      <article class="dashboard-card ocr-card">
        <div class="card-heading">
          <span class="card-icon">📷</span>
          <span>OCR 성공률</span>
        </div>

        <div
          v-if="latestOcrCards.length > 0"
          class="ocr-photo-grid"
        >
          <button
            v-for="item in latestOcrCards"
            :key="item.cameraDataNo"
            type="button"
            class="ocr-photo-card"
            @click="goCameraDataDetail(item.cameraDataNo)"
          >
            <div class="ocr-photo-frame">
              <img
                v-if="item.imageUrl"
                :src="item.imageUrl"
                :alt="`${item.carNoText} 차량 사진`"
              >

              <span v-else>사진 없음</span>
            </div>

            <div class="ocr-photo-info">
              <strong>{{ item.carNoText }}</strong>
              <span>인식률 {{ item.confidenceText }}</span>
            </div>
          </button>
        </div>

        <p
          v-else
          class="ocr-empty"
        >
          최근 인식된 차량이 없습니다.
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
        <!-- 최근 7일 입차 막대그래프 -->
        <section class="weekly-entry-panel">
          <h4 class="weekly-entry-title">
            최근 7일 입차 기록
          </h4>

          <div class="weekly-entry-chart">
            <div
              v-for="day in weeklyEntryStats"
              :key="day.dateKey"
              class="weekly-entry-item"
            >
              <span class="weekly-entry-count">
                {{ day.count }}건
              </span>

              <div class="weekly-entry-track">
                <div
                  class="weekly-entry-bar"
                  :style="{
                    '--bar-height': `${day.barHeight}%`
                  }" />
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
                    {{
                      log.parkingStateText
                      || log.parkingState
                      || '-'
                    }}
                  </td>

                  <td>
                    {{ log.inTimeText || log.inTime || '-' }}
                  </td>

                  <td>
                    {{ log.outTimeText || log.outTime || '-' }}
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

          <!-- 페이지 번호 -->
          <div class="carlog-pagination">
            <button
              type="button"
              :disabled="currentCarlogPage === 1"
              @click="setCarlogPage(currentCarlogPage - 1)"
            >
              이전
            </button>

            <button
              v-for="page in carlogPageNumbers"
              :key="page"
              type="button"
              :class="{
                active: currentCarlogPage === page
              }"
              @click="setCarlogPage(page)"
            >
              {{ page }}
            </button>

            <button
              type="button"
              :disabled="
                currentCarlogPage === carlogTotalPages
              "
              @click="setCarlogPage(currentCarlogPage + 1)"
            >
              다음
            </button>
          </div>
        </section>
      </div>
    </article>
  </section>
</template>

<script setup>
import { useAdminDashboardStore } from '@/stores/adminDashboard'
import { storeToRefs } from 'pinia'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const dashboardStore = useAdminDashboardStore()

// store의 상태와 계산 결과를 반응성을 유지한 상태로 사용
const {
  loading,
  errorMessage,
  unresolvedNoticeCount,
  registeredVehicleCount,
  parkingStatusList,
  latestOcrCards,
  weeklyEntryStats,
  currentCarlogPage,
  carlogTotalPages,
  carlogPageNumbers,
  paginatedCarlogs
} = storeToRefs(dashboardStore)

// 새로고침 버튼과 최초 화면 진입 시 사용
const loadDashboard = async () => {
  await dashboardStore.loadDashboard()
}

// 입출차 목록 페이지 변경
const setCarlogPage = (page) => {
  dashboardStore.setCarlogPage(page)
}

// OCR 사진 카드를 누르면 해당 카메라 데이터 상세로 이동
const goCameraDataDetail = (cameraDataNo) => {
  router.push({
    name: 'CameraDataDetail',
    params: {
      cameraDataNo
    }
  })
}

// 화면에 처음 들어왔을 때 대시보드 데이터 조회
onMounted(loadDashboard)
</script>