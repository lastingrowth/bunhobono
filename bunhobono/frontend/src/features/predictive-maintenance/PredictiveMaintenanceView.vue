<template>
  <main class="pdm-page management-list-page facility-list-page">
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
          </button>
        </nav>

        <div class="live-clock">
          <span><i></i> 실시간 연결</span>
          <strong>{{ clockText }}</strong>
          <small>5초마다 자동 갱신</small>
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
            <div><h2>오늘 기록 통계</h2><p>알파 시연용 데이터 · 백엔드 통계 미연동</p></div>
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
      <article class="summary-card fault">
        <span>고장</span><strong>{{ statusCounts.FAULT }}</strong><small>즉시 확인 필요</small>
      </article>
      <article class="summary-card maintenance">
        <span>점검 중</span><strong>{{ statusCounts.MAINTENANCE }}</strong><small>유지보수 대상</small>
      </article>
    </section>

    <section v-if="selectedEquipment !== 'STATS'" class="monitor-grid">
      <article class="camera-panel">
        <header class="panel-header">
          <div><h2>{{ equipmentLabel }} 상태</h2><p>마지막 수신 {{ lastUpdatedText }}</p></div>
          <button type="button" :disabled="loading" @click="refreshCurrent(true)">{{ loading ? '갱신 중' : '지금 갱신' }}</button>
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
            <strong>{{ camera.cameraName }}</strong>
            <p>{{ camera.parkingName || '주차장 미지정' }}</p>
            <dl>
              <div><dt>게이트</dt><dd>{{ camera.gateName || '-' }}</dd></div>
              <div><dt>용도</dt><dd>{{ camera.cameraType === 'In' ? '입차' : '출차' }}</dd></div>
              <div><dt>장비번호</dt><dd>#{{ camera.cameraNo }}</dd></div>
              <div><dt>수신</dt><dd>{{ responseTime }}ms</dd></div>
            </dl>
            <div class="signal-line"><span></span><span></span><span></span><span></span><span></span></div>
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
            <strong>{{ gate.gateName }}</strong>
            <p>{{ gate.parkingName || '주차장 미지정' }}</p>
            <dl>
              <div><dt>분류</dt><dd>{{ gate.gateType === 'In' || gate.gateType === 'IN' ? '입차' : '출차' }}</dd></div>
              <div><dt>개폐</dt><dd>{{ gate.gateStatus === 1 ? '열림' : '닫힘' }}</dd></div>
              <div><dt>장비번호</dt><dd>#{{ gate.gateNo }}</dd></div>
              <div><dt>수신</dt><dd>{{ responseTime }}ms</dd></div>
            </dl>
            <div class="signal-line"><span></span><span></span><span></span><span></span><span></span></div>
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
              <span class="status-badge"><i></i>{{ robotStatusText(robot.robotStatus) }}</span>
            </div>
            <strong>{{ robot.robotCode }}</strong>
            <p>SET {{ robot.setNo }} / {{ robot.setPosition }}</p>
            <dl>
              <div><dt>배터리</dt><dd>{{ formatBattery(robot.batteryLevel) }}</dd></div>
              <div><dt>운전시간</dt><dd>{{ formatOperatingHours(robot.operatingHours) }}</dd></div>
              <div><dt>장비번호</dt><dd>#{{ robot.robotNo }}</dd></div>
              <div><dt>최근 통신</dt><dd>{{ formatHeartbeat(robot.lastHeartbeatAt) }}</dd></div>
            </dl>
            <div class="signal-line"><span></span><span></span><span></span><span></span><span></span></div>
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
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getCameraList } from '@/features/camera/cameraApi';
import { getList as getGateList } from '@/features/gates/gateApi';
import { getRobotList } from '@/features/robot/robotApi';
import {
  analyzeAllCameras,
  analyzeAllGates,
  getLatestCameraPdm,
  getLatestGatePdm,
} from './predictiveMaintenanceApi';

