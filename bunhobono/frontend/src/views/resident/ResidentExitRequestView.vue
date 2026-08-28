<template>
  <main class="exit-request-page">
    <section class="exit-kiosk-card">
      <template v-if="step === 'menu'">
        <header class="kiosk-welcome">
          <div class="kiosk-brand"><i aria-hidden="true"></i>BONO SMART PARKING</div>
          <h1>차량 출차 서비스</h1>
          <p>이용하실 메뉴를 선택해 주세요</p>
        </header>

        <div class="kiosk-menu-actions">
          <button type="button" class="kiosk-menu-button request" @click="startExitRequest">
            <span class="kiosk-button-icon" aria-hidden="true">
              <svg viewBox="0 0 64 64"><path d="M13 38h38l-4-13a7 7 0 0 0-7-5H24a7 7 0 0 0-7 5l-4 13Z"/><path d="M10 38v10h5m39-10v10h-5M19 47h26"/><circle cx="20" cy="38" r="3"/><circle cx="44" cy="38" r="3"/><path d="M32 7v8m-8-4 8-4 8 4"/></svg>
            </span>
            <span class="kiosk-button-copy">
              <strong>출차 신청</strong>
              <small>주차 중인 차량을 불러옵니다</small>
            </span>
            <span class="menu-arrow" aria-hidden="true">›</span>
          </button>

          <button type="button" class="kiosk-menu-button status" @click="showExitStatus">
            <span class="kiosk-button-icon" aria-hidden="true">
              <svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="22"/><path d="M32 19v14l10 6"/><path d="M21 8h22"/></svg>
            </span>
            <span class="kiosk-button-copy">
              <strong>출차 현황</strong>
              <small>차량 이동 상태를 확인합니다</small>
            </span>
            <span class="menu-arrow" aria-hidden="true">›</span>
          </button>
        </div>

        <button type="button" class="kiosk-return-button" @click="returnDashboard">
          <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M3 11.5 12 4l9 7.5M6 10v10h12V10"/></svg>
          입주민 홈으로
        </button>
      </template>

      <template v-else-if="step === 'status'">
        <header class="exit-header">
          <button type="button" class="back-button" @click="step = 'menu'">←</button>
          <div>
            <span>ROBOT PARKING</span>
            <h1>출차 현황</h1>
            <p>신청한 차량의 이동 상태를 확인합니다.</p>
          </div>
        </header>

        <section v-if="billingStore.residentExitTask" class="status-preparing">
          <div class="status-display-icon" aria-hidden="true">P</div>
          <h2>{{ billingStore.residentExitTask.carNo || selectedLocation?.carNo || '선택 차량' }}</h2>
          <p :class="{ 'status-error': billingStore.residentExitTask.taskStatus === 'FAILED' }">
            {{ residentExitStatusText }}
          </p>
          <ol class="status-flow">
            <li :class="{ active: residentExitStep >= 1 }">신청 접수</li>
            <li :class="{ active: residentExitStep >= 2 }">로봇 배정</li>
            <li :class="{ active: residentExitStep >= 3 }">차량 이동</li>
            <li :class="{ active: residentExitStep >= 4 }">출차 준비 완료</li>
          </ol>
          <button type="button" class="status-home-button" @click="step = 'menu'">처음 화면</button>
        </section>

        <section v-else class="status-preparing">
          <div class="status-display-icon" aria-hidden="true">P</div>
          <h2>출차 신청 내역이 없습니다</h2>
          <p>출차를 신청하면 차량 이동 단계가 이곳에 표시됩니다.</p>
          <ol class="status-flow">
            <li>신청 접수</li>
            <li>로봇 배정</li>
            <li>차량 이동</li>
            <li>출차 준비 완료</li>
          </ol>
          <button type="button" class="status-home-button" @click="step = 'menu'">처음 화면</button>
        </section>
      </template>

      <template v-else-if="step === 'select'">
        <header class="exit-header">
          <button type="button" class="back-button" @click="step = 'menu'">←</button>
          <div>
            <span>ROBOT PARKING</span>
            <h1>차량 선택</h1>
            <p>출차할 차량을 선택해 주세요.</p>
          </div>
        </header>

        <div v-if="loading" class="page-state">주차 차량을 확인하고 있습니다.</div>

        <div v-else-if="errorMessage" class="page-state error">
          <p>{{ errorMessage }}</p>
          <button type="button" class="retry-button" @click="loadLocations">다시 불러오기</button>
        </div>

        <section v-else-if="locations.length" class="vehicle-choice-list">
          <button
            v-for="location in locations"
            :key="location.vehicleCarNo"
            type="button"
            class="vehicle-choice-card"
            @click="selectVehicle(location)"
          >
            <span class="choice-car-icon" aria-hidden="true">
              <svg viewBox="0 0 64 64"><path d="M13 38h38l-4-13a7 7 0 0 0-7-5H24a7 7 0 0 0-7 5l-4 13Z"/><path d="M10 38v10h5m39-10v10h-5M19 47h26"/><circle cx="20" cy="38" r="3"/><circle cx="44" cy="38" r="3"/></svg>
            </span>
            <span class="choice-car-copy">
              <small>차량번호</small>
              <strong>{{ location.carNo }}</strong>
              <span>{{ parkingNameText(location) }} · {{ location.spaceCode || '위치 배정 중' }}</span>
              <time>입차 {{ dateTimeText(location.inTime) }}</time>
            </span>
            <span class="choice-arrow" aria-hidden="true">›</span>
          </button>
        </section>

        <div v-else class="page-state">
          <strong>현재 주차 중인 차량이 없습니다.</strong>
          <p>차량이 주차된 후 출차를 신청할 수 있습니다.</p>
          <button type="button" class="retry-button" @click="step = 'menu'">처음 화면</button>
        </div>
      </template>

      <template v-else>
      <header class="exit-header">
        <button type="button" class="back-button" @click="step = 'select'">←</button>
        <div>
          <span>ROBOT PARKING</span>
          <h1>출차 신청</h1>
          <p>차량과 현재 주차 위치를 확인해 주세요.</p>
        </div>
      </header>

      <div v-if="loading" class="page-state">주차 차량을 확인하고 있습니다.</div>

      <div v-else-if="errorMessage" class="page-state error">
        <p>{{ errorMessage }}</p>
        <button type="button" class="retry-button" @click="loadLocations">다시 불러오기</button>
      </div>

      <template v-else-if="selectedLocation">
        <section class="vehicle-display">
          <small>차량번호</small>
          <strong>{{ selectedLocation.carNo }}</strong>
          <span>현재 주차 중</span>
        </section>

        <dl class="location-details">
          <div>
            <dt>현재 주차장</dt>
            <dd>{{ parkingNameText(selectedLocation) }}</dd>
          </div>
          <div>
            <dt>현재 주차위치</dt>
            <dd class="space-code">{{ selectedLocation.spaceCode || '위치 배정 중' }}</dd>
          </div>
          <div>
            <dt>입차시간</dt>
            <dd>{{ dateTimeText(selectedLocation.inTime) }}</dd>
          </div>
          <div>
            <dt>출차 대기위치</dt>
            <dd>신청 후 자동 배정</dd>
          </div>
        </dl>

        <p v-if="!isB1Parking" class="eligibility-message error">
          현재는 지하 1층 로봇 주차 차량만 출차 신청할 수 있습니다.
        </p>
        <p v-else class="eligibility-message">
          신청하면 로봇이 차량을 출차 대기 위치로 이동합니다.
        </p>

        <div class="request-actions">
          <button type="button" class="cancel-button" @click="step = 'select'">이전으로</button>
          <button
            type="button"
            class="request-button"
            :disabled="!isB1Parking || billingStore.loading"
            @click="requestExit"
          >
            {{ billingStore.loading ? '신청 중' : '출차 신청' }}
          </button>
        </div>
      </template>

      <div v-else class="page-state">
        <strong>현재 주차 중인 차량이 없습니다.</strong>
        <p>차량이 주차된 후 출차를 신청할 수 있습니다.</p>
        <button type="button" class="retry-button" @click="step = 'select'">돌아가기</button>
      </div>
      </template>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getMyVehicleLocations } from '@/shared/api/residentDashboardApi';
