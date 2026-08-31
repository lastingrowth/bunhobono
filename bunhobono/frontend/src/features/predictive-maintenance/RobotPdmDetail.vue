<template>
  <main class="robot-pdm-detail">
    <header class="detail-header">
      <div>
        <span>ROBOT PREDICTIVE MAINTENANCE</span>
        <h1>{{ robot?.robotCode || `주차로봇 #${robotNo}` }}</h1>
        <p>SET {{ robot?.setNo ?? '-' }} · {{ robot?.setPosition || '위치 미확인' }}</p>
      </div>
      <div class="header-actions">
        <button type="button" :disabled="loading" @click="refresh(true)">
          {{ loading ? '분석 중' : '지금 분석' }}
        </button>
        <button type="button" class="secondary" @click="goList">목록으로</button>
      </div>
    </header>

    <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
    <p v-else-if="loading && !latest" class="message">로봇 예지보전 정보를 불러오는 중입니다.</p>

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
          <header><h2>로봇 기본정보</h2><p>설비 등록 정보입니다.</p></header>
          <dl class="info-list">
            <div><dt>로봇 번호</dt><dd>#{{ robotNo }}</dd></div>
            <div><dt>로봇 코드</dt><dd>{{ robot?.robotCode || '-' }}</dd></div>
            <div><dt>소속 세트</dt><dd>SET {{ robot?.setNo ?? '-' }}</dd></div>
            <div><dt>세트 위치</dt><dd>{{ robot?.setPosition || '-' }}</dd></div>
            <div><dt>배터리</dt><dd>{{ formatBattery(robot?.batteryLevel) }}</dd></div>
            <div><dt>운전시간</dt><dd>{{ formatOperatingHours(robot?.operatingHours) }}</dd></div>
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
            <button type="button" :class="{ active: activeHistoryTab === 'realtime' }" @click="changeHistoryTab('realtime')">실시간 분석 기록</button>
            <button type="button" :class="{ active: activeHistoryTab === 'saved' }" @click="changeHistoryTab('saved')">저장 이력</button>
          </div>
        </header>
        <div class="table-wrap">
          <table>
            <thead><tr><th>분석시각</th><th>등급</th><th>정상</th><th>주의</th><th>위험</th><th v-if="activeHistoryTab === 'saved'">조치 상태</th><th v-if="activeHistoryTab === 'saved'">관리</th></tr></thead>
            <tbody>
              <tr v-for="record in visibleHistory" :key="record.pdmNo || `${record.robotNo}-${record.predictedAt}`">
                <td>{{ formatDateTime(record.predictedAt) }}</td>
                <td><span class="risk-badge" :class="riskClass(record.riskLevel)">{{ record.riskLevel }}</span></td>
                <td>{{ formatPercent(record.normalProbability) }}</td>
                <td>{{ formatPercent(record.warningProbability) }}</td>
                <td>{{ formatPercent(record.criticalProbability) }}</td>
                <td v-if="activeHistoryTab === 'saved'"><span class="action-status" :class="record.actionStatus?.toLowerCase()">{{ actionStatusLabel(record.actionStatus) }}</span></td>
                <td v-if="activeHistoryTab === 'saved'" class="action-cell">
                  <button v-if="record.actionStatus === 'ACTION_REQUIRED'" type="button" class="action-button" @click="openActionDialog(record)">조치 등록</button>
                  <div v-else-if="record.actionStatus === 'COMPLETED'" class="completed-action"><button type="button" class="action-view-button" @click="openActionDialog(record)">조치내용 보기</button></div>
                  <span v-else>-</span>
                </td>
              </tr>
              <tr v-if="visibleHistory.length === 0"><td :colspan="activeHistoryTab === 'saved' ? 7 : 5" class="empty">{{ emptyHistoryMessage }}</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
    <PdmActionDialog v-if="selectedAction" :record="selectedAction" equipment-type="ROBOT" :equipment-name="robot?.robotCode || `주차로봇 #${robotNo}`" :loading="actionSubmitting" :read-only="selectedAction.actionStatus === 'COMPLETED'" @cancel="closeActionDialog" @submit="completeSelectedAction" />
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getRobotList } from '@/features/robot/robotApi';
import PdmActionDialog from './PdmActionDialog.vue';
import {
  analyzeAllRobots,
  completeRobotPdmAction,
  getLatestRobotPdm,
  getRecentRobotPdm,
  getSavedRobotPdm,
} from './predictiveMaintenanceApi';

