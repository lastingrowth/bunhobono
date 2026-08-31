<template>
  <main class="pdm-page management-list-page facility-list-page">
    <ManagementFeedbackToast :message="feedbackMessage" :type="feedbackType" />
    <header class="pdm-header facility-list-heading">
      <div>
        <h1 class="management-list-title">예지보전</h1>
        <p>{{ equipmentDescription }}</p>
      </div>
      <div class="header-monitor-tools">
        <nav class="equipment-tabs" aria-label="예지보전 장비 선택">
          <button
            v-for="equipment in equipmentOptions"
            :key="equipment.value"
            type="button"
            :class="{ active: selectedEquipment === equipment.value }"
            @click="selectEquipment(equipment.value)"
          >
            {{ equipment.label }}
            <span
              v-if="equipment.value !== 'STATS' && equipmentFaultCounts[equipment.value] > 0"
              class="equipment-tab-alert"
              :aria-label="`${equipment.label} 미조치 위험 ${equipmentFaultCounts[equipment.value]}대`"
            >
              {{ equipmentFaultCounts[equipment.value] }}
            </span>
          </button>
        </nav>

        <div class="live-clock">
          <div class="live-clock-meta">
            <span><i></i> 실시간 연결</span>
          </div>
          <strong>{{ clockText }}</strong>
        </div>
      </div>
    </header>

    <section v-if="selectedEquipment === 'STATS'" class="stats-dashboard">
      <section class="stats-summary-grid">
        <article class="stats-summary-card total"><span>전체 설비</span><strong>{{ overallCounts.TOTAL }}</strong><small>통합 관제 장비</small></article>
        <article class="stats-summary-card normal"><span>정상</span><strong>{{ overallCounts.NORMAL }}</strong><small>{{ overallNormalRate }}% 정상 운영</small></article>
        <article class="stats-summary-card maintenance"><span>주의</span><strong>{{ overallCounts.MAINTENANCE }}</strong><small>상태 확인 권장</small></article>
        <article class="stats-summary-card fault"><span>위험</span><strong>{{ overallCounts.FAULT }}</strong><small>즉시 점검 필요</small></article>
      </section>

      <section class="stats-content-grid">
        <article class="stats-panel distribution-panel">
          <header class="stats-panel-header"><div><h2>통합 상태 분포</h2><p>현재 조회된 전체 설비 기준</p></div><span class="stats-live"><i></i> LIVE</span></header>
          <div class="donut-area">
            <div class="status-donut" :style="donutStyle"><div><strong>{{ overallNormalRate }}%</strong><span>정상률</span></div></div>
            <ul class="donut-legend">
              <li><i class="normal"></i><span>정상</span><strong>{{ overallCounts.NORMAL }}대</strong></li>
              <li><i class="maintenance"></i><span>주의</span><strong>{{ overallCounts.MAINTENANCE }}대</strong></li>
              <li><i class="fault"></i><span>위험</span><strong>{{ overallCounts.FAULT }}대</strong></li>
            </ul>
          </div>
        </article>

        <article class="stats-panel equipment-status-panel">
          <header class="stats-panel-header"><div><h2>장비 유형별 현황</h2><p>카메라 · 게이트 · 주차로봇 비교</p></div></header>
          <div class="equipment-bars">
            <div v-for="row in equipmentStatistics" :key="row.type" class="equipment-bar-row">
              <div class="equipment-bar-label"><strong>{{ row.label }}</strong><span>{{ row.total }}대</span></div>
              <div class="stacked-bar"><i class="normal" :style="{ width: `${row.normalRate}%` }"></i><i class="maintenance" :style="{ width: `${row.maintenanceRate}%` }"></i><i class="fault" :style="{ width: `${row.faultRate}%` }"></i></div>
              <div class="equipment-bar-counts"><span>정상 {{ row.normal }}</span><span>주의 {{ row.maintenance }}</span><span>위험 {{ row.fault }}</span></div>
            </div>
          </div>
        </article>

        <article class="stats-panel today-panel">
          <header class="stats-panel-header">
            <div><h2>오늘 기록 통계</h2><p>오늘 분석은 5초 주기 환산 · 저장/감지 기록 DB 연동</p></div>
            <span class="today-date">{{ todayDateText }}</span>
          </header>
          <div class="today-layout">
            <div class="today-metrics">
              <div><span>오늘 분석</span><strong>{{ todayDemoStats.analysis.toLocaleString() }}</strong><small>건</small></div>
              <div><span>저장 기록</span><strong>{{ todayDemoStats.saved }}</strong><small>건</small></div>
              <div class="warning"><span>주의 감지</span><strong>{{ todayDemoStats.warning }}</strong><small>건</small></div>
              <div class="fault"><span>위험 감지</span><strong>{{ todayDemoStats.fault }}</strong><small>건</small></div>
            </div>
            <div class="hourly-chart">
              <div class="chart-heading"><span>시간대별 이상 감지</span><div><i class="warning"></i>주의 <i class="fault"></i>위험</div></div>
              <div class="chart-body">
                <div v-for="point in todayHourlyData" :key="point.label" class="chart-column">
                  <div class="chart-bars">
                    <i class="warning" :style="{ height: `${point.warningHeight}%` }" :title="`주의 ${point.warning}건`"></i>
                    <i class="fault" :style="{ height: `${point.faultHeight}%` }" :title="`위험 ${point.fault}건`"></i>
                  </div>
                  <span>{{ point.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </article>

      </section>
    </section>

    <section v-else class="summary-grid">
      <article class="summary-card total">
        <span>전체 {{ equipmentLabel }}</span><strong>{{ currentItems.length }}</strong><small>운영 대상 장비</small>
      </article>
      <article class="summary-card normal">
        <span>정상</span><strong>{{ statusCounts.NORMAL }}</strong><small>{{ normalRate }}% 정상 작동</small>
      </article>
      <article class="summary-card maintenance">
        <span>주의</span><strong>{{ statusCounts.MAINTENANCE }}</strong><small>상태 확인 권장</small>
      </article>
      <article class="summary-card fault">
        <span>위험</span><strong>{{ statusCounts.FAULT }}</strong><small>즉시 확인 필요</small>
      </article>
    </section>

    <section v-if="selectedEquipment !== 'STATS'" class="monitor-grid">
      <article class="camera-panel">
        <header class="panel-header">
          <div><h2>{{ equipmentLabel }} 상태</h2><p>마지막 수신 {{ lastUpdatedText }}</p></div>
          <div class="panel-actions">
            <button type="button" class="register-equipment-button" :disabled="loading" @click="openRegisterDialog">기기 등록</button>
            <button type="button" :disabled="loading" @click="refreshCurrent(true)">{{ loading ? '갱신 중' : '지금 갱신' }}</button>
          </div>
        </header>

        <div v-if="errorMessage" class="monitor-error">{{ errorMessage }}</div>
        <div v-else-if="!currentItems.length && !loading" class="monitor-empty">등록된 {{ equipmentLabel }}가 없습니다.</div>
        <div v-else-if="selectedEquipment === 'CAMERA'" class="camera-grid">
          <article
            v-for="camera in cameras"
            :key="camera.cameraNo"
            class="camera-card"
            :class="statusClass(camera.cameraStatus)"
            role="button"
            tabindex="0"
            @click="goCameraDetail(camera.cameraNo)"
            @keydown.enter="goCameraDetail(camera.cameraNo)"
          >
            <div class="camera-card-top">
              <span class="camera-icon" aria-hidden="true"><i></i></span>
              <span class="status-badge"><i></i>{{ statusText(camera.cameraStatus) }}</span>
            </div>
            <div class="equipment-title-row">
              <strong>{{ camera.cameraName }}</strong>
              <p>{{ camera.parkingName || '주차장 미지정' }}</p>
            </div>
            <dl class="equipment-info">
              <div class="equipment-info-row"><dt>게이트</dt><dd>{{ compactCameraGateName(camera) }}</dd></div>
              <div class="equipment-info-row"><dt>용도</dt><dd>{{ camera.cameraType === 'In' ? '입차' : '출차' }}</dd></div>
            </dl>
            <div class="signal-line"><span></span><span></span><span></span><span></span><span></span><button type="button" class="card-delete-button" aria-label="카메라 삭제" title="카메라 삭제" @click.stop="requestDelete('CAMERA', camera)">−</button></div>
          </article>
        </div>
        <div v-else-if="selectedEquipment === 'GATE'" class="camera-grid">
          <article
            v-for="gate in gates"
            :key="gate.gateNo"
            class="camera-card"
            :class="statusClass(gate.operatingStatus)"
            role="button"
            tabindex="0"
            @click="goGateDetail(gate.gateNo)"
            @keydown.enter="goGateDetail(gate.gateNo)"
          >
            <div class="camera-card-top">
              <span class="gate-icon" :class="{ open: gate.gateStatus === 1 }" aria-hidden="true"><i></i></span>
              <span class="status-badge"><i></i>{{ statusText(gate.operatingStatus) }}</span>
            </div>
            <div class="equipment-title-row">
              <strong>{{ gate.gateName }}</strong>
              <p>{{ gate.parkingName || '주차장 미지정' }}</p>
            </div>
            <dl class="equipment-info">
              <div class="equipment-info-row"><dt>분류</dt><dd>{{ gate.gateType === 'In' || gate.gateType === 'IN' ? '입차' : '출차' }}</dd></div>
              <div class="equipment-info-row"><dt>개폐</dt><dd>{{ gate.gateStatus === 1 ? '열림' : '닫힘' }}</dd></div>
            </dl>
            <div class="signal-line"><span></span><span></span><span></span><span></span><span></span><button type="button" class="card-delete-button" aria-label="게이트 삭제" title="게이트 삭제" @click.stop="requestDelete('GATE', gate)">−</button></div>
          </article>
        </div>
        <div v-else class="camera-grid">
          <article
            v-for="robot in robots"
            :key="robot.robotNo"
            class="camera-card"
            :class="statusClass(robot.monitoringStatus)"
            role="button"
            tabindex="0"
            @click="goRobotDetail(robot.robotNo)"
            @keydown.enter="goRobotDetail(robot.robotNo)"
          >
            <div class="camera-card-top">
              <span class="robot-icon" aria-hidden="true"><i></i></span>
              <span class="status-badge"><i></i>{{ statusText(robot.monitoringStatus) }}</span>
            </div>
            <div class="equipment-title-row">
              <strong>{{ robot.robotCode }}</strong>
              <p>SET {{ robot.setNo }} / {{ robot.setPosition }}</p>
            </div>
            <dl class="equipment-info">
              <div class="equipment-info-row"><dt>예측등급</dt><dd>{{ robot.riskLevel }}</dd></div>
              <div class="equipment-info-row"><dt>예측확률</dt><dd>{{ formatRiskScore(robot.riskScore) }}</dd></div>
            </dl>
            <div class="signal-line"><span></span><span></span><span></span><span></span><span></span><button type="button" class="card-delete-button" aria-label="주차로봇 삭제" title="주차로봇 삭제" @click.stop="requestDelete('ROBOT', robot)">−</button></div>
          </article>
        </div>
      </article>

      <aside class="event-panel">
        <header class="panel-header"><div><h2>실시간 이벤트</h2><p>최근 상태 수신 기록</p></div><span class="event-count">{{ events.length }}</span></header>
        <ol class="event-list">
          <li
            v-for="event in events"
            :key="event.id"
            :class="[event.type, { clickable: event.equipmentNo }]"
            :role="event.equipmentNo ? 'button' : undefined"
            :tabindex="event.equipmentNo ? 0 : undefined"
            @click="goEventDetail(event)"
            @keydown.enter="goEventDetail(event)"
          >
            <i></i>
            <div><strong>{{ event.title }}</strong><p>{{ event.message }}</p><time>{{ event.time }}</time></div>
          </li>
        </ol>
      </aside>
    </section>

    <dialog ref="registerDialog" class="equipment-register-dialog" @close="resetRegisterForm" @click="closeRegisterOnBackdrop">
      <form class="equipment-register-form" @submit.prevent="submitEquipment">
        <header class="dialog-header">
          <div><h3>{{ equipmentLabel }} 등록</h3><p>새 {{ equipmentLabel }}의 기본 정보를 입력합니다.</p></div>
          <button type="button" class="dialog-close-button" aria-label="등록 창 닫기" @click="closeRegisterDialog">×</button>
        </header>

        <div v-if="selectedEquipment === 'CAMERA'" class="equipment-register-fields">
          <label><span>주차장</span><select v-model="cameraForm.parkingNo" required @change="cameraForm.gateNo = ''"><option disabled value="">주차장 선택</option><option v-for="parking in parkingStore.list" :key="parking.parkingNo" :value="parking.parkingNo">{{ parking.parkingName }}</option></select></label>
          <label><span>게이트</span><select v-model="cameraForm.gateNo" required :disabled="!cameraForm.parkingNo"><option disabled value="">게이트 선택</option><option v-for="gate in cameraGateOptions" :key="gate.gateNo" :value="gate.gateNo">{{ gate.gateName }}</option></select></label>
          <label><span>카메라 이름</span><input v-model.trim="cameraForm.cameraName" type="text" placeholder="예: 정문 입구 카메라" required /></label>
          <label><span>카메라 종류</span><select v-model="cameraForm.cameraType" required><option value="In">입차 (In)</option><option value="Out">출차 (Out)</option></select></label>
          <label><span>설치 날짜</span><input v-model="cameraForm.installDate" type="date" required /></label>
        </div>

        <div v-else-if="selectedEquipment === 'GATE'" class="equipment-register-fields">
          <label><span>주차장</span><select v-model="gateForm.parkingNo" required><option disabled value="">주차장 선택</option><option v-for="parking in parkingStore.list" :key="parking.parkingNo" :value="parking.parkingNo">{{ parking.parkingName }}</option></select></label>
          <label><span>게이트 코드</span><input v-model.trim="gateForm.gateCode" type="text" placeholder="예: B1-IN-1" required /></label>
          <label><span>게이트 이름</span><input v-model.trim="gateForm.gateName" type="text" placeholder="예: B1 1번 입구" required /></label>
          <label><span>게이트 분류</span><select v-model="gateForm.gateType" required><option value="In">입차 (In)</option><option value="Out">출차 (Out)</option></select></label>
          <label><span>게이트 구역</span><input v-model.trim="gateForm.gateArea" type="text" placeholder="예: B1" required /></label>
        </div>

        <div v-else class="equipment-register-fields">
          <label><span>로봇 코드</span><input v-model.trim="robotForm.robotCode" type="text" maxlength="20" placeholder="예: ROBOT-09" required /></label>
          <label><span>세트 번호</span><input v-model.number="robotForm.setNo" type="number" min="1" step="1" required /></label>
          <fieldset><legend>세트 위치</legend><div class="set-position-options"><label><input v-model="robotForm.setPosition" type="radio" value="A" /><span>A</span></label><label><input v-model="robotForm.setPosition" type="radio" value="B" /><span>B</span></label></div></fieldset>
        </div>

        <footer class="dialog-actions">
          <button type="button" class="cancel-button" :disabled="registering" @click="closeRegisterDialog">취소</button>
          <button type="submit" class="submit-button" :disabled="registering">{{ registering ? '등록 중' : '등록' }}</button>
        </footer>
      </form>
    </dialog>

    <ManagementDeleteConfirm
      :open="Boolean(deleteTarget)"
      :title="`${deleteEquipmentLabel} 삭제`"
      :item-name="deleteItemName"
      message="선택한 장비를 삭제하시겠습니까?"
      :caution="deleteCaution"
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getCameraList } from '@/features/camera/cameraApi';
import { useCameraStore } from '@/features/camera/cameraStore';
import { getList as getGateList } from '@/features/gates/gateApi';
import { useGateStore } from '@/features/gates/gateStore';
import { useParkingsStore } from '@/features/parking/parkingsStore';
import { getRobotList } from '@/features/robot/robotApi';
import { useRobotStore } from '@/features/robot/robotStore';
import ManagementDeleteConfirm from '@/shared/components/ManagementDeleteConfirm.vue';
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue';
import {
  analyzeAllCameras,
  analyzeAllGates,
  analyzeAllRobots,
  getLatestCameraPdm,
  getLatestGatePdm,
  getLatestRobotPdm,
  getSavedCameraPdm,
  getSavedGatePdm,
  getSavedRobotPdm,
} from './predictiveMaintenanceApi';

const router = useRouter();
const route = useRoute();
const cameraStore = useCameraStore();
const gateStore = useGateStore();
const parkingStore = useParkingsStore();
const robotStore = useRobotStore();
const cameras = ref([]);
const gates = ref([]);
const robots = ref([]);
const savedPdmRecords = ref([]);
const selectedEquipment = ref(
  ['STATS', 'CAMERA', 'GATE', 'ROBOT'].includes(route.query.equipment)
    ? route.query.equipment
    : 'STATS'
);
const loading = ref(false);
const errorMessage = ref('');
const currentTime = ref(new Date());
const lastUpdatedAt = ref(null);
const responseTime = ref(0);
const events = ref([]);
const registerDialog = ref(null);
const registering = ref(false);
const deleteTarget = ref(null);
const deleting = ref(false);
const feedbackMessage = ref('');
const feedbackType = ref('success');
let pollTimer;
let clockTimer;
let feedbackTimer;
let eventSequence = 0;

const createCameraForm = () => ({ parkingNo: '', gateNo: '', cameraName: '', cameraType: 'In', installDate: '' });
const createGateForm = () => ({ parkingNo: '', gateCode: '', gateName: '', gateType: 'In', gateArea: '' });
const createRobotForm = () => ({ robotCode: '', setNo: null, setPosition: 'A' });
const cameraForm = ref(createCameraForm());
const gateForm = ref(createGateForm());
const robotForm = ref(createRobotForm());

const equipmentOptions = [
  { value: 'STATS', label: '통계' },
  { value: 'CAMERA', label: '카메라' },
  { value: 'GATE', label: '게이트' },
  { value: 'ROBOT', label: '주차로봇' },
];

const selectEquipment = (equipment) => {
  if (selectedEquipment.value === equipment) return;

  selectedEquipment.value = equipment;
  router.replace({ query: { ...route.query, equipment } });
  events.value = [];
  lastUpdatedAt.value = null;
  refreshCurrent();
};

const normalizeStatus = (status) => ['NORMAL', 'FAULT', 'MAINTENANCE'].includes(status) ? status : 'UNKNOWN';
const riskLevelToStatus = (riskLevel) => ({ 정상: 'NORMAL', 주의: 'MAINTENANCE', 위험: 'FAULT' }[riskLevel] || 'UNKNOWN');
const statusText = (status) => ({ NORMAL: '정상', FAULT: '위험', MAINTENANCE: '주의', UNKNOWN: '상태 미확인' }[normalizeStatus(status)]);
const statusClass = (status) => normalizeStatus(status).toLowerCase();
const compactCameraGateName = (camera) => {
  const gateName = camera?.gateName || '-';
  return String(camera?.cameraName || '').toUpperCase().includes('GROUND')
    ? gateName.replace(/^GROUND-/i, '')
    : gateName;
};
const currentItems = computed(() => ({ CAMERA: cameras.value, GATE: gates.value, ROBOT: robots.value }[selectedEquipment.value] || []));
const equipmentLabel = computed(() => ({ STATS: '통합 통계', CAMERA: '카메라', GATE: '게이트', ROBOT: '주차로봇' }[selectedEquipment.value]));
const cameraGateOptions = computed(() => gateStore.list.filter((gate) => Number(gate.parkingNo) === Number(cameraForm.value.parkingNo)));
const deleteEquipmentLabel = computed(() => ({ CAMERA: '카메라', GATE: '게이트', ROBOT: '주차로봇' }[deleteTarget.value?.type] || '장비'));
const deleteItemName = computed(() => deleteTarget.value?.item?.cameraName || deleteTarget.value?.item?.gateName || deleteTarget.value?.item?.robotCode || '선택한 장비');
const deleteCaution = computed(() => ({
  CAMERA: '연결된 카메라 데이터가 있으면 삭제할 수 없습니다.',
  GATE: '연결된 카메라·입출차 기록 또는 주차면이 있으면 삭제할 수 없습니다.',
  ROBOT: '작업 중이거나 원시 로그가 있는 로봇은 삭제할 수 없습니다.',
}[deleteTarget.value?.type] || '연결된 데이터가 있으면 삭제할 수 없습니다.'));
const itemStatus = (item) => ({ CAMERA: item.cameraStatus, GATE: item.operatingStatus, ROBOT: item.monitoringStatus }[selectedEquipment.value]);
const statusCounts = computed(() => currentItems.value.reduce((counts, item) => {
  const status = normalizeStatus(itemStatus(item));
  if (status in counts) counts[status] += 1;
  return counts;
}, { NORMAL: 0, FAULT: 0, MAINTENANCE: 0 }));
const normalRate = computed(() => currentItems.value.length ? Math.round(statusCounts.value.NORMAL / currentItems.value.length * 100) : 0);
const equipmentFaultCounts = computed(() => ({
  CAMERA: cameras.value.filter((item) => item.cameraStatus === 'FAULT' && item.actionStatus !== 'COMPLETED').length,
  GATE: gates.value.filter((item) => item.operatingStatus === 'FAULT' && item.actionStatus !== 'COMPLETED').length,
  ROBOT: robots.value.filter((item) => item.monitoringStatus === 'FAULT' && item.actionStatus !== 'COMPLETED').length,
}));
const allMonitoringItems = computed(() => [
  ...cameras.value.map((item) => ({ type: 'CAMERA', typeLabel: '카메라', no: item.cameraNo, name: item.cameraName, status: normalizeStatus(item.cameraStatus) })),
  ...gates.value.map((item) => ({ type: 'GATE', typeLabel: '게이트', no: item.gateNo, name: item.gateName, status: normalizeStatus(item.operatingStatus) })),
  ...robots.value.map((item) => ({ type: 'ROBOT', typeLabel: '주차로봇', no: item.robotNo, name: item.robotCode, status: normalizeStatus(item.monitoringStatus) })),
]);
const overallCounts = computed(() => allMonitoringItems.value.reduce((counts, item) => {
  counts.TOTAL += 1;
  if (item.status in counts) counts[item.status] += 1;
  return counts;
}, { TOTAL: 0, NORMAL: 0, MAINTENANCE: 0, FAULT: 0 }));
const overallNormalRate = computed(() => overallCounts.value.TOTAL
  ? Math.round(overallCounts.value.NORMAL / overallCounts.value.TOTAL * 100)
  : 0);
const donutStyle = computed(() => {
  const total = overallCounts.value.TOTAL || 1;
  const normalEnd = overallCounts.value.NORMAL / total * 100;
  const maintenanceEnd = normalEnd + overallCounts.value.MAINTENANCE / total * 100;
  return { background: `conic-gradient(#76ad8d 0 ${normalEnd}%, #d8a653 ${normalEnd}% ${maintenanceEnd}%, #d9747b ${maintenanceEnd}% 100%)` };
});
const equipmentStatistics = computed(() => [
  { type: 'CAMERA', label: '카메라', items: cameras.value.map((item) => normalizeStatus(item.cameraStatus)) },
  { type: 'GATE', label: '게이트', items: gates.value.map((item) => normalizeStatus(item.operatingStatus)) },
  { type: 'ROBOT', label: '주차로봇', items: robots.value.map((item) => normalizeStatus(item.monitoringStatus)) },
].map((group) => {
  const total = group.items.length;
  const normal = group.items.filter((status) => status === 'NORMAL').length;
  const maintenance = group.items.filter((status) => status === 'MAINTENANCE').length;
  const fault = group.items.filter((status) => status === 'FAULT').length;
  return {
    ...group, total, normal, maintenance, fault,
    normalRate: total ? normal / total * 100 : 0,
    maintenanceRate: total ? maintenance / total * 100 : 0,
    faultRate: total ? fault / total * 100 : 0,
  };
}));
const attentionItems = computed(() => allMonitoringItems.value
  .filter((item) => item.status === 'FAULT' || item.status === 'MAINTENANCE')
  .sort((a, b) => (a.status === 'FAULT' ? 0 : 1) - (b.status === 'FAULT' ? 0 : 1))
  .slice(0, 6));
const todayDateText = computed(() => currentTime.value.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' }));
const todayPdmRecords = computed(() => {
  const today = currentTime.value;

  return savedPdmRecords.value.filter((record) => {
    if (!record?.predictedAt) return false;

    const predictedAt = new Date(record.predictedAt);
    return predictedAt.getFullYear() === today.getFullYear()
      && predictedAt.getMonth() === today.getMonth()
      && predictedAt.getDate() === today.getDate();
  });
});
const todayHourlyData = computed(() => {
  const labels = ['00시', '04시', '08시', '12시', '16시', '20시'];
  const data = labels.map((label) => ({ label, warning: 0, fault: 0 }));

  todayPdmRecords.value.forEach((record) => {
    const slot = Math.min(5, Math.floor(new Date(record.predictedAt).getHours() / 4));

    if (record.riskLevel === '주의') data[slot].warning += 1;
    if (record.riskLevel === '위험') data[slot].fault += 1;
  });

  const maxValue = Math.max(1, ...data.flatMap((point) => [point.warning, point.fault]));
  return data.map((point) => ({
    ...point,
    warningHeight: point.warning ? Math.max(10, point.warning / maxValue * 100) : 0,
    faultHeight: point.fault ? Math.max(10, point.fault / maxValue * 100) : 0,
  }));
});
const todayDemoStats = computed(() => {
  const startOfToday = new Date(currentTime.value);
  startOfToday.setHours(0, 0, 0, 0);
  const elapsedFiveSecondIntervals = Math.floor(
    (currentTime.value.getTime() - startOfToday.getTime()) / 5000,
  );
  const warning = todayHourlyData.value.reduce((sum, point) => sum + point.warning, 0);
  const fault = todayHourlyData.value.reduce((sum, point) => sum + point.fault, 0);
  return {
    analysis: overallCounts.value.TOTAL * elapsedFiveSecondIntervals,
    saved: todayPdmRecords.value.length,
    warning,
    fault,
  };
});
const clockText = computed(() => currentTime.value.toLocaleTimeString('ko-KR', { hour12: false }));
const lastUpdatedText = computed(() => lastUpdatedAt.value ? lastUpdatedAt.value.toLocaleTimeString('ko-KR', { hour12: false }) : '-');

const pushEvent = (type, title, message, equipmentType = null, equipmentNo = null) => {
  events.value.unshift({
    id: ++eventSequence,
    type,
    title,
    message,
    equipmentType,
    equipmentNo,
    time: new Date().toLocaleTimeString('ko-KR', { hour12: false }),
  });
  events.value = events.value.slice(0, 12);
};

const goCameraDetail = (cameraNo) => {
  router.push(`/admin/predictive-maintenance/cameras/${cameraNo}`);
};

const goGateDetail = (gateNo) => {
  router.push(`/admin/predictive-maintenance/gates/${gateNo}`);
};

const goRobotDetail = (robotNo) => {
  router.push(`/admin/predictive-maintenance/robots/${robotNo}`);
};

const goEventDetail = (event) => {
  if (event.equipmentType === 'CAMERA') goCameraDetail(event.equipmentNo);
  if (event.equipmentType === 'GATE') goGateDetail(event.equipmentNo);
  if (event.equipmentType === 'ROBOT') goRobotDetail(event.equipmentNo);
};

const goAttentionDetail = (item) => goEventDetail({ equipmentType: item.type, equipmentNo: item.no });

const showFeedback = (message, type = 'success') => {
  feedbackMessage.value = message;
  feedbackType.value = type;
  window.clearTimeout(feedbackTimer);
  feedbackTimer = window.setTimeout(() => { feedbackMessage.value = ''; }, 2500);
};

const resetRegisterForm = () => {
  cameraForm.value = createCameraForm();
  gateForm.value = createGateForm();
  robotForm.value = createRobotForm();
};

const openRegisterDialog = async () => {
  resetRegisterForm();
  try {
    if (selectedEquipment.value === 'CAMERA') {
      await Promise.all([parkingStore.loadList(), gateStore.loadList()]);
    } else if (selectedEquipment.value === 'GATE') {
      await Promise.all([parkingStore.loadList(), gateStore.loadList()]);
    }
    registerDialog.value?.showModal();
  } catch (error) {
    console.error('장비 등록 준비 실패', error);
    showFeedback('등록에 필요한 정보를 불러오지 못했습니다.', 'error');
  }
};

const closeRegisterDialog = () => {
  if (!registering.value) registerDialog.value?.close();
};

const closeRegisterOnBackdrop = (event) => {
  if (event.target === registerDialog.value) closeRegisterDialog();
};

const submitEquipment = async () => {
  if (registering.value) return;
  registering.value = true;
  const type = selectedEquipment.value;

  try {
    let result;
    if (type === 'CAMERA') result = await cameraStore.signup({ ...cameraForm.value });
    if (type === 'GATE') result = await gateStore.signup({ ...gateForm.value });
    if (type === 'ROBOT') result = await robotStore.signup({ ...robotForm.value, setNo: Number(robotForm.value.setNo) });

    const success = type === 'ROBOT' ? result === true : result?.success;
    if (!success) {
      showFeedback(result?.message || `${equipmentLabel.value} 등록에 실패했습니다.`, 'error');
      return;
    }

    registerDialog.value?.close();
    showFeedback(`${equipmentLabel.value}를 등록했습니다.`);
    await refreshCurrent(false);
  } catch (error) {
    console.error('장비 등록 실패', error);
    showFeedback(error.response?.data?.message || '장비 등록에 실패했습니다.', 'error');
  } finally {
    registering.value = false;
  }
};

const requestDelete = (type, item) => {
  deleteTarget.value = { type, item };
};

const cancelDelete = () => {
  if (!deleting.value) deleteTarget.value = null;
};

const confirmDelete = async () => {
  if (!deleteTarget.value || deleting.value) return;
  deleting.value = true;
  const { type, item } = deleteTarget.value;

  try {
    let result;
    if (type === 'CAMERA') result = await cameraStore.remove(item.cameraNo);
    if (type === 'GATE') result = await gateStore.remove(item.gateNo);
    if (type === 'ROBOT') result = await robotStore.remove(item.robotNo);

    const success = type === 'ROBOT' ? result === true : result?.success;
    if (!success) {
      showFeedback(result?.message || `${deleteEquipmentLabel.value}를 삭제할 수 없습니다.`, 'error');
      return;
    }

    showFeedback(`${deleteItemName.value} 장비를 삭제했습니다.`);
    deleteTarget.value = null;
    await refreshCurrent(false);
  } catch (error) {
    console.error('장비 삭제 실패', error);
    showFeedback(error.response?.data?.message || '장비를 삭제하지 못했습니다.', 'error');
  } finally {
    deleting.value = false;
  }
};

const refreshCameras = async (manual = false) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    if (manual) {
      await analyzeAllCameras();
    }

    const [cameraResponse, pdmResponse] = await Promise.all([
      getCameraList(),
      getLatestCameraPdm(),
    ]);

    const cameraList = Array.isArray(cameraResponse.data) ? cameraResponse.data : [];
    const pdmList = Array.isArray(pdmResponse.data) ? pdmResponse.data : [];
    const pdmByCameraNo = new Map(pdmList.map((pdm) => [Number(pdm.cameraNo), pdm]));
    const nextCameras = cameraList.map((camera) => {
      const pdm = pdmByCameraNo.get(Number(camera.cameraNo));
      return {
        ...camera,
        cameraStatus: riskLevelToStatus(pdm?.riskLevel),
        riskLevel: pdm?.riskLevel || '미확인',
        riskScore: pdm?.riskScore ?? null,
        predictedAt: pdm?.predictedAt || null,
        actionStatus: pdm?.actionStatus || null,
      };
    });
    const previousByNo = new Map(cameras.value.map((camera) => [camera.cameraNo, normalizeStatus(camera.cameraStatus)]));

    nextCameras.forEach((camera) => {
      const previous = previousByNo.get(camera.cameraNo);
      const current = normalizeStatus(camera.cameraStatus);
      if (previous && previous !== current) {
        pushEvent(
          current === 'FAULT' ? 'fault' : 'change',
          `${camera.cameraName} 상태 변경`,
          `${statusText(previous)} → ${statusText(current)}`,
          'CAMERA',
          camera.cameraNo,
        );
      }
    });

    cameras.value = nextCameras;
    responseTime.value = Math.max(1, Math.round(performance.now() - startedAt));
    lastUpdatedAt.value = new Date();
    pushEvent('receive', manual ? '수동 상태 갱신' : '상태 데이터 수신', `카메라 ${nextCameras.length}대 · 응답 ${responseTime.value}ms`);
  } catch (error) {
    console.error('카메라 예지보전 상태 조회 실패', error);
    errorMessage.value = '카메라 상태를 불러오지 못했습니다.';
    pushEvent('fault', '관제 서버 연결 실패', '카메라 상태 데이터를 수신하지 못했습니다.');
  } finally {
    loading.value = false;
  }
};