const router = useRouter();
const route = useRoute();
const cameras = ref([]);
const gates = ref([]);
const robots = ref([]);
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
let pollTimer;
let clockTimer;
let eventSequence = 0;

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
const robotStatusToMonitoringStatus = (status) => ({
  STANDBY: 'NORMAL',
  WORKING: 'NORMAL',
  CHARGING: 'NORMAL',
  LOW_BATTERY: 'MAINTENANCE',
  WARNING: 'MAINTENANCE',
  ERROR: 'FAULT',
  OFFLINE: 'FAULT',
}[status] || 'UNKNOWN');
const robotStatusText = (status) => ({
  STANDBY: '대기',
  WORKING: '작업 중',
  CHARGING: '충전 중',
  LOW_BATTERY: '배터리 부족',
  WARNING: '주의',
  ERROR: '오류',
  OFFLINE: '연결 끊김',
}[status] || status || '상태 미확인');
const statusText = (status) => ({ NORMAL: '정상', FAULT: '고장', MAINTENANCE: '점검 중', UNKNOWN: '상태 미확인' }[normalizeStatus(status)]);
const statusClass = (status) => normalizeStatus(status).toLowerCase();
const currentItems = computed(() => ({ CAMERA: cameras.value, GATE: gates.value, ROBOT: robots.value }[selectedEquipment.value] || []));
const equipmentLabel = computed(() => ({ STATS: '통합 통계', CAMERA: '카메라', GATE: '게이트', ROBOT: '주차로봇' }[selectedEquipment.value]));
const equipmentDescription = computed(() => selectedEquipment.value === 'STATS'
  ? '카메라·게이트·주차로봇의 현재 예지보전 상태를 한눈에 확인합니다.'
  : `${equipmentLabel.value} 장비의 작동 상태와 실시간 수신 기록을 확인합니다.`);
const itemStatus = (item) => ({ CAMERA: item.cameraStatus, GATE: item.operatingStatus, ROBOT: item.monitoringStatus }[selectedEquipment.value]);
const statusCounts = computed(() => currentItems.value.reduce((counts, item) => {
  const status = normalizeStatus(itemStatus(item));
  if (status in counts) counts[status] += 1;
  return counts;
}, { NORMAL: 0, FAULT: 0, MAINTENANCE: 0 }));
const normalRate = computed(() => currentItems.value.length ? Math.round(statusCounts.value.NORMAL / currentItems.value.length * 100) : 0);
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
const todayHourlyData = computed(() => {
  const day = currentTime.value.getDate();
  const currentSlot = Math.min(5, Math.floor(currentTime.value.getHours() / 4));
  const labels = ['00시', '04시', '08시', '12시', '16시', '20시'];
  const data = labels.map((label, index) => {
    if (index > currentSlot) return { label, warning: 0, fault: 0 };
    const warning = (day * (index + 2) + index * 3) % 5;
    const fault = (day + index * 2) % 3;
    return { label, warning, fault };
  });
  const maxValue = Math.max(1, ...data.flatMap((point) => [point.warning, point.fault]));
  return data.map((point) => ({
    ...point,
    warningHeight: point.warning ? Math.max(10, point.warning / maxValue * 100) : 0,
    faultHeight: point.fault ? Math.max(10, point.fault / maxValue * 100) : 0,
  }));
});
const todayDemoStats = computed(() => {
  const minutes = currentTime.value.getHours() * 60 + currentTime.value.getMinutes();
  const warning = todayHourlyData.value.reduce((sum, point) => sum + point.warning, 0);
  const fault = todayHourlyData.value.reduce((sum, point) => sum + point.fault, 0);
  return {
    analysis: overallCounts.value.TOTAL * Math.max(1, Math.floor(minutes / 5)),
    saved: overallCounts.value.TOTAL * (currentTime.value.getHours() + 1) + warning + fault,
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
  router.push(`/admin/robots/${robotNo}`);
};

const goEventDetail = (event) => {
  if (event.equipmentType === 'CAMERA') goCameraDetail(event.equipmentNo);
  if (event.equipmentType === 'GATE') goGateDetail(event.equipmentNo);
  if (event.equipmentType === 'ROBOT') goRobotDetail(event.equipmentNo);
};

const goAttentionDetail = (item) => goEventDetail({ equipmentType: item.type, equipmentNo: item.no });

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

const refreshRobots = async () => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    const response = await getRobotList();
    const robotList = Array.isArray(response.data) ? response.data : [];
    const nextRobots = robotList.map((robot) => ({
      ...robot,
      monitoringStatus: robotStatusToMonitoringStatus(robot.robotStatus),
    }));
    const previousByNo = new Map(robots.value.map((robot) => [robot.robotNo, robot.robotStatus]));

    nextRobots.forEach((robot) => {
      const previous = previousByNo.get(robot.robotNo);
      if (previous && previous !== robot.robotStatus) {
        pushEvent(
          robot.monitoringStatus === 'FAULT' ? 'fault' : 'change',
          `${robot.robotCode} 상태 변경`,
          `${robotStatusText(previous)} → ${robotStatusText(robot.robotStatus)}`,
          'ROBOT',
          robot.robotNo,
        );
      }
    });

    robots.value = nextRobots;
    responseTime.value = Math.max(1, Math.round(performance.now() - startedAt));
    lastUpdatedAt.value = new Date();
    pushEvent('receive', '상태 데이터 수신', `주차로봇 ${nextRobots.length}대 · 응답 ${responseTime.value}ms`);
  } catch (error) {
    console.error('주차로봇 상태 조회 실패', error);
    errorMessage.value = '주차로봇 상태를 불러오지 못했습니다.';
    pushEvent('fault', '관제 서버 연결 실패', '주차로봇 상태 데이터를 수신하지 못했습니다.');
  } finally {
    loading.value = false;
  }
};

