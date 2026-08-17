<template>
  <main class="robot-detail">
    <header class="detail-header">
      <div>
        <span>ROBOT OPERATION MONITORING</span>
        <h1>{{ robot?.robotCode || `주차로봇 #${robotNo}` }}</h1>
        <p>SET {{ robot?.setNo ?? '-' }} · {{ robot?.setPosition || '위치 미확인' }}</p>
      </div>
      <div class="header-actions">
        <button type="button" :disabled="robotStore.loading" @click="refreshDetail">
          {{ robotStore.loading ? '갱신 중' : '지금 갱신' }}
        </button>
        <button type="button" class="secondary" @click="goList">목록으로</button>
      </div>
    </header>

    <p v-if="robotStore.errorMessage" class="message error">{{ robotStore.errorMessage }}</p>
    <p v-else-if="robotStore.loading && !robot" class="message">로봇 정보를 불러오는 중입니다.</p>

    <template v-else>
      <section class="summary-grid">
        <article class="summary-card status" :class="statusClass(robot?.robotStatus)">
          <span>현재 상태</span>
          <strong>{{ statusText(robot?.robotStatus) }}</strong>
          <small>{{ formatDateTime(robot?.lastHeartbeatAt) }}</small>
        </article>
        <article class="summary-card">
          <span>배터리</span>
          <strong>{{ formatBattery(robot?.batteryLevel) }}</strong>
          <small>현재 배터리 잔량</small>
        </article>
        <article class="summary-card status" :class="currentMaintenanceState.className">
          <span>점검 필요도</span>
          <strong>{{ currentMaintenanceState.label }}</strong>
          <small>상태·배터리 기준</small>
        </article>
        <article class="summary-card">
          <span>최근 통신</span>
          <strong class="date-value">{{ formatDateTime(robot?.lastHeartbeatAt) }}</strong>
          <small>로봇 상태 수신 기준</small>
        </article>
      </section>

      <section class="detail-layout">
        <article class="panel">
          <header><h2>등급별 상태 분포</h2><p>알파 시연용 상태 분포이며 예지보전 모델은 미연동입니다.</p></header>
          <div class="probability-list">
            <div v-for="item in demoProbabilities" :key="item.label">
              <div class="probability-label">
                <span>{{ item.label }}</span>
                <strong>{{ formatPercent(item.value) }}</strong>
              </div>
              <div class="probability-bar">
                <i :class="item.className" :style="{ width: `${item.value}%` }"></i>
              </div>
            </div>
          </div>
        </article>

        <article class="panel">
          <header><h2>로봇 기본정보</h2><p>설비 등록 정보입니다.</p></header>
          <dl class="info-list">
            <div><dt>로봇 번호</dt><dd>#{{ robotNo }}</dd></div>
            <div><dt>로봇 코드</dt><dd>{{ robot?.robotCode || '-' }}</dd></div>
            <div><dt>소속 세트</dt><dd>SET {{ robot?.setNo ?? '-' }}</dd></div>
            <div><dt>세트 위치</dt><dd>{{ robot?.setPosition || '-' }}</dd></div>
            <div><dt>등록 일시</dt><dd>{{ formatDateTime(robot?.createdAt) }}</dd></div>
            <div><dt>최근 수정</dt><dd>{{ formatDateTime(robot?.updatedAt) }}</dd></div>
          </dl>
        </article>
      </section>

      <section class="panel history-panel">
        <header>
          <h2>실시간 상태 기록</h2>
          <p>5초마다 조회한 최근 20건이며 DB에는 저장되지 않습니다.</p>
        </header>
        <div class="table-wrap">
          <table>
            <thead><tr><th>갱신 시각</th><th>운행 상태</th><th>배터리</th><th>통신 상태</th><th>점검 필요도</th></tr></thead>
            <tbody>
              <tr v-for="record in realtimeHistory" :key="record.snapshotNo">
                <td>{{ formatDateTime(record.sampledAt) }}</td>
                <td><span class="status-badge" :class="statusClass(record.robotStatus)">{{ statusText(record.robotStatus) }}</span></td>
                <td>{{ formatBattery(record.batteryLevel) }}</td>
                <td><span class="status-badge normal">응답 정상</span></td>
                <td><span class="status-badge" :class="maintenanceState(record).className">{{ maintenanceState(record).label }}</span></td>
              </tr>
              <tr v-if="realtimeHistory.length === 0"><td colspan="5" class="empty">실시간 상태 기록이 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import { useRobotStore } from './robotStore';

