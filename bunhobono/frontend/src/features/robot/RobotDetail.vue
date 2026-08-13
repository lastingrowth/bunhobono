<template>
  <main class="robot-detail-page">
    <ManagementFeedbackToast
      :message="feedbackMessage"
      :type="feedbackType"
    />

    <!-- 로봇 기본 정보 -->
    <section class="info-detail-card robot-detail-card">
      <header class="info-detail-header">
        <div>
          <span class="info-detail-category">
            PARKING ROBOT
          </span>
          <h2>{{ robot?.robotCode ?? "로봇 상세" }}</h2>
        </div>

        <div class="header-actions">
          <button
            type="button"
            :disabled="completing"
            @click="completeMaintenance">
            {{ completing ? "처리 중" : "점검 완료" }}
          </button>

          <button type="button" @click="goList">
            목록으로
          </button>
        </div>
      </header>

      <p
        v-if="robotStore.loading && !robot"
        class="detail-message">
        로봇 정보를 불러오는 중입니다.
      </p>

      <p
        v-else-if="robotStore.errorMessage"
        class="detail-message error">
        {{ robotStore.errorMessage }}
      </p>

      <template v-else-if="robot">
        <section class="robot-current-summary">
          <div>
            <span>현재 상태</span>
            <strong
              class="robot-status"
              :class="statusClass(robot.robotStatus)">
              {{ statusText(robot.robotStatus) }}
            </strong>
          </div>

          <div>
            <span>배터리</span>
            <strong>{{ formatBattery(robot.batteryLevel) }}</strong>
          </div>

          <div>
            <span>소속 세트</span>
            <strong>
              SET {{ robot.setNo }} / {{ robot.setPosition }}
            </strong>
          </div>

          <div>
            <span>최근 통신</span>
            <strong>
              {{ formatDateTime(robot.lastHeartbeatAt) }}
            </strong>
          </div>
        </section>

        <dl class="info-detail-list robot-detail-list">
          <div
            v-for="row in detailRows"
            :key="row.label">
            <dt>{{ row.label }}</dt>
            <dd>{{ row.value }}</dd>
          </div>
        </dl>
      </template>
    </section>

    <!-- 가장 최근 원시 상태값 -->
    <section class="robot-log-card">
      <header class="robot-log-header">
        <div>
          <h3>최근 원시 데이터</h3>
          <p>
            로봇 작업 중 마지막으로 전송된 상태값입니다.
          </p>
        </div>

        <time v-if="latestLog">
          {{ formatDateTime(latestLog.sampledAt) }}
        </time>
      </header>

      <p
        v-if="robotStore.logErrorMessage"
        class="detail-message error">
        {{ robotStore.logErrorMessage }}
      </p>

      <div v-else-if="latestLog" class="raw-data-grid">
        <div
          v-for="item in latestRawData"
          :key="item.label"
          class="raw-data-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <p v-else class="empty-log-message">
        이 로봇에서 전송된 원시 데이터가 없습니다.
      </p>
    </section>

    <!-- 최근 원시 상태 로그 -->
    <section class="robot-log-card">
      <header class="robot-log-header">
        <div>
          <h3>원시 데이터 전송 로그</h3>
          <p>최근 전송된 원시 데이터 10건입니다.</p>
        </div>
      </header>

      <div class="robot-log-table-wrap">
        <table class="robot-log-table">
          <thead>
            <tr>
              <th>전송 시각</th>
              <th>작업 번호</th>
              <th>작업 단계</th>
              <th>적재 상태</th>
              <th>모터 온도</th>
              <th>모터 전류</th>
              <th>진동</th>
              <th>배터리</th>
              <th>알람</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="log in recentLogs"
              :key="log.robotLogNo">
              <td>{{ formatDateTime(log.sampledAt) }}</td>
              <td>{{ log.taskNo ?? "-" }}</td>
              <td>{{ phaseText(log.taskPhase) }}</td>
              <td>{{ payloadText(log.payloadState) }}</td>
              <td>
                {{ formatNumber(
                  log.driveMotorTemperatureC,
                  1,
                  "℃"
                ) }}
              </td>
              <td>
                {{ formatNumber(
                  log.driveMotorCurrentA,
                  2,
                  "A"
                ) }}
              </td>
              <td>
                {{ formatNumber(
                  log.driveVibrationMmS,
                  2,
                  "mm/s"
                ) }}
              </td>
              <td>{{ formatBattery(log.batteryLevel) }}</td>
              <td>{{ log.alarmCode || "-" }}</td>
            </tr>

            <tr v-if="recentLogs.length === 0">
              <td colspan="9" class="empty-row">
                전송된 원시 데이터가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>

<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref
} from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";

import ManagementFeedbackToast
  from "@/shared/components/ManagementFeedbackToast.vue";

import { useRobotStore } from "./robotStore";

const route = useRoute();
const router = useRouter();
const robotStore = useRobotStore();

const {
  detail: robot,
  logs
} = storeToRefs(robotStore);

