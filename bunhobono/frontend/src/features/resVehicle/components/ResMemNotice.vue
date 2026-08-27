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
          <col class="col-title">
          <col class="col-created">
          <col class="col-manage">
        </colgroup>

        <thead>
          <tr>
            <th>번호</th>
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
            <td class="notification-mobile-summary">
              <span class="notification-type">
                {{ notification.referenceTable || "-" }}
              </span>
              <button type="button" @click="$emit('open-detail', notification)">
                <span v-if="notification.readAt == null" class="unread-dot" aria-label="읽지 않은 알림"></span>
                {{ notification.title }}
              </button>
              <small class="notification-mobile-created-at">
                {{ formatDateTime(notification.createdAt) }}
              </small>
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
.notification-mobile-summary { display: none; }
.notification-title { font-weight: 700; line-height: 1.45; text-align: left; white-space: normal; }
.notification-title button { width: 100%; min-width: 0; padding: 0; display: flex; align-items: flex-start; gap: 9px; overflow: visible; border: 0; color: inherit; font-weight: 700; line-height: 1.45; text-align: left; white-space: normal; overflow-wrap: anywhere; word-break: keep-all; background: transparent; cursor: pointer; }
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
@media (any-pointer: coarse) and (max-width: 820px),
       (any-pointer: coarse) and (max-height: 820px) {
  .member-notification {
    box-sizing: border-box;
    min-width: 0;
    max-width: 100%;
    padding: 18px 16px;
    overflow: hidden;
    border: 1px solid var(--border-color);
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 6px 18px rgba(35, 76, 112, .08);
  }
  .notification-table-wrap { min-width: 0; max-width: 100%; overflow: hidden; }
  table {
    display: block;
    width: 100% !important;
    min-width: 0 !important;
    max-width: 100% !important;
    table-layout: auto;
  }
  tbody { display: grid; width: 100%; min-width: 0; max-width: 100%; }
  colgroup, col, thead { display: none; width: 0 !important; }
  tbody { display: grid; gap: 0; }
  tbody tr {
    box-sizing: border-box;
    display: grid;
    grid-template-columns: 34px minmax(0, 1fr) 68px;
    grid-template-rows: auto;
    gap: 0 6px;
    width: 100%;
    min-width: 0;
    max-width: 100%;
    overflow: hidden;
    padding: 0;
    border: 0;
    border-top: 1px solid #e2e9ef;
    border-radius: 0;
    background: transparent;
  }
  tbody tr.unread { background: transparent; }
  tbody td { width: auto; min-width: 0; padding: 0; border: 0 !important; }
  .notification-index {
    grid-column: 1;
    grid-row: 1;
    align-self: center;
    height: 100%;
    display: grid;
    place-items: center;
    border-right: 1px solid #e2e9ef !important;
    font-size: 16px;
  }
  .notification-mobile-summary {
    display: grid;
    grid-column: 2;
    grid-row: 1;
    grid-template-columns: auto minmax(0, 1fr);
    min-height: 50px;
    align-content: center;
    align-items: center;
    gap: 6px;
    padding: 8px 0;
    text-align: left;
  }
  .notification-mobile-summary button {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 7px;
    padding: 0;
    overflow: visible;
    border: 0;
    color: #243746;
    background: transparent;
    font-weight: 800;
    text-align: left;
    cursor: pointer;
    white-space: normal;
    overflow-wrap: anywhere;
    word-break: keep-all;
  }
  .notification-mobile-summary .notification-type {
    grid-column: 1;
    grid-row: 1;
  }
  .notification-mobile-summary button {
    grid-column: 2;
    grid-row: 1;
  }
  .notification-mobile-created-at {
    grid-column: 1 / -1;
    grid-row: 2;
    color: #66798c;
    font-size: 11px;
    font-weight: 600;
    line-height: 1.3;
  }
  tbody tr > td:nth-child(3),
  tbody tr > .notification-title,
  tbody tr > .created-at { display: none; }
  tbody tr::before { display: none; }
  .created-at {
    grid-column: 3;
    grid-row: 1;
    align-self: center;
    box-sizing: border-box;
    min-height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px 0;
    color: #66798c;
    font-size: 10px;
    text-align: center;
    white-space: normal;
  }
  .created-at::before { display: none; }
  .notification-manage {
    grid-column: 3;
    grid-row: 1;
    align-self: center;
    justify-self: end;
    margin-right: 10px;
  }
  .delete-notification-button {
    border-color: #1677d2 !important;
    color: #ffffff !important;
    background: #1677d2 !important;
    font-weight: 800;
  }
  .empty-message { grid-column: 1 / -1; padding: 20px 4px; }
}
</style>
