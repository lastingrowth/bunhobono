<template>
  <main
    class="exit-request-page resident-welcome"
    :class="{ 'request-mode': step !== 'menu' }"
  >
    <section class="exit-kiosk-card welcome-mode-card">
      <template v-if="step === 'menu'">
        <header class="kiosk-welcome">
          <div class="kiosk-brand"><i aria-hidden="true"></i>BONO SMART PARKING</div>
          <h1>반갑습니다, {{ welcomeName }}님</h1>
          <p>오늘도 쾌적한 주차 서비스를 이용하세요</p>
        </header>

        <div class="kiosk-menu-actions">
          <button type="button" class="kiosk-menu-button request" @click="startExitRequest">
            <span class="kiosk-button-icon" aria-hidden="true">
              <svg viewBox="0 0 64 64"><path d="M13 38h38l-4-13a7 7 0 0 0-7-5H24a7 7 0 0 0-7 5l-4 13Z"/><path d="M10 38v10h5m39-10v10h-5M19 47h26"/><circle cx="20" cy="38" r="3"/><circle cx="44" cy="38" r="3"/><path d="M32 7v8m-8-4 8-4 8 4"/></svg>
            </span>
            <span class="kiosk-button-copy">
              <strong>출차 신청</strong>
              <small>출차를 요청합니다</small>
            </span>
            <span class="menu-arrow" aria-hidden="true">›</span>
          </button>

          <button type="button" class="kiosk-menu-button status" @click="showExitStatus">
            <span class="kiosk-button-icon" aria-hidden="true">
              <svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="22"/><path d="M32 19v14l10 6"/><path d="M21 8h22"/></svg>
            </span>
            <span class="kiosk-button-copy">
              <strong>출차 현황</strong>
              <small>요청 상태를 확인합니다</small>
            </span>
            <span class="menu-arrow" aria-hidden="true">›</span>
          </button>
        </div>

        <div class="kiosk-footer-actions">
          <button type="button" class="kiosk-return-button vehicle-register-button" @click="goVehicleRegistration">
            <span class="footer-button-copy">
              <strong>방문차량 등록</strong>
            </span>
            <b aria-hidden="true">→</b>
          </button>
          <button type="button" class="kiosk-return-button" @click="returnDashboard">
            <svg aria-hidden="true" viewBox="0 0 24 24"><path d="M3 11.5 12 4l9 7.5M6 10v10h12V10"/></svg>
            홈으로 이동하기
          </button>
        </div>
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

        <section class="status-preparing">
          <div class="status-display-icon" aria-hidden="true">P</div>
          <h2>출차 신청 내역이 없습니다</h2>
          <p>출차 신청 API가 연결되면 차량 이동 단계가 이곳에 표시됩니다.</p>
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
              <span>{{ location.parkingName || '-' }} · {{ location.spaceCode || '위치 배정 중' }}</span>
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
            <dd>{{ selectedLocation.parkingName || '-' }}</dd>
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
            :disabled="!isB1Parking"
            @click="startExitRequest"
          >
            출차 신청
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
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getMyVehicleLocations } from '@/shared/api/residentDashboardApi';
import { useMemStore } from '@/features/member/memStore';

const router = useRouter();
const memberStore = useMemStore();
const welcomeName = computed(() => memberStore.member.memName || '입주민');

const step = ref('menu');
const loading = ref(false);
const errorMessage = ref('');
const locations = ref([]);
const selectedVehicleNo = ref(null);

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