const route = useRoute();
const router = useRouter();
const robot = ref(null);
const latest = ref(null);
const history = ref([]);
const savedHistory = ref([]);
const activeHistoryTab = ref('realtime');
const loading = ref(false);
const errorMessage = ref('');
const selectedAction = ref(null);
const actionSubmitting = ref(false);
let timer;

const robotNo = computed(() => Number(route.params.robotNo));
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
const visibleHistory = computed(() => activeHistoryTab.value === 'realtime' ? history.value : savedHistory.value);
const historyDescription = computed(() => activeHistoryTab.value === 'realtime'
  ? '5초마다 실제 모델로 분석한 최근 20건을 메모리에서 조회합니다.'
  : '문제 발생 시 또는 정상 상태를 매시 정각에 DB에 저장한 기록입니다.');
const emptyHistoryMessage = computed(() => activeHistoryTab.value === 'realtime'
  ? '실시간 분석 기록이 없습니다.'
  : '저장된 예지보전 이력이 없습니다.');

const loadSavedHistory = async () => {
  const response = await getSavedRobotPdm();
  savedHistory.value = (Array.isArray(response.data) ? response.data : [])
    .filter((item) => Number(item.robotNo) === robotNo.value);
};

const changeHistoryTab = async (tab) => {
  activeHistoryTab.value = tab;
  if (tab === 'saved') {
    try {
      await loadSavedHistory();
    } catch (error) {
      console.error('로봇 예지보전 저장 이력 조회 실패', error);
      errorMessage.value = '저장 이력을 불러오지 못했습니다.';
    }
  }
};

const actionStatusLabel = (status) => ({ NOT_REQUIRED: '조치 불필요', ACTION_REQUIRED: '조치 필요', COMPLETED: '조치 완료' }[status] || '-');
const openActionDialog = (record) => { if (record?.pdmNo && ['ACTION_REQUIRED', 'COMPLETED'].includes(record.actionStatus)) selectedAction.value = record; };
const closeActionDialog = () => { if (!actionSubmitting.value) selectedAction.value = null; };
const completeSelectedAction = async (actionNote) => {
  if (!selectedAction.value?.pdmNo || actionSubmitting.value) return;
  actionSubmitting.value = true;
  errorMessage.value = '';
  try {
    await completeRobotPdmAction(selectedAction.value.pdmNo, actionNote);
    selectedAction.value = null;
    await loadSavedHistory();
    await refresh();
  } catch (error) {
    console.error('로봇 예지보전 조치 완료 실패', error);
    errorMessage.value = '조치 완료 처리에 실패했습니다.';
  } finally {
    actionSubmitting.value = false;
  }
};

const refresh = async (manual = false) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    if (manual) await analyzeAllRobots();
    const [robotResponse, latestResponse, historyResponse] = await Promise.all([
      getRobotList(), getLatestRobotPdm(), getRecentRobotPdm(robotNo.value),
    ]);
    robot.value = (Array.isArray(robotResponse.data) ? robotResponse.data : [])
      .find((item) => Number(item.robotNo) === robotNo.value) || null;
    latest.value = (Array.isArray(latestResponse.data) ? latestResponse.data : [])
      .find((item) => Number(item.robotNo) === robotNo.value) || null;
    history.value = Array.isArray(historyResponse.data) ? historyResponse.data : [];
    if (manual || activeHistoryTab.value === 'saved') await loadSavedHistory();
  } catch (error) {
    console.error('로봇 예지보전 상세 조회 실패', error);
    errorMessage.value = '로봇 예지보전 정보를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

const riskClass = (level) => ({ 정상: 'normal', 주의: 'warning', 위험: 'critical' }[level] || 'unknown');
const percentValue = (value) => value == null ? 0 : Math.max(0, Math.min(100, Number(value) * 100));
const formatPercent = (value) => value == null ? '-' : `${percentValue(value).toFixed(1)}%`;
const formatDateTime = (value) => value ? new Date(value).toLocaleString('ko-KR') : '-';
const formatBattery = (value) => value == null ? '-' : `${Number(value).toFixed(1)}%`;
const formatOperatingHours = (value) => value == null ? '-' : `${Number(value).toFixed(1)}시간`;
const goList = () => router.push({ path: '/admin/predictive-maintenance', query: { equipment: 'ROBOT' } });

onMounted(async () => {
  await refresh();
  timer = window.setInterval(refresh, 5000);
});
onUnmounted(() => window.clearInterval(timer));
</script>

<style scoped src="./pdmDetail.css"></style>
