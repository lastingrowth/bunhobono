<template>
  <main class="parking-map-page">
    <ManagementFeedbackToast :message="feedbackMessage" :type="feedbackType" />

    <header class="page-heading facility-list-heading">
      <div>
        <h1 class="management-list-title">B1 로봇 주차장 배치도</h1>
        <p>대기면에 차량을 맡기면 주차 로봇이 100개 주차면까지 자동으로 이송합니다.</p>
      </div>
      <div class="map-actions">
        <span class="live"><i></i> 작업 상태 빠른 갱신</span>
        <button type="button" :disabled="loading" @click="refreshMap">{{ loading ? '갱신 중' : '지금 갱신' }}</button>
      </div>
    </header>

    <section class="map-summary">
      <div><span>로봇 주차면</span><strong>{{ parkingSpaces.length }}</strong></div>
      <div class="available"><span>빈자리</span><strong>{{ availableCount }}</strong></div>
      <div class="reserved"><span>예약</span><strong>{{ reservedCount }}</strong></div>
      <div class="occupied"><span>주차 중</span><strong>{{ occupiedCount }}</strong></div>
      <div><span>사용률</span><strong>{{ usageRate }}%</strong></div>
    </section>

    <div v-if="errorMessage" class="map-message error">{{ errorMessage }}</div>
    <div v-else-if="loading && !spaces.length" class="map-message">배치도를 불러오는 중입니다.</div>

    <section v-else class="garage-shell">
      <div class="garage-title"><span>BONO APARTMENT</span><strong>BASEMENT 1 · ROBOT PARKING</strong><small>마지막 수신 {{ lastUpdatedText }}</small></div>

      <div ref="simulationStage" class="access-layout">
        <div class="robot-simulation-layer" aria-hidden="true">
          <div
            v-for="simulation in robotSimulations"
            :key="`${simulation.setNo}-${simulation.taskNo}`"
            class="moving-robot-pair"
            :data-simulation-set="simulation.setNo"
            :class="simulationClass(simulation)"
            :style="simulationStyle(simulation)">
            <span class="sim-robot">{{ simulation.setNo }}A</span>
            <span v-if="simulation.loaded" class="sim-vehicle">{{ simulation.carNo || '차량' }}</span>
            <span class="sim-robot">{{ simulation.setNo }}B</span>
            <small>{{ phaseText(simulation.phase) }}</small>
          </div>
        </div>

        <aside class="side-entrance left-entrance">
          <strong class="entrance-title"><i class="camera-dot"></i>1번 출입구</strong>
          <span class="outside-label">외부 도로</span>
          <div class="gate-barrier"><i></i><span>입차</span></div>
          <div class="side-waiting">
            <button v-for="space in leftEntrySpaces" :key="space.spaceNo" type="button" class="waiting-space entry" :class="{ occupied: space.carLogNo }" :data-space-no="space.spaceNo" @click="selectedSpace = space">
              <small>입차 대기</small>
              <strong v-if="space.carNo" class="waiting-car"><i></i>{{ space.carNo }}</strong>
              <strong v-else>{{ shortCode(space.spaceCode) }}</strong>
            </button>
          </div>
          <span class="flow-arrow">→</span>
          <div class="side-road"><span></span><b>로봇 인수·인계 통로</b><span></span></div>
          <span class="flow-arrow reverse">←</span>
          <div class="side-waiting">
            <button v-for="space in leftExitSpaces" :key="space.spaceNo" type="button" class="waiting-space exit" :class="{ occupied: space.carLogNo }" :data-space-no="space.spaceNo" @click="selectedSpace = space">
              <small>출차 대기</small>
              <strong v-if="space.carNo" class="waiting-car"><i></i>{{ space.carNo }}</strong>
              <strong v-else>{{ shortCode(space.spaceCode) }}</strong>
            </button>
          </div>
          <div class="gate-barrier exit-barrier"><i></i><span>출차</span></div>
        </aside>

        <div class="parking-layout">
        <section class="parking-zone floor-zone">
          <header><strong>B1 주차구역</strong><span>B1-P001 ~ B1-P100</span></header>
          <div class="robot-set-row top-sets">
            <div v-for="setNo in [1, 2]" :key="setNo" class="robot-set" :class="{ simulating: activeSetNos.has(setNo) }" :data-set-no="setNo">
              <strong>ROBOT SET {{ setNo }}</strong>
              <div><span>{{ setNo }}A</span><i></i><span>{{ setNo }}B</span></div>
            </div>
          </div>
          <div class="top-transfer-aisle">
            <span class="direction">←</span>
            <span class="cross-line"></span>
            <strong>ROBOT TRANSFER RAIL · 상부 이송 레일</strong>
            <span class="cross-line"></span>
            <span class="direction">→</span>
          </div>
          <div class="bank-label"><b>1·2열</b><span>B1-P001 ~ B1-P040</span></div>
          <div class="parking-bank north-bank">
            <template v-for="(space, index) in parkingRows[0]" :key="space.spaceNo">
              <ParkingSpaceButton :space="space" @select="selectedSpace = $event" />
              <span v-if="isPassage(index)" class="robot-cross-road" aria-label="로봇 연결 도로"><i></i></span>
            </template>
          </div>
          <div class="parking-bank south-bank">
            <template v-for="(space, index) in parkingRows[1]" :key="space.spaceNo">
              <ParkingSpaceButton :space="space" @select="selectedSpace = $event" />
              <span v-if="isPassage(index)" class="robot-cross-road" aria-label="로봇 연결 도로"><i></i></span>
            </template>
          </div>

          <div class="cross-aisle">
            <span class="cross-line"></span>
            <div class="cross-copy"><strong>ROBOT TRANSFER LANE</strong><small>자동 주차 이송 통로</small></div>
            <span class="cross-line"></span>
          </div>

          <div class="bank-label"><b>3·4열</b><span>B1-P041 ~ B1-P080</span></div>
          <div class="parking-bank north-bank">
            <template v-for="(space, index) in parkingRows[2]" :key="space.spaceNo">
              <ParkingSpaceButton :space="space" @select="selectedSpace = $event" />
              <span v-if="isPassage(index)" class="robot-cross-road" aria-label="로봇 연결 도로"><i></i></span>
            </template>
          </div>
          <div class="parking-bank south-bank">
            <template v-for="(space, index) in parkingRows[3]" :key="space.spaceNo">
              <ParkingSpaceButton :space="space" @select="selectedSpace = $event" />
              <span v-if="isPassage(index)" class="robot-cross-road" aria-label="로봇 연결 도로"><i></i></span>
            </template>
          </div>
          <div class="drive-aisle">
            <i class="lane center-line"></i>
            <span class="direction forward">→</span><span class="aisle-name">ROBOT TRANSFER RAIL · 하부 이송 레일</span><span class="direction backward">←</span>
          </div>
          <div class="bank-label"><b>5열</b><span>B1-P081 ~ B1-P100</span></div>
          <div class="parking-bank north-bank single-bank">
            <template v-for="(space, index) in parkingRows[4]" :key="space.spaceNo">
              <ParkingSpaceButton :space="space" @select="selectedSpace = $event" />
              <span v-if="isPassage(index)" class="robot-cross-road" aria-label="로봇 연결 도로"><i></i></span>
            </template>
          </div>
          <div class="robot-set-row bottom-sets">
            <div v-for="setNo in [3, 4]" :key="setNo" class="robot-set" :class="{ simulating: activeSetNos.has(setNo) }" :data-set-no="setNo">
              <strong>ROBOT SET {{ setNo }}</strong>
              <div><span>{{ setNo }}A</span><i></i><span>{{ setNo }}B</span></div>
            </div>
          </div>
        </section>
        </div>

        <aside class="side-entrance right-entrance">
          <strong class="entrance-title"><i class="camera-dot"></i>2번 출입구</strong>
          <span class="outside-label">외부 도로</span>
          <div class="gate-barrier"><i></i><span>입차</span></div>
          <div class="side-waiting">
            <button v-for="space in rightEntrySpaces" :key="space.spaceNo" type="button" class="waiting-space entry" :class="{ occupied: space.carLogNo }" :data-space-no="space.spaceNo" @click="selectedSpace = space">
              <small>입차 대기</small>
              <strong v-if="space.carNo" class="waiting-car"><i></i>{{ space.carNo }}</strong>
              <strong v-else>{{ shortCode(space.spaceCode) }}</strong>
            </button>
          </div>
          <span class="flow-arrow reverse">←</span>
          <div class="side-road"><span></span><b>로봇 인수·인계 통로</b><span></span></div>
          <span class="flow-arrow">→</span>
          <div class="side-waiting">
            <button v-for="space in rightExitSpaces" :key="space.spaceNo" type="button" class="waiting-space exit" :class="{ occupied: space.carLogNo }" :data-space-no="space.spaceNo" @click="selectedSpace = space">
              <small>출차 대기</small>
              <strong v-if="space.carNo" class="waiting-car"><i></i>{{ space.carNo }}</strong>
              <strong v-else>{{ shortCode(space.spaceCode) }}</strong>
            </button>
          </div>
          <div class="gate-barrier exit-barrier"><i></i><span>출차</span></div>
        </aside>
      </div>

      <footer class="map-legend">
        <span><i class="empty"></i>빈자리</span><span><i class="reserved"></i>예약</span><span><i class="used"></i>주차 중</span><span><i class="wait"></i>대기면</span><span><i class="robot-key"></i>로봇</span>
      </footer>
    </section>

    <aside v-if="selectedSpace" ref="spaceDetailPanel" class="space-detail">
      <button type="button" aria-label="닫기" @click="selectedSpace = null">×</button>
      <span>{{ typeText(selectedSpace.spaceType) }}</span>
      <h2>{{ selectedSpace.spaceCode }}</h2>
      <dl>
        <div><dt>현재 상태</dt><dd :class="{ active: selectedSpace.carLogNo || selectedSpace.carNo || selectedSpace.reservedTaskNo }">{{ spaceStatusText(selectedSpace) }}</dd></div>
        <div><dt>차량번호</dt><dd>{{ selectedSpace.carNo || selectedSpace.reservedCarNo || '-' }}</dd></div>
        <div v-if="selectedSpace.reservedTaskNo"><dt>예약 작업</dt><dd>#{{ selectedSpace.reservedTaskNo }}</dd></div>
        <div><dt>차량유형</dt><dd>{{ selectedSpace.carKind || '-' }}</dd></div>
        <div><dt>연결 게이트</dt><dd>{{ selectedSpace.gateCode || '-' }}</dd></div>
      </dl>
      <div v-if="selectedSpace.spaceType === 'EXIT_WAIT' && selectedSpace.carLogNo" class="space-detail-action">
        <button type="button" :disabled="!canReparkSelected || reparkRequesting" @click="requestRepark">
          {{ reparkButtonText }}
        </button>
        <small v-if="selectedReparkTask">다시 입차 작업이 진행 중입니다.</small>
        <small v-else>미출차 시 10분 후 자동으로 다시 입차합니다.</small>
      </div>
    </aside>

  </main>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, onUnmounted, ref } from 'vue';