import { useBillingStore } from '@/features/billing/billingStore';

const route = useRoute();
const router = useRouter();
const billingStore = useBillingStore();

const step = ref('menu');
const loading = ref(false);
const errorMessage = ref('');
const locations = ref([]);
const selectedVehicleNo = ref(null);
let residentExitPollTimer = null;

const selectedLocation = computed(() =>
  locations.value.find(
    (location) => location.vehicleCarNo === selectedVehicleNo.value
  ) ?? locations.value[0] ?? null
);

const isB1Parking = computed(() => {
  const location = selectedLocation.value;
  if (!location) return false;

  return location.parkingCode === 'B1'
    || String(location.spaceCode || '').startsWith('B1-')
    || String(location.parkingName || '').includes('지하 1층');
});

// 로봇 출차 작업 단계를 화면의 네 단계로 구분한다.
const residentExitStep = computed(() => {
  const task = billingStore.residentExitTask;

  if (!task) return 0;
  if (task.taskStatus === 'COMPLETED') return 4;

  return {
    WAITING: 1,
    TRAFFIC_WAIT_EMPTY: 2,
    MOVING_TO_PICKUP: 2,
    PICKUP_POSITIONING: 2,
    LIFTING: 2,
    TRAFFIC_WAIT_LOADED: 3,
    MOVING_TO_DROPOFF: 3,
    DROPOFF_POSITIONING: 3,
    LOWERING: 3,
  }[task.taskPhase] ?? 1;
});

