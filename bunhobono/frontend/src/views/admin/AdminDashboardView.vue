<template>
    <section class="admin-dashboard"> 
        <!-- 관리자 대시보드 제목 영역 -->
        <div class="dashboard-header">
            <div>
                <h2>관리자 대시보드</h2>
            </div>
        </div>

        <!-- 데이터 조회 상태 -->
        <p v-if="loading" class="dashboard-message">
            대시보드 정보를 불러오는 중입니다
        </p>

        <p v-else-if="errorMessage" class="dashboard-error">
            {{ errorMessage }}
        </p>

        <!-- 차량 등록과 알림을 하나의 카드 안에 한 줄로 배치 -->
        <div class="dashboard-top-strip">
            <!-- 관리자 차량 등록 영역 -->
            <!-- 현재 백엔드 정책상 등록 시 WAITING 상태로 저장 -->
            <form 
                class="quick-register-inline"
                @submit.prevent="submitQuickVehicle">
                
                <span class="quick-register-label">
                    차량 등록
                </span>

                <input
                    v-model="quickVehicle.carNo"
                    type="text"
                    placeholder="차량번호" />

                <select v-model="quickVehicle.memDong">
                    <option value="">동</option>
                    <option 
                        v-for="dong in dongOptions"
                        :key="dong"
                        :value="dong">
                        {{ dongText(dong) }}
                    </option>
                </select>

                <select v-model="quickVehicle.memHo">
                    <option value="">호</option>
                    <option 
                        v-for="ho in hoOptions"
                        :key="ho"
                        :value="ho">
                        {{ hoText(ho) }}
                    </option>
                </select>  

                <select v-model.number="quickVehicle.memberNo">
                    <option :value="null">입주민</option>
                    <option
                        v-for="member in filteredMembers"
                        :key="member.memberNo"
                        :value="member.memberNo">
                        {{ memberLabel(member) }}
                    </option>
                </select>

                <button type="submit">
                    등록
                </button>              
            </form>

            <!-- 상단 알림 영역 -->
            <div class="dashboard-alert-chips">
                <button
                    v-for="alert in dashboardAlerts"
                    :key="alert.key"
                    type="button"
                    class="alert-chip"
                    @click="router.push(alert.path)">
                    <span>{{ alert.title }}</span>
                    <strong>{{ alert.count }}</strong>
                </button>
            </div>
        </div>
 

        <!-- 메인 영역 : 왼쪽은 영상, 오른쪽은 입출차 로그와 상세 정보 -->
        <div class="admin-control-layout">
            <!-- 주차장 모니터링 영역 -->
            <article class="dashboard-card monitoring-card">
                <div class="section-heading">
                    <h3>주차장 모니터링</h3>
                </div>

                <!-- A/B/C/D 주차장 카드 v-for로 반복해서 표시 -->
                <div class="parking-monitor-grid">
                    <section
                        v-for="panel in parkingMonitorPanels"
                        :key="panel.parkingNo"
                        class="parking-monitor-card"
                        @click="openParkingDialog(panel)">

                        <div class="parking-monitor-header">
                            <div>
                                <strong>{{ panel.parkingName }}</strong>
                                <span class="parking-location">
                                    {{ panel.parkingLocation || '-' }}
                                </span>
                            </div>

                            <!-- 입차/출차 화면 전환 버튼 -->
                            <button
                                type="button"
                                class="camera-mode-badge"
                                :class="panel.modeClass"
                                @click.stop="toggleParkingCamera(panel.parkingName)">
                                {{ panel.modeText }}
                            </button>
                        </div>

                        <!-- 실제 영상이 준비되면 이 영역 안에 video, img, iframe 등을 넣는다 -->
                        <div class="parking-video-box">
                            <div class="parking-video-placeholder">
                                <strong> {{ panel.parkingName }} {{ panel.modeText }}영상</strong>

                                <span
                                    v-if="panel.gate"
                                    class="video-gate-status"
                                    :class="{ open: panel.gate.gateStatus === 1, closed: panel.gate.gateStatus !== 1 }">
                                    {{ panel.gate.gateStatus === 1 ? '열림' : '닫힘' }}
                                </span>

                                <span v-else class="video-gate-status closed">
                                    게이트 없음
                                </span>
                            </div>
                        </div>

                        <div class="parking-monitor-footer">
                            <!-- 선택된 화면에 연결된 게이트를 연다 -->
                            <button
                                type="button"
                                class="camera-gate-button"
                                :disabled="!panel.gate"
                                @click.stop="openManualGate(panel.gate)">
                                게이트 열기
                            </button>
                        </div>
                    </section>
                </div>
            </article>

            <!-- 오른쪽 입출차 로그 + 선택 상세 영역 -->
            <section class="admin-control-right">
                <article class="dashboard-card carlog-preview-card">
                    <div class="section-heading">
                        <h3>입출차 로그</h3>
                    </div>

                    <div class="carlog-table-wrap">
                        <table class="dashboard-log-table">
                            <thead>
                                <tr>
                                    <th>차량번호</th>
                                    <th>구분</th>
                                    <th>상태</th>
                                    <th>입차</th>
                                </tr>
                            </thead>

                            <tbody>
                                <tr
                                    v-for="log in recentCarlogs"
                                    :key="log.carLogNo"
                                    :class="{ selected: selectedCarlog?.carLogNo === log.carLogNo }"
                                    @click="selectCarlog(log)">
                                    <td>{{ log.carNo || '미인식' }}</td>
                                    <td>{{ log.carKindText }}</td>
                                    <td>
                                        <span
                                            class="state-badge"
                                            :class="parkingStateClass(log)">
                                            {{ log.parkingStateText }}
                                        </span>
                                    </td>
                                    <td>{{ log.inTimeText }}</td>
                                </tr>

                                <tr v-if="recentCarlogs.length === 0">
                                    <td colspan="4">
                                        입출차 로그가 없습니다
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </article>

                <article class="dashboard-card selected-log-card">
                    <div class="section-heading">
                        <h3>선택 로그 상세 정보</h3>
                    </div>

                    <div v-if="selectedCarlog" class="selected-log-content">
                        <!-- 나중에 FastAPI에서 넘겨준 원본 이미지와 크롭 이미지를 넣을 자리 -->
                        <div class="selected-log-images">
                            <div class="capture-image-box">
                                <span>캡쳐 이미지</span>
                                <div class="crop-image-box">
                                    크롭 이미지
                                </div>
                            </div>
                        </div>

                        <dl class="selected-log-info">
                            <div>
                                <dt>차량번호</dt>
                                <dd>{{ selectedCarlog.carNo || '미인식' }}</dd>
                            </div>

                            <div>
                                <dt>차량 구분</dt>
                                <dd>{{ selectedCarlog.carKindText }}</dd>
                            </div>

                            <div>
                                <dt>주차 상태</dt>
                                <dd>{{ selectedCarlog.parkingStateText }}</dd>
                            </div>

                            <div>
                                <dt>입차 시간</dt>
                                <dd>{{ selectedCarlog.inTimeText }}</dd>
                            </div>

                            <div>
                                <dt>출차 시간</dt>
                                <dd>{{ selectedCarlog.outTime ? selectedCarlog.outTimeText : '-' }}</dd>
                            </div>

                            <div>
                                <dt>입차 게이트</dt>
                                <dd>{{ selectedCarlog.inGateText }}</dd>
                            </div>

                            <div>
                                <dt>출차 게이트</dt>
                                <dd>{{ selectedCarlog.outGateText }}</dd>
                            </div>
                        </dl>
                    </div>

                    <div v-else class="selected-log-empty">
                        입출차 로그를 선택하세요
                    </div>
                </article>
            </section>
        </div>

        <!-- 주차장 카드를 클릭했을 때 크게 보여주는 dialog 화면 -->
        <dialog
            ref="parkingDialog"
            class="parking-dialog"
            @close="closeParkingPanel">

            <div v-if="selectedParkingPanel" class="parking-dialog-content">
                <div class="parking-dialog-header">
                    <div>
                        <span>{{ selectedParkingPanel.modeText }} 화면</span>
                        <h3>{{ selectedParkingPanel.parkingName }}</h3>
                    </div>

                    <button
                        type="button"
                        class="parking-dialog-close"
                        @click="closeParkingDialog">
                        ×
                    </button>
                </div>

                <!-- 나중에 실제 영상이 들어올 자리 -->
                <div class="parking-dialog-video">
                    <div class="parking-video-placeholder">
                        <strong>
                            {{ selectedParkingPanel.parkingName }} {{ selectedParkingPanel.modeText }} 화면
                        </strong>

                        <span
                            v-if="selectedParkingPanel.gate"
                            class="video-gate-status"
                            :class="{ open: selectedParkingPanel.gate.gateStatus === 1, closed: selectedParkingPanel.gate.gateStatus !== 1 }">
                            {{ selectedParkingPanel.gate.gateStatus === 1 ? '열림' : '닫힘' }}
                        </span>

                        <span v-else class="video-gate-status closed">
                            게이트 없음
                        </span>
                    </div>
                </div>

                <div class="parking-dialog-actions">
                    <!-- 확대 화면에서도 입차/출차 화면을 바꿀 수 있게 둔다 -->
                    <button
                        type="button"
                        class="camera-mode-badge"
                        :class="selectedParkingPanel.modeClass"
                        @click="toggleParkingCamera(selectedParkingPanel.parkingName)">
                        {{ selectedParkingPanel.modeText }}
                    </button>

                    <!-- 백엔드 open API 호출. 자동 닫힘은 백엔드 scheduleClose에서 처리 -->
                    <button
                        type="button"
                        class="camera-gate-button"
                        :disabled="!selectedParkingPanel.gate"
                        @click="openManualGate(selectedParkingPanel.gate)">
                        게이트 열기
                    </button>
                </div>
            </div>
        </dialog>

    </section>