import ManagementFeedbackToast from '@/shared/components/ManagementFeedbackToast.vue';
import { getParkingSpaces, getRobotTasks, reparkVehicle } from './parkingSpaceApi';

const spaces = ref([]);
const rawSpaces = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const lastUpdatedAt = ref(null);
const selectedSpace = ref(null);
const spaceDetailPanel = ref(null);
const simulationStage = ref(null);
const robotSimulations = ref([]);
const tasks = ref([]);
const parkingClock = ref(Date.now());
const reparkRequesting = ref(false);
const feedbackMessage = ref('');
const feedbackType = ref('success');
const setAnimations = new Map();
let taskTimer;
let spaceTimer;
let parkingClockTimer;
let taskRequesting = false;
let spaceRequesting = false;
let taskSignature = '';
let spaceSignature = '';
let reservationSignature = '';
let feedbackTimer;

const MOVE_DURATION = 15000;
const POSITIONING_DURATION = 15000;
const RETURN_DURATION = 15000;
const COMPLETED_VISIBLE_DURATION = 16000;

const loadedPhases = new Set([
  'LIFTING',
  'TRAFFIC_WAIT_LOADED',
  'MOVING_TO_DROPOFF',
  'DROPOFF_POSITIONING',
  'LOWERING',
]);

const activeSetNos = computed(() => new Set(
  robotSimulations.value.map((simulation) => Number(simulation.setNo))
));

const phaseLabels = {
  WAITING: '작업 대기',
  MOVING_TO_PICKUP: '차량 위치로 이동',
  PICKUP_POSITIONING: '차량 인양 위치 조정',
  LIFTING: '차량 리프팅',
  TRAFFIC_WAIT_EMPTY: '통행 대기',
  TRAFFIC_WAIT_LOADED: '차량 적재 통행 대기',
  TRAFFIC_WAIT_RETURN: '복귀 통행 대기',
  MOVING_TO_DROPOFF: '목적지로 이동',
  DROPOFF_POSITIONING: '주차 위치 조정',
  LOWERING: '차량 내려놓기',
  RETURNING_HOME: '대기 위치로 복귀',
  COMPLETED: '작업 완료',
};

const phaseText = (phase) => phaseLabels[phase] || phase || '-';

// 입차 시각부터 현재까지의 주차 경과시간 표시
const parkingElapsedText = (inTime) => {
  const startedAt = Date.parse(inTime);

  if (!Number.isFinite(startedAt)) return '';

  const totalMinutes = Math.max(
    0,
    Math.floor((parkingClock.value - startedAt) / 60000)
  );
  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  if (days > 0) return `${days}일 ${hours}시간`;
  if (hours > 0) return `${hours}시간 ${minutes}분`;
  return `${minutes}분`;
};

const simulationClass = (simulation) => ({
  loaded: simulation.loaded,
  lifting: simulation.phase === 'LIFTING',
  lowering: simulation.phase === 'LOWERING',
  positioning: ['PICKUP_POSITIONING', 'DROPOFF_POSITIONING'].includes(simulation.phase),
  waiting: ['TRAFFIC_WAIT_EMPTY', 'TRAFFIC_WAIT_LOADED', 'TRAFFIC_WAIT_RETURN'].includes(simulation.phase),
  returning: simulation.phase === 'RETURNING_HOME',
});

const simulationStyle = (simulation) => ({
  transform: `translate3d(${simulation.x}px, ${simulation.y}px, 0)`,
  opacity: simulation.visible ? 1 : 0,
});

const ParkingSpaceButton = defineComponent({
  props: { space: { type: Object, required: true } },
  emits: ['select'],
  setup(props, { emit }) {
    return () => {
      const occupied = Boolean(props.space.carLogNo || props.space.carNo);
      const reserved = Boolean(props.space.reservedTaskNo) && !occupied;
      return h('button', {
      type: 'button',
      'data-space-no': props.space.spaceNo,
      class: [
        'parking-space',
        {
          occupied,
          reserved,
          'has-car-number': props.space.carNo,
        },
      ],
      title: reserved
        ? `${props.space.spaceCode} · ${props.space.reservedCarNo || ''} 주차 예약`
        : `${props.space.spaceCode} · ${props.space.carNo || '빈자리'}`,
      onClick: () => emit('select', props.space),
    }, [
      props.space.carNo
        ? h('span', { class: 'car-number-lines' }, [
            h('b', props.space.carNo.slice(0, -4)),
            h('b', props.space.carNo.slice(-4)),
            props.space.inTime
              ? h('small', { class: 'parking-elapsed' }, parkingElapsedText(props.space.inTime))
              : null,
          ])
        : reserved
          ? h('span', { class: 'reservation-label' }, [
              h('b', '예약'),
              h('small', props.space.spaceCode.replace('B1-P', '')),
            ])
        : h('span', props.space.spaceCode.replace('B1-P', '')),
      occupied ? h('i', { class: 'occupancy-dot', title: '주차 중' }) : null,
    ]);
    };
  },
});

const parkingSpaces = computed(() => spaces.value.filter((space) => space.spaceType === 'PARKING'));
const parkingRows = computed(() => Array.from({ length: 5 }, (_, index) => parkingSpaces.value.slice(index * 20, (index + 1) * 20)));
const occupiedCount = computed(() => parkingSpaces.value.filter((space) => space.carLogNo || space.carNo).length);
const reservedCount = computed(() => parkingSpaces.value.filter((space) => space.reservedTaskNo && !space.carLogNo && !space.carNo).length);
const availableCount = computed(() => parkingSpaces.value.length - occupiedCount.value - reservedCount.value);
const usageRate = computed(() => parkingSpaces.value.length ? Math.round(occupiedCount.value / parkingSpaces.value.length * 100) : 0);
const lastUpdatedText = computed(() => lastUpdatedAt.value?.toLocaleTimeString('ko-KR', { hour12: false }) || '-');