// 로봇 작업상태를 입주민 화면용 문구로 표시한다.
const residentExitStatusText = computed(() => {
  const task = billingStore.residentExitTask;

  if (!task) return '출차 신청 내역이 없습니다.';
  if (task.taskStatus === 'COMPLETED') return '차량이 출차 대기위치에 도착했습니다.';
  if (task.taskStatus === 'FAILED') return task.failureReason || '차량 출차 작업에 실패했습니다.';

  return {
    WAITING: '출차 신청이 접수되었습니다.',
    TRAFFIC_WAIT_EMPTY: '로봇 이동 경로를 확인하고 있습니다.',
    MOVING_TO_PICKUP: '로봇이 차량 위치로 이동하고 있습니다.',
    PICKUP_POSITIONING: '로봇이 차량을 들어 올릴 준비를 하고 있습니다.',
    LIFTING: '로봇이 차량을 들어 올리고 있습니다.',
    TRAFFIC_WAIT_LOADED: '출차 이동 경로를 확인하고 있습니다.',
    MOVING_TO_DROPOFF: '차량을 출차 대기위치로 이동하고 있습니다.',
    DROPOFF_POSITIONING: '차량을 출차 대기위치에 배치하고 있습니다.',
    LOWERING: '차량을 출차 대기위치에 내려놓고 있습니다.',
  }[task.taskPhase] || '출차 작업을 준비하고 있습니다.';
});

const dateTimeText = (value) => {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';

  const pad = (number) => String(number).padStart(2, '0');

  return `${date.getFullYear()}년 ${pad(date.getMonth() + 1)}월 ${pad(date.getDate())}일 ${pad(date.getHours())}시 ${pad(date.getMinutes())}분`;
};

// 주차장 내부 코드 대신 입주민에게 익숙한 층 이름을 표시한다.
const parkingNameText = (location) => {
  const parkingCode = String(location?.parkingCode || '').toUpperCase();
  const parkingName = String(location?.parkingName || '').trim();

  if (parkingCode === 'B1' || /^B1(?:\b|-)/i.test(parkingName)) return '지하 1층';
  if (parkingCode === 'B2' || /^B2(?:\b|-)/i.test(parkingName)) return '지하 2층';

  return parkingName || '-';
};

