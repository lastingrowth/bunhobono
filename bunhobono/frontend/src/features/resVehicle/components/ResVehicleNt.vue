<template>
  <div class="vehicle-notification">
    <div class="notification-header">
      <h3>차량 알림</h3>

      <span class="notification-count">
        총 {{ notifications.length }}건
      </span>
    </div>

    <div class="notification-table-wrap">
      <table>
        <thead>
          <tr>
            <th>구분</th>
            <th>차량번호</th>
            <th>발신자</th>
            <th>알림 내용</th>
            <th>발생시간</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="notification in notifications"
            :key="notification.vehicleNtNo"
          >
            <td>
              <span
                class="notification-type"
                :class="notificationTypeClass(
                  notification.notificationType
                )"
              >
                {{ notificationTypeText(
                  notification.notificationType
                ) }}
              </span>
            </td>

            <td class="car-number">
              {{ notification.snapshotCarNo || "-" }}
            </td>

            <td class="sender-name">
              {{ notification.senderName || "시스템" }}
            </td>

            <td class="notification-message">
              {{ notification.message }}
            </td>

            <td class="created-at">
              {{ formatDateTime(notification.createdAt) }}
            </td>

            <td class="notification-manage">
              <button
                v-if="
                  notification.notificationType
                    !== 'VISIT_OVERDUE_EXIT'
                "
                type="button"
                class="delete-notification-button"
                @click="openDeleteDialog(notification)"
              >
                삭제
              </button>

              <span
                v-else
                class="protected-notification"
              >
                보관
              </span>
            </td>
          </tr>

          <tr v-if="notifications.length === 0">
            <td colspan="6" class="empty-message">
              도착한 차량 알림이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <dialog
      ref="deleteDialog"
      class="delete-dialog"
      @cancel.prevent="closeDeleteDialog"
    >
      <div class="delete-dialog-content">
        <div class="delete-dialog-heading">
          <span class="delete-dialog-indicator"></span>
          <h4>알림 삭제</h4>
        </div>

        <p>선택한 알림을 삭제하시겠습니까?</p>

        <div class="delete-dialog-actions">
          <button
            type="button"
            @click="closeDeleteDialog"
          >
            취소
          </button>

          <button
            type="button"
            class="delete-confirm-button"
            @click="confirmDelete"
          >
            삭제
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup>
import { ref } from "vue"

defineProps({
  notifications: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(["delete"])

const deleteDialog = ref(null)
const selectedVehicleNtNo = ref(null)

function notificationTypeText(type) {
  const typeNames = {
    ADMIN_APPROVED: "관리자 승인",
    ADMIN_REJECTED: "관리자 반려",
    APPROVAL_TIMEOUT: "승인시간 초과",
    NO_ENTRY_EXPIRED: "미입차 만기",
    VISIT_OVERDUE: "주차시간 초과",
    VISIT_OVERDUE_EXIT: "초과 후 출차"
  }

  return typeNames[type] || type || "-"
}

function notificationTypeClass(type) {
  const typeClasses = {
    ADMIN_APPROVED: "type-approved",
    ADMIN_REJECTED: "type-rejected",
    APPROVAL_TIMEOUT: "type-timeout",
    NO_ENTRY_EXPIRED: "type-expired",
    VISIT_OVERDUE: "type-overdue",
    VISIT_OVERDUE_EXIT: "type-exit"
  }

  return typeClasses[type] || "type-default"
}

function openDeleteDialog(notification) {
  selectedVehicleNtNo.value = notification.vehicleNtNo
  deleteDialog.value?.showModal()
}

function closeDeleteDialog() {
  deleteDialog.value?.close()
  selectedVehicleNtNo.value = null
}

function confirmDelete() {
  const vehicleNtNo = selectedVehicleNtNo.value

  closeDeleteDialog()

  if (vehicleNtNo !== null) {
    emit("delete", vehicleNtNo)
  }
}

function formatDateTime(value) {
  if (!value) {
    return "-"
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value).replace("T", " ")
  }

  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hour = pad(date.getHours())
  const minute = pad(date.getMinutes())

  return `${year}.${month}.${day} ${hour}:${minute}`
}

function pad(value) {
  return String(value).padStart(2, "0")
}
</script>

<style scoped>
.vehicle-notification { width: 100%; }
.notification-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.notification-header h3 { margin: 0; }
.notification-count { color: var(--text-muted); font-size: 14px; }
.notification-table-wrap { width: 100%; overflow-x: auto; }

table { width: 100%; border-collapse: collapse; table-layout: fixed; }
th, td { box-sizing: border-box; padding: 12px 8px; border: 1px solid var(--border-color); text-align: left; vertical-align: middle; }
th { background: var(--bg-soft); font-weight: 700; white-space: nowrap; }

th:nth-child(1) { width: 96px; }
th:nth-child(2) { width: 90px; }
th:nth-child(3) { width: 64px; }
th:nth-child(5) { width: 130px; }
th:nth-child(6) { width: 66px; }

.notification-type { display: inline-flex; min-height: 27px; align-items: center; justify-content: center; padding: 4px 7px; border-radius: 6px; font-size: 12px; font-weight: 700; white-space: nowrap; }
.type-approved { color: #087443; background: #d9f5e7; }
.type-rejected, .type-timeout { color: #b42318; background: #fee4e2; }
.type-expired, .type-overdue { color: #b54708; background: #fef0c7; }
.type-exit { color: #175cd3; background: #dbeafe; }
.type-default { color: var(--text-muted); background: var(--bg-soft); }

.car-number { font-weight: 700; white-space: nowrap; }
.sender-name { white-space: nowrap; }
.notification-message { line-height: 1.5; word-break: keep-all; overflow-wrap: break-word; }
.created-at { color: var(--text-muted); white-space: nowrap; }
.notification-manage { padding: 8px 6px; text-align: center; }

.delete-notification-button { display: inline-flex; width: 50px; min-width: 50px; align-items: center; justify-content: center; padding: 7px 5px; white-space: nowrap; word-break: keep-all; writing-mode: horizontal-tb; }
.protected-notification { color: var(--text-muted); font-size: 12px; white-space: nowrap; }
.empty-message { padding: 28px; color: var(--text-muted); text-align: center; }

.delete-dialog { width: min(360px, calc(100vw - 32px)); margin: auto; padding: 0; border: 1px solid #ccd8e3; border-radius: 8px; background: #fff; box-shadow: 0 20px 55px rgba(22, 43, 64, .25); }
.delete-dialog::backdrop { background: rgba(24, 39, 54, .45); }
.delete-dialog-content { padding: 24px; }
.delete-dialog-heading { display: flex; align-items: center; gap: 9px; }
.delete-dialog-indicator { width: 4px; height: 21px; border-radius: 2px; background: #db4b4b; }
.delete-dialog-content h4 { margin: 0; color: #1d354b; font-size: 19px; }
.delete-dialog-content p { margin: 20px 0 24px; color: #5a6f82; }
.delete-dialog-actions { display: flex; justify-content: flex-end; gap: 8px; }
.delete-dialog-actions button { display: inline-flex; min-width: 72px; height: 38px; align-items: center; justify-content: center; border: 1px solid #ccd7e1; border-radius: 6px; background: #fff; cursor: pointer; white-space: nowrap; }
.delete-dialog-actions .delete-confirm-button { border-color: #db4b4b; color: #fff; background: #db4b4b; }
.delete-dialog-actions .delete-confirm-button:hover { background: #c83d3d; }

@media (max-width: 700px) {
  table { min-width: 680px; }
}
</style>