const selectedReparkTask = computed(() => {
  if (!selectedSpace.value?.carLogNo) return null;

  return tasks.value.find((task) => (
    Number(task.carLogNo) === Number(selectedSpace.value.carLogNo)
      && task.taskType === 'PARK_IN'
      && ['WAITING', 'RUNNING'].includes(task.taskStatus)
  )) || null;
});

const canReparkSelected = computed(() => Boolean(
  selectedSpace.value?.spaceType === 'EXIT_WAIT'
    && selectedSpace.value?.carLogNo
    && !selectedReparkTask.value
));

const reparkButtonText = computed(() => {
  if (reparkRequesting.value) return '처리 중';
  if (selectedReparkTask.value) return '다시 입차 진행 중';
  return '다시 입차';
});

const showFeedback = (message, type = 'success') => {
  feedbackMessage.value = message;
  feedbackType.value = type;
  window.clearTimeout(feedbackTimer);
  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = '';
  }, 2500);
};

// 상세 박스 바깥을 클릭하면 선택을 해제한다.
const closeSpaceDetailOnOutside = (event) => {
  if (!selectedSpace.value) return;
  if (spaceDetailPanel.value?.contains(event.target)) return;

  selectedSpace.value = null;
};

// 출차대기 차량을 빈 주차면으로 다시 이동한다.
const requestRepark = async () => {
  if (!canReparkSelected.value || reparkRequesting.value) return;

  reparkRequesting.value = true;

  try {
    await reparkVehicle(selectedSpace.value.carLogNo);
    showFeedback('다시 입차 작업을 등록했습니다.');
    await loadRobotTasks(false);
    await loadParkingSpaces();
  } catch (error) {
    console.error('다시 입차 작업 등록 실패', error);
    showFeedback(
      error.response?.data?.message
        || error.response?.data?.detail
        || '다시 입차 작업을 등록하지 못했습니다.',
      'error'
    );
  } finally {
    reparkRequesting.value = false;
  }
};

const groupedWaiting = (type) => {
  const map = new Map();
  spaces.value.filter((space) => space.spaceType === type).forEach((space) => {
    const key = space.gateCode || 'UNKNOWN';
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(space);
  });
  return [...map.entries()].map(([gateCode, items]) => ({ gateCode, items }));
};
const entryGroups = computed(() => groupedWaiting('ENTRY_WAIT'));
const exitGroups = computed(() => groupedWaiting('EXIT_WAIT'));
const waitingByGateNumber = (groups, number) => groups.value.find((group) => group.gateCode?.endsWith(String(number)))?.items || [];
const leftEntrySpaces = computed(() => waitingByGateNumber(entryGroups, 1));
const leftExitSpaces = computed(() => waitingByGateNumber(exitGroups, 1));
const rightEntrySpaces = computed(() => waitingByGateNumber(entryGroups, 2));
const rightExitSpaces = computed(() => waitingByGateNumber(exitGroups, 2));

const shortCode = (code) => code.replace(/^B1-/, '');
const isPassage = (index) => (index + 1) % 5 === 0 && index < 19;
const typeText = (type) => ({ PARKING: '로봇 주차면', ENTRY_WAIT: '입차 대기면', EXIT_WAIT: '출차 대기면' }[type] || type);
const spaceStatusText = (space) => {
  if (space.carLogNo || space.carNo) return '주차 중';
  if (space.reservedTaskNo) return '주차 예정';
  return '빈자리';
};

const pointFromElement = (element) => {
  const stage = simulationStage.value;

  if (!stage || !element) return null;

  const stageRect = stage.getBoundingClientRect();
  const rect = element.getBoundingClientRect();

  return {
    x: rect.left - stageRect.left + rect.width / 2 - 38,
    y: rect.top - stageRect.top + rect.height / 2 - 14,
  };
};

const homePoint = (setNo) => pointFromElement(
  simulationStage.value?.querySelector(`[data-set-no="${setNo}"]`)
);

const spacePoint = (spaceNo) => pointFromElement(
  simulationStage.value?.querySelector(`[data-space-no="${spaceNo}"]`)
);

const laneY = (selector) => {
  const stage = simulationStage.value;
  const lane = stage?.querySelector(selector);

  if (!stage || !lane) return 0;

  const stageRect = stage.getBoundingClientRect();
  const laneRect = lane.getBoundingClientRect();

  return laneRect.top - stageRect.top + laneRect.height / 2 - 14;
};

const topLaneY = () => laneY('.top-transfer-aisle');
const middleLaneY = () => laneY('.cross-aisle');
const bottomLaneY = () => laneY('.drive-aisle');

const verticalRoadXs = () => {
  const stage = simulationStage.value;

  if (!stage) return [];

  const stageRect = stage.getBoundingClientRect();
  const positions = [...stage.querySelectorAll('.robot-cross-road')]
    .map((road) => {
      const rect = road.getBoundingClientRect();
      return Math.round(rect.left - stageRect.left + rect.width / 2 - 38);
    });

  return [...new Set(positions)].sort((a, b) => a - b);
};

const nearestRoadX = (x) => verticalRoadXs().reduce(
  (nearest, roadX) => (
    nearest === null || Math.abs(roadX - x) < Math.abs(nearest - x)
      ? roadX
      : nearest
  ),
  null
);

// 출발지와 목적지 사이에서 이동거리가 가장 짧은 세로 연결통로를 선택한다.
const bestRoadX = (startX, targetX) => verticalRoadXs().reduce(
  (best, roadX) => {
    const distance = Math.abs(startX - roadX)
      + Math.abs(targetX - roadX);

    if (best === null || distance < best.distance) {
      return { roadX, distance };
    }

    return best;
  },
  null
)?.roadX ?? nearestRoadX(startX) ?? startX;

const spaceTypeByNo = (spaceNo) => spaces.value.find(
  (space) => Number(space.spaceNo) === Number(spaceNo)
)?.spaceType;

const spaceByNo = (spaceNo) => spaces.value.find(
  (space) => Number(space.spaceNo) === Number(spaceNo)
);

// 주차면과 입·출차 대기면에 가장 가까운 가로 통로를 선택한다.
const laneYForSpace = (spaceNo) => {
  const space = spaceByNo(spaceNo);

  if (!space) return middleLaneY();
  if (space.spaceType === 'ENTRY_WAIT') return topLaneY();
  if (space.spaceType === 'EXIT_WAIT') return bottomLaneY();

  const parkingNumber = Number(space.spaceCode?.match(/P(\d+)$/)?.[1]);

  if (parkingNumber <= 20) return topLaneY();
  if (parkingNumber <= 60) return middleLaneY();
  return bottomLaneY();
};

const homeLaneY = (setNo) => Number(setNo) <= 2
  ? topLaneY()
  : bottomLaneY();

const simulationElement = (setNo) => simulationStage.value?.querySelector(
  `[data-simulation-set="${Number(setNo)}"]`
);

// 브라우저가 실제로 그리고 있는 로봇의 현재 좌표를 구한다.
const currentSimulationPoint = (simulation) => {
  const stage = simulationStage.value;
  const element = simulationElement(simulation.setNo);

  if (!stage || !element) {
    return {
      x: simulation.x,
      y: simulation.y,
    };
  }

  const stageRect = stage.getBoundingClientRect();
  const elementRect = element.getBoundingClientRect();

  return {
    x: elementRect.left - stageRect.left,
    y: elementRect.top - stageRect.top,
  };
};

const cancelSetAnimation = (setNo) => {
  const animation = setAnimations.get(Number(setNo));

  if (animation) {
    animation.cancel();
    setAnimations.delete(Number(setNo));
  }
};

// 대기 단계에서는 화면에 보이는 현재 위치에 로봇을 정지시킨다.
const freezeSimulation = (simulation) => {
  const current = currentSimulationPoint(simulation);

  cancelSetAnimation(simulation.setNo);
  simulation.x = current.x;
  simulation.y = current.y;
};

const removeDuplicatePoints = (points) => points.filter((point, index) => (
  index === 0
    || point.x !== points[index - 1].x
    || point.y !== points[index - 1].y
));

// 모든 구간을 X축 또는 Y축 한 방향으로만 이동시킨다.
const orthogonalizePoints = (points) => {
  const result = [];

  points.forEach((point) => {
    const previous = result[result.length - 1];

    if (previous && previous.x !== point.x && previous.y !== point.y) {
      result.push({ x: point.x, y: previous.y });
    }

    result.push(point);
  });

  return removeDuplicatePoints(result);
};