const refreshGates = async (manual = false) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    if (manual) {
      await analyzeAllGates();
    }

    const [gateResponse, pdmResponse] = await Promise.all([
      getGateList(),
      getLatestGatePdm(),
    ]);

    const gateList = Array.isArray(gateResponse.data) ? gateResponse.data : [];
    const pdmList = Array.isArray(pdmResponse.data) ? pdmResponse.data : [];
    const pdmByGateNo = new Map(pdmList.map((pdm) => [Number(pdm.gateNo), pdm]));
    const nextGates = gateList.map((gate) => {
      const pdm = pdmByGateNo.get(Number(gate.gateNo));
      return {
        ...gate,
        operatingStatus: riskLevelToStatus(pdm?.riskLevel),
        riskLevel: pdm?.riskLevel || '미확인',
        riskScore: pdm?.riskScore ?? null,
        predictedAt: pdm?.predictedAt || null,
        actionStatus: pdm?.actionStatus || null,
      };
    });
    const previousByNo = new Map(gates.value.map((gate) => [gate.gateNo, {
      operatingStatus: normalizeStatus(gate.operatingStatus),
      gateStatus: gate.gateStatus,
    }]));

    nextGates.forEach((gate) => {
      const previous = previousByNo.get(gate.gateNo);
      const current = normalizeStatus(gate.operatingStatus);
      if (previous && previous.operatingStatus !== current) {
        pushEvent(
          current === 'FAULT' ? 'fault' : 'change',
          `${gate.gateName} 상태 변경`,
          `${statusText(previous.operatingStatus)} → ${statusText(current)}`,
          'GATE',
          gate.gateNo,
        );
      }
      if (previous && previous.gateStatus !== gate.gateStatus) {
        pushEvent(
          'change',
          `${gate.gateName} 개폐 상태 변경`,
          `${previous.gateStatus === 1 ? '열림' : '닫힘'} → ${gate.gateStatus === 1 ? '열림' : '닫힘'}`,
          'GATE',
          gate.gateNo,
        );
      }
    });

    gates.value = nextGates;
    responseTime.value = Math.max(1, Math.round(performance.now() - startedAt));
    lastUpdatedAt.value = new Date();
    pushEvent('receive', manual ? '수동 상태 갱신' : '상태 데이터 수신', `게이트 ${nextGates.length}대 · 응답 ${responseTime.value}ms`);
  } catch (error) {
    console.error('게이트 예지보전 상태 조회 실패', error);
    errorMessage.value = '게이트 상태를 불러오지 못했습니다.';
    pushEvent('fault', '관제 서버 연결 실패', '게이트 상태 데이터를 수신하지 못했습니다.');
  } finally {
    loading.value = false;
  }
};

