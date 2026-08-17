<template>
  <main class="camera-pdm-detail">
    <header class="detail-header">
      <div>
        <span>CAMERA PREDICTIVE MAINTENANCE</span>
        <h1>{{ camera?.cameraName || `카메라 #${cameraNo}` }}</h1>
        <p>{{ camera?.parkingName || '주차장 미지정' }} · {{ camera?.gateName || '게이트 미지정' }}</p>
      </div>
      <div class="header-actions">
        <button type="button" :disabled="loading" @click="refresh(true)">
          {{ loading ? '분석 중' : '지금 분석' }}
        </button>
        <button type="button" class="secondary" @click="goList">목록으로</button>
      </div>
    </header>

    <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
    <p v-else-if="loading && !latest" class="message">카메라 예지보전 정보를 불러오는 중입니다.</p>

    <template v-else>
      <section class="summary-grid">
        <article class="summary-card risk" :class="riskClass(latest?.riskLevel)">
          <span>현재 위험등급</span>
          <strong>{{ latest?.riskLevel || '미확인' }}</strong>
          <small>{{ formatDateTime(latest?.predictedAt) }}</small>
        </article>
        <article class="summary-card">
          <span>이상 확률</span>
          <strong>{{ formatPercent(abnormalProbability) }}</strong>
          <small>주의와 위험 확률의 합계</small>
        </article>
        <article class="summary-card">
          <span>위험 확률</span>
          <strong>{{ formatPercent(latest?.criticalProbability) }}</strong>
          <small>위험 등급으로 판단될 가능성</small>
        </article>
        <article class="summary-card">
          <span>실제 분석시각</span>
          <strong class="date-value">{{ formatDateTime(latest?.predictedAt) }}</strong>
          <small>FastAPI 모델 실행 기준</small>
        </article>
      </section>

      <section class="detail-layout">
        <article class="panel">
          <header><h2>등급별 예측 확률</h2><p>최근 분석 결과의 확률 분포입니다.</p></header>
          <div class="probability-list">
            <div v-for="item in probabilities" :key="item.label">
              <div><span>{{ item.label }}</span><strong>{{ formatPercent(item.value) }}</strong></div>
              <div class="bar"><i :class="item.className" :style="{ width: `${percentValue(item.value)}%` }"></i></div>
            </div>
          </div>
        </article>

        <article class="panel">
          <header><h2>카메라 기본정보</h2><p>설비 등록 정보입니다.</p></header>
          <dl class="info-list">
            <div><dt>카메라 번호</dt><dd>#{{ cameraNo }}</dd></div>
            <div><dt>카메라 이름</dt><dd>{{ camera?.cameraName || '-' }}</dd></div>
            <div><dt>카메라 유형</dt><dd>{{ camera?.cameraType || '-' }}</dd></div>
            <div><dt>연결 게이트</dt><dd>{{ camera?.gateName || '-' }}</dd></div>
            <div><dt>주차장</dt><dd>{{ camera?.parkingName || '-' }}</dd></div>
            <div><dt>설치일</dt><dd>{{ formatDate(camera?.installDate) }}</dd></div>
          </dl>
        </article>
      </section>

      <section class="panel history-panel">
        <header class="history-header">
          <div>
            <h2>{{ activeHistoryTab === 'realtime' ? '실시간 분석 기록' : '저장 이력' }}</h2>
            <p>{{ historyDescription }}</p>
          </div>
          <div class="history-tabs">
            <button
              type="button"
              :class="{ active: activeHistoryTab === 'realtime' }"
              @click="changeHistoryTab('realtime')"
            >실시간 분석 기록</button>
            <button
              type="button"
              :class="{ active: activeHistoryTab === 'saved' }"
              @click="changeHistoryTab('saved')"
            >저장 이력</button>
          </div>
        </header>
        <div class="table-wrap">
          <table>
            <thead><tr><th>분석시각</th><th>등급</th><th>정상</th><th>주의</th><th>위험</th></tr></thead>
            <tbody>
              <tr v-for="record in visibleHistory" :key="record.pdmNo || `${record.cameraNo}-${record.predictedAt}`">
                <td>{{ formatDateTime(record.predictedAt) }}</td>
                <td><span class="risk-badge" :class="riskClass(record.riskLevel)">{{ record.riskLevel }}</span></td>
                <td>{{ formatPercent(record.normalProbability) }}</td>
                <td>{{ formatPercent(record.warningProbability) }}</td>
                <td>{{ formatPercent(record.criticalProbability) }}</td>
              </tr>
              <tr v-if="visibleHistory.length === 0"><td colspan="5" class="empty">{{ emptyHistoryMessage }}</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getCameraList } from '@/features/camera/cameraApi';