const completing = ref(false);
const feedbackMessage = ref("");
const feedbackType = ref("success");

let refreshTimer = null;
let feedbackTimer = null;
let refreshing = false;

const statusLabels = {
  STANDBY: "대기",
  WORKING: "작업 중",
  CHARGING: "충전 중",
  LOW_BATTERY: "배터리 부족",
  WARNING: "주의",
  ERROR: "오류",
  OFFLINE: "연결 끊김"
};

const phaseLabels = {
  WAITING: "작업 대기",
  TRAFFIC_WAIT_EMPTY: "빈 로봇 통행 대기",
  MOVING_TO_PICKUP: "차량 위치로 이동",
  PICKUP_POSITIONING: "차량 인양 위치 조정",
  LIFTING: "차량 리프팅",
  TRAFFIC_WAIT_LOADED: "차량 적재 통행 대기",
  MOVING_TO_DROPOFF: "목적지로 이동",
  DROPOFF_POSITIONING: "주차 위치 조정",
  LOWERING: "차량 내려놓기",
  COMPLETED: "작업 완료",
  FAILED: "작업 실패"
};

const payloadLabels = {
  EMPTY: "미적재",
  LOADED: "차량 적재"
};

const robotNo = computed(() => {
  return Number(route.params.robotNo);
});

const latestLog = computed(() => {
  return logs.value[0] ?? null;
});

const recentLogs = computed(() => {
  return logs.value.slice(0, 10);
});

const statusText = (status) => {
  return statusLabels[status] ?? status ?? "-";
};

const statusClass = (status) => {
  return String(status ?? "").toLowerCase();
};

const phaseText = (phase) => {
  return phaseLabels[phase] ?? phase ?? "-";
};

const payloadText = (payloadState) => {
  return payloadLabels[payloadState]
    ?? payloadState
    ?? "-";
};

const formatBattery = (value) => {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${Number(value).toFixed(1)}%`;
};

const formatOperatingHours = (value) => {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${Number(value).toFixed(1)}시간`;
};

const formatNumber = (
  value,
  digits = 1,
  unit = ""
) => {
  if (value === null || value === undefined) {
    return "-";
  }

  return `${Number(value).toFixed(digits)}${unit}`;
};

const formatDateTime = (value) => {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString("ko-KR");
};

const formatMaintenanceDays = (value) => {
  if (
    value === null
    || value === undefined
    || !robot.value?.lastMaintenanceAt
  ) {
    return "점검 기록 없음";
  }

  return `${value}일`;
};

const detailRows = computed(() => {
  if (!robot.value) {
    return [];
  }

  return [
    {
      label: "로봇 번호",
      value: robot.value.robotNo
    },
    {
      label: "로봇 코드",
      value: robot.value.robotCode
    },
    {
      label: "소속 세트",
      value: `SET ${robot.value.setNo}`
    },
    {
      label: "세트 위치",
      value: robot.value.setPosition
    },
    {
      label: "누적 운전시간",
      value: formatOperatingHours(
        robot.value.operatingHours
      )
    },
    {
      label: "최근 점검일",
      value: formatDateTime(
        robot.value.lastMaintenanceAt
      )
    },
    {
      label: "점검 후 경과일",
      value: formatMaintenanceDays(
        robot.value.daysSinceMaintenance
      )
    },
    {
      label: "등록 일시",
      value: formatDateTime(robot.value.createdAt)
    },
    {
      label: "최근 수정 일시",
      value: formatDateTime(robot.value.updatedAt)
    }
  ];
});

const latestRawData = computed(() => {
  if (!latestLog.value) {
    return [];
  }

  const log = latestLog.value;

  return [
    {
      label: "작업 번호",
      value: log.taskNo ?? "-"
    },
    {
      label: "작업 단계",
      value: phaseText(log.taskPhase)
    },
    {
      label: "적재 상태",
      value: payloadText(log.payloadState)
    },
    {
      label: "모터 온도",
      value: formatNumber(
        log.driveMotorTemperatureC,
        1,
        "℃"
      )
    },
    {
      label: "모터 전류",
      value: formatNumber(
        log.driveMotorCurrentA,
        2,
        "A"
      )
    },
    {
      label: "주행부 진동",
      value: formatNumber(
        log.driveVibrationMmS,
        2,
        "mm/s"
      )
    },
    {
      label: "배터리 전압",
      value: formatNumber(
        log.batteryVoltageV,
        2,
        "V"
      )
    },
    {
      label: "배터리 온도",
      value: formatNumber(
        log.batteryTemperatureC,
        1,
        "℃"
      )
    },
    {
      label: "배터리 잔량",
      value: formatBattery(log.batteryLevel)
    },
    {
      label: "장애물 감지",
      value: log.obstacleDetected ? "감지" : "정상"
    },
    {
      label: "안전 정지",
      value: log.safetyStop ? "정지" : "정상"
    },
    {
      label: "알람 코드",
      value: log.alarmCode || "-"
    }
  ];
});