const refreshRobots = async (manual = false) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    if (manual) {
      await analyzeAllRobots();
    }

    const [robotResponse, pdmResponse] = await Promise.all([
      getRobotList(),
      getLatestRobotPdm(),
    ]);
    const robotList = Array.isArray(robotResponse.data) ? robotResponse.data : [];
    const pdmList = Array.isArray(pdmResponse.data) ? pdmResponse.data : [];
    const pdmByRobotNo = new Map(pdmList.map((pdm) => [Number(pdm.robotNo), pdm]));
    const nextRobots = robotList.map((robot) => {
      const pdm = pdmByRobotNo.get(Number(robot.robotNo));
      return {
        ...robot,
        monitoringStatus: riskLevelToStatus(pdm?.riskLevel),
        riskLevel: pdm?.riskLevel || '미확인',
        riskScore: pdm?.riskScore ?? null,
        predictedAt: pdm?.predictedAt || null,
        actionStatus: pdm?.actionStatus || null,
      };
    });
    const previousByNo = new Map(robots.value.map((robot) => [robot.robotNo, normalizeStatus(robot.monitoringStatus)]));

    nextRobots.forEach((robot) => {
      const previous = previousByNo.get(robot.robotNo);
      const current = normalizeStatus(robot.monitoringStatus);
      if (previous && previous !== current) {
        pushEvent(
          current === 'FAULT' ? 'fault' : 'change',
          `${robot.robotCode} 상태 변경`,
          `${statusText(previous)} → ${statusText(current)}`,
          'ROBOT',
          robot.robotNo,
        );
      }
    });

    robots.value = nextRobots;
    responseTime.value = Math.max(1, Math.round(performance.now() - startedAt));
    lastUpdatedAt.value = new Date();
    pushEvent('receive', manual ? '수동 상태 갱신' : '상태 데이터 수신', `주차로봇 ${nextRobots.length}대 · 응답 ${responseTime.value}ms`);
  } catch (error) {
    console.error('주차로봇 상태 조회 실패', error);
    errorMessage.value = '주차로봇 상태를 불러오지 못했습니다.';
    pushEvent('fault', '관제 서버 연결 실패', '주차로봇 상태 데이터를 수신하지 못했습니다.');
  } finally {
    loading.value = false;
  }
};

