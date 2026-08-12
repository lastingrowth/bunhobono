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

    <section class="summary-grid">
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

    <section class="monitor-grid">
      <article class="camera-panel">
        <header class="panel-header">
          <div><h2>{{ equipmentLabel }} 상태</h2><p>마지막 수신 {{ lastUpdatedText }}</p></div>
          <button type="button" :disabled="loading" @click="refreshCurrent(true)">{{ loading ? '갱신 중' : '지금 갱신' }}</button>
        </header>

        <div v-if="errorMessage" class="monitor-error">{{ errorMessage }}</div>
        <div v-else-if="!currentItems.length && !loading" class="monitor-empty">등록된 {{ equipmentLabel }}가 없습니다.</div>
        <div v-else-if="selectedEquipment === 'CAMERA'" class="camera-grid">
          <article v-for="camera in cameras" :key="camera.cameraNo" class="camera-card" :class="statusClass(camera.cameraStatus)">
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
        <div v-else class="camera-grid">
          <article v-for="gate in gates" :key="gate.gateNo" class="camera-card" :class="statusClass(gate.operatingStatus)">
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
      </article>

      <aside class="event-panel">
        <header class="panel-header"><div><h2>실시간 이벤트</h2><p>최근 상태 수신 기록</p></div><span class="event-count">{{ events.length }}</span></header>
        <ol class="event-list">
          <li v-for="event in events" :key="event.id" :class="event.type">
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
import { getCameraList } from '@/features/camera/cameraApi';
import { getList as getGateList } from '@/features/gates/gateApi';

const cameras = ref([]);
const gates = ref([]);
const selectedEquipment = ref('CAMERA');
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
  { value: 'CAMERA', label: '카메라' },
  { value: 'GATE', label: '게이트' },
  { value: 'ROBOT', label: '주차로봇' },
];

const selectEquipment = (equipment) => {
  if (equipment === 'ROBOT') {
    window.alert('주차로봇 예지보전 화면은 준비 중입니다.');
    return;
  }

  if (selectedEquipment.value === equipment) return;

  selectedEquipment.value = equipment;
  events.value = [];
  lastUpdatedAt.value = null;
  refreshCurrent();
};

const normalizeStatus = (status) => ['NORMAL', 'FAULT', 'MAINTENANCE'].includes(status) ? status : 'UNKNOWN';
const statusText = (status) => ({ NORMAL: '정상', FAULT: '고장', MAINTENANCE: '점검 중', UNKNOWN: '상태 미확인' }[normalizeStatus(status)]);
const statusClass = (status) => normalizeStatus(status).toLowerCase();
const currentItems = computed(() => selectedEquipment.value === 'CAMERA' ? cameras.value : gates.value);
const equipmentLabel = computed(() => selectedEquipment.value === 'CAMERA' ? '카메라' : '게이트');
const equipmentDescription = computed(() => `${equipmentLabel.value} 장비의 작동 상태와 실시간 수신 기록을 확인합니다.`);
const itemStatus = (item) => selectedEquipment.value === 'CAMERA' ? item.cameraStatus : item.operatingStatus;
const statusCounts = computed(() => currentItems.value.reduce((counts, item) => {
  const status = normalizeStatus(itemStatus(item));
  if (status in counts) counts[status] += 1;
  return counts;
}, { NORMAL: 0, FAULT: 0, MAINTENANCE: 0 }));
const normalRate = computed(() => currentItems.value.length ? Math.round(statusCounts.value.NORMAL / currentItems.value.length * 100) : 0);
const clockText = computed(() => currentTime.value.toLocaleTimeString('ko-KR', { hour12: false }));
const lastUpdatedText = computed(() => lastUpdatedAt.value ? lastUpdatedAt.value.toLocaleTimeString('ko-KR', { hour12: false }) : '-');

const pushEvent = (type, title, message) => {
  events.value.unshift({ id: ++eventSequence, type, title, message, time: new Date().toLocaleTimeString('ko-KR', { hour12: false }) });
  events.value = events.value.slice(0, 12);
};

const refreshCameras = async (manual = false) => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';
  const startedAt = performance.now();

  try {
    const response = await getCameraList();
    const nextCameras = Array.isArray(response.data) ? response.data : [];
    const previousByNo = new Map(cameras.value.map((camera) => [camera.cameraNo, normalizeStatus(camera.cameraStatus)]));

    nextCameras.forEach((camera) => {
      const previous = previousByNo.get(camera.cameraNo);
      const current = normalizeStatus(camera.cameraStatus);
      if (previous && previous !== current) {
        pushEvent(current === 'FAULT' ? 'fault' : 'change', `${camera.cameraName} 상태 변경`, `${statusText(previous)} → ${statusText(current)}`);
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
    const response = await getGateList();
    const nextGates = Array.isArray(response.data) ? response.data : [];
    const previousByNo = new Map(gates.value.map((gate) => [gate.gateNo, {
      operatingStatus: normalizeStatus(gate.operatingStatus),
      gateStatus: gate.gateStatus,
    }]));

    nextGates.forEach((gate) => {
      const previous = previousByNo.get(gate.gateNo);
      const current = normalizeStatus(gate.operatingStatus);
      if (previous && previous.operatingStatus !== current) {
        pushEvent(current === 'FAULT' ? 'fault' : 'change', `${gate.gateName} 장비 상태 변경`, `${statusText(previous.operatingStatus)} → ${statusText(current)}`);
      }
      if (previous && previous.gateStatus !== gate.gateStatus) {
        pushEvent('change', `${gate.gateName} 개폐 상태 변경`, `${previous.gateStatus === 1 ? '열림' : '닫힘'} → ${gate.gateStatus === 1 ? '열림' : '닫힘'}`);
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

const refreshCurrent = (manual = false) => selectedEquipment.value === 'CAMERA'
  ? refreshCameras(manual)
  : refreshGates(manual);

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

@media (max-width: 760px) {
  .pdm-page { padding: 12px; }
  .pdm-header { min-height: 0; }
  .header-monitor-tools { width: 100%; justify-items: stretch; }
  .equipment-tabs { width: 100%; }
  .equipment-tabs button { flex: 1; min-width: 0; }
  .live-clock { justify-items: start; }
}
</style>
