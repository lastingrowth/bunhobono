<template>
  <Teleport to="body">
    <div class="action-dialog-backdrop" @click.self="$emit('cancel')">
      <section class="action-dialog" role="dialog" aria-modal="true" aria-labelledby="pdm-action-title">
        <header>
          <div>
            <span>PREDICTIVE MAINTENANCE ACTION</span>
            <h2 id="pdm-action-title">{{ readOnly ? '조치내용 확인' : '조치 완료' }}</h2>
          </div>
          <button type="button" class="close-button" :disabled="loading" aria-label="닫기" @click="$emit('cancel')">×</button>
        </header>

        <dl>
          <div><dt>장비</dt><dd>{{ equipmentName }}</dd></div>
          <div><dt>위험 발생</dt><dd>{{ formatDateTime(record?.predictedAt) }}</dd></div>
          <div><dt>위험 확률</dt><dd>{{ formatPercent(record?.criticalProbability) }}</dd></div>
          <div v-if="readOnly"><dt>조치 완료</dt><dd>{{ formatDateTime(record?.actionCompletedAt) }}</dd></div>
        </dl>

        <section v-if="abnormalSensors.length" class="sensor-section">
          <strong>기준 이탈 센서 ({{ abnormalSensors.length }}건)</strong>
          <div class="sensor-grid">
            <div v-for="sensor in abnormalSensors" :key="sensor.key" :class="{ priority: sensor.key === prioritySensor?.key }">
              <span>{{ sensor.label }}</span>
              <b>{{ formatSensorValue(sensor) }}</b>
            </div>
          </div>
        </section>

        <aside class="inspection-guide">
          <strong>우선 점검 안내</strong>
          <p>{{ inspectionGuide }}</p>
        </aside>

        <label for="pdm-action-note">조치 내용</label>
        <textarea
          id="pdm-action-note"
          v-model="actionNote"
          maxlength="500"
          rows="5"
          :disabled="loading"
          :readonly="readOnly"
          placeholder="점검 및 조치한 내용을 입력하세요."
          @keydown.ctrl.enter="submit"
        ></textarea>
        <small v-if="!readOnly">{{ actionNote.length }} / 500자</small>

        <footer>
          <button type="button" class="secondary" :disabled="loading" @click="$emit('cancel')">{{ readOnly ? '닫기' : '취소' }}</button>
          <button v-if="!readOnly" type="button" :disabled="loading" @click="submit">
            {{ loading ? '처리 중' : '조치 완료' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref } from 'vue';

const emit = defineEmits(['cancel', 'submit']);
const props = defineProps({
  record: { type: Object, required: true },
  equipmentName: { type: String, required: true },
  loading: { type: Boolean, default: false },
  readOnly: { type: Boolean, default: false },
  equipmentType: { type: String, required: true },
});
const actionNote = ref(props.readOnly ? (props.record?.actionNote || '등록된 조치내용이 없습니다.') : '');
const SENSOR_CONFIG = {
  CAMERA: {
    temperature_c: { label: '장비 온도', unit: '℃', direction: 'high', threshold: 35, action: '냉각팬·통풍구와 장비 과열 여부를 가장 먼저 점검하세요.' },
    voltage_v: { label: '공급 전압', unit: 'V', direction: 'deviation', target: 12, tolerance: 0.5, action: '전원 공급장치와 배선의 전압 불안정을 가장 먼저 점검하세요.' },
    success_rate: { label: '처리 성공률', unit: '%', direction: 'low', threshold: 90, priorityEligible: false, action: '영상 처리 및 통신 연결 상태를 확인하세요.' },
    error_count: { label: '오류 횟수', unit: '회', direction: 'high', threshold: 3, priorityEligible: false, action: '오류 로그와 영상 처리 모듈을 확인하세요.' },
    days_since_maintenance: { label: '정비 경과일', unit: '일', direction: 'high', threshold: 90, action: '정기점검 누락 여부와 소모 부품을 가장 먼저 확인하세요.' },
  },
  GATE: {
    motor_temperature_c: { label: '모터 온도', unit: '℃', direction: 'high', threshold: 55, action: '구동 모터의 과열과 냉각 상태를 가장 먼저 점검하세요.' },
    motor_current_a: { label: '모터 전류', unit: 'A', direction: 'high', threshold: 7.5, action: '게이트 걸림과 모터 과부하 여부를 가장 먼저 점검하세요.' },
    voltage_v: { label: '공급 전압', unit: 'V', direction: 'deviation', target: 24, tolerance: 1, action: '전원 공급장치와 배선 상태를 가장 먼저 점검하세요.' },
    vibration_mm_s: { label: '진동', unit: 'mm/s', direction: 'high', threshold: 4, action: '구동축·베어링의 풀림과 마모를 가장 먼저 점검하세요.' },
    open_close_time_sec: { label: '개폐 시간', unit: '초', direction: 'high', threshold: 4.5, action: '레일 걸림과 구동부 마찰 상태를 가장 먼저 점검하세요.' },
    operation_count: { label: '누적 동작', unit: '회', direction: 'high', threshold: 60000, action: '구동부 소모품과 교체 주기를 가장 먼저 확인하세요.' },
    error_count: { label: '오류 횟수', unit: '회', direction: 'high', threshold: 3, priorityEligible: false, action: '게이트 제어기 오류 로그를 확인하세요.' },
    days_since_maintenance: { label: '정비 경과일', unit: '일', direction: 'high', threshold: 60, action: '정기점검 누락 여부와 구동부 소모품을 우선 확인하세요.' },
  },
  ROBOT: {
    drive_motor_temperature_c: { label: '주행 모터 온도', unit: '℃', direction: 'high', threshold: 60, action: '주행 모터의 과열과 냉각 상태를 가장 먼저 점검하세요.' },
    drive_motor_current_a: { label: '주행 모터 전류', unit: 'A', direction: 'high', threshold: 12, action: '주행부 걸림과 모터 과부하 여부를 가장 먼저 점검하세요.' },
    drive_vibration_mm_s: { label: '주행부 진동', unit: 'mm/s', direction: 'high', threshold: 4, action: '휠·구동축·베어링의 풀림과 마모를 가장 먼저 점검하세요.' },
    battery_voltage_v: { label: '배터리 전압', unit: 'V', direction: 'low', threshold: 45, action: '배터리 충전 상태와 전원 연결부를 가장 먼저 점검하세요.' },
    battery_temperature_c: { label: '배터리 온도', unit: '℃', direction: 'high', threshold: 45, action: '배터리 과열 여부와 냉각 상태를 가장 먼저 점검하세요.' },
    days_since_maintenance: { label: '정비 경과일', unit: '일', direction: 'high', threshold: 45, action: '정기점검 누락 여부와 주행부 소모품을 우선 확인하세요.' },
  },
};
const severity = (value, config) => {
  if (config.direction === 'low') return Math.max(0, (config.threshold - value) / Math.max(config.threshold, 1));
  if (config.direction === 'deviation') return Math.abs(value - config.target) / config.tolerance;
  return Math.max(0, (value - config.threshold) / Math.max(config.threshold, 1));
};
const sensorEntries = computed(() => {
  const config = SENSOR_CONFIG[props.equipmentType] || {};
  return Object.entries(props.record?.sensorValues || {}).map(([key, rawValue]) => {
    const item = config[key] || { label: key, unit: '', direction: 'high', threshold: Number.MAX_VALUE };
    const value = Number(rawValue);
    return { key, value, ...item, severity: severity(value, item) };
  });
});
const abnormalSensors = computed(() => sensorEntries.value
  .filter((sensor) => sensor.severity > 0)
  .sort((left, right) => right.severity - left.severity));
const prioritySensor = computed(() => {
  const causeSensors = abnormalSensors.value.filter((sensor) => sensor.priorityEligible !== false);
  return causeSensors[0] || abnormalSensors.value[0] || null;
});
const inspectionGuide = computed(() => {
  const sensor = prioritySensor.value;
  if (sensor) {
    return `1순위: ${sensor.label} ${formatSensorValue(sensor)}. ${sensor.action}`;
  }
  if (sensorEntries.value.length) {
    return '개별 센서는 설정 기준 안에 있지만 여러 센서의 복합 패턴이 위험으로 판정됐습니다. 장비 운전을 중지하고 구동부와 전원 계통을 종합 점검하세요.';
  }
  return '저장된 센서값이 없습니다. 장비 운전을 중지하고 주요 구동부와 전원 계통을 우선 점검하세요.';
});

const submit = () => { if (!props.readOnly) emit('submit', actionNote.value.trim()); };
const formatDateTime = (value) => value ? new Date(value).toLocaleString('ko-KR') : '-';
const formatPercent = (value) => value == null ? '-' : `${(Number(value) * 100).toFixed(1)}%`;
const formatSensorValue = (sensor) => `${Number(sensor.value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}${sensor.unit}`;
</script>

<style scoped>
.action-dialog-backdrop { position: fixed; inset: 0; z-index: 3000; display: grid; place-items: center; padding: 20px; background: rgba(10,14,18,.72); }
.action-dialog { width: min(460px,100%); border: 1px solid var(--admin-line, #46515c); color: var(--admin-ink, #f1f5f9); background: var(--admin-surface, #202a33); box-shadow: 0 18px 55px rgba(0,0,0,.55); }
header { padding: 14px 16px; display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid var(--admin-line, #46515c); }
header span { color: var(--admin-muted, #a7b1bb); font-size: 9px; font-weight: 800; letter-spacing: .12em; }
h2 { margin: 4px 0 0; font-size: 18px; }
.close-button { width: 30px; height: 30px; padding: 0; border: 1px solid var(--admin-line, #46515c); color: var(--admin-muted, #a7b1bb); background: var(--admin-surface-muted, #2a3540); font-size: 20px; cursor: pointer; }
dl { margin: 0; padding: 12px 16px; display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 8px; }
dl div { min-width: 0; padding: 8px; border: 1px solid var(--admin-line, #46515c); background: var(--admin-surface-muted, #2a3540); }
dt { color: var(--admin-muted, #a7b1bb); font-size: 9px; }
dd { margin: 4px 0 0; overflow: visible; font-size: 11px; font-weight: 700; line-height: 1.45; overflow-wrap: anywhere; white-space: normal; }
.sensor-section { margin: 0 16px 12px; }
.sensor-section > strong { display: block; margin-bottom: 6px; color: var(--admin-muted, #a7b1bb); font-size: 11px; }
.sensor-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; }
.sensor-grid div { padding: 7px 9px; display: flex; justify-content: space-between; gap: 8px; border: 1px solid var(--admin-line, #46515c); background: var(--admin-surface-muted, #2a3540); }
.sensor-grid div.priority { border-color: #b8862f; background: #3b3424; }
.sensor-grid span { overflow: hidden; color: var(--admin-muted, #a7b1bb); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.sensor-grid b { color: var(--admin-ink, #f1f5f9); font-size: 10px; white-space: nowrap; }
.inspection-guide { margin: 0 16px 12px; padding: 11px 12px; border: 1px solid #b8862f; color: #ffe4a3; background: #3b3424; }
.inspection-guide strong { display: block; font-size: 11px; }
.inspection-guide p { margin: 5px 0 0; color: #f2d99c; font-size: 11px; line-height: 1.55; }
label { margin: 0 16px 6px; display: block; color: var(--admin-muted, #a7b1bb); font-size: 11px; font-weight: 700; }
textarea { box-sizing: border-box; width: calc(100% - 32px); margin: 0 16px; padding: 10px; resize: vertical; border: 1px solid var(--admin-line, #46515c); outline: none; color: var(--admin-ink, #f1f5f9); background: var(--admin-surface-muted, #2a3540); font: inherit; }
textarea:focus { border-color: #5b88b2; }
.action-dialog > small { margin: 5px 16px 0; display: block; color: var(--admin-muted, #a7b1bb); text-align: right; font-size: 9px; }
footer { margin-top: 12px; padding: 12px 16px; display: flex; justify-content: flex-end; gap: 6px; border-top: 1px solid var(--admin-line, #46515c); }
footer button { min-height: 32px; padding: 5px 13px; border: 1px solid #5b88b2; color: #d8ecff; background: #334c63; font-weight: 700; cursor: pointer; }
footer button.secondary { border-color: var(--admin-line, #46515c); color: var(--admin-ink, #f1f5f9); background: var(--admin-surface-muted, #2a3540); }
button:disabled,textarea:disabled { opacity: .55; cursor: default; }
@media(max-width:560px){dl{grid-template-columns:1fr}}
</style>