const route = useRoute();
const router = useRouter();
const robotStore = useRobotStore();
const { detail: robot } = storeToRefs(robotStore);
const realtimeHistory = ref([]);
let snapshotSequence = 0;
let refreshTimer;

const robotNo = computed(() => Number(route.params.robotNo));

const statusText = (status) => ({
  STANDBY: '대기', WORKING: '작업 중', CHARGING: '충전 중',
  LOW_BATTERY: '배터리 부족', WARNING: '주의', ERROR: '오류', OFFLINE: '연결 끊김',
}[status] || status || '상태 미확인');

const statusClass = (status) => ({
  STANDBY: 'normal', WORKING: 'normal', CHARGING: 'normal',
  LOW_BATTERY: 'warning', WARNING: 'warning', ERROR: 'critical', OFFLINE: 'critical',
}[status] || 'unknown');

const formatBattery = (value) => value == null ? '-' : `${Number(value).toFixed(1)}%`;
const formatOperatingHours = (value) => value == null ? '-' : `${Number(value).toFixed(1)}시간`;
const formatDays = (value) => value == null ? '점검 기록 없음' : `${value}일`;
const formatDateTime = (value) => value ? new Date(value).toLocaleString('ko-KR') : '-';

const maintenanceState = (record) => {
  if (['ERROR', 'OFFLINE'].includes(record.robotStatus)) {
    return { label: '즉시 점검', className: 'critical' };
  }
  if (['WARNING', 'LOW_BATTERY'].includes(record.robotStatus) || Number(record.batteryLevel) <= 20) {
    return { label: '점검 필요', className: 'warning' };
  }
  if (Number(record.batteryLevel) <= 40) {
    return { label: '관찰', className: 'watch' };
  }
  return { label: '정상', className: 'normal' };
};

const currentMaintenanceState = computed(() => maintenanceState(robot.value || {}));

// 예지보전 모델 연동 전 알파 시연용 분포다.
const demoProbabilities = computed(() => {
  const state = currentMaintenanceState.value.className;

  if (state === 'critical') return [
    { label: '정상', value: 5, className: 'normal' },
    { label: '주의', value: 15, className: 'warning' },
    { label: '위험', value: 80, className: 'critical' },
  ];
  if (state === 'warning') return [
    { label: '정상', value: 20, className: 'normal' },
    { label: '주의', value: 70, className: 'warning' },
    { label: '위험', value: 10, className: 'critical' },
  ];
  if (state === 'watch') return [
    { label: '정상', value: 60, className: 'normal' },
    { label: '주의', value: 35, className: 'warning' },
    { label: '위험', value: 5, className: 'critical' },
  ];
  return [
    { label: '정상', value: 90, className: 'normal' },
    { label: '주의', value: 8, className: 'warning' },
    { label: '위험', value: 2, className: 'critical' },
  ];
});

const formatPercent = (value) => `${Number(value).toFixed(1)}%`;

const refreshDetail = async () => {
  if (robotStore.loading || !robotNo.value) return;

  await robotStore.loadDetail(robotNo.value);

  if (robot.value) {
    realtimeHistory.value.unshift({
      snapshotNo: ++snapshotSequence,
      sampledAt: new Date().toISOString(),
      robotStatus: robot.value.robotStatus,
      batteryLevel: robot.value.batteryLevel,
      operatingHours: robot.value.operatingHours,
      lastHeartbeatAt: robot.value.lastHeartbeatAt,
    });
    realtimeHistory.value = realtimeHistory.value.slice(0, 20);
  }
};

const goList = () => router.push({
  path: '/admin/predictive-maintenance',
  query: { equipment: 'ROBOT' },
});

onMounted(async () => {
  await refreshDetail();
  refreshTimer = window.setInterval(refreshDetail, 5000);
});

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer);
  robotStore.clearDetail();
});
</script>

