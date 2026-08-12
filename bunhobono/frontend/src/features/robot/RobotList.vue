<template>
  <section class="robot-list-page management-list-page">
    <ManagementFeedbackToast
      :message="feedbackMessage"
      :type="feedbackType"
    />

    <div class="robot-list-header management-list-header">
      <div>
        <h2 class="management-list-title">로봇 관리</h2>
        <p>등록된 주차로봇의 현재 상태를 확인합니다.</p>
      </div>

      <div class="robot-list-actions">
      
        <button
          type="button"
          class="robot-add-button"
          @click="openRegisterDialog">
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
                :disabled="deleting"
                @click="requestDelete(robot)">
                {{ deletingRobotNo === robot.robotNo ? "삭제 중" : "삭제" }}
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

    <dialog
      ref="registerDialog"
      class="robot-register-dialog"
      @close="resetRegisterForm"
      @click="closeOnBackdrop">
      <form
        class="robot-register-form"
        @submit.prevent="submitRobot">
        <header class="dialog-header">
          <div>
            <h3>주차로봇 등록</h3>
            <p>로봇의 식별 코드와 세트 구성을 입력합니다.</p>
          </div>

          <button
            type="button"
            class="dialog-close-button"
            aria-label="등록 창 닫기"
            @click="closeRegisterDialog">
            ×
          </button>
        </header>

        <div class="robot-register-fields">
          <label>
            <span>로봇 코드</span>
            <input
              v-model.trim="robotForm.robotCode"
              type="text"
              maxlength="20"
              placeholder="예: ROBOT-09"
              required
            />
          </label>

          <label>
            <span>세트 번호</span>
            <input
              v-model.number="robotForm.setNo"
              type="number"
              min="1"
              step="1"
              placeholder="예: 5"
              required
            />
          </label>

          <fieldset>
            <legend>세트 위치</legend>

            <div class="set-position-options">
              <label>
                <input
                  v-model="robotForm.setPosition"
                  type="radio"
                  value="A"
                />
                <span>A</span>
              </label>

              <label>
                <input
                  v-model="robotForm.setPosition"
                  type="radio"
                  value="B"
                />
                <span>B</span>
              </label>
            </div>
          </fieldset>
        </div>

        <footer class="dialog-actions">
          <button
            type="button"
            class="cancel-button"
            :disabled="registering"
            @click="closeRegisterDialog">
            취소
          </button>

          <button
            type="submit"
            class="submit-button"
            :disabled="registering">
            {{ registering ? "등록 중" : "등록" }}
          </button>
        </footer>
      </form>
    </dialog>

    <ManagementDeleteConfirm
      :open="deleteConfirmOpen"
      title="주차로봇 삭제"
      :item-name="selectedRobot?.robotCode || '선택한 로봇'"
      message="등록된 로봇을 삭제하시겠습니까?"
      caution="작업 중이거나 원시 로그가 있는 로봇은 삭제할 수 없습니다."
      :deleting="deleting"
      @cancel="cancelDelete"
      @confirm="confirmDelete"
    />
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import ManagementDeleteConfirm
  from "@/shared/components/ManagementDeleteConfirm.vue";
import ManagementFeedbackToast
  from "@/shared/components/ManagementFeedbackToast.vue";
import { useRobotStore } from "./robotStore";

const router = useRouter();
const robotStore = useRobotStore();

const registerDialog = ref(null);
const registering = ref(false);
const deleteConfirmOpen = ref(false);
const deleting = ref(false);
const deletingRobotNo = ref(null);
const selectedRobot = ref(null);
const feedbackMessage = ref("");
const feedbackType = ref("success");

const createEmptyRobot = () => ({
  robotCode: "",
  setNo: null,
  setPosition: "A"
});

const robotForm = ref(createEmptyRobot());

let refreshTimer = null;
let feedbackTimer = null;
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

const showFeedback = (message, type = "success") => {
  feedbackMessage.value = message;
  feedbackType.value = type;

  window.clearTimeout(feedbackTimer);
  feedbackTimer = window.setTimeout(() => {
    feedbackMessage.value = "";
  }, 2500);
};

const openRegisterDialog = () => {
  robotForm.value = createEmptyRobot();
  registerDialog.value?.showModal();
};

const closeRegisterDialog = () => {
  if (!registering.value) {
    registerDialog.value?.close();
  }
};

const resetRegisterForm = () => {
  robotForm.value = createEmptyRobot();
};

const closeOnBackdrop = (event) => {
  if (event.target === registerDialog.value) {
    closeRegisterDialog();
  }
};

const requestDelete = (robot) => {
  selectedRobot.value = robot;
  deleteConfirmOpen.value = true;
};

const cancelDelete = () => {
  if (deleting.value) return;

  deleteConfirmOpen.value = false;
  selectedRobot.value = null;
};