const formatHeartbeat = (value) => value ? new Date(value).toLocaleTimeString('ko-KR', { hour12: false }) : '-';
const formatRiskScore = (value) => value == null ? '-' : `${(Number(value) * 100).toFixed(1)}%`;

const refreshStatistics = async () => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    const [
      cameraResponse, cameraPdmResponse, gateResponse, gatePdmResponse, robotResponse, robotPdmResponse,
      savedCameraResponse, savedGateResponse, savedRobotResponse,
    ] = await Promise.all([
      getCameraList(), getLatestCameraPdm(), getGateList(), getLatestGatePdm(), getRobotList(), getLatestRobotPdm(),
      getSavedCameraPdm(), getSavedGatePdm(), getSavedRobotPdm(),
    ]);
    const cameraPdmByNo = new Map((Array.isArray(cameraPdmResponse.data) ? cameraPdmResponse.data : []).map((item) => [Number(item.cameraNo), item]));
    const gatePdmByNo = new Map((Array.isArray(gatePdmResponse.data) ? gatePdmResponse.data : []).map((item) => [Number(item.gateNo), item]));
    const robotPdmByNo = new Map((Array.isArray(robotPdmResponse.data) ? robotPdmResponse.data : []).map((item) => [Number(item.robotNo), item]));

    cameras.value = (Array.isArray(cameraResponse.data) ? cameraResponse.data : []).map((item) => ({
      ...item,
      cameraStatus: riskLevelToStatus(cameraPdmByNo.get(Number(item.cameraNo))?.riskLevel),
      actionStatus: cameraPdmByNo.get(Number(item.cameraNo))?.actionStatus || null,
    }));
    gates.value = (Array.isArray(gateResponse.data) ? gateResponse.data : []).map((item) => ({
      ...item,
      operatingStatus: riskLevelToStatus(gatePdmByNo.get(Number(item.gateNo))?.riskLevel),
      actionStatus: gatePdmByNo.get(Number(item.gateNo))?.actionStatus || null,
    }));
    robots.value = (Array.isArray(robotResponse.data) ? robotResponse.data : []).map((item) => ({
      ...item,
      monitoringStatus: riskLevelToStatus(robotPdmByNo.get(Number(item.robotNo))?.riskLevel),
      actionStatus: robotPdmByNo.get(Number(item.robotNo))?.actionStatus || null,
    }));
    savedPdmRecords.value = [
      ...(Array.isArray(savedCameraResponse.data) ? savedCameraResponse.data : []),
      ...(Array.isArray(savedGateResponse.data) ? savedGateResponse.data : []),
      ...(Array.isArray(savedRobotResponse.data) ? savedRobotResponse.data : []),
    ];
    responseTime.value = Math.max(1, Math.round(performance.now() - startedAt));
    lastUpdatedAt.value = new Date();
  } catch (error) {
    console.error('예지보전 통합 통계 조회 실패', error);
    errorMessage.value = '통합 상태를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

const refreshCurrent = (manual = false) => ({
  STATS: () => refreshStatistics(),
  CAMERA: () => refreshCameras(manual),
  GATE: () => refreshGates(manual),
  ROBOT: () => refreshRobots(manual),
}[selectedEquipment.value]?.());

onMounted(async () => {
  // 탭 배지는 세 장비의 최신 상태를 함께 사용하므로, 새로고침 직후에도
  // 미조치 위험 건수가 사라지지 않도록 전체 스냅샷을 먼저 구성한다.
  await refreshStatistics();
  if (selectedEquipment.value !== 'STATS') {
    await refreshCurrent();
  }
  pollTimer = window.setInterval(refreshCurrent, 5000);
  clockTimer = window.setInterval(() => { currentTime.value = new Date(); }, 1000);
});

onUnmounted(() => {
  window.clearInterval(pollTimer);
  window.clearInterval(clockTimer);
  window.clearTimeout(feedbackTimer);
});
</script>

<style scoped>
.pdm-page { min-height: 100%; padding: 28px; color: #263d50; background: #f3f7fa; }
.pdm-header { margin-bottom: 22px; padding: 27px 30px; display: flex; justify-content: space-between; align-items: center; border-radius: 19px; color: #fff; background: linear-gradient(125deg, #173d58, #245f82 62%, #2387a8); box-shadow: 0 15px 34px rgba(24, 65, 92, .2); }
.eyebrow { display: flex; align-items: center; gap: 9px; font-size: 10px; font-weight: 900; letter-spacing: .18em; opacity: .8; }.eyebrow i,.live-clock span i { width: 8px; height: 8px; border-radius: 50%; background: #5ff0ae; box-shadow: 0 0 0 5px rgba(95,240,174,.13); animation: pulse 1.6s infinite; }
.pdm-header h1 { margin: 8px 0 4px; font-size: 29px; }.pdm-header p { margin: 0; opacity: .72; }.live-clock { display: grid; justify-items: end; gap: 2px; }.live-clock span { display: flex; align-items: center; gap: 8px; color: #8dffc5; font-size: 11px; font-weight: 800; }.live-clock strong { font-size: 28px; letter-spacing: .04em; }.live-clock small { opacity: .58; }
.summary-grid { margin-bottom: 18px; display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 14px; }.summary-card { padding: 19px 21px; display: grid; grid-template-columns: 1fr auto; align-items: end; gap: 3px 10px; border: 1px solid #dfe9ef; border-radius: 15px; background: #fff; box-shadow: 0 8px 20px rgba(47,77,97,.06); }.summary-card span { color: #718797; font-size: 12px; font-weight: 800; }.summary-card strong { grid-row: span 2; font-size: 34px; }.summary-card small { color: #9aabb7; }.summary-card.normal strong { color: #1b9a67; }.summary-card.fault strong { color: #d64c55; }.summary-card.maintenance strong { color: #d58a26; }
.monitor-grid { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 18px; }.camera-panel,.event-panel { padding: 21px; border: 1px solid #dfe8ee; border-radius: 17px; background: #fff; box-shadow: 0 10px 28px rgba(42,75,96,.07); }.panel-header { margin-bottom: 18px; display: flex; justify-content: space-between; align-items: center; }.panel-header h2 { margin: 0 0 3px; font-size: 18px; }.panel-header p { margin: 0; color: #91a1ad; font-size: 11px; }.panel-header button { padding: 8px 12px; border: 0; border-radius: 9px; color: #2776a5; background: #eaf5fb; font-weight: 800; cursor: pointer; }.panel-header button:disabled { opacity: .55; }.camera-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 13px; }.camera-card { padding: 17px; border: 1px solid #e0e9ef; border-left: 4px solid #a6b6c1; border-radius: 13px; background: #fbfdfe; }.camera-card.normal { border-left-color: #28a973; }.camera-card.fault { border-left-color: #dc4b55; background: #fffafa; }.camera-card.maintenance { border-left-color: #dc922b; background: #fffdf8; }.camera-card-top { display: flex; justify-content: space-between; align-items: center; }.camera-icon { width: 32px; height: 25px; position: relative; border-radius: 6px; background: #dceaf2; }.camera-icon::after { content:''; position:absolute; right:-8px; top:7px; border-width:6px 0 6px 9px; border-style:solid; border-color:transparent transparent transparent #aac6d6; }.camera-icon i { position:absolute; width:8px; height:8px; left:12px; top:8px; border-radius:50%; background:#5d899f; }.status-badge { padding: 4px 8px; display:flex; align-items:center; gap:5px; border-radius:999px; color:#657d8d; background:#edf2f5; font-size:10px; font-weight:900; }.status-badge i { width:6px;height:6px;border-radius:50%;background:currentColor; }.normal .status-badge { color:#16865a;background:#e7f7ef; }.fault .status-badge { color:#c83c46;background:#ffebed; }.maintenance .status-badge { color:#b86e12;background:#fff1d8; }.camera-card > strong { margin-top:13px; display:block; font-size:15px; }.camera-card > p { margin:4px 0 14px; color:#8194a2;font-size:11px; }.camera-card dl { margin:0;display:grid;grid-template-columns:1fr 1fr;gap:7px 12px; }.camera-card dl div { display:flex;justify-content:space-between;gap:6px; }.camera-card dt { color:#9aabb6;font-size:10px; }.camera-card dd { margin:0;color:#536c7d;font-size:10px;font-weight:800; }.signal-line { height:22px;margin-top:12px;display:flex;align-items:center;gap:4px;overflow:hidden; }.signal-line span { width:4px;border-radius:3px;background:#4fb88a;animation:signal .9s ease-in-out infinite alternate; }.signal-line span:nth-child(1){height:6px}.signal-line span:nth-child(2){height:15px;animation-delay:.1s}.signal-line span:nth-child(3){height:9px;animation-delay:.2s}.signal-line span:nth-child(4){height:19px;animation-delay:.3s}.signal-line span:nth-child(5){height:11px;animation-delay:.4s}.fault .signal-line span { background:#dc5961; }.maintenance .signal-line span { background:#df9a3d; }
.event-count { min-width:27px;height:27px;display:grid;place-items:center;border-radius:50%;color:#327a9f;background:#e8f4fa;font-size:11px;font-weight:900; }.event-list { max-height:560px;margin:0;padding:0;overflow:auto;list-style:none; }.event-list li { position:relative;padding:0 0 20px 22px;display:grid;grid-template-columns:8px 1fr;gap:10px; }.event-list li::after { content:'';position:absolute;left:3px;top:13px;bottom:2px;width:1px;background:#dce6ec; }.event-list li:last-child::after { display:none; }.event-list > li > i { width:8px;height:8px;margin-top:4px;border-radius:50%;background:#48a9d4;box-shadow:0 0 0 4px #e8f5fa; }.event-list li.fault > i { background:#d84c56;box-shadow:0 0 0 4px #fdecee; }.event-list li.change > i { background:#d8902e;box-shadow:0 0 0 4px #fff3df; }.event-list strong { font-size:12px; }.event-list p { margin:3px 0;color:#78909f;font-size:10px;line-height:1.45; }.event-list time { color:#a0afb9;font-size:9px; }.monitor-error,.monitor-empty { padding:70px 20px;text-align:center;color:#9a6470; }
@keyframes pulse { 50% { opacity:.45;transform:scale(.8); } } @keyframes signal { from { transform:scaleY(.45); } to { transform:scaleY(1); } }
@media(max-width:1100px){.monitor-grid{grid-template-columns:1fr}.event-panel{order:2}.event-list{max-height:300px}}@media(max-width:760px){.pdm-page{padding:14px}.pdm-header{align-items:flex-start;gap:20px}.summary-grid{grid-template-columns:repeat(2,1fr)}.camera-grid{grid-template-columns:1fr}.live-clock strong{font-size:20px}}@media(max-width:520px){.pdm-header{flex-direction:column}.live-clock{justify-items:start}.summary-grid{grid-template-columns:1fr}}

/* 관리자 공통 목록 화면과 동일한 톤 */
.pdm-page {
  padding: 8px 0 32px;
  color: #202833;
  background: transparent;
}

.pdm-header {
  min-height: 98px;
  margin: 0 0 12px;
  padding: 0;
  color: #202833;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
}

.pdm-header h1 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
  line-height: 36px;
}

.pdm-header p {
  color: #68717d;
  font-size: 14px;
  opacity: 1;
}

.live-clock {
  box-sizing: border-box;
  width: 160px;
  min-width: 160px;
  height: 52px;
  padding: 8px 14px;
  border: 1px solid #cfd5dc;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 5px 14px rgba(35, 52, 66, .06);
}

.live-clock span {
  color: #3d6b55;
}

.live-clock strong {
  width: 100%;
  color: #202833;
  font-size: 21px;
  font-variant-numeric: tabular-nums;
  text-align: right;
  white-space: nowrap;
}

.live-clock small {
  color: #8996a7;
  opacity: 1;
}

.summary-grid {
  gap: 10px;
  margin-bottom: 12px;
}

.summary-card,
.camera-panel,
.event-panel {
  border: 1px solid #d8dde3;
  border-radius: 6px;
  box-shadow: 0 7px 18px rgba(35, 52, 66, .07);
}

.summary-card {
  padding: 15px 17px;
  border-top: 3px solid #333e49;
}

.summary-card strong {
  color: #27313c;
  font-size: 28px;
}

.summary-card.normal strong { color: #365f4d; }
.summary-card.fault strong { color: #9f3f46; }
.summary-card.maintenance strong { color: #966728; }

.monitor-grid {
  gap: 12px;
}

.camera-panel,
.event-panel {
  padding: 18px;
}

.panel-header {
  min-height: 38px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 2px solid #343f4a;
}

.panel-header h2 {
  color: #252e38;
  font-size: 16px;
}

.panel-header button {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #aeb6bf;
  border-radius: 4px;
  color: #fff;
  background: #394550;
  font-size: 12px;
}

.camera-grid {
  gap: 9px;
}

.camera-card {
  padding: 14px;
  border-color: #d4dae0;
  border-radius: 5px;
  background: #f7f8f9;
  box-shadow: none;
}

.camera-card.normal { border-left-color: #476c59; background: #f7f9f8; }
.camera-card.fault { border-left-color: #a7444b; background: #fbf7f7; }
.camera-card.maintenance { border-left-color: #9b702f; background: #faf9f5; }

.camera-icon { background: #dce0e4; }
.camera-icon::after { border-left-color: #9ba5ad; }
.camera-icon i { background: #515e68; }
.camera-card > strong { color: #27313b; }
.camera-card > p { color: #697580; }
.camera-card dd { color: #414c56; }

.normal .status-badge { color: #365f4d; background: #e5ece8; }
.fault .status-badge { color: #963d44; background: #f1e4e5; }
.maintenance .status-badge { color: #8a6127; background: #f2eadc; }

.signal-line span { background: #506b5b; }
.fault .signal-line span { background: #9f4b51; }
.maintenance .signal-line span { background: #95703b; }

.event-count { color: #fff; background: #3d4853; }
.event-list > li > i { background: #566572; box-shadow: 0 0 0 4px #e5e8eb; }
.event-list li.fault > i { background: #a7444b; box-shadow: 0 0 0 4px #f1e4e5; }
.event-list li.change > i { background: #95703b; box-shadow: 0 0 0 4px #f2eadc; }

/* 관제 영역의 흰 배경을 없애고 회색 박스로 통일한다. */
.pdm-page {
  padding: 18px;
  border-radius: 7px;
  background: #d4d8dc;
}

.pdm-header {
  padding: 16px 18px;
  border: 1px solid #aeb5bc;
  border-radius: 6px;
  background: #c2c7cc;
}

.live-clock {
  border-color: #9ea6ae;
  background: #aeb5bb;
  box-shadow: none;
}

.live-clock span { color: #315c47; }
.live-clock small { color: #56616a; }

.summary-card {
  border-color: #a7aeb5;
  background: #b9bec3;
  box-shadow: none;
}

.summary-card span,
.summary-card small {
  color: #535e67;
}

.camera-panel,
.event-panel {
  border-color: #a5acb3;
  background: #b7bcc1;
  box-shadow: none;
}

.panel-header {
  border-bottom-color: #4b555e;
}

.panel-header p { color: #5e6871; }

.camera-card,
.camera-card.normal,
.camera-card.fault,
.camera-card.maintenance {
  border-color: #989fa6;
  background: #aeb4b9;
}

.camera-card.normal { border-left-color: #3f6652; }
.camera-card.fault { border-left-color: #913b43; }
.camera-card.maintenance { border-left-color: #866028; }

.camera-icon { background: #90989f; }
.camera-icon::after { border-left-color: #727c84; }
.camera-icon i { background: #39444d; }
.camera-card > p { color: #4e5a63; }
.camera-card dt { color: #59636b; }
.camera-card dd { color: #303a42; }

.normal .status-badge { color: #2e5341; background: #96aaa0; }
.fault .status-badge { color: #792f36; background: #b89a9c; }
.maintenance .status-badge { color: #71501f; background: #b9aa8e; }

.event-list li::after { background: #929aa1; }
.event-list > li > i { box-shadow: 0 0 0 4px #a6acb1; }
.event-list li.fault > i { box-shadow: 0 0 0 4px #b6a1a3; }
.event-list li.change > i { box-shadow: 0 0 0 4px #b7aa93; }
.event-list p { color: #4e5a63; }
.event-list time { color: #626c74; }

.monitor-error,
.monitor-empty {
  color: #59494d;
  background: #aeb4b9;
}

/* 입출차 기록 관리와 동일한 관리자 다크 팔레트 */
.pdm-page {
  padding: 0 0 32px;
  color: #f1f3f5;
  background: #24292e;
}

.pdm-header {
  padding: 0;
  border: 0;
  color: #f1f3f5;
  background: #24292e;
}

.pdm-header h1,
.panel-header h2,
.camera-card > strong,
.event-list strong {
  color: #f1f3f5;
}

.pdm-header p,
.panel-header p,
.camera-card > p,
.event-list p,
.event-list time {
  color: #9da6ad;
}

.live-clock,
.summary-card,
.camera-panel,
.event-panel {
  border-color: #505960;
  color: #f1f3f5;
  background: #2b3035;
  box-shadow: none;
}

.live-clock span { color: #9fc9ad; }
.live-clock strong { color: #ffffff; }
.live-clock small { color: #9da6ad; }

.live-clock {
  border-color: transparent;
  background: transparent;
  box-shadow: none;
}

.header-monitor-tools {
  display: grid;
  justify-items: end;
  gap: 8px;
}

.equipment-tabs {
  display: flex;
  border: 1px solid #505960;
  background: #2b3035;
}

.equipment-tabs button {
  min-width: 78px;
  height: 31px;
  padding: 0 12px;
  border: 0;
  border-right: 1px solid #505960;
  color: #aeb6bc;
  background: #2b3035;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.equipment-tabs button:last-child { border-right: 0; }
.equipment-tabs button:hover { color: #fff; background: #3a4147; }
.equipment-tabs button.active { color: #171b1f; background: #ffc928; }

.pdm-page .equipment-tabs button .equipment-tab-alert {
  margin-left: 6px;
  color: #ff7479 !important;
  -webkit-text-fill-color: #ff7479 !important;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.live-clock-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  width: 100%;
  text-align: right;
  white-space: nowrap;
}

.pdm-page .equipment-tabs button.active .equipment-tab-alert {
  color: #d9535a !important;
  -webkit-text-fill-color: #d9535a !important;
}

.summary-card {
  border-top-color: #69737b;
}

.summary-card.normal { border-top-color: #76ad8d; }
.summary-card.maintenance { border-top-color: #d8a653; }
.summary-card.fault { border-top-color: #d9747b; }

.summary-card span,
.summary-card small {
  color: #aeb6bc;
}

/* 장비별 요약 카드는 통계 요약 카드의 타이포그래피와 간격을 그대로 따른다. */
.summary-card {
  min-height: 92px;
  padding: 16px 18px;
  gap: 4px 12px;
}

.summary-card span {
  font-size: 12px;
}

.summary-card strong {
  font-size: 31px;
  line-height: 1;
}

.summary-card small {
  font-size: 10px;
  line-height: normal;
}

.summary-card strong { color: #f1f3f5; }
.summary-card.normal strong { color: #8fc7a6; }
.summary-card.fault strong { color: #ef969c; }
.summary-card.maintenance strong { color: #e6bd79; }

.panel-header {
  border-bottom-color: #69737b;
}

.panel-header button {
  border-color: #69737b;
  color: #ffffff;
  background: #3a4147;
}

.panel-header button:hover:not(:disabled) {
  border-color: #8d969d;
  background: #4a5259;
}

.camera-card,
.camera-card.normal,
.camera-card.fault,
.camera-card.maintenance,
.monitor-error,
.monitor-empty {
  border-color: #596168;
  color: #f1f3f5;
  background: #343a40;
}

.camera-card.normal { border-left-color: #76ad8d; }
.camera-card.fault { border-left-color: #d9747b; }
.camera-card.maintenance { border-left-color: #d8a653; }

.camera-icon { background: #4a5259; }
.camera-icon::after { border-left-color: #69737b; }
.camera-icon i { background: #d2d7db; }
.gate-icon {
  width: 32px;
  height: 25px;
  position: relative;
  display: inline-block;
}

.gate-icon::before {
  content: '';
  width: 7px;
  height: 22px;
  position: absolute;
  left: 2px;
  bottom: 0;
  border-radius: 2px 2px 0 0;
  background: #69737b;
}

.gate-icon i {
  width: 23px;
  height: 5px;
  position: absolute;
  left: 7px;
  top: 5px;
  border-radius: 1px;
  background: repeating-linear-gradient(90deg, #d9747b 0 6px, #e1e5e8 6px 12px);
  transform: rotate(0deg);
  transform-origin: left center;
  transition: transform .25s ease;
}

.gate-icon.open i {
  transform: rotate(-55deg);
}
.robot-icon {
  box-sizing: border-box;
  width: 32px;
  height: 25px;
  position: relative;
  display: inline-block;
  border: 2px solid #69737b;
  border-radius: 5px;
  background: #4a5259;
}

.robot-icon::before,
.robot-icon::after {
  content: '';
  width: 5px;
  height: 5px;
  position: absolute;
  top: 6px;
  border-radius: 50%;
  background: #d2d7db;
}

.robot-icon::before { left: 6px; }
.robot-icon::after { right: 6px; }

.robot-icon i {
  width: 16px;
  height: 3px;
  position: absolute;
  left: 6px;
  bottom: 4px;
  background: #8b949c;
}
.camera-card dl div {
  min-width: 0;
  align-items: center;
}

.camera-card .equipment-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.equipment-info-row {
  display: grid !important;
  grid-template-columns: max-content minmax(0, 1fr);
  justify-content: initial !important;
  align-items: center;
  gap: 7px !important;
}

.equipment-info-row dd {
  min-width: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.equipment-chip-row {
  display: flex !important;
  justify-content: flex-start !important;
  flex-wrap: wrap;
  gap: 6px !important;
}

.equipment-chip {
  padding: 4px 7px;
  border: 1px solid #69737b;
  border-radius: 4px;
  color: #e1e5e8;
  background: #4a5259;
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.camera-card dt {
  flex: 0 0 auto;
  padding: 3px 6px;
  border: 1px solid #69737b;
  border-radius: 4px;
  color: #d6dce0;
  background: #4a5259;
  font-size: 9px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.camera-card dd {
  min-width: 0;
  color: #e1e5e8;
  line-height: 1.2;
  overflow-wrap: anywhere;
  text-align: right;
}

.camera-card .equipment-info-row {
  grid-template-columns: max-content max-content;
  justify-content: start !important;
}

.camera-card .equipment-info-row dd {
  text-align: left;
}

.normal .status-badge { color: #b9dec7; background: #405b4b; }
.fault .status-badge { color: #f2b8bc; background: #664147; }
.maintenance .status-badge { color: #f0d39c; background: #65543a; }

.signal-line span { background: #76ad8d; }
.fault .signal-line span { background: #d9747b; }
.maintenance .signal-line span { background: #d8a653; }

.event-count { color: #ffffff; background: #4a5259; }
.event-list li::after { background: #596168; }
.event-list > li > i { background: #8b949c; box-shadow: 0 0 0 4px #3a4147; }
.event-list li.fault > i { background: #d9747b; box-shadow: 0 0 0 4px #533a3f; }
.event-list li.change > i { background: #d8a653; box-shadow: 0 0 0 4px #554a38; }

.event-list li.clickable {
  margin: 0 -8px 8px;
  padding: 8px 8px 12px 30px;
  border-radius: 6px;
  cursor: pointer;
  transition: background .15s ease;
}

.event-list li.clickable:hover,
.event-list li.clickable:focus-visible {
  outline: none;
  background: #3a4147;
}

.event-list li.clickable::after {
  left: 11px;
}

/* 카메라 12대를 한눈에 볼 수 있도록 카드 밀도를 높인다. */
.monitor-grid {
  grid-template-columns: minmax(0, 1fr) 240px;
  align-items: start;
}

.camera-panel,
.event-panel {
  align-self: start;
}

.camera-panel {
  height: auto;
}

.event-panel {
  height: 0;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.event-panel .event-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
}

.camera-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.camera-card {
  padding: 11px 12px;
  border-left-width: 3px;
}

.camera-card > strong {
  margin-top: 8px;
  font-size: 14px;
}

.camera-card > p {
  margin: 2px 0 9px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.equipment-title-row {
  min-width: 0;
  margin: 6px 0 5px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.equipment-title-row strong {
  min-width: 0;
  overflow: hidden;
  color: #f1f3f5;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.equipment-title-row p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #9da6ad;
  font-size: 11px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.camera-card dl {
  gap: 4px 8px;
}

.signal-line {
  height: 15px;
  margin-top: 7px;
}

@media (max-width: 1350px) {
  .camera-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1050px) {
  .camera-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .pdm-page { padding: 12px; }
  .pdm-header { min-height: 0; }
  .header-monitor-tools { width: 100%; justify-items: stretch; }
  .equipment-tabs { width: 100%; }
  .equipment-tabs button { flex: 1; min-width: 0; }
  .live-clock { justify-items: end; }
  .camera-grid { grid-template-columns: 1fr; }
}
.camera-card[role="button"] { cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.camera-card[role="button"]:hover { transform: translateY(-2px); box-shadow: 0 10px 22px rgba(42,75,96,.12); }
.camera-card[role="button"]:focus-visible { outline: 3px solid rgba(35,135,168,.28); outline-offset: 3px; }

/* 통합 통계 대시보드 */
.stats-dashboard { display: grid; gap: 12px; }
.stats-summary-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 10px; }
.stats-summary-card { min-height: 92px; padding: 16px 18px; display: grid; grid-template-columns: 1fr auto; align-items: end; gap: 4px 12px; border: 1px solid #505960; border-top: 3px solid #69737b; background: linear-gradient(145deg,#30363c,#292e33); }
.stats-summary-card span { color: #aeb6bc; font-size: 12px; font-weight: 800; }.stats-summary-card strong { grid-row: span 2; color: #f1f3f5; font-size: 31px; line-height: 1; }.stats-summary-card small { color: #8f989f; font-size: 10px; }
.stats-summary-card.normal { border-top-color: #76ad8d; }.stats-summary-card.normal strong { color: #9ed0b2; }.stats-summary-card.maintenance { border-top-color: #d8a653; }.stats-summary-card.maintenance strong { color: #e8c27f; }.stats-summary-card.fault { border-top-color: #d9747b; }.stats-summary-card.fault strong { color: #efa0a6; }
.stats-content-grid { display: grid; grid-template-columns: .85fr 1.35fr; gap: 12px; }
.stats-panel { min-width: 0; padding: 18px; border: 1px solid #505960; background: #2b3035; }.stats-panel-header { min-height: 42px; margin-bottom: 16px; padding-bottom: 11px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #596168; }.stats-panel-header h2 { margin: 0 0 4px; color: #f1f3f5; font-size: 16px; }.stats-panel-header p { margin: 0; color: #9da6ad; font-size: 10px; }
.stats-live { display: flex; align-items: center; gap: 7px; color: #9ed0b2; font-size: 10px; font-weight: 900; letter-spacing: .08em; }.stats-live i { width: 7px; height: 7px; border-radius: 50%; background: #76ad8d; box-shadow: 0 0 0 4px rgba(118,173,141,.14); animation: pulse 1.6s infinite; }
.donut-area { min-height: 220px; display: flex; justify-content: center; align-items: center; gap: 34px; }.status-donut { width: 162px; height: 162px; padding: 18px; display: grid; place-items: center; border-radius: 50%; box-shadow: 0 0 30px rgba(0,0,0,.18); }.status-donut > div { width: 100%; height: 100%; display: grid; place-content: center; justify-items: center; border-radius: 50%; background: #2b3035; }.status-donut strong { color: #fff; font-size: 29px; }.status-donut span { color: #9da6ad; font-size: 10px; }
.donut-legend { width: 125px; margin: 0; padding: 0; display: grid; gap: 14px; list-style: none; }.donut-legend li { display: grid; grid-template-columns: 9px 1fr auto; align-items: center; gap: 8px; color: #aeb6bc; font-size: 11px; }.donut-legend i { width: 8px; height: 8px; border-radius: 2px; }.donut-legend i.normal,.stacked-bar i.normal { background: #76ad8d; }.donut-legend i.maintenance,.stacked-bar i.maintenance { background: #d8a653; }.donut-legend i.fault,.stacked-bar i.fault { background: #d9747b; }.donut-legend strong { color: #f1f3f5; }
.equipment-bars { min-height: 220px; display: grid; align-content: center; gap: 22px; }.equipment-bar-label { margin-bottom: 7px; display: flex; justify-content: space-between; }.equipment-bar-label strong { color: #e5e8eb; font-size: 12px; }.equipment-bar-label span { color: #9da6ad; font-size: 10px; }.stacked-bar { height: 13px; display: flex; overflow: hidden; border: 1px solid #596168; background: #24292e; }.stacked-bar i { height: 100%; transition: width .3s ease; }.equipment-bar-counts { margin-top: 7px; display: flex; gap: 15px; color: #8f989f; font-size: 9px; }
.today-panel,.attention-panel { grid-column: 1 / -1; }.today-date { padding: 4px 8px; border: 1px solid #596168; color: #aeb6bc; background: #343a40; font-size: 10px; font-weight: 800; }.today-layout { display: grid; grid-template-columns: minmax(310px,.8fr) minmax(420px,1.2fr); gap: 24px; }.today-metrics { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }.today-metrics > div { min-height: 72px; padding: 11px 13px; display: grid; grid-template-columns: 1fr auto auto; align-items: end; gap: 4px; border: 1px solid #505960; background: #343a40; }.today-metrics span { grid-column: 1 / -1; color: #9da6ad; font-size: 10px; }.today-metrics strong { color: #f1f3f5; font-size: 23px; }.today-metrics small { margin-bottom: 3px; color: #8f989f; font-size: 9px; }.today-metrics .warning strong { color: #e8c27f; }.today-metrics .fault strong { color: #efa0a6; }
.hourly-chart { min-width: 0; }.chart-heading { height: 25px; display: flex; justify-content: space-between; color: #aeb6bc; font-size: 10px; }.chart-heading div { display: flex; align-items: center; gap: 5px; color: #8f989f; font-size: 9px; }.chart-heading i { width: 7px; height: 7px; display: inline-block; }.chart-heading i.warning { background: #d8a653; }.chart-heading i.fault { margin-left: 7px; background: #d9747b; }.chart-body { height: 128px; padding: 9px 8px 0; display: grid; grid-template-columns: repeat(6,1fr); align-items: end; gap: 10px; border-left: 1px solid #505960; border-bottom: 1px solid #505960; background: repeating-linear-gradient(to top,transparent 0 31px,rgba(89,97,104,.38) 31px 32px); }.chart-column { height: 100%; display: grid; grid-template-rows: 1fr 19px; align-items: end; gap: 4px; }.chart-bars { height: 100%; display: flex; justify-content: center; align-items: end; gap: 3px; }.chart-bars i { width: min(14px,35%); min-height: 0; transition: height .3s ease; }.chart-bars i.warning { background: #d8a653; }.chart-bars i.fault { background: #d9747b; }.chart-column > span { color: #8f989f; text-align: center; font-size: 8px; }
.stats-panel-header button { height: 30px; padding: 0 13px; border: 1px solid #69737b; color: #fff; background: #3a4147; font-size: 11px; font-weight: 800; cursor: pointer; }.attention-list { margin: 0; padding: 0; display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 8px; list-style: none; }.attention-list li { min-width: 0; padding: 11px 12px; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; border: 1px solid #596168; border-left: 3px solid #d8a653; background: #343a40; cursor: pointer; }.attention-list li.fault { border-left-color: #d9747b; }.attention-state { padding: 3px 6px; color: #f0d39c; background: #65543a; font-size: 9px; font-weight: 900; }.attention-list li.fault .attention-state { color: #f2b8bc; background: #664147; }.attention-list div { min-width: 0; display: grid; gap: 3px; }.attention-list strong { overflow: hidden; color: #f1f3f5; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.attention-list small { color: #9da6ad; font-size: 9px; }.attention-list b { color: #aeb6bc; font-size: 9px; }.all-clear { min-height: 70px; display: grid; place-items: center; color: #9ed0b2; font-size: 12px; }
@media(max-width:1000px){.stats-content-grid{grid-template-columns:1fr}.today-panel,.attention-panel{grid-column:auto}.today-layout{grid-template-columns:1fr}.attention-list{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:760px){.stats-summary-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.attention-list{grid-template-columns:1fr}}@media(max-width:520px){.stats-summary-grid{grid-template-columns:1fr}.donut-area{flex-direction:column;gap:20px}.today-metrics{grid-template-columns:1fr}.chart-body{gap:4px}}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-header .register-equipment-button {
  border-color: #69737b;
  color: #f1f3f5;
  background: #3a4147;
}

.pdm-page .camera-card .card-delete-button {
  width: 18px !important;
  min-width: 18px !important;
  height: 18px !important;
  min-height: 18px !important;
  padding: 0 !important;
  display: grid !important;
  place-items: center !important;
  border: 1px solid #ff626b !important;
  border-radius: 50% !important;
  color: #ff626b !important;
  -webkit-text-fill-color: #ff626b !important;
  background: transparent !important;
  font-size: 14px !important;
  line-height: 1 !important;
  font-weight: 900;
  cursor: pointer;
  margin-left: auto;
}

.pdm-page .camera-card .signal-line {
  height: 19px;
  margin-top: 5px;
}

.pdm-page .camera-card .card-delete-button:hover {
  border-color: #ff7b82 !important;
  color: #ffffff !important;
  -webkit-text-fill-color: #ffffff !important;
  background: #d9535a !important;
}

.equipment-register-dialog {
  width: min(92vw, 560px);
  padding: 0;
  border: 1px solid #596168;
  border-radius: 8px;
  color: #e5e8eb;
  background: #2b3035;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .48);
}

.equipment-register-dialog::backdrop {
  background: rgba(15, 18, 21, .72);
}

.equipment-register-form {
  padding: 20px;
}

.equipment-register-form .dialog-header {
  margin-bottom: 18px;
  padding-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #505960;
}

.equipment-register-form .dialog-header h3 {
  margin: 0 0 4px;
  color: #f1f3f5;
  font-size: 18px;
}

.equipment-register-form .dialog-header p {
  margin: 0;
  color: #9da6ad;
  font-size: 11px;
}

.equipment-register-form .dialog-close-button {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid #596168;
  color: #d7dce0;
  background: #343a40;
  font-size: 19px;
  cursor: pointer;
}

.equipment-register-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.equipment-register-fields label,
.equipment-register-fields fieldset {
  min-width: 0;
  margin: 0;
  display: grid;
  gap: 6px;
  border: 0;
}

.equipment-register-fields label > span,
.equipment-register-fields legend {
  color: #aeb6bc;
  font-size: 11px;
  font-weight: 800;
}

.equipment-register-fields :is(input, select) {
  min-width: 0;
  height: 36px;
  padding: 0 9px;
  border: 1px solid #596168;
  border-radius: 4px;
  color: #eef1f3;
  background: #343a40;
}

.set-position-options {
  height: 36px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.set-position-options label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.set-position-options input {
  width: 15px;
  height: 15px;
}

.equipment-register-form .dialog-actions {
  margin-top: 20px;
  padding-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  border-top: 1px solid #505960;
}

.equipment-register-form .dialog-actions button {
  min-width: 72px;
  min-height: 32px;
  border: 1px solid #69737b;
  color: #f1f3f5;
  background: #3a4147;
  font-weight: 800;
  cursor: pointer;
}

.equipment-register-form .dialog-actions .submit-button {
  border-color: #5b88b2;
  background: #334c63;
}

@media (max-width: 600px) {
  .equipment-register-fields { grid-template-columns: 1fr; }
  .panel-actions { gap: 4px; }
  .panel-header .panel-actions button { padding: 6px 8px; font-size: 10px; }
}

/* 데스크톱 관제 화면에서 핵심 현황이 한 화면에 들어오도록 밀도를 높인다. */
:global(.admin-layout .content:has(.pdm-page)) {
  padding: 10px 24px 24px;
  background: #24292e;
}

.pdm-page {
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

@media (min-width: 1001px) {
  .pdm-page { padding-bottom: 12px; }

  .pdm-header,
  :global(.admin-layout .content) .pdm-page.facility-list-page > .pdm-header.facility-list-heading {
    min-height: 0 !important;
    margin-bottom: 8px !important;
    align-items: center !important;
  }

  .pdm-header h1 { line-height: 28px; }
  .pdm-header p { font-size: 12px; }
  .header-monitor-tools { grid-template-columns: auto auto; align-items: center; gap: 8px; }
  .equipment-tabs button { height: 29px; min-width: 70px; padding-inline: 10px; }
  .live-clock { width: 160px; min-width: 160px; height: 48px; padding: 5px 10px; gap: 0; justify-items: stretch; }
  .live-clock-meta { gap: 8px; }
  .live-clock strong { font-size: 17px; }
  .live-clock small { font-size: 9px; }

  .stats-dashboard { gap: 8px; }
  .stats-summary-grid,
  .summary-grid {
    width: 100%;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 8px;
  }
  .stats-summary-card,
  .summary-card { min-height: 68px; padding: 10px 13px; gap: 4px 12px; }
  .stats-summary-card strong,
  .summary-card strong { font-size: 26px; }
  .stats-content-grid { gap: 8px; }
  .stats-panel { padding: 12px; }
  .stats-panel-header { min-height: 32px; margin-bottom: 9px; padding-bottom: 7px; }
  .stats-panel-header h2 { font-size: 14px; }
  .donut-area,
  .equipment-bars { min-height: 150px; }
  .status-donut { width: 118px; height: 118px; padding: 13px; }
  .status-donut strong { font-size: 23px; }
  .donut-legend { gap: 9px; }
  .equipment-bars { gap: 12px; }
  .today-layout { grid-template-columns: minmax(280px, .75fr) minmax(380px, 1.25fr); gap: 15px; }
  .today-metrics > div { min-height: 54px; padding: 7px 10px; }
  .today-metrics strong { font-size: 19px; }
  .chart-body { height: 92px; }

  .summary-grid { margin-bottom: 8px; }
  .monitor-grid { gap: 8px; }
  .camera-panel,
  .event-panel { padding: 12px; }
  .panel-header { margin-bottom: 8px; padding-bottom: 7px; }
  .camera-grid { grid-auto-rows: 112px; }
  .camera-card { height: 100%; padding: 9px 10px; }
  .camera-card > strong { margin-top: 5px; }
  .camera-card > p { margin-bottom: 5px; }
  .equipment-chip { padding: 3px 6px; }
  .event-list { max-height: 430px; }
}

@media (max-width: 760px) {
  :global(.admin-layout .content:has(.pdm-page)) {
    padding: 10px 12px 16px;
  }
}
</style>
