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

            <td>
              {{ notification.senderName || "시스템" }}
            </td>

            <td class="notification-message">
              {{ notification.message }}
            </td>

            <td class="created-at">
              {{ formatDateTime(notification.createdAt) }}
            </td>
          </tr>

          <tr v-if="notifications.length === 0">
            <td colspan="5" class="empty-message">
              도착한 차량 알림이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
defineProps({
  notifications: {
    type: Array,
    default: () => []
  }
})

function notificationTypeText(type) {
  const typeNames = {
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
    ADMIN_REJECTED: "type-rejected",
    APPROVAL_TIMEOUT: "type-timeout",
    NO_ENTRY_EXPIRED: "type-expired",
    VISIT_OVERDUE: "type-overdue",
    VISIT_OVERDUE_EXIT: "type-exit"
  }

  return typeClasses[type] || "type-default"
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
th, td { padding: 12px 14px; border: 1px solid var(--border-color); text-align: left; vertical-align: middle; }
th { background: var(--bg-soft); font-weight: 700; white-space: nowrap; }

th:nth-child(1) { width: 130px; }
th:nth-child(2) { width: 120px; }
th:nth-child(3) { width: 100px; }
th:nth-child(5) { width: 150px; }

.notification-type { display: inline-flex; min-height: 28px; align-items: center; justify-content: center; padding: 4px 9px; border-radius: 6px; font-size: 13px; font-weight: 700; white-space: nowrap; }

.type-rejected, .type-timeout { color: #b42318; background: #fee4e2; }
.type-expired, .type-overdue { color: #b54708; background: #fef0c7; }
.type-exit { color: #175cd3; background: #dbeafe; }
.type-default { color: var(--text-muted); background: var(--bg-soft); }

.car-number { font-weight: 700; white-space: nowrap; }
.notification-message { line-height: 1.5; word-break: keep-all; }
.created-at { color: var(--text-muted); white-space: nowrap; }
.empty-message { padding: 28px; color: var(--text-muted); text-align: center; }

@media (max-width: 800px) {
  table { min-width: 760px; }
}
</style>