const routeBetween = (
  start,
  target,
  {
    fromSpaceNo = null,
    toSpaceNo = null,
    fromSetNo = null,
    toSetNo = null,
  } = {}
) => {
  if (!start || !target) return [];

  const fromLaneY = fromSpaceNo == null
    ? homeLaneY(fromSetNo)
    : laneYForSpace(fromSpaceNo);
  const toLaneY = toSpaceNo == null
    ? homeLaneY(toSetNo)
    : laneYForSpace(toSpaceNo);
  const route = [
    start,
    // 로봇 위치 또는 주차면에서 가장 가까운 가로 통로로 진입한다.
    { x: start.x, y: fromLaneY },
  ];

  // 다른 가로 통로로 이동할 때만 실제 세로 연결통로를 이용한다.
  if (Math.abs(fromLaneY - toLaneY) > 1) {
    const roadX = bestRoadX(start.x, target.x);

    route.push(
      { x: roadX, y: fromLaneY },
      { x: roadX, y: toLaneY }
    );
  }

  route.push(
    // 목적지 앞 통로까지 이동한 뒤 목적지로 진입한다.
    { x: target.x, y: toLaneY },
    target
  );

  return orthogonalizePoints(route);
};

// 위치 조정 단계가 시작되는 통로 쪽 접근 지점이다.
const approachPoint = (spaceNo) => {
  const target = spacePoint(spaceNo);

  if (!target) return null;

  if (spaceTypeByNo(spaceNo) === 'PARKING') {
    return {
      x: target.x,
      y: laneYForSpace(spaceNo),
    };
  }

  return {
    x: target.x,
    y: laneYForSpace(spaceNo),
  };
};

const phaseElapsedMs = (task) => {
  const phaseTime = Date.parse(
    task.phaseUpdatedAt
      || task.startedAt
      || task.requestedAt
  );

  return Number.isFinite(phaseTime)
    ? Math.max(0, Date.now() - phaseTime)
    : 0;
};

const taskPhaseDuration = (
  task,
  fallback
) => {
  const duration = Number(task.phaseDurationMs);

  return Number.isFinite(duration) && duration > 0
    ? duration
    : fallback;
};

const animateSimulation = (
  simulation,
  points,
  duration,
  elapsed = 0
) => {
  cancelSetAnimation(simulation.setNo);

  if (points.length < 2) return;

  const safePoints = orthogonalizePoints(points);
  const distances = safePoints.slice(1).map((point, index) => (
    Math.abs(point.x - safePoints[index].x)
    + Math.abs(point.y - safePoints[index].y)
  ));
  const totalDistance = distances.reduce((sum, distance) => sum + distance, 0) || 1;
  const safeElapsed = Math.min(Math.max(elapsed, 0), duration);
  let traveled = 0;
  const keyframes = safePoints.map((point, index) => {
    if (index > 0) traveled += distances[index - 1];

    return {
      transform: `translate3d(${point.x}px, ${point.y}px, 0)`,
      offset: index === safePoints.length - 1
        ? 1
        : traveled / totalDistance,
    };
  });
  const finalPoint = safePoints[safePoints.length - 1];

  const startAnimation = () => {
    const element = simulationElement(simulation.setNo);

    if (!element) {
      simulation.x = finalPoint.x;
      simulation.y = finalPoint.y;
      return;
    }

    const animation = element.animate(
      keyframes,
      {
        duration,
        easing: 'linear',
        fill: 'forwards',
      }
    );

    animation.currentTime = safeElapsed;
    setAnimations.set(Number(simulation.setNo), animation);

    animation.onfinish = () => {
      if (setAnimations.get(Number(simulation.setNo)) !== animation) return;

      simulation.x = finalPoint.x;
      simulation.y = finalPoint.y;

      nextTick(() => {
        if (setAnimations.get(Number(simulation.setNo)) === animation) {
          animation.cancel();
          setAnimations.delete(Number(simulation.setNo));
        }
      });
    };
  };

  if (safeElapsed >= duration) {
    simulation.x = finalPoint.x;
    simulation.y = finalPoint.y;
    return;
  }

  if (simulationElement(simulation.setNo)) {
    startAnimation();
  } else {
    nextTick(startAnimation);
  }
};

const phaseStartPoint = (task) => {
  if (task.taskPhase === 'PICKUP_POSITIONING') {
    return approachPoint(task.pickupSpaceNo);
  }

  if (['LIFTING', 'TRAFFIC_WAIT_LOADED', 'MOVING_TO_DROPOFF'].includes(task.taskPhase)) {
    return spacePoint(task.pickupSpaceNo);
  }

  if (task.taskPhase === 'DROPOFF_POSITIONING') {
    return approachPoint(task.dropoffSpaceNo);
  }

  if (['LOWERING', 'TRAFFIC_WAIT_RETURN', 'RETURNING_HOME'].includes(task.taskPhase)) {
    return spacePoint(task.dropoffSpaceNo);
  }

  if (task.taskPhase === 'COMPLETED') {
    return homePoint(task.setNo);
  }

  return homePoint(task.setNo);
};

const moveSimulationForPhase = (
  simulation,
  task
) => {
  simulation.phase = task.taskPhase;
  simulation.loaded = loadedPhases.has(task.taskPhase);
  simulation.carNo = task.carNo;

  const current = currentSimulationPoint(simulation);
  const pickup = spacePoint(task.pickupSpaceNo);
  const pickupApproach = approachPoint(task.pickupSpaceNo);
  const dropoff = spacePoint(task.dropoffSpaceNo);
  const dropoffApproach = approachPoint(task.dropoffSpaceNo);
  const home = homePoint(task.setNo);
  const elapsed = phaseElapsedMs(task);

  const move = (points, duration) => {
    animateSimulation(
      simulation,
      points,
      duration,
      elapsed
    );
  };

  if (task.taskPhase === 'MOVING_TO_PICKUP' && pickupApproach) {
    move(
      routeBetween(current, pickupApproach, {
        fromSetNo: task.setNo,
        toSpaceNo: task.pickupSpaceNo,
      }),
      taskPhaseDuration(task, MOVE_DURATION)
    );
    return;
  }

  if (task.taskPhase === 'PICKUP_POSITIONING' && pickup) {
    move(
      orthogonalizePoints([
        current,
        pickupApproach,
        pickup,
      ]),
      taskPhaseDuration(task, POSITIONING_DURATION)
    );
    return;
  }

  if (['LIFTING', 'TRAFFIC_WAIT_EMPTY', 'TRAFFIC_WAIT_LOADED', 'TRAFFIC_WAIT_RETURN', 'LOWERING'].includes(task.taskPhase)) {
    freezeSimulation(simulation);
    return;
  }

  if (task.taskPhase === 'MOVING_TO_DROPOFF' && dropoffApproach) {
    move(
      routeBetween(current, dropoffApproach, {
        fromSpaceNo: task.pickupSpaceNo,
        toSpaceNo: task.dropoffSpaceNo,
      }),
      taskPhaseDuration(task, MOVE_DURATION)
    );
    return;
  }

  if (task.taskPhase === 'DROPOFF_POSITIONING' && dropoff) {
    move(
      orthogonalizePoints([
        current,
        dropoffApproach,
        dropoff,
      ]),
      taskPhaseDuration(task, POSITIONING_DURATION)
    );
    return;
  }

  if (task.taskPhase === 'RETURNING_HOME' && home) {
    simulation.loaded = false;
    move(
      routeBetween(current, home, {
        fromSpaceNo: task.dropoffSpaceNo,
        toSetNo: task.setNo,
      }),
      taskPhaseDuration(task, RETURN_DURATION)
    );
    return;
  }

  if (task.taskPhase === 'COMPLETED' && home) {
    cancelSetAnimation(simulation.setNo);
    simulation.loaded = false;
    simulation.x = home.x;
    simulation.y = home.y;
  }
};

