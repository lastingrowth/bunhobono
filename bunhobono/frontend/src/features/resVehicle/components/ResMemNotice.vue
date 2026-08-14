<template>
  <div class="member-notification">
    <div class="notification-header">
      <h3>알림</h3>
      <span>총 {{ notifications.length }}건</span>
    </div>

    <div class="notification-table-wrap">
      <table>
        <colgroup>
          <col class="col-index">
          <col class="col-type">
          <col class="col-title">
          <col class="col-created">
          <col class="col-manage">
        </colgroup>

        <thead>
          <tr>
            <th>번호</th>
            <th>구분</th>
            <th>제목</th>
            <th>발생시간</th>
            <th>관리</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(notification, index) in notifications"
            :key="notification.memNoticeNo"
            :class="{ unread: notification.readAt == null }"
          >
            <td class="notification-index">
              {{ index + 1 }}
            </td>
            <td>
              <span class="notification-type">
                {{ notification.referenceTable || "-" }}
              </span>
            </td>
            <td class="notification-title">
              <button type="button" @click="$emit('open-detail', notification)">
                <span v-if="notification.readAt == null" class="unread-dot" aria-label="읽지 않은 알림"></span>
                {{ notification.title }}
              </button>
            </td>
            <td class="created-at">{{ formatDateTime(notification.createdAt) }}</td>
            <td class="notification-manage">
              <button
                type="button"
                class="delete-notification-button"
                @click="openDeleteDialog(notification)"
              >
                삭제
              </button>
            </td>
          </tr>

          <tr v-if="notifications.length === 0">
            <td colspan="5" class="empty-message">
              도착한 알림이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <dialog ref="deleteDialog" class="delete-dialog" @cancel.prevent="closeDeleteDialog">
      <div class="delete-dialog-content">
        <h4>알림 삭제</h4>
        <p>선택한 알림을 삭제하시겠습니까?</p>
        <div class="delete-dialog-actions">
          <button type="button" @click="closeDeleteDialog">취소</button>
          <button type="button" class="delete-confirm-button" @click="confirmDelete">삭제</button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup>
import { ref } from "vue";

defineProps({
  notifications: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(["open-detail", "delete"]);
const deleteDialog = ref(null);
const selectedMemNoticeNo = ref(null);

const openDeleteDialog = (notification) => {
  selectedMemNoticeNo.value = notification.memNoticeNo;
  deleteDialog.value?.showModal();
};

const closeDeleteDialog = () => {
  deleteDialog.value?.close();
  selectedMemNoticeNo.value = null;
};

const confirmDelete = () => {
  const memNoticeNo = selectedMemNoticeNo.value;
  closeDeleteDialog();

  if (memNoticeNo !== null) {
    emit("delete", memNoticeNo);
  }
};

const formatDateTime = (value) => {
  if (!value) return "-";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ");

  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
};
</script>

<style scoped>
.member-notification { width: 100%; }
.notification-header { margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.notification-header h3 { margin: 0; }
.notification-header span { color: var(--text-muted); font-size: 14px; }
.notification-table-wrap { width: 100%; overflow: visible; }
table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.col-index { width: 72px; }
.col-type { width: 130px; }
.col-title { width: auto; }
.col-created { width: 180px; }
.col-manage { width: 90px; }
th,td { box-sizing: border-box; padding: 15px 12px; border: 1px solid var(--border-color); color: #111; text-align: center; vertical-align: middle; }
th { background: var(--bg-soft); font-weight: 700; white-space: nowrap; }
tr.unread { background: #f2fbfe; }
.notification-type { display: inline-flex; padding: 4px 7px; border-radius: 6px; color: #087443; background: #d9f5e7; font-size: 12px; font-weight: 700; white-space: nowrap; }
.notification-index { color: #555; font-weight: 700; }
.notification-title { overflow: hidden; font-weight: 700; text-align: left; text-overflow: ellipsis; white-space: nowrap; }
.notification-title button { width: 100%; padding: 0; display: flex; align-items: center; gap: 9px; overflow: hidden; border: 0; color: inherit; text-align: left; text-overflow: ellipsis; background: transparent; cursor: pointer; }
.notification-title button { font-weight: 700; white-space: nowrap; }
.notification-title button:hover { color: var(--resident-accent); }
.unread-dot { width: 8px; height: 8px; flex: 0 0 8px; border-radius: 50%; background: var(--resident-accent); }
.created-at { color: #555; white-space: nowrap; }
.notification-manage { text-align: center; }
.delete-notification-button { width: 50px; padding: 7px 5px; white-space: nowrap; }
.empty-message { padding: 28px; color: #555; text-align: center; }
.delete-dialog { width: min(360px,calc(100vw - 32px)); margin: auto; padding: 0; border: 1px solid #ccd8e3; border-radius: 8px; background: #fff; }
.delete-dialog::backdrop { background: rgba(24,39,54,.45); }
.delete-dialog-content { padding: 24px; }
.delete-dialog-content h4 { margin: 0; color: #111; font-size: 19px; }
.delete-dialog-content p { margin: 20px 0 24px; color: #555; }
.delete-dialog-actions { display: flex; justify-content: flex-end; gap: 8px; }
.delete-dialog-actions button { min-width: 72px; height: 38px; border: 1px solid #ccd7e1; border-radius: 6px; background: #fff; cursor: pointer; }
.delete-dialog-actions .delete-confirm-button { border-color: #db4b4b; color: #fff; background: #db4b4b; }
@media (max-width:700px){.col-index{width:52px}.col-type{width:92px}.col-created{width:120px}.col-manage{width:68px}th,td{padding:12px 7px}.created-at{white-space:normal}}
</style>
