<template>
  <section class="robot-list-page management-list-page">
    <div class="robot-list-header management-list-header">
      <div>
        <h2 class="management-list-title">로봇 관리</h2>
        <p>등록된 주차로봇의 현재 상태를 확인합니다.</p>
      </div>

      <div class="robot-list-actions">
      
        <button type="button" class="robot-add-button" disabled>
          로봇 추가
        </button>
      </div>
    </div>

    <p
      v-if="robotStore.loading && robotStore.list.length === 0"
      class="robot-message">
      로봇 목록을 불러오는 중입니다.
    </p>

    <p
      v-else-if="robotStore.errorMessage"
      class="robot-message error">
      {{ robotStore.errorMessage }}
    </p>

    <div v-else class="robot-table-wrap management-list-table">
      <table>
        <colgroup>
          <col class="col-number">
          <col class="col-code">
          <col class="col-set">
          <col class="col-status">
          <col class="col-battery">
          <col class="col-hours">
          <col class="col-heartbeat">
          <col class="col-action">
        </colgroup>

        <thead>
          <tr>
            <th>번호</th>
            <th>로봇 코드</th>
            <th>세트 구성</th>
            <th>현재 상태</th>
            <th>배터리</th>
            <th>누적 운전시간</th>
            <th>최근 통신</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="robot in robotStore.list"
            :key="robot.robotNo">
            <td>{{ robot.robotNo }}</td>

            <td>
              <button
                type="button"
                class="robot-code-button"
                @click="goDetail(robot.robotNo)">
                {{ robot.robotCode }}
              </button>
            </td>

            <td>SET {{ robot.setNo }} / {{ robot.setPosition }}</td>

            <td>
              <span
                class="robot-status"
                :class="statusClass(robot.robotStatus)">
                {{ statusText(robot.robotStatus) }}
              </span>
            </td>

            <td>{{ formatBattery(robot.batteryLevel) }}</td>

            <td>{{ formatOperatingHours(robot.operatingHours) }}</td>

            <td :title="formatDateTime(robot.lastHeartbeatAt)">
              {{ formatRelativeTime(robot.lastHeartbeatAt) }}
            </td>

            <td>
              <button
                type="button"
                class="robot-delete-button"
                disabled>
                삭제
              </button>
            </td>
          </tr>

          <tr v-if="robotStore.list.length === 0">
            <td colspan="8" class="empty-row">
              등록된 로봇이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRobotStore } from "./robotStore";

const router = useRouter();
const robotStore = useRobotStore();

let refreshTimer = null;
let refreshing = false;

const statusLabels = {
  STANDBY: "대기",
  WORKING: "작업 중",
  CHARGING: "충전 중",
  WARNING: "주의",
  ERROR: "오류",
  OFFLINE: "연결 끊김"
};

const statusText = (status) => {
  return statusLabels[status] ?? status ?? "-";
};

const statusClass = (status) => {
  return String(status ?? "").toLowerCase();
};

const formatBattery = (value) => {
  if (value === null || value === undefined) return "-";
  return `${Number(value).toFixed(1)}%`;
};

const formatOperatingHours = (value) => {
  if (value === null || value === undefined) return "-";
  return `${Number(value).toFixed(1)}시간`;
};

const formatDateTime = (value) => {
  if (!value) return "-";

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString("ko-KR");
};

const formatRelativeTime = (value) => {
  if (!value) return "-";

  const time = new Date(value).getTime();

  if (Number.isNaN(time)) return "-";

  const seconds = Math.max(
    0,
    Math.floor((Date.now() - time) / 1000)
  );

  if (seconds < 5) return "방금 전";
  if (seconds < 60) return `${seconds}초 전`;

  const minutes = Math.floor(seconds / 60);

  if (minutes < 60) return `${minutes}분 전`;

  const hours = Math.floor(minutes / 60);

  if (hours < 24) return `${hours}시간 전`;

  return `${Math.floor(hours / 24)}일 전`;
};

const goDetail = (robotNo) => {
  router.push(`/admin/robots/${robotNo}`);
};

const refreshList = async () => {
  if (refreshing) return;

  refreshing = true;

  try {
    await robotStore.loadList();
  } finally {
    refreshing = false;
  }
};

onMounted(async () => {
  await refreshList();
  refreshTimer = window.setInterval(refreshList, 2000);
});

onBeforeUnmount(() => {
  window.clearInterval(refreshTimer);
});
</script>

<style scoped>
.robot-list-header,
.robot-list-actions {
  display: flex;
  align-items: center;
}

.robot-list-header {
  justify-content: space-between;
}

.robot-list-header p {
  margin: 2px 0 0;
  color: var(--admin-muted);
  font-size: 12px;
}

.robot-list-actions {
  gap: 6px;
}

.robot-count,
.robot-add-button {
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--admin-line);
  border-radius: 0;
  font-size: 12px;
}

.robot-count {
  display: inline-flex;
  align-items: center;
  font-weight: 700;
  color: var(--admin-ink);
  background: var(--admin-surface);
}

.robot-message {
  padding: 14px;
  border: 1px solid var(--admin-line);
  color: var(--admin-muted);
  background: var(--admin-surface);
}

.robot-message.error {
  color: #ff8c91;
}

.robot-table-wrap {
  overflow-x: auto;
}

.robot-table-wrap table {
  table-layout: fixed;
}

.col-number { width: 7%; }
.col-code { width: 11%; }
.col-set { width: 12%; }
.col-status { width: 12%; }
.col-battery { width: 9%; }
.col-hours { width: 13%; }
.col-heartbeat { width: 25%; }
.col-action { width: 11%; }

.robot-table-wrap th,
.robot-table-wrap td {
  height: 30px;
  padding: 2px 5px;
  text-align: center;
  font-size: 12px;
  line-height: 1.2;
}

.robot-code-button {
  min-height: 0;
  padding: 0;
  border: 0;
  font-weight: 800;
  text-decoration: underline;
  color: var(--admin-accent);
  background: transparent;
}

.robot-status {
  min-width: 56px;
  padding: 2px 5px;
  display: inline-block;
  border: 1px solid #737b82;
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

.robot-status.error {
  border-color: #c45a60;
  color: #ffdadd;
  background: #66383c;
}

.robot-status.offline {
  color: #aeb6bd;
  background: #30363b;
}

.robot-delete-button {
  height: 21px;
  min-height: 21px;
  padding: 0 8px;
  border: 1px solid #805156;
  border-radius: 0;
  font-size: 11px;
  color: #ffb5b9;
  background: #513237;
}

.empty-row {
  height: 70px !important;
  color: var(--admin-muted) !important;
}

@media (max-width: 800px) {
  .robot-list-header {
    align-items: flex-start;
    gap: 8px;
    flex-direction: column;
  }

  .robot-table-wrap table {
    min-width: 720px;
  }
}
</style>