const formatBattery = (value) => value == null ? '-' : `${Number(value).toFixed(1)}%`;
const formatOperatingHours = (value) => value == null ? '-' : `${Number(value).toFixed(1)}시간`;
const formatHeartbeat = (value) => value ? new Date(value).toLocaleTimeString('ko-KR', { hour12: false }) : '-';

const refreshStatistics = async () => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    const [cameraResponse, cameraPdmResponse, gateResponse, gatePdmResponse, robotResponse] = await Promise.all([
      getCameraList(), getLatestCameraPdm(), getGateList(), getLatestGatePdm(), getRobotList(),
    ]);
    const cameraPdmByNo = new Map((Array.isArray(cameraPdmResponse.data) ? cameraPdmResponse.data : []).map((item) => [Number(item.cameraNo), item]));
    const gatePdmByNo = new Map((Array.isArray(gatePdmResponse.data) ? gatePdmResponse.data : []).map((item) => [Number(item.gateNo), item]));

    cameras.value = (Array.isArray(cameraResponse.data) ? cameraResponse.data : []).map((item) => ({
      ...item,
      cameraStatus: riskLevelToStatus(cameraPdmByNo.get(Number(item.cameraNo))?.riskLevel),
    }));
    gates.value = (Array.isArray(gateResponse.data) ? gateResponse.data : []).map((item) => ({
      ...item,
      operatingStatus: riskLevelToStatus(gatePdmByNo.get(Number(item.gateNo))?.riskLevel),
    }));
    robots.value = (Array.isArray(robotResponse.data) ? robotResponse.data : []).map((item) => ({
      ...item,
      monitoringStatus: robotStatusToMonitoringStatus(item.robotStatus),
    }));
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
  ROBOT: () => refreshRobots(),
}[selectedEquipment.value]?.());

onMounted(async () => {
  await refreshCurrent();
  pollTimer = window.setInterval(refreshCurrent, 5000);
  clockTimer = window.setInterval(() => { currentTime.value = new Date(); }, 1000);
});

onUnmounted(() => {
  window.clearInterval(pollTimer);
  window.clearInterval(clockTimer);
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
  min-width: 176px;
  padding: 12px 16px;
  border: 1px solid #cfd5dc;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 5px 14px rgba(35, 52, 66, .06);
}

.live-clock span {
  color: #3d6b55;
}

.live-clock strong {
  color: #202833;
  font-size: 21px;
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

.summary-card {
  border-top-color: #69737b;
}

.summary-card span,
.summary-card small {
  color: #aeb6bc;
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
  width: 34px;
  height: 28px;
  position: relative;
  display: inline-block;
}

.gate-icon::before {
  content: '';
  width: 7px;
  height: 25px;
  position: absolute;
  left: 2px;
  bottom: 0;
  border-radius: 2px 2px 0 0;
  background: #69737b;
}

.gate-icon i {
  width: 25px;
  height: 5px;
  position: absolute;
  left: 7px;
  top: 6px;
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
  width: 34px;
  height: 28px;
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
  top: 7px;
  border-radius: 50%;
  background: #d2d7db;
}

.robot-icon::before { left: 7px; }
.robot-icon::after { right: 7px; }

.robot-icon i {
  width: 18px;
  height: 3px;
  position: absolute;
  left: 6px;
  bottom: 5px;
  background: #8b949c;
}
.camera-card dt { color: #9da6ad; }
.camera-card dd { color: #e1e5e8; }

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
  grid-template-columns: minmax(0, 1fr) 290px;
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
    grid-template-columns: repeat(3, minmax(0, 1fr));
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
  .live-clock { justify-items: start; }
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
</style>