const dateTimeText = (value) => {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
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

const startExitRequest = () => {
  router.push({ name: 'ResidentExitRequest', query: { mode: 'request' } });
};

const showExitStatus = () => {
  router.push({ name: 'ResidentExitRequest', query: { mode: 'status' } });
};

const selectVehicle = (location) => {
  selectedVehicleNo.value = location.vehicleCarNo;
  step.value = 'request';
};

const returnDashboard = () => router.push('/resident/dashboard');
const goVehicleRegistration = () => router.push('/resident/vehicles?mode=form');

onMounted(() => memberStore.loadMypage());

</script>

<style scoped>
.exit-request-page { min-height: calc(100vh - 150px); padding: 54px 20px 80px; display: grid; place-items: start center; background: radial-gradient(circle at 50% 0%, #fff 0, #f3f8fc 40%, #e9f2f8 100%); }
.exit-kiosk-card { width: min(880px, 100%); padding: 46px 52px 38px; border: 1px solid rgba(166, 192, 210, .6); border-radius: 30px; background: rgba(255, 255, 255, .94); box-shadow: 0 24px 70px rgba(41, 78, 104, .14); }
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
.kiosk-footer-actions { display:flex;justify-content:center;gap:10px;margin-top:28px; }
.kiosk-return-button { min-height: 52px; margin: 0; padding: 0 22px; display: flex; align-items: center; justify-content: center; gap: 8px; border: 1px solid #c9d8e3; border-radius: 14px; color: #637c8e; background: #fff; font-size: 15px; font-weight: 800; cursor: pointer; }
.welcome-mode-card .kiosk-footer-actions .vehicle-register-button { position:relative;color:#fff;border:0 !important;background:linear-gradient(145deg,#168bd0 0%,#0871b4 100%);box-shadow:none;transition:background .18s ease; }
.footer-button-copy{display:flex;align-items:center;justify-content:center;flex-direction:column}.footer-button-copy strong{font-size:21px;font-weight:600;line-height:1}.kiosk-return-button>b{font-size:21px}
.welcome-mode-card .kiosk-welcome{padding-bottom:34px}.welcome-mode-card .kiosk-welcome h1{margin-bottom:8px;font-size:30px}.welcome-mode-card .kiosk-menu-actions{width:min(560px,100%);margin-right:auto;margin-left:auto}.welcome-mode-card .kiosk-menu-button{min-height:170px}.welcome-mode-card .kiosk-footer-actions{width:min(560px,100%);display:grid;grid-template-columns:1fr;margin:38px auto 0}.welcome-mode-card .vehicle-register-button{min-height:72px;justify-content:center;padding:0 58px;text-align:center}.welcome-mode-card .vehicle-register-button>b{position:absolute;right:22px;margin:0}.welcome-mode-card .kiosk-return-button:not(.vehicle-register-button){width:max-content;margin:12px auto 0;border:0;background:transparent;font-size:18px;text-decoration:underline;text-underline-offset:4px}
.kiosk-return-button:hover { color: #267fb8; background: #eff7fb; }
.welcome-mode-card .kiosk-footer-actions .vehicle-register-button:hover { color:#fff;border:0 !important;background:linear-gradient(145deg,#087bc0 0%,#05639f 100%);box-shadow:none;transform:none; }
.welcome-mode-card .kiosk-footer-actions .vehicle-register-button:active { color:#fff;background:#05639f;box-shadow:none;transform:none; }
.kiosk-return-button svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.status-preparing { padding: 48px 10px 12px; text-align: center; }
.status-display-icon { width: 82px; height: 82px; margin: 0 auto 18px; display: grid; place-items: center; border: 5px solid #9bb4c5; border-radius: 20px; color: #66879d; font-size: 43px; font-weight: 900; }
.status-preparing h2 { margin: 0 0 10px; color: #294559; }
.status-preparing > p { margin: 0; color: #7890a2; }
.status-flow { margin: 34px 0; padding: 0; display: grid; grid-template-columns: repeat(4, 1fr); list-style: none; counter-reset: status-step; }
.status-flow li { position: relative; padding-top: 42px; color: #668096; font-size: 12px; font-weight: 800; counter-increment: status-step; }
.status-flow li::before { content: counter(status-step); position: absolute; top: 0; left: 50%; z-index: 1; width: 30px; height: 30px; display: grid; place-items: center; border: 3px solid #b8cbd8; border-radius: 50%; color: #668096; background: #fff; transform: translateX(-50%); }
.status-flow li:not(:last-child)::after { content: ''; position: absolute; top: 15px; left: calc(50% + 15px); width: calc(100% - 30px); height: 3px; background: #d7e2e9; }
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
.exit-header { display: flex; align-items: flex-start; gap: 18px; padding-bottom: 24px; border-bottom: 1px solid #e1ebf2; }
.exit-header span { color: #2d82c7; font-size: 11px; font-weight: 900; letter-spacing: .18em; }
.exit-header h1 { margin: 4px 0 6px; color: #20394d; font-size: 30px; }
.exit-header p { margin: 0; color: #7890a2; }
.back-button { width: 42px; height: 42px; border: 0; border-radius: 50%; color: #315f83; background: #e9f3fa; font-size: 20px; cursor: pointer; }
.vehicle-selector { margin-top: 22px; }
.vehicle-selector > span { display: block; margin-bottom: 9px; color: #5c7182; font-size: 12px; font-weight: 800; }
.vehicle-selector > div { display: flex; gap: 8px; flex-wrap: wrap; }
.vehicle-selector button { padding: 8px 13px; border: 1px solid #cbdde9; border-radius: 999px; color: #587184; background: #fff; cursor: pointer; }
.vehicle-selector button.selected { border-color: #2f83d5; color: #fff; background: #2f83d5; }
.vehicle-display { margin: 24px 0; padding: 28px; display: grid; justify-items: center; border-radius: 20px; color: #fff; background: linear-gradient(135deg, #236fae, #3e98dd); }
.vehicle-display small { font-weight: 700; opacity: .8; }
.vehicle-display strong { margin: 6px 0; font-size: clamp(30px, 7vw, 48px); letter-spacing: .05em; }
.vehicle-display span { padding: 5px 12px; border-radius: 999px; background: rgba(255,255,255,.18); font-size: 12px; font-weight: 800; }
.location-details { margin: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.location-details div { padding: 17px; border: 1px solid #e0eaf1; border-radius: 14px; background: #f8fbfd; }
.location-details dt { margin-bottom: 7px; color: #8093a2; font-size: 11px; font-weight: 800; }
.location-details dd { margin: 0; color: #2b465a; font-size: 14px; font-weight: 800; }
.location-details .space-code { color: #1676c3; font-size: 20px; }
.eligibility-message { margin: 18px 0 0; padding: 14px 16px; border-radius: 12px; color: #2e6e51; background: #ecf8f1; font-size: 13px; }
.eligibility-message.error { color: #a34343; background: #fff0f0; }
.request-actions { margin-top: 24px; display: grid; grid-template-columns: 1fr 2fr; gap: 12px; }
.request-actions button, .retry-button { min-height: 50px; border: 0; border-radius: 13px; font-weight: 900; cursor: pointer; }
.cancel-button { color: #526b7e; background: #edf2f6; }
.request-button { color: #fff; background: #2383cf; }
.request-button:disabled { cursor: not-allowed; opacity: .45; }
.page-state { padding: 70px 20px; color: #657d8f; text-align: center; }
.page-state p { margin: 10px 0 20px; }
.page-state.error { color: #a34343; }
.retry-button { min-height: 42px; padding: 0 18px; color: #fff; background: #2f83d5; }
/* 1280 x 720 화면에서 웰컴 메뉴가 한눈에 보이도록 높이를 줄인다. */
@media (pointer: fine) and (min-width: 821px) and (max-height: 760px) {
  .exit-request-page { width:100%;height:calc(100dvh - var(--header-height));min-height:0;padding:10px 20px;box-sizing:border-box;place-items:center;overflow:hidden; }
  .exit-kiosk-card { width:min(820px,100%);padding:16px 36px 14px;box-sizing:border-box;border-radius:24px; }
  .welcome-mode-card .kiosk-welcome { padding:0 16px 12px; }
  .kiosk-brand { font-size: 10px; }
  .welcome-mode-card .kiosk-welcome h1 { margin: 12px 0 5px; font-size: 27px; }
  .kiosk-welcome p { font-size: 14px; }
  .kiosk-menu-actions { gap: 14px; }
  .welcome-mode-card .kiosk-menu-button { min-height:128px;padding:16px 22px;gap:9px;border-radius:18px; }
  .kiosk-button-icon { width:46px;height:46px;border-radius:14px; }
  .kiosk-button-icon svg { width:28px;height:28px; }
  .kiosk-menu-button strong { font-size: 24px; }
  .kiosk-menu-button small { font-size: 12px; }
  .menu-arrow { right: 18px; bottom: 18px; width: 32px; height: 32px; font-size: 25px; }
  .welcome-mode-card .kiosk-footer-actions { margin-top:18px; }
  .welcome-mode-card .vehicle-register-button { min-height:50px; }
  .footer-button-copy strong { font-size: 19px; }
  .welcome-mode-card .kiosk-return-button:not(.vehicle-register-button) { min-height:36px;margin-top:4px;font-size:17px; }

  /* 125% 확대 상태의 출차 단계는 내용 높이만큼 늘어나고 푸터를 아래로 민다. */
  .exit-request-page.request-mode {
    height: auto;
    min-height: calc(100dvh - var(--header-height));
    padding: 10px 20px 32px;
    place-items: start center;
    overflow: visible;
  }
  .request-mode .exit-kiosk-card { padding:18px 30px 22px; }
  .request-mode .exit-header { gap:10px;padding-bottom:9px; }
  .request-mode .exit-header h1 { margin:0 0 2px;font-size:22px; }
  .request-mode .exit-header p { font-size:12px; }
  .request-mode .back-button { width:32px;height:32px;font-size:17px; }
  .request-mode .vehicle-display { margin:10px 0;padding:11px; }
  .request-mode .vehicle-display strong { margin:1px 0;font-size:31px; }
  .request-mode .location-details { gap:6px; }
  .request-mode .location-details div { padding:8px 11px; }
  .request-mode .eligibility-message { margin-top:8px;padding:7px 11px; }
  .request-mode .request-actions { margin-top:8px;gap:8px; }
  .request-mode .request-actions button { min-height:38px; }
}
.resident-welcome {
    min-height: calc(100vh - var(--header-height));
    display: grid;
    place-items: center;
    padding: 24px;
    background:
        linear-gradient(
            180deg,
            rgba(248, 252, 255, 0.44) 0%,
            rgba(250, 253, 255, 0.63) 45%,
            rgba(255, 255, 255, 0.81) 75%,
            rgba(255, 255, 255, 0.91) 100%
        ),
        url('@/assets/images/back.jpg')
            center center / cover fixed no-repeat;
}
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
  .exit-request-page { padding: 20px 12px 50px; }
  .exit-kiosk-card { padding: 28px 18px 22px; border-radius: 24px; }
  .kiosk-welcome { padding-bottom: 28px; }
  .kiosk-menu-actions { grid-template-columns: 1fr; gap: 14px; }
  .kiosk-menu-button { min-height: 190px; padding: 24px; gap: 16px; }
  .kiosk-button-icon { width: 58px; height: 58px; border-radius: 17px; }
  .kiosk-button-icon svg { width: 35px; height: 35px; }
  .kiosk-footer-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .kiosk-return-button { width: 100%; padding: 0 10px; font-size: 13px; }
  .vehicle-choice-card { padding: 18px 46px 18px 15px; grid-template-columns: 56px minmax(0, 1fr); gap: 13px; }
  .choice-car-icon { width: 54px; height: 54px; border-radius: 15px; }
  .choice-car-icon svg { width: 33px; height: 33px; }
  .choice-car-copy strong { font-size: 20px; }
  .location-details, .request-actions { grid-template-columns: 1fr; }
  .status-flow { grid-template-columns: repeat(2, 1fr); gap: 20px 0; }
  .status-flow li:nth-child(2)::after { display: none; }
  .welcome-mode-card .kiosk-welcome h1 { font-size: 24px; }
  .welcome-mode-card .kiosk-menu-actions { grid-template-columns: 1fr 1fr; gap: 10px; }
  .welcome-mode-card .kiosk-menu-button { min-height: 160px; padding: 18px 14px; }
  .welcome-mode-card .kiosk-button-icon { top: 18px; left: 14px; width: 48px; height: 48px; }
  .welcome-mode-card .kiosk-button-copy strong { font-size: 20px; }
  .welcome-mode-card .kiosk-button-copy small { font-size: 11px; }
  .welcome-mode-card .menu-arrow { right: 13px; bottom: 15px; }
  .welcome-mode-card .kiosk-footer-actions { grid-template-columns: 1fr; gap: 4px; }
  .welcome-mode-card .kiosk-return-button { width: 100%; }
  .welcome-mode-card .kiosk-return-button:not(.vehicle-register-button) { width: max-content; margin-top: 0; }
  .welcome-mode-card .vehicle-register-button { width: 100%; min-height: 52px; margin: 0 auto; padding: 0 40px; border-radius: 10px; }
  .welcome-mode-card .vehicle-register-button .footer-button-copy strong { font-size: 18px; }
  .welcome-mode-card .vehicle-register-button > b { right: 15px; font-size: 18px; }
}
</style>