import {
  analyzeAllCameras,
  getLatestCameraPdm,
  getRecentCameraPdm,
  getSavedCameraPdm,
} from './predictiveMaintenanceApi';

const route = useRoute();
const router = useRouter();
const camera = ref(null);
const latest = ref(null);
const history = ref([]);
const savedHistory = ref([]);
const activeHistoryTab = ref('realtime');
const loading = ref(false);
const errorMessage = ref('');
let timer;

const cameraNo = computed(() => Number(route.params.cameraNo));
const probabilities = computed(() => [
  { label: '정상', value: latest.value?.normalProbability, className: 'normal' },
  { label: '주의', value: latest.value?.warningProbability, className: 'warning' },
  { label: '위험', value: latest.value?.criticalProbability, className: 'critical' },
]);
const abnormalProbability = computed(() => {
  const warning = latest.value?.warningProbability;
  const critical = latest.value?.criticalProbability;
  if (warning == null && critical == null) return null;
  return Number(warning || 0) + Number(critical || 0);
});
const visibleHistory = computed(() =>
  activeHistoryTab.value === 'realtime' ? history.value : savedHistory.value
);
const historyDescription = computed(() =>
  activeHistoryTab.value === 'realtime'
    ? '5초마다 분석한 최근 20건을 메모리에서 조회합니다.'
    : '문제 발생 시 또는 정상 상태를 1시간마다 DB에 저장한 기록입니다.'
);
const emptyHistoryMessage = computed(() =>
  activeHistoryTab.value === 'realtime'
    ? '실시간 분석 기록이 없습니다.'
    : '저장된 예지보전 이력이 없습니다.'
);

const loadSavedHistory = async () => {
  const response = await getSavedCameraPdm();
  savedHistory.value = (Array.isArray(response.data) ? response.data : [])
    .filter((item) => Number(item.cameraNo) === cameraNo.value);
};

const changeHistoryTab = async (tab) => {
  activeHistoryTab.value = tab;
  if (tab === 'saved') {
    try {
      await loadSavedHistory();
    } catch (error) {
      console.error('카메라 예지보전 저장 이력 조회 실패', error);
      errorMessage.value = '저장 이력을 불러오지 못했습니다.';
    }
  }
};