<style scoped>
.robot-detail { width: 100%; display: grid; gap: 12px; color: var(--admin-ink); }
.detail-header,.summary-grid,.panel { border: 1px solid var(--admin-line); background: var(--admin-surface); }
.detail-header { min-height: 74px; padding: 12px 18px; display: flex; justify-content: space-between; align-items: center; }
.detail-header span { font-size: 10px; font-weight: 800; letter-spacing: .12em; color: var(--admin-muted); }
.detail-header h1 { margin: 4px 0 2px; font-size: 22px; color: var(--admin-ink); }
.detail-header p,.panel header p,.summary-card span,.summary-card small { margin: 0; color: var(--admin-muted); }
.header-actions { display: flex; gap: 6px; }
.header-actions button { min-height: 32px; padding: 5px 12px; border: 1px solid #5b88b2; color: #d8ecff; background: #334c63; font-weight: 700; cursor: pointer; }
.header-actions button.secondary { border-color: var(--admin-line); color: var(--admin-ink); background: var(--admin-surface-muted); }
.header-actions button:disabled { opacity: .55; }
.summary-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); background: var(--admin-surface-muted); }
.summary-card { min-width: 0; min-height: 78px; padding: 10px 14px; display: flex; justify-content: center; flex-direction: column; gap: 5px; border-right: 1px solid var(--admin-line); }
.summary-card:last-child { border-right: 0; }.summary-card strong { font-size: 17px; color: var(--admin-ink); }.summary-card strong.date-value { font-size: 13px; }
.summary-card.status strong,.status-badge { width: fit-content; min-width: 64px; padding: 3px 8px; border: 1px solid #737b82; text-align: center; color: #e5e7eb; background: #454c52; font-size: 11px; font-weight: 800; }
.summary-card.status.normal strong,.status-badge.normal { border-color: #4f8c6b; color: #d9f7e6; background: #315641; }
.summary-card.status.warning strong,.status-badge.warning { border-color: #d3a92e; color: #ffe9a6; background: #655525; }
.summary-card.status.critical strong,.status-badge.critical { border-color: #c45a60; color: #ffdadd; background: #66383c; }
.summary-card.status.watch strong,.status-badge.watch { border-color: #7e8e9d; color: #e0e8ef; background: #4b5964; }
.detail-layout { display: grid; grid-template-columns: 1.25fr 1fr; gap: 12px; }.panel { min-width: 0; }
.panel header { min-height: 52px; padding: 9px 14px; border-bottom: 1px solid var(--admin-line); }.panel h2 { margin: 0 0 3px; font-size: 16px; color: var(--admin-ink); }
.probability-list { padding: 16px; display: grid; gap: 15px; }
.probability-label { margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--admin-muted); }.probability-label strong { color: var(--admin-ink); }
.probability-bar { height: 9px; overflow: hidden; border: 1px solid var(--admin-line); background: var(--admin-surface-muted); }.probability-bar i { display: block; height: 100%; transition: width .25s ease; }.probability-bar i.normal { background: #4f8c6b; }.probability-bar i.warning { background: #d3a92e; }.probability-bar i.critical { background: #c45a60; }
.info-list { margin: 0; padding: 0 14px 12px; display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); }.info-list div { min-height: 42px; padding: 6px 10px; border-bottom: 1px solid var(--admin-line); }.info-list dt { font-size: 10px; color: var(--admin-muted); }.info-list dd { margin: 4px 0 0; font-size: 13px; font-weight: 700; color: var(--admin-ink); }
.history-panel { width: 100%; }.table-wrap { max-height: 216px; overflow: auto; }table { width: 100%; min-width: 760px; border-collapse: collapse; table-layout: fixed; }
th,td { box-sizing: border-box; height: 36px; padding: 3px 6px; border-bottom: 1px solid var(--admin-line); text-align: center; font-size: 11px; white-space: nowrap; }th { position: sticky; top: 0; z-index: 1; color: var(--admin-muted); background: var(--admin-surface-muted); }.status-badge { display: inline-block; }
.message,.empty { margin: 0; padding: 18px; color: var(--admin-muted); background: var(--admin-surface); }.message.error { color: #ff8c91; }.empty { height: 60px; text-align: center; }
@media(max-width:900px){.summary-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.detail-layout{grid-template-columns:1fr}}
@media(max-width:700px){.detail-header{align-items:flex-start;flex-direction:column;gap:12px}.summary-grid,.info-list{grid-template-columns:1fr}}
</style>