const confirmDelete = async () => {
  if (deleting.value || !selectedRobot.value) return;

  const robot = selectedRobot.value;
  deleting.value = true;
  deletingRobotNo.value = robot.robotNo;

  try {
    const success = await robotStore.remove(robot.robotNo);

    if (!success) {
      showFeedback(
        "작업 중이거나 사용 이력이 있는 로봇은 삭제할 수 없습니다.",
        "error"
      );
      return;
    }

    deleteConfirmOpen.value = false;
    selectedRobot.value = null;
    showFeedback(`${robot.robotCode} 로봇을 삭제했습니다.`);
  } catch (error) {
    console.error("로봇 삭제 실패", error);
    showFeedback(
      error.response?.data?.message
        || "로봇을 삭제하지 못했습니다.",
      "error"
    );
  } finally {
    deleting.value = false;
    deletingRobotNo.value = null;
  }
};

const submitRobot = async () => {
  if (registering.value) return;

  registering.value = true;

  try {
    const success = await robotStore.signup({
      robotCode: robotForm.value.robotCode,
      setNo: Number(robotForm.value.setNo),
      setPosition: robotForm.value.setPosition
    });

    if (!success) {
      showFeedback("로봇 등록에 실패했습니다.", "error");
      return;
    }

    const registeredCode = robotForm.value.robotCode;
    registerDialog.value?.close();
    showFeedback(`${registeredCode} 로봇을 등록했습니다.`);
  } catch (error) {
    console.error("로봇 등록 실패", error);
    showFeedback(
      error.response?.data?.message
        || "로봇 코드 또는 세트 위치가 이미 등록되어 있습니다.",
      "error"
    );
  } finally {
    registering.value = false;
  }
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
  window.clearTimeout(feedbackTimer);
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

.robot-add-button {
  color: #171b1f;
  background: var(--admin-accent);
}

.robot-add-button:hover {
  color: #171b1f !important;
  background: #ffd85b !important;
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

.robot-delete-button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.empty-row {
  height: 70px !important;
  color: var(--admin-muted) !important;
}

.robot-register-dialog {
  width: min(480px, calc(100% - 32px));
  padding: 0;
  border: 1px solid var(--admin-line);
  border-radius: 0;
  color: var(--admin-ink);
  background: var(--admin-surface);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
}

.robot-register-dialog::backdrop {
  background: rgba(10, 13, 16, 0.72);
}

.robot-register-form {
  width: auto !important;
  max-width: none !important;
  padding: 0 !important;
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.dialog-header {
  min-height: 66px;
  padding: 13px 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--admin-line);
  background: var(--admin-surface-muted);
}

.dialog-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: var(--admin-ink);
}

.dialog-header p {
  margin: 0;
  font-size: 11px;
  color: var(--admin-muted);
}

.dialog-close-button {
  width: 30px;
  height: 30px;
  min-height: 30px;
  padding: 0;
  border: 1px solid var(--admin-line);
  border-radius: 0;
  font-size: 20px;
  line-height: 1;
  color: var(--admin-ink);
  background: var(--admin-surface);
}

.robot-register-fields {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(110px, 0.6fr);
  gap: 14px;
}

.robot-register-fields > label,
.robot-register-fields fieldset {
  min-width: 0;
  margin: 0;
}

.robot-register-fields > label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.robot-register-fields > label > span,
.robot-register-fields legend {
  font-size: 12px;
  font-weight: 700;
  color: var(--admin-muted);
}

.robot-register-fields input[type="text"],
.robot-register-fields input[type="number"] {
  width: 100%;
  height: 34px;
  padding: 0 9px;
  border: 1px solid var(--admin-line);
  border-radius: 0;
  color: var(--admin-ink);
  background: #252b30;
}

.robot-register-fields fieldset {
  grid-column: 1 / -1;
  padding: 0;
  border: 0;
}

.set-position-options {
  margin-top: 6px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.set-position-options label {
  position: relative;
  cursor: pointer;
}

.set-position-options input {
  position: absolute;
  opacity: 0;
}

.set-position-options span {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-line);
  color: var(--admin-muted);
  background: #252b30;
}

.set-position-options input:checked + span {
  border-color: var(--admin-accent);
  color: #171b1f;
  background: var(--admin-accent);
}

.dialog-actions {
  padding: 10px 16px 14px;
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  border-top: 1px solid var(--admin-line);
}

.dialog-actions button {
  height: 30px;
  min-height: 30px;
  padding: 0 14px;
  border-radius: 0;
  font-size: 12px;
}

.dialog-actions .cancel-button {
  color: var(--admin-ink);
  background: #343a40;
}

.dialog-actions .submit-button {
  color: #171b1f;
  background: var(--admin-accent);
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

  .robot-register-fields {
    grid-template-columns: 1fr;
  }
}
</style>