</template>

<script setup>
import { useAdminDashboardStore } from '@/stores/adminDashboard';
import { storeToRefs } from 'pinia';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';


const router = useRouter()
const dashboardStore = useAdminDashboardStore()
const parkingDialog = ref(null)

// storeToRefs 를 사용하면 store 안의 ref/computed 반응성이 유지된다
const {
    loading,
    errorMessage,
    quickVehicle,
    dongOptions,
    hoOptions,
    filteredMembers,
    dashboardAlerts,
    parkingMonitorPanels,
    selectedParkingPanel,
    recentCarlogs,
    selectedCarlog,
} = storeToRefs(dashboardStore)

const {
    dongText,
    hoText,
    memberLabel,
    submitQuickVehicle,
    toggleParkingCamera,
    selectParkingPanel,
    closeParkingPanel,
    selectCarlog,
    parkingStateClass,
    openManualGate,
    loadDashboard,
} = dashboardStore

const openParkingDialog = (panel) => {
    selectParkingPanel(panel)
    parkingDialog.value?.showModal()
}

const closeParkingDialog = () => {
    closeParkingPanel()
    parkingDialog.value?.close()
}
// 화면에 처음 들어왔을 때 상단 카드에 필요한 데이터를 조회
onMounted(async () => {
    await loadDashboard()
})
</script>