const syncRobotSimulations = () => {
  const now = Date.now();
  const latestBySet = new Map();

  tasks.value.forEach((task) => {
    if (task.setNo == null) return;

    const completedRecently = task.taskStatus === 'COMPLETED'
      && task.completedAt
      && now - new Date(task.completedAt).getTime() < COMPLETED_VISIBLE_DURATION;

    if (task.taskStatus !== 'RUNNING' && !completedRecently) return;
    if (!latestBySet.has(Number(task.setNo))) latestBySet.set(Number(task.setNo), task);
  });

  robotSimulations.value
    .filter((simulation) => !latestBySet.has(Number(simulation.setNo)))
    .forEach((simulation) => cancelSetAnimation(simulation.setNo));

  robotSimulations.value = robotSimulations.value.filter(
    (simulation) => latestBySet.has(Number(simulation.setNo))
  );

  latestBySet.forEach((task, setNo) => {
    let simulation = robotSimulations.value.find(
      (item) => Number(item.setNo) === setNo && Number(item.taskNo) === Number(task.taskNo)
    );
    if (!simulation) {
      const start = phaseStartPoint(task);

      if (!start) return;

      simulation = {
        setNo,
        taskNo: task.taskNo,
        phase: null,
        carNo: task.carNo,
        loaded: false,
        visible: true,
        x: start.x,
        y: start.y,
      };

      robotSimulations.value.push(simulation);
    }

    if (simulation.phase !== task.taskPhase) {
      moveSimulationForPhase(
        simulation,
        task
      );
    }
  });
};

const taskStateSignature = (items) => JSON.stringify(
  items.map((task) => [
    task.taskNo,
    task.setNo,
    task.taskStatus,
    task.taskPhase,
    task.phaseUpdatedAt,
    task.phaseDurationMs,
    task.completedAt,
  ])
);

const taskReservationSignature = (items) => JSON.stringify(
  items
    .filter((task) => task.taskType === 'PARK_IN'
      && ['WAITING', 'RUNNING'].includes(task.taskStatus))
    .map((task) => [
      task.taskNo,
      task.dropoffSpaceNo,
      task.carNo,
    ])
);

const parkingStateSignature = (items) => JSON.stringify(
  items.map((space) => [
    space.spaceNo,
    space.carLogNo,
    space.carNo,
    space.active,
  ])
);

// 주차면 원본에 현재 입차 작업의 예약 정보를 결합한다.
const applySpaces = () => {
  const activeParkInTasks = tasks.value
    .filter((task) => task.taskType === 'PARK_IN'
      && ['WAITING', 'RUNNING'].includes(task.taskStatus));

  const reservationBySpaceNo = new Map(
    activeParkInTasks.map((task) => [
      Number(task.dropoffSpaceNo),
      task,
    ])
  );

  spaces.value = rawSpaces.value.map((space) => {
    const task = reservationBySpaceNo.get(
      Number(space.spaceNo)
    );

    return {
      ...space,
      reservedTaskNo: task?.taskNo ?? null,
      reservedCarNo: task?.carNo ?? null,
    };
  });

  if (selectedSpace.value) {
    selectedSpace.value = spaces.value.find(
      (space) => space.spaceNo === selectedSpace.value.spaceNo
    ) || null;
  }
};

// 주차면은 실제 값이 바뀌었을 때만 다시 그린다.
const loadParkingSpaces = async () => {
  if (spaceRequesting) return false;
  spaceRequesting = true;

  try {
    const response = await getParkingSpaces('B1');
    const nextSpaces = Array.isArray(response.data)
      ? response.data
      : [];
    const nextSignature = parkingStateSignature(nextSpaces);

    if (nextSignature === spaceSignature) {
      return false;
    }

    rawSpaces.value = nextSpaces;
    spaceSignature = nextSignature;
    applySpaces();
    lastUpdatedAt.value = new Date();

    return true;
  } finally {
    spaceRequesting = false;
  }
};

// 작업 상태는 짧은 간격으로 확인하고 변경 순간에만 화면에 반영한다.
const loadRobotTasks = async (
  reloadSpacesOnChange = true
) => {
  if (taskRequesting) return false;
  taskRequesting = true;

  try {
    const response = await getRobotTasks();
    const nextTasks = Array.isArray(response.data)
      ? response.data
      : [];
    const nextSignature = taskStateSignature(nextTasks);

    if (nextSignature === taskSignature) {
      return false;
    }

    const nextReservationSignature =
      taskReservationSignature(nextTasks);

    tasks.value = nextTasks;
    taskSignature = nextSignature;

    if (nextReservationSignature !== reservationSignature) {
      reservationSignature = nextReservationSignature;
      applySpaces();
    }

    if (reloadSpacesOnChange) {
      await loadParkingSpaces();
    }

    lastUpdatedAt.value = new Date();
    await nextTick();
    syncRobotSimulations();

    return true;
  } finally {
    taskRequesting = false;
  }
};

const refreshMap = async () => {
  if (loading.value) return;
  loading.value = true;
  errorMessage.value = '';

  try {
    await loadRobotTasks(false);
    await loadParkingSpaces();
    await nextTick();
    syncRobotSimulations();
  } catch (error) {
    console.error('주차장 배치도 조회 실패', error);
    errorMessage.value = '주차장 배치도를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await refreshMap();

  taskTimer = window.setInterval(
    () => loadRobotTasks().catch((error) => {
      console.error('로봇 작업 상태 조회 실패', error);
    }),
    500
  );

  spaceTimer = window.setInterval(
    () => loadParkingSpaces().catch((error) => {
      console.error('주차면 상태 조회 실패', error);
    }),
    5000
  );

  parkingClockTimer = window.setInterval(
    () => {
      parkingClock.value = Date.now();
    },
    60000
  );

  window.addEventListener('resize', syncRobotSimulations);
  document.addEventListener('pointerdown', closeSpaceDetailOnOutside);
});
onUnmounted(() => {
  window.clearInterval(taskTimer);
  window.clearInterval(spaceTimer);
  window.clearInterval(parkingClockTimer);
  window.clearTimeout(feedbackTimer);
  window.removeEventListener('resize', syncRobotSimulations);
  document.removeEventListener('pointerdown', closeSpaceDetailOnOutside);
  setAnimations.forEach((animation) => animation.cancel());
  setAnimations.clear();
});
</script>