const refresh = async (manual = false) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    if (manual) await analyzeAllCameras();
    const [cameraResponse, latestResponse, historyResponse] = await Promise.all([
      getCameraList(), getLatestCameraPdm(), getRecentCameraPdm(cameraNo.value),
    ]);
    camera.value = (Array.isArray(cameraResponse.data) ? cameraResponse.data : [])
      .find((item) => Number(item.cameraNo) === cameraNo.value) || null;
    latest.value = (Array.isArray(latestResponse.data) ? latestResponse.data : [])
      .find((item) => Number(item.cameraNo) === cameraNo.value) || null;
    history.value = Array.isArray(historyResponse.data)
      ? historyResponse.data
      : [];
    if (manual || activeHistoryTab.value === 'saved') {
      await loadSavedHistory();
    }
  } catch (error) {
    console.error('카메라 예지보전 상세 조회 실패', error);
    errorMessage.value = '카메라 예지보전 정보를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

const riskClass = (level) => ({ 정상: 'normal', 주의: 'warning', 위험: 'critical' }[level] || 'unknown');
const percentValue = (value) => value == null ? 0 : Math.max(0, Math.min(100, Number(value) * 100));
const formatPercent = (value) => value == null ? '-' : `${percentValue(value).toFixed(1)}%`;
const formatDateTime = (value) => value ? new Date(value).toLocaleString('ko-KR') : '-';
const formatDate = (value) => value ? new Date(value).toLocaleDateString('ko-KR') : '-';
const goList = () => router.push('/admin/predictive-maintenance');

onMounted(async () => {
  await refresh();
  timer = window.setInterval(refresh, 5000);
});
onUnmounted(() => window.clearInterval(timer));
</script>

<style scoped>
.camera-pdm-detail {
  width: 100%;
  display: grid;
  gap: 12px;
  color: var(--admin-ink);
}

.detail-header,
.summary-grid,
.panel {
  border: 1px solid var(--admin-line);
  background: var(--admin-surface);
}

.detail-header {
  min-height: 74px;
  padding: 12px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-header span {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .12em;
  color: var(--admin-muted);
}

.detail-header h1 {
  margin: 4px 0 2px;
  font-size: 22px;
  color: var(--admin-ink);
}

.detail-header p,
.panel header p,
.summary-card span,
.summary-card small {
  margin: 0;
  color: var(--admin-muted);
}

.header-actions {
  display: flex;
  gap: 6px;
}

.header-actions button {
  min-height: 32px;
  padding: 5px 12px;
  border: 1px solid #5b88b2;
  color: #d8ecff;
  background: #334c63;
  font-weight: 700;
  cursor: pointer;
}

.header-actions button.secondary {
  border-color: var(--admin-line);
  color: var(--admin-ink);
  background: var(--admin-surface-muted);
}

.header-actions button:disabled {
  opacity: .55;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: var(--admin-surface-muted);
}

.summary-card {
  min-width: 0;
  min-height: 78px;
  padding: 10px 14px;
  display: flex;
  justify-content: center;
  flex-direction: column;
  gap: 5px;
  border-right: 1px solid var(--admin-line);
}

.summary-card:last-child {
  border-right: 0;
}

.summary-card strong {
  font-size: 17px;
  color: var(--admin-ink);
}

.summary-card strong.date-value {
  font-size: 13px;
}

.summary-card.risk strong {
  width: fit-content;
  min-width: 72px;
  padding: 3px 8px;
  border: 1px solid #737b82;
  text-align: center;
  font-size: 13px;
  color: #e5e7eb;
  background: #454c52;
}

.summary-card.risk.normal strong {
  border-color: #4f8c6b;
  color: #d9f7e6;
  background: #315641;
}

.summary-card.risk.warning strong {
  border-color: #d3a92e;
  color: #ffe9a6;
  background: #655525;
}

.summary-card.risk.critical strong {
  border-color: #c45a60;
  color: #ffdadd;
  background: #66383c;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 12px;
}

.panel {
  min-width: 0;
}

.panel header {
  min-height: 52px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--admin-line);
}

.panel h2 {
  margin: 0 0 3px;
  font-size: 16px;
  color: var(--admin-ink);
}

.probability-list {
  padding: 16px;
  display: grid;
  gap: 15px;
}

.probability-list > div > div:first-child {
  margin-bottom: 6px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.bar {
  height: 8px;
  overflow: hidden;
  background: var(--admin-surface-muted);
}

.bar i {
  height: 100%;
  display: block;
}

.bar i.normal { background: #4f8c6b; }
.bar i.warning { background: #d3a92e; }
.bar i.critical { background: #c45a60; }

.info-list {
  margin: 0;
  padding: 0 14px 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-list div {
  min-height: 42px;
  padding: 6px 10px;
  border-bottom: 1px solid var(--admin-line);
}

.info-list dt {
  font-size: 10px;
  color: var(--admin-muted);
}

.info-list dd {
  margin: 4px 0 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--admin-ink);
}

.history-panel {
  width: 100%;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.history-tabs {
  display: flex;
  gap: 4px;
}

.history-tabs button {
  min-height: 30px;
  padding: 4px 10px;
  border: 1px solid var(--admin-line);
  color: var(--admin-muted);
  background: var(--admin-surface-muted);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.history-tabs button.active {
  border-color: #5b88b2;
  color: #d8ecff;
  background: #334c63;
}

.table-wrap {
  max-height: 216px;
  overflow: auto;
}

table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  table-layout: fixed;
}

th,
td {
  box-sizing: border-box;
  height: 36px;
  padding: 3px 6px;
  border-bottom: 1px solid var(--admin-line);
  text-align: center;
  font-size: 11px;
  white-space: nowrap;
}

th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: var(--admin-muted);
  background: var(--admin-surface-muted);
}

.risk-badge {
  min-width: 54px;
  padding: 3px 7px;
  display: inline-block;
  border: 1px solid #737b82;
  color: #e5e7eb;
  background: #454c52;
  font-weight: 800;
}

.risk-badge.normal {
  border-color: #4f8c6b;
  color: #d9f7e6;
  background: #315641;
}

.risk-badge.warning {
  border-color: #d3a92e;
  color: #ffe9a6;
  background: #655525;
}

.risk-badge.critical {
  border-color: #c45a60;
  color: #ffdadd;
  background: #66383c;
}

.message,
.empty {
  margin: 0;
  padding: 18px;
  color: var(--admin-muted);
  background: var(--admin-surface);
}

.message.error {
  color: #ff8c91;
}

.empty {
  height: 60px;
  text-align: center;
}

@media (max-width: 900px) {
  .summary-grid,
  .detail-layout {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-card:nth-child(2) {
    border-right: 0;
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .detail-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .summary-grid,
  .info-list {
    grid-template-columns: 1fr;
  }

  .summary-card {
    border-right: 0;
    border-bottom: 1px solid var(--admin-line);
  }

  .history-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