const loadLocations = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const response = await getMyVehicleLocations();
    locations.value = Array.isArray(response.data) ? response.data : [];

    selectedVehicleNo.value = null;
  } catch (error) {
    console.error('출차 차량 정보를 불러오지 못했습니다.', error);
    errorMessage.value = '출차 차량 정보를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

const startExitRequest = async () => {
  step.value = 'select';
  await loadLocations();
};

const selectVehicle = async (location) => {
  selectedVehicleNo.value = location.vehicleCarNo;
  errorMessage.value = '';
  await billingStore.selectCar(location, null, 'RESIDENT');
  step.value = 'request';
};

const returnDashboard = () => router.push('/resident/dashboard');

// 로봇 출차 작업의 반복 조회를 중지한다.
const stopResidentExitPolling = () => {
  if (residentExitPollTimer !== null) {
    window.clearInterval(residentExitPollTimer);
    residentExitPollTimer = null;
  }
};

// 출차 작업의 최신 상태를 조회하고 완료·실패 시 반복 조회를 끝낸다.
const loadResidentExitStatus = async () => {
  const result = await billingStore.loadResidentExitTask();

  if (!result.success) {
    errorMessage.value = result.message;
    stopResidentExitPolling();
    return;
  }

  if (result.task?.taskStatus === 'COMPLETED' || result.task?.taskStatus === 'FAILED') {
    stopResidentExitPolling();
  }
};

// 저장된 출차 작업이 있으면 현황 화면에서 최신 상태를 다시 조회한다.
const showExitStatus = async () => {
  step.value = 'status';

  if (!billingStore.residentExitTask) return;

  stopResidentExitPolling();
  await loadResidentExitStatus();

  const taskStatus = billingStore.residentExitTask?.taskStatus;
  if (taskStatus !== 'COMPLETED' && taskStatus !== 'FAILED') {
    residentExitPollTimer = window.setInterval(loadResidentExitStatus, 5000);
  }
};

// 선택한 B1 차량의 출차 게이트를 조회하고 로봇 출차 작업을 요청한다.
const requestExit = async () => {
  if (!selectedLocation.value?.carLogNo || !isB1Parking.value) return;

  stopResidentExitPolling();
  errorMessage.value = '';

  const result = await billingStore.requestResidentExit();

  if (!result.success) {
    errorMessage.value = result.message;
    return;
  }

  step.value = 'status';
  await loadResidentExitStatus();

  const taskStatus = billingStore.residentExitTask?.taskStatus;
  if (taskStatus !== 'COMPLETED' && taskStatus !== 'FAILED') {
    residentExitPollTimer = window.setInterval(loadResidentExitStatus, 5000);
  }
};

onMounted(async () => {
  if (route.query.mode === 'request') {
    await startExitRequest();
  } else if (route.query.mode === 'status') {
    await showExitStatus();
  }
});

onUnmounted(stopResidentExitPolling);

</script>

<style scoped>
.exit-request-page { width: 100%; min-height: 0; padding: clamp(24px, 5vw, 54px) 20px clamp(48px, 7vw, 80px); display: flex; align-items: flex-start; justify-content: center; overflow: visible; background: radial-gradient(circle at 50% 0%, #fff 0, #f3f8fc 40%, #e9f2f8 100%); }
.exit-kiosk-card { width: min(880px, 100%); min-width: 0; padding: 30px 42px 32px; border: 1px solid rgba(166, 192, 210, .6); border-radius: 30px; background: rgba(255, 255, 255, .94); box-shadow: 0 24px 70px rgba(41, 78, 104, .14); }
.kiosk-welcome { padding: 10px 20px 42px; text-align: center; }
.kiosk-brand { display: inline-flex; align-items: center; gap: 8px; color: #2580bd; font-size: 11px; font-weight: 900; letter-spacing: .16em; }
.kiosk-brand i { width: 9px; height: 9px; border-radius: 50%; background: #30a5e8; box-shadow: 0 0 0 5px #e3f4fd; }
.kiosk-welcome h1 { margin: 16px 0 9px; color: #18364b; font-size: clamp(32px, 6vw, 45px); letter-spacing: -.04em; }
.kiosk-welcome p { margin: 0; color: #8094a3; font-size: 16px; }
.kiosk-menu-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.kiosk-menu-actions button { width: 100%; border-radius: 22px; cursor: pointer; transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease; }
.kiosk-menu-actions button:hover { transform: translateY(-3px); }
.kiosk-menu-button { position: relative; min-height: 245px; padding: 30px; display: flex; flex-direction: column; align-items: flex-start; justify-content: center; gap: 22px; overflow: hidden; text-align: left; }
.kiosk-menu-button::after { content: ''; position: absolute; right: -45px; bottom: -55px; width: 175px; height: 175px; border: 28px solid currentColor; border-radius: 50%; opacity: .055; }
.kiosk-menu-button.request { border: 1px solid #238ed0; color: #fff; background: linear-gradient(145deg, #168bd0 0%, #0871b4 100%); box-shadow: 0 15px 32px rgba(14, 119, 183, .22); }
.kiosk-menu-button.status { border: 1px solid #d7e5ee; color: #31566f; background: linear-gradient(145deg, #f8fcff, #edf5fa); box-shadow: 0 13px 30px rgba(61, 96, 120, .1); }
.kiosk-menu-button.status:hover { border-color: #a9c8da; box-shadow: 0 16px 34px rgba(61, 96, 120, .16); }
.kiosk-button-icon { width: 70px; height: 70px; display: grid; place-items: center; border-radius: 21px; }
.request .kiosk-button-icon { background: rgba(255,255,255,.16); box-shadow: inset 0 0 0 1px rgba(255,255,255,.2); }
.status .kiosk-button-icon { color: #247dab; background: #dceff8; }
.kiosk-button-icon svg { width: 43px; height: 43px; fill: none; stroke: currentColor; stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }
.kiosk-button-copy { display: grid; gap: 7px; }
.kiosk-menu-button strong { font-size: 29px; letter-spacing: -.03em; }
.kiosk-menu-button small { font-size: 14px; opacity: .78; }
.menu-arrow { position: absolute; right: 25px; bottom: 25px; width: 37px; height: 37px; display: grid; place-items: center; border-radius: 50%; background: rgba(255,255,255,.15); font-size: 30px; line-height: 1; }
.status .menu-arrow { background: #fff; box-shadow: 0 5px 15px rgba(58, 92, 115, .1); }
.kiosk-return-button { min-height: 52px; margin: 28px auto 0; padding: 0 22px; display: flex; align-items: center; justify-content: center; gap: 8px; border: 0; border-radius: 14px; color: #637c8e; background: transparent; font-size: 15px; font-weight: 800; cursor: pointer; }
.kiosk-return-button:hover { color: #267fb8; background: #eff7fb; }
.kiosk-return-button svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.status-preparing { padding: 48px 10px 12px; text-align: center; }
.status-display-icon { width: 82px; height: 82px; margin: 0 auto 18px; display: grid; place-items: center; border: 5px solid #9bb4c5; border-radius: 20px; color: #66879d; font-size: 43px; font-weight: 900; }
.status-preparing h2 { margin: 0 0 10px; color: #294559; }
.status-preparing > p { margin: 0; color: #7890a2; }
.status-preparing > p.status-error { color: #a34343; }
.status-flow { margin: 34px 0; padding: 0; display: grid; grid-template-columns: repeat(4, 1fr); list-style: none; counter-reset: status-step; }
.status-flow li { position: relative; padding-top: 42px; color: #668096; font-size: 12px; font-weight: 800; counter-increment: status-step; }
.status-flow li::before { content: counter(status-step); position: absolute; top: 0; left: 50%; z-index: 1; width: 30px; height: 30px; display: grid; place-items: center; border: 3px solid #b8cbd8; border-radius: 50%; color: #668096; background: #fff; transform: translateX(-50%); }
.status-flow li:not(:last-child)::after { content: ''; position: absolute; top: 15px; left: calc(50% + 15px); width: calc(100% - 30px); height: 3px; background: #d7e2e9; }
.status-flow li.active { color: #2383cf; }
.status-flow li.active::before { border-color: #2383cf; color: #fff; background: #2383cf; }
.status-home-button { min-width: 180px; min-height: 50px; border: 0; border-radius: 13px; color: #fff; background: #315f83; font-weight: 900; cursor: pointer; }
.vehicle-choice-list { margin-top: 26px; display: grid; gap: 14px; }
.vehicle-choice-card { position: relative; width: 100%; min-height: 142px; padding: 22px 64px 22px 22px; display: grid; grid-template-columns: 74px minmax(0, 1fr); gap: 20px; align-items: center; border: 1px solid #d9e6ee; border-radius: 20px; color: #28485e; background: #fff; box-shadow: 0 9px 24px rgba(53, 88, 111, .08); text-align: left; cursor: pointer; transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease; }
.vehicle-choice-card:hover { border-color: #53a3d4; box-shadow: 0 14px 30px rgba(35, 126, 184, .15); transform: translateY(-2px); }
.choice-car-icon { width: 70px; height: 70px; display: grid; place-items: center; border-radius: 19px; color: #1885c7; background: #e5f4fc; }
.choice-car-icon svg { width: 43px; height: 43px; fill: none; stroke: currentColor; stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }
.choice-car-copy { min-width: 0; display: grid; gap: 4px; }
.choice-car-copy small { color: #8297a6; font-size: 11px; font-weight: 800; }
.choice-car-copy strong { color: #163e58; font-size: 25px; letter-spacing: .02em; }
.choice-car-copy span { overflow: hidden; color: #4d7188; font-size: 13px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.choice-car-copy time { color: #8a9ca8; font-size: 12px; }
.choice-arrow { position: absolute; right: 24px; top: 50%; color: #58a0ce; font-size: 38px; line-height: 1; transform: translateY(-50%); }
.exit-header { display: flex; align-items: flex-start; gap: 14px; padding-bottom: 14px; border-bottom: 1px solid #e1ebf2; }
.exit-header span { color: var(--resident-accent); font-size: 11px; font-weight: 900; letter-spacing: .18em; }
.exit-header h1 { margin: 2px 0 3px; color: #20394d; font-size: 27px; }
.exit-header p { margin: 0; color: #7890a2; font-size: 14px; }
.back-button { width: 38px; height: 38px; border: 0; border-radius: 50%; color: #315f83; background: #e9f3fa; font-size: 20px; cursor: pointer; }
.vehicle-selector { margin-top: 22px; }
.vehicle-selector > span { display: block; margin-bottom: 9px; color: #5c7182; font-size: 12px; font-weight: 800; }
.vehicle-selector > div { display: flex; gap: 8px; flex-wrap: wrap; }
.vehicle-selector button { padding: 8px 13px; border: 1px solid #cbdde9; border-radius: 999px; color: #587184; background: #fff; cursor: pointer; }
.vehicle-selector button.selected { border-color: #2f83d5; color: #fff; background: #2f83d5; }
.vehicle-display { margin: 16px 0; padding: 18px; display: grid; justify-items: center; border-radius: 20px; color: #fff; background: linear-gradient(135deg, #236fae, #3e98dd); }
.vehicle-display small { font-weight: 700; opacity: .8; }
.vehicle-display strong { margin: 3px 0; font-size: clamp(30px, 5vw, 40px); letter-spacing: .05em; }
.vehicle-display span { padding: 5px 12px; border-radius: 999px; background: rgba(255,255,255,.18); font-size: 12px; font-weight: 800; }
.location-details { margin: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.location-details div { padding: 12px 14px; border: 1px solid #e0eaf1; border-radius: 14px; background: #f8fbfd; }
.location-details dt { margin-bottom: 4px; color: #8093a2; font-size: 11px; font-weight: 800; }
.location-details dd { margin: 0; color: #2b465a; font-size: 14px; font-weight: 800; }
.location-details .space-code { color: var(--resident-accent); font-size: 20px; }
.eligibility-message { margin: 12px 0 0; padding: 10px 14px; border-radius: 12px; color: #2e6e51; background: #ecf8f1; font-size: 13px; }
.eligibility-message.error { color: #a34343; background: #fff0f0; }
.request-actions { margin-top: 14px; display: grid; grid-template-columns: 1fr 2fr; gap: 12px; }
.request-actions button, .retry-button { min-height: 46px; border: 0; border-radius: 13px; font-weight: 900; cursor: pointer; }
.cancel-button { color: #526b7e; background: #edf2f6; }
.request-button { color: #fff; background: #2383cf; }
.request-button:disabled { cursor: not-allowed; opacity: .45; }
.page-state { padding: 70px 20px; color: #657d8f; text-align: center; }
.page-state p { margin: 10px 0 20px; }
.page-state.error { color: #a34343; }
.retry-button { min-height: 42px; padding: 0 18px; color: #fff; background: #2f83d5; }
@media (pointer: fine) and (min-width: 821px) and (max-height: 950px) { .exit-request-page { padding-top: 18px; padding-bottom: 36px; } }
/* 브라우저 125% 확대처럼 CSS 뷰포트 높이가 줄어든 데스크톱 화면 */
@media (pointer: fine) and (min-width: 821px) and (max-height: 760px) {
  .exit-request-page { padding: 10px 16px 24px; }
  .exit-kiosk-card { padding: 18px 30px 22px; border-radius: 22px; }
  .exit-header { gap: 10px; padding-bottom: 9px; }
  .exit-header h1 { margin: 0 0 2px; font-size: 22px; }
  .exit-header p { font-size: 12px; }
  .exit-header span { font-size: 9px; }
  .back-button { width: 32px; height: 32px; font-size: 17px; }
  .vehicle-display { margin: 10px 0; padding: 11px; border-radius: 14px; }
  .vehicle-display strong { margin: 1px 0; font-size: 31px; }
  .vehicle-display span { padding: 3px 9px; font-size: 10px; }
  .location-details { gap: 6px; }
  .location-details div { padding: 8px 11px; border-radius: 10px; }
  .location-details dt { margin-bottom: 2px; font-size: 9px; }
  .location-details dd { font-size: 12px; }
  .location-details .space-code { font-size: 16px; }
  .eligibility-message { margin-top: 8px; padding: 7px 11px; font-size: 11px; }
  .request-actions { margin-top: 8px; gap: 8px; }
  .request-actions button { min-height: 38px; border-radius: 10px; font-size: 12px; }
}
@media (max-width: 900px) { .exit-kiosk-card { padding-right: clamp(22px, 5vw, 42px); padding-left: clamp(22px, 5vw, 42px); } }
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) { .exit-request-page { padding: 12px 0 36px; } .exit-kiosk-card { padding: 28px 18px 22px; border-radius: 24px; } .kiosk-welcome { padding-bottom: 28px; } .kiosk-menu-actions { grid-template-columns: 1fr; gap: 14px; } .kiosk-menu-button { min-height: 190px; padding: 24px; gap: 16px; } .kiosk-button-icon { width: 58px; height: 58px; border-radius: 17px; } .kiosk-button-icon svg { width: 35px; height: 35px; } .exit-header { flex-wrap: wrap; } .vehicle-choice-card { padding: 18px 46px 18px 15px; grid-template-columns: 56px minmax(0, 1fr); gap: 13px; } .choice-car-icon { width: 54px; height: 54px; border-radius: 15px; } .choice-car-icon svg { width: 33px; height: 33px; } .choice-car-copy strong { font-size: 20px; } .location-details { grid-template-columns: 1fr; } .request-actions { grid-template-columns: 1fr; } .status-flow { grid-template-columns: repeat(2, 1fr); gap: 20px 0; } .status-flow li:nth-child(2)::after { display: none; } }
</style>