<style scoped>
.parking-map-page{min-height:100%;padding:28px;color:#eef1f3;background:#24292e}.page-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}.page-heading h1{margin:0 0 6px;font-size:24px}.page-heading p{margin:0;color:#9da6ad}.map-actions{display:flex;align-items:center;gap:12px}.map-actions button{padding:9px 14px;border:1px solid #69737b;color:#fff;background:#3a4147;cursor:pointer}.live{color:#b8c0c6;font-size:12px}.live i{width:7px;height:7px;display:inline-block;margin-right:6px;border-radius:50%;background:#78bd91;box-shadow:0 0 0 4px #354c3d}.map-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-bottom:14px}.map-summary div{padding:14px 17px;border:1px solid #505960;border-top:3px solid #69737b;background:#2b3035}.map-summary span{display:block;color:#aeb6bc;font-size:12px}.map-summary strong{font-size:27px}.map-summary .available strong{color:#8fc7a6}.map-summary .occupied strong{color:#ef969c}.garage-shell{position:relative;padding:18px;border:1px solid #505960;background:#2b3035;overflow:auto}.garage-title{display:flex;align-items:baseline;gap:12px;padding-bottom:13px;border-bottom:1px solid #505960}.garage-title span{color:#ffc928;font-size:11px;font-weight:900;letter-spacing:.12em}.garage-title strong{font-size:15px}.garage-title small{margin-left:auto;color:#8e989f}.gate-row{min-width:1220px;display:grid;grid-template-columns:1fr 1fr;gap:60px;padding:16px 5%}.gate-group{display:flex;align-items:center;justify-content:center;gap:12px}.gate-label{padding:8px 11px;border:1px solid #69737b;color:#d8dde1;background:#343a40;font-size:11px;font-weight:800}.camera-dot{width:7px;height:7px;display:inline-block;margin-right:7px;border-radius:50%;background:#78bd91}.waiting-spaces{display:flex;gap:5px}.waiting-space{min-width:65px;padding:6px;border:1px solid #b58c32;color:#f0d99f;background:#554a31;cursor:pointer}.waiting-space.occupied{border-color:#c96770;color:#f6c3c7;background:#603c41}.waiting-space small,.waiting-space strong{display:block;font-size:9px}.parking-layout{min-width:1220px;display:block}.parking-zone{padding:13px;border:1px solid #505960;background:#30363b}.parking-zone header,.bank-label{display:flex;justify-content:space-between;align-items:center}.parking-zone header{margin-bottom:12px;padding-bottom:9px;border-bottom:1px solid #505960}.parking-zone header strong{color:#ffc928}.parking-zone header span,.bank-label span{color:#8e989f;font-size:10px}.bank-label{padding:5px 2px}.bank-label b{color:#c8ced2;font-size:11px}.parking-bank{display:grid;grid-template-columns:repeat(25,minmax(40px,1fr));gap:3px}.parking-space{height:76px;position:relative;padding:4px 2px;border:1px solid #667078;border-top:3px solid #768088;color:#aeb6bc;background:#292f34;overflow:hidden;cursor:pointer}.south-bank .parking-space{border-top:1px solid #667078;border-bottom:3px solid #768088}.parking-space:hover{border-color:#ffc928}.parking-space.occupied{border-color:#b85c64;background:#4c3438}.parking-space>span{position:absolute;top:3px;left:4px;font-size:9px}.parking-space small{position:absolute;right:2px;bottom:3px;left:2px;color:#7f8a92;font-size:7px;overflow:hidden;text-overflow:ellipsis}.parking-space.occupied small{color:#e8b1b5}.car-shape{width:25px;height:43px;position:absolute;left:50%;top:16px;border-radius:7px 7px 4px 4px;background:#bb6269;transform:translateX(-50%);box-shadow:inset 0 7px #793f45}.drive-aisle{height:88px;position:relative;display:flex;align-items:center;justify-content:space-around;margin:4px 0;border-right:2px solid #e8d36c;border-left:2px solid #e8d36c;background:#272d32;overflow:hidden}.drive-aisle .pillar{width:25px;height:25px;z-index:2;position:relative;display:grid;place-items:center;border:3px solid #171b1f;color:#505960;background:#8d969d;font-size:8px;font-weight:900}.drive-aisle .pillar:nth-of-type(odd){align-self:flex-start}.drive-aisle .pillar:nth-of-type(even){align-self:flex-end}.lane{position:absolute;right:0;left:0;top:50%;border-top:2px dashed #68727a}.direction{z-index:1;color:#d8c968;font-size:28px}.aisle-name{z-index:1;padding:5px 15px;color:#9ca5ab;background:#272d32;font-size:10px;font-weight:900;letter-spacing:.16em}.cross-aisle{height:70px;display:flex;align-items:center;justify-content:center;gap:18px;margin:14px -13px;padding:0 25px;border-top:2px solid #596168;border-bottom:2px solid #596168;background:#242a2f}.cross-line{height:2px;flex:1;border-top:2px dashed #69737b}.robot{z-index:1;width:54px;padding:9px 0;border:2px solid #ffc928;border-radius:5px;color:#171b1f;background:#ffc928;text-align:center;font-size:10px;font-weight:900}.cross-copy{text-align:center}.cross-copy strong,.cross-copy small{display:block}.cross-copy strong{color:#b9c0c5;font-size:9px;letter-spacing:.12em}.cross-copy small{color:#747f87;font-size:8px}.map-legend{display:flex;justify-content:center;gap:20px;padding-top:14px;border-top:1px solid #505960;color:#aeb6bc;font-size:10px}.map-legend i{width:10px;height:10px;display:inline-block;margin-right:5px;border:1px solid #69737b;vertical-align:-1px}.map-legend .used{border-color:#b85c64;background:#4c3438}.map-legend .wait{border-color:#b58c32;background:#554a31}.map-legend .robot-key{border-color:#ffc928;background:#ffc928}.space-detail{width:250px;position:fixed;right:28px;bottom:28px;z-index:20;padding:20px;border:1px solid #69737b;color:#eef1f3;background:#343a40;box-shadow:0 15px 35px rgba(0,0,0,.4)}.space-detail>button{position:absolute;right:10px;top:8px;border:0;color:#bbb;background:none;font-size:22px;cursor:pointer}.space-detail>span{color:#ffc928;font-size:10px}.space-detail h2{margin:5px 0 15px}.space-detail dl{margin:0}.space-detail dl div{display:flex;justify-content:space-between;padding:7px 0;border-top:1px solid #505960}.space-detail dt{color:#9da6ad;font-size:11px}.space-detail dd{margin:0;font-size:11px;font-weight:800}.space-detail dd.active{color:#ef969c}.map-message{padding:80px;text-align:center;background:#2b3035}.map-message.error{color:#ef969c}@media(max-width:900px){.parking-map-page{padding:14px}.page-heading{align-items:flex-start;flex-direction:column;gap:12px}.map-summary{grid-template-columns:repeat(2,1fr)}.space-detail{right:14px;bottom:14px}}
.access-layout{min-width:1510px;display:grid;grid-template-columns:135px minmax(1220px,1fr) 135px;gap:10px;padding-top:16px}.side-entrance{display:flex;flex-direction:column;align-items:center;padding:10px 7px;border:1px solid #505960;background:#282e33}.entrance-title{width:100%;padding-bottom:9px;border-bottom:1px solid #505960;color:#ffc928;text-align:center;font-size:11px}.outside-label{width:100%;margin:10px 0;padding:8px 0;border:1px dashed #69737b;color:#9da6ad;text-align:center;font-size:9px}.gate-barrier{width:100%;position:relative;padding:9px 0 7px;border-top:3px solid #d8c968;text-align:center}.gate-barrier i{width:42px;height:5px;position:absolute;left:7px;top:-4px;background:repeating-linear-gradient(90deg,#d9747b 0 8px,#e1e5e8 8px 16px);transform:rotate(-28deg);transform-origin:left}.gate-barrier span{color:#b9c0c5;font-size:9px}.side-waiting{width:100%;display:grid;gap:5px}.side-waiting .waiting-space{min-width:0;width:100%;min-height:43px}.flow-arrow{padding:8px;color:#d8c968;font-size:24px;font-weight:900}.side-road{width:62px;flex:1;min-height:80px;display:flex;position:relative;align-items:center;justify-content:center;border-right:2px solid #596168;border-left:2px solid #596168;background:#242a2f}.side-road span{position:absolute;top:0;bottom:0;left:50%;border-left:2px dashed #69737b}.side-road b{z-index:1;padding:5px 2px;color:#8f999f;background:#242a2f;font-size:8px;writing-mode:vertical-rl}.exit-barrier{margin-top:9px}.right-entrance .gate-barrier i{right:7px;left:auto;transform:rotate(28deg);transform-origin:right}.parking-layout{min-width:1220px}.map-legend{min-width:1510px}
.parking-map-page,.garage-shell,.access-layout,.parking-layout{color-scheme:dark}.parking-map-page button{appearance:none;-webkit-appearance:none}.garage-shell{scrollbar-color:#596168 #24292e}.garage-shell::-webkit-scrollbar{width:11px;height:11px}.garage-shell::-webkit-scrollbar-track{background:#24292e}.garage-shell::-webkit-scrollbar-thumb{border:2px solid #24292e;border-radius:7px;background:#596168}.gate-barrier i{background:repeating-linear-gradient(90deg,#b85c64 0 8px,#69737b 8px 16px)}.map-actions button{color:#d8dde1}.parking-space{color:#aeb6bc;background-color:#292f34}.waiting-space{color:#d7c58e;background-color:#554a31}.space-detail>button{color:#9da6ad;background-color:transparent}
.floor-zone>header{padding:10px 12px!important;border:1px solid #596168!important;background:#343a40!important}.floor-zone>header strong{color:#ffc928!important;background:transparent!important}.floor-zone>header span{color:#aeb6bc!important;background:transparent!important}.bank-label{background:#30363b!important}.bank-label b,.bank-label span{background:transparent!important}
.parking-map-page{width:100%;min-height:100vh;box-sizing:border-box;border:0!important;color:#eef1f3!important;background:#24292e!important;box-shadow:none!important}.parking-map-page>.page-heading{padding:0!important;border:0!important;color:#eef1f3!important;background:#24292e!important;box-shadow:none!important}.parking-map-page>.page-heading>div{background:transparent!important}.parking-map-page .management-list-title{color:#f1f3f5!important;background:transparent!important}.parking-map-page .map-actions{background:transparent!important}.parking-map-page .map-summary{background:#24292e!important}.parking-map-page .map-summary>div{color:#eef1f3!important;background:#2b3035!important}.parking-map-page .garage-shell{color:#eef1f3!important;background:#2b3035!important}.parking-map-page .garage-title{background:#2b3035!important}.parking-map-page .access-layout{background:#2b3035!important}.parking-map-page .parking-layout{background:#2b3035!important}.parking-map-page .floor-zone{background:#30363b!important}.parking-map-page .side-entrance{background:#282e33!important}.parking-map-page .map-legend{background:#2b3035!important}.parking-map-page .map-message{background:#2b3035!important}.parking-map-page .space-detail{background:#343a40!important}
.parking-bank{grid-template-columns:repeat(5,minmax(40px,1fr)) 34px repeat(5,minmax(40px,1fr)) 34px repeat(5,minmax(40px,1fr)) 34px repeat(5,minmax(40px,1fr)) 34px repeat(5,minmax(40px,1fr))}.robot-cross-passage{height:76px;position:relative;display:flex;align-items:center;justify-content:center;border-right:1px dashed #7d878e;border-left:1px dashed #7d878e;background:#242a2f;overflow:hidden}.robot-cross-passage i{height:100%;position:absolute;left:50%;border-left:2px dashed #d8c968}.robot-cross-passage small{z-index:1;padding:3px;color:#aeb6bc;background:#242a2f;font-size:7px;line-height:1.1;text-align:center;writing-mode:vertical-rl}.south-bank .robot-cross-passage{border-top:0;border-bottom:0}
.drive-aisle{display:grid;grid-template-columns:repeat(6,1fr);align-items:center;justify-items:center}.drive-aisle .pillar,.drive-aisle .pillar:nth-of-type(odd),.drive-aisle .pillar:nth-of-type(even){width:22px;height:22px;align-self:center!important;border:3px solid #171b1f;border-radius:2px;background:#737d84;box-shadow:inset 0 0 0 2px #555f66}.drive-aisle .direction,.drive-aisle .aisle-name{position:absolute}.drive-aisle .forward{left:22%}.drive-aisle .backward{right:22%}.drive-aisle .aisle-name{left:50%;transform:translateX(-50%)}
.access-layout{width:100%;min-width:1040px;grid-template-columns:112px minmax(800px,1fr) 112px;gap:7px}.parking-layout{min-width:800px}.parking-bank{grid-template-columns:repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr));gap:2px}.parking-space{height:54px;padding:0}.parking-space>span{inset:50% auto auto 50%;z-index:2;color:#c8cfd4;font-size:8px;font-weight:800;transform:translate(-50%,-50%)}.parking-space small{display:none}.car-shape{width:18px;height:34px;top:10px;border-radius:5px;opacity:.72;box-shadow:inset 0 6px #793f45}.robot-cross-passage{height:54px}.robot-cross-passage small{font-size:6px}.side-pillar{width:13px;height:13px;position:absolute;z-index:2;border:2px solid #171b1f;border-radius:1px;background:#737d84;box-shadow:inset 0 0 0 1px #555f66}.side-pillar.top{top:0}.side-pillar.bottom{bottom:0}.drive-aisle{height:58px;display:flex}.drive-aisle .direction{font-size:20px}.drive-aisle .aisle-name{font-size:8px}.cross-aisle{height:52px;margin:9px -13px}.robot{width:45px;padding:6px 0;font-size:8px}.side-entrance{padding:8px 5px}.waiting-space{padding:4px}.side-waiting .waiting-space{min-height:36px}.side-road{width:52px;min-height:58px}.map-legend{min-width:1040px}
.parking-bank{grid-template-columns:repeat(5,minmax(24px,1fr)) 20px repeat(5,minmax(24px,1fr)) 20px repeat(5,minmax(24px,1fr)) 20px repeat(5,minmax(24px,1fr)) 20px repeat(5,minmax(24px,1fr))}.parking-column-gap{height:54px;display:grid;place-items:center;background:#30363b}.parking-column-gap b{width:14px;height:14px;border:2px solid #171b1f;border-radius:2px;background:#737d84;box-shadow:inset 0 0 0 2px #555f66}.drive-aisle{z-index:1}.drive-aisle::before,.drive-aisle::after{content:none}
.parking-bank{grid-template-columns:repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr)) 24px repeat(5,minmax(24px,1fr))}.robot-cross-road{height:54px;position:relative;display:block;border-right:1px solid #505960;border-left:1px solid #505960;background:#242a2f}.robot-cross-road i{height:100%;position:absolute;left:50%;border-left:2px dashed #d8c968;transform:translateX(-50%)}
.parking-bank{grid-template-columns:repeat(5,minmax(26px,1fr)) 26px repeat(5,minmax(26px,1fr)) 26px repeat(5,minmax(26px,1fr)) 26px repeat(5,minmax(26px,1fr))}.single-bank{margin-bottom:4px}
.parking-space.has-car-number>span{width:125%;max-width:none;padding:0;box-sizing:border-box;color:#efb0b5;font-size:5px;font-weight:800;line-height:1;letter-spacing:-.45px;white-space:nowrap;overflow:visible;transform:translate(-50%,-50%) scaleX(.82);transform-origin:center}.occupancy-dot{width:5px;height:5px;position:absolute;top:3px;right:3px;z-index:3;border-radius:50%;background:#d9747b;box-shadow:0 0 0 1px #44343a}
.parking-space,.north-bank .parking-space,.south-bank .parking-space{border:1px solid #3f474d!important;outline:0!important;color:#aeb6bc!important;background-color:#292f34!important;background-image:none!important;box-shadow:none!important}.parking-space::before,.parking-space::after{content:none!important;display:none!important}.parking-space.occupied,.north-bank .parking-space.occupied,.south-bank .parking-space.occupied{border-color:#5c646b!important;background-color:#292f34!important;background-image:none!important;box-shadow:none!important}.parking-space:hover,.parking-space:focus,.parking-space:active{border-color:#b99a3f!important;outline:0!important;background-image:none!important;box-shadow:none!important}
.parking-map-page .garage-shell{border:0!important;outline:0!important;box-shadow:none!important}.parking-map-page .access-layout{border:0!important;outline:0!important}.parking-map-page .map-legend{border-top-color:#3f474d!important}
.parking-map-page .floor-zone{border:0!important;outline:0!important;box-shadow:none!important}
.map-legend .used{width:10px!important;height:10px!important;border:1px solid #d7c36c!important;border-radius:1px;background:#d8c56f!important;box-shadow:none!important}
.parking-map-page .parking-bank .parking-space.occupied{border:1px solid #d7c36c!important;outline:0!important;background-color:#d8c56f!important;background-image:none!important;box-shadow:none!important}.parking-map-page .parking-bank .parking-space.occupied:hover,.parking-map-page .parking-bank .parking-space.occupied:focus,.parking-map-page .parking-bank .parking-space.occupied:active{border-color:#f0dd80!important;outline:0!important;background-color:#e0cd78!important;box-shadow:none!important}.parking-map-page .parking-space.has-car-number>span{color:#24292e!important}.parking-map-page .parking-space .occupancy-dot{display:none!important}
.parking-map-page .parking-bank .parking-space{border-color:#69737b!important}.parking-map-page .parking-bank .parking-space.occupied{border-color:#fff0b5!important}.parking-map-page .parking-bank .parking-space.occupied:hover,.parking-map-page .parking-bank .parking-space.occupied:focus{border-color:#fff7d6!important}
.map-legend .used{border-color:#f8f2d8!important;background:#eee8c9!important}.parking-map-page .parking-bank .parking-space.occupied{border-color:#f8f2d8!important;background-color:#eee8c9!important}.parking-map-page .parking-bank .parking-space.occupied:hover,.parking-map-page .parking-bank .parking-space.occupied:focus,.parking-map-page .parking-bank .parking-space.occupied:active{border-color:#fffbed!important;background-color:#f5efd6!important}.parking-map-page .parking-space.has-car-number>span{color:#343a40!important}
.parking-map-page .parking-bank .parking-space>span,.parking-map-page .parking-bank .parking-space.has-car-number>span{color:#ffd84d!important;text-shadow:-1px 0 #4a3b00,0 1px #4a3b00,1px 0 #4a3b00,0 -1px #4a3b00!important}
.parking-map-page .floor-zone .bank-label b,.parking-map-page .floor-zone .bank-label span{color:#e7c94e!important}.parking-map-page .floor-zone>header span{color:#d9bd4c!important}.parking-map-page .aisle-name,.parking-map-page .cross-copy strong,.parking-map-page .cross-copy small{color:#d8c45e!important}
.parking-map-page :deep(.parking-space > span){color:#d2bb62!important;font-weight:600!important;text-shadow:none!important}.parking-map-page :deep(.parking-space.has-car-number > span){color:#8b741d!important;font-weight:700!important;text-shadow:none!important}
.robot-set-row{display:grid;grid-template-columns:1fr 1fr;gap:34%;padding:8px 11%;background:#2a3035}.robot-set-row.top-sets{margin-bottom:7px;border-bottom:1px dashed #596168}.robot-set-row.bottom-sets{margin-top:7px;border-top:1px dashed #596168}.robot-set{min-width:115px;padding:6px 8px;border:1px solid #b58c32;border-radius:4px;background:#3b3b32;text-align:center}.robot-set>strong{display:block;margin-bottom:5px;color:#e2c768;font-size:8px;letter-spacing:.08em}.robot-set>div{display:flex;align-items:center;justify-content:center;gap:0}.robot-set span{width:34px;padding:5px 0;border:1px solid #d1a92f;color:#171b1f;background:#ffc928;font-size:8px;font-weight:900}.robot-set span:first-child{border-radius:4px 0 0 4px}.robot-set span:last-child{margin-left:-1px;border-radius:0 4px 4px 0}.robot-set i{display:none}.cross-aisle{padding-right:12%;padding-left:12%}
.robot-set{width:84px;min-width:0;justify-self:center;padding:5px 6px}.robot-set>strong{margin-bottom:4px;font-size:7px;letter-spacing:.04em}.robot-set span{width:31px;padding:4px 0}
.parking-map-page .map-legend .used{border-color:#6f9588!important;background:#49675d!important}.parking-map-page .parking-bank .parking-space.occupied{border-color:#6f9588!important;background-color:#49675d!important}.parking-map-page .parking-bank .parking-space.occupied:hover,.parking-map-page .parking-bank .parking-space.occupied:focus,.parking-map-page .parking-bank .parking-space.occupied:active{border-color:#83aa9c!important;background-color:#557469!important}.parking-map-page :deep(.parking-space.has-car-number>span){color:#e3f1ec!important;font-weight:700!important;text-shadow:none!important}
.parking-map-page :deep(.parking-space.has-car-number>span){width:calc(100% - 2px)!important;max-width:calc(100% - 2px)!important;padding:0!important;font-size:5px!important;letter-spacing:-.3px!important;white-space:nowrap!important;overflow:visible!important;transform:translate(-50%,-50%)!important}.parking-map-page :deep(.parking-space.medium-car-number>span){font-size:4.5px!important;letter-spacing:-.4px!important}.parking-map-page :deep(.parking-space.long-car-number>span){font-size:4px!important;letter-spacing:-.5px!important}
.parking-map-page :deep(.parking-space.has-car-number>.car-number-lines){width:100%!important;height:100%!important;max-width:100%!important;position:absolute!important;inset:0!important;display:flex!important;flex-direction:column;align-items:center;justify-content:center;gap:4px;padding:0 1px!important;box-sizing:border-box;font-size:inherit!important;letter-spacing:0!important;line-height:1!important;text-align:center;white-space:normal!important;transform:none!important}.parking-map-page :deep(.car-number-lines b){display:block;color:#e3f1ec;font-size:11px;font-weight:800;line-height:1;letter-spacing:-.35px;text-align:center;white-space:nowrap}
.map-summary{grid-template-columns:repeat(5,minmax(0,1fr))}.map-summary .reserved strong{color:#8fc1e8}.map-legend .reserved{border-color:#7098b8!important;background:#38556c!important}.parking-map-page :deep(.parking-space.reserved){border-color:#7098b8!important;background-color:#38556c!important}.parking-map-page :deep(.parking-space.reserved:hover),.parking-map-page :deep(.parking-space.reserved:focus){border-color:#9fc9e8!important;background-color:#45677f!important}.parking-map-page :deep(.parking-space.reserved>.reservation-label){width:100%!important;height:100%!important;position:absolute!important;inset:0!important;display:flex!important;flex-direction:column;align-items:center;justify-content:center;gap:3px;color:#e4f3ff!important;transform:none!important}.parking-map-page :deep(.reservation-label b){font-size:9px;line-height:1}.parking-map-page :deep(.reservation-label small){display:block!important;position:static!important;color:#b9d7eb!important;font-size:7px!important;line-height:1}
@media(max-width:900px){.map-summary{grid-template-columns:repeat(2,minmax(0,1fr))}}
.access-layout{position:relative}.robot-simulation-layer{position:absolute;z-index:30;inset:0;pointer-events:none;overflow:visible}.moving-robot-pair{width:76px;height:28px;position:absolute;left:0;top:0;display:flex;align-items:center;justify-content:center;contain:layout style;backface-visibility:hidden;filter:drop-shadow(0 3px 4px rgba(0,0,0,.55));will-change:transform}.moving-robot-pair .sim-robot{width:27px;height:22px;display:grid;place-items:center;border:1px solid #e6bd34;color:#171b1f;background:#ffc928;font-size:8px;font-weight:900}.moving-robot-pair .sim-robot:first-child{border-radius:3px 0 0 3px}.moving-robot-pair .sim-robot:nth-last-child(2),.moving-robot-pair .sim-robot:last-of-type{border-radius:0 3px 3px 0}.moving-robot-pair>small{position:absolute;top:29px;left:50%;padding:2px 5px;border:1px solid #596168;color:#e0e4e7;background:#242a2f;font-size:7px;line-height:1;white-space:nowrap;transform:translateX(-50%)}.moving-robot-pair .sim-vehicle{min-width:38px;height:22px;display:grid;place-items:center;padding:0 3px;border-top:1px solid #f0a1a6;border-bottom:1px solid #f0a1a6;color:#fff4f4;background:#a64f56;font-size:6px;font-weight:900;white-space:nowrap}.moving-robot-pair.loaded .sim-robot{border-color:#e98990;color:#fff;background:#b85c64}.moving-robot-pair.lifting .sim-robot,.moving-robot-pair.lowering .sim-robot{animation:lift-pulse .75s ease-in-out infinite alternate}.moving-robot-pair.positioning{animation:positioning-pulse .65s ease-in-out infinite alternate}.moving-robot-pair.waiting>small{border-color:#d1aa38;color:#ffe08a}.robot-set.simulating>div{opacity:.18}.waiting-space .waiting-car{display:flex!important;align-items:center;justify-content:center;gap:3px;color:#fff0ae!important;font-size:7px!important;white-space:nowrap}.waiting-car i{width:8px;height:8px;flex:0 0 8px;border:1px solid #ffe178;background:#d6ae31}.waiting-space.occupied{box-shadow:inset 0 0 0 1px rgba(255,225,120,.25)}@keyframes lift-pulse{from{filter:brightness(1)}to{filter:brightness(1.35)}}@keyframes positioning-pulse{from{filter:drop-shadow(0 3px 4px rgba(0,0,0,.55))}to{filter:drop-shadow(0 0 7px #ffc928)}}
.robot-set-row.top-sets{margin-bottom:0}.top-transfer-aisle{height:58px;position:relative;display:flex;align-items:center;justify-content:center;gap:12px;margin:4px 0 5px;padding:0 12%;border-right:2px solid #e8d36c;border-left:2px solid #e8d36c;background:#272d32;overflow:hidden}.top-transfer-aisle::before{content:"";position:absolute;right:0;left:0;top:50%;border-top:2px dashed #68727a}.top-transfer-aisle .direction{z-index:1;position:absolute;color:#d8c968;font-size:20px}.top-transfer-aisle .direction:first-child{left:22%}.top-transfer-aisle .direction:last-child{right:22%}.top-transfer-aisle .cross-line{display:none}.top-transfer-aisle strong{z-index:1;padding:5px 15px;color:#d8c45e;background:#272d32;font-size:8px;font-weight:900;letter-spacing:.12em;white-space:nowrap}
.parking-map-page :deep(.parking-space.has-car-number>.car-number-lines){gap:2px}.parking-map-page :deep(.car-number-lines .parking-elapsed){display:block!important;color:#c8ded6!important;font-size:6px!important;font-weight:600!important;line-height:1!important;white-space:nowrap!important}
.space-detail-action{margin-top:14px;padding-top:12px;border-top:1px solid #505960}.space-detail-action button{width:100%;padding:9px 10px;border:1px solid #d0aa37;color:#171b1f;background:#ffc928;font-size:11px;font-weight:900;cursor:pointer}.space-detail-action button:disabled{border-color:#596168;color:#8f999f;background:#2b3035;cursor:not-allowed}.space-detail-action small{display:block;margin-top:7px;color:#9da6ad;font-size:9px;line-height:1.4;text-align:center}
</style>