const showFeedback = (
  message,
  type = "success"
) => {
  feedbackMessage.value = message;
  feedbackType.value = type;

  window.clearTimeout(feedbackTimer);

  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = "";
  }, 2500);
};

const goList = () => {
  router.push("/admin/robots");
};

const refreshDetail = async () => {
  if (refreshing || !robotNo.value) {
    return;
  }

  refreshing = true;

  try {
    await robotStore.loadDetailData(robotNo.value);
  } finally {
    refreshing = false;
  }
};

const completeMaintenance = async () => {
  if (completing.value) {
    return;
  }

  completing.value = true;

  try {
    const success =
      await robotStore.completeMaintenance(
        robotNo.value
      );

    showFeedback(
      success
        ? "로봇 점검을 완료했습니다."
        : "로봇 점검 처리에 실패했습니다.",
      success ? "success" : "error"
    );
  } catch (error) {
    console.error("로봇 점검 완료 실패", error);

    showFeedback(
      "로봇 점검 처리에 실패했습니다.",
      "error"
    );
  } finally {
    completing.value = false;
  }
};

onMounted(async () => {
  await refreshDetail();

  refreshTimer = window.setInterval(
    refreshDetail,
    2000
  );
});

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer);
  window.clearTimeout(feedbackTimer);
  robotStore.clearDetail();
});
</script>

<style scoped>
.robot-detail-page {
  width: 100%;
  display: grid;
  gap: 12px;
}

.robot-detail-card,
.robot-log-card {
  width: 100%;
  max-width: none;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 6px;
}

.robot-current-summary {
  display: grid;
  grid-template-columns:
    0.8fr 0.8fr 1fr 1.5fr;
  border-bottom: 1px solid var(--admin-line);
  background: var(--admin-surface-muted);
}

.robot-current-summary > div {
  min-width: 0;
  min-height: 64px;
  padding: 9px 14px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  flex-direction: column;
  gap: 5px;
  border-right: 1px solid var(--admin-line);
}

.robot-current-summary > div:last-child {
  border-right: 0;
}

.robot-current-summary span {
  font-size: 11px;
  color: var(--admin-muted);
}

.robot-current-summary strong {
  font-size: 14px;
  color: var(--admin-ink);
}

.robot-status {
  min-width: 72px;
  padding: 3px 7px;
  display: inline-block;
  border: 1px solid #737b82;
  text-align: center;
  color: #e5e7eb;
  background: #454c52;
}

.robot-status.working {
  border-color: #5b88b2;
  color: #d8ecff;
  background: #334c63;
}

.robot-status.charging {
  border-color: #4f8c6b;
  color: #d9f7e6;
  background: #315641;
}

.robot-status.warning {
  border-color: #d3a92e;
  color: #ffe9a6;
  background: #655525;
}

.robot-status.low_battery {
  border-color: #d3a92e;
  color: #f0cf6b;
  background: #4b4329;
}

.robot-status.error {
  border-color: #c45a60;
  color: #ffdadd;
  background: #66383c;
}

.robot-status.offline {
  color: #aeb6bd;
  background: #30363b;
}

.robot-detail-list {
  padding: 0 18px 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.robot-detail-list > div {
  min-height: 42px;
  padding: 5px 10px;
}

.robot-log-card {
  border: 1px solid var(--admin-line);
  background: var(--admin-surface);
}

.robot-log-header {
  min-height: 52px;
  padding: 9px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--admin-line);
}

.robot-log-header h3 {
  margin: 0 0 3px;
  font-size: 16px;
}

.robot-log-header p,
.robot-log-header time {
  margin: 0;
  font-size: 11px;
  color: var(--admin-muted);
}

.raw-data-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.raw-data-item {
  min-width: 0;
  min-height: 54px;
  padding: 8px 10px;
  display: flex;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  border-right: 1px solid var(--admin-line);
  border-bottom: 1px solid var(--admin-line);
}

.raw-data-item:nth-child(6n) {
  border-right: 0;
}

.raw-data-item span {
  font-size: 10px;
  color: var(--admin-muted);
}

.raw-data-item strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--admin-ink);
}

.robot-log-table-wrap {
  overflow-x: auto;
}

.robot-log-table {
  min-width: 980px;
  table-layout: fixed;
  box-shadow: none !important;
}

.robot-log-table th,
.robot-log-table td {
  height: 29px;
  padding: 2px 5px;
  text-align: center;
  font-size: 11px;
  white-space: nowrap;
}

.detail-message,
.empty-log-message {
  margin: 0;
  padding: 18px;
  color: var(--admin-muted);
}

.detail-message.error {
  color: #ff8c91;
}

.empty-row {
  height: 60px !important;
  color: var(--admin-muted) !important;
}

@media (max-width: 900px) {
  .robot-current-summary,
  .raw-data-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .raw-data-item:nth-child(6n) {
    border-right: 1px solid var(--admin-line);
  }

  .raw-data-item:nth-child(2n) {
    border-right: 0;
  }
}

@media (max-width: 700px) {
  .info-detail-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .robot-detail-list {
    grid-template-columns: 1fr;
  }
}
</style>
