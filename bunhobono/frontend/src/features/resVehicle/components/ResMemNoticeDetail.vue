<template>
  <article v-if="notification" class="mem-notice-detail">
    <header>
      <div>
        <span class="notice-type">{{ typeText }}</span>
        <h3>{{ notification.title }}</h3>
      </div>
      <span class="read-state">{{ notification.readAt ? "확인 완료" : "새 알림" }}</span>
    </header>

    <dl>
      <div>
        <dt>발생시간</dt>
        <dd>{{ formatDateTime(notification.createdAt) }}</dd>
      </div>
      <div>
        <dt>확인시간</dt>
        <dd>{{ formatDateTime(notification.readAt) }}</dd>
      </div>
    </dl>

    <section class="notice-message">
      <h4>알림 내용</h4>
      <p>{{ notification.message }}</p>
    </section>

    <div class="detail-actions">
      <button type="button" class="delete" @click="$emit('delete', notification.memNoticeNo)">삭제</button>
      <button type="button" @click="$emit('back')">알림 목록</button>
      <button
        v-if="canPay"
        type="button"
        class="payment"
        @click="$emit('pay', notification.referenceNo)"
      >
        결제하기
      </button>
    </div>
  </article>

  <p v-else class="missing-notice">알림을 찾을 수 없습니다.</p>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({ notification: { type: Object, default: null } });
defineEmits(["back", "delete", "pay"]);

const canPay = computed(() => {
  return props.notification?.referenceTable === "bill"
    && props.notification?.noticeType === "VISIT_PARKING_FEE_ISSUED"
    && Number(props.notification?.referenceNo) > 0;
});

const typeText = computed(() => {
  if (props.notification?.noticeType === "VISIT_PARKING_FEE_ISSUED") return "주차요금";
  return props.notification?.noticeType || "알림";
});

const formatDateTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace("T", " ");
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(date);
};
</script>

<style scoped>
.mem-notice-detail { color: #111; }
.mem-notice-detail header { padding-bottom: 24px; display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--border-color); }
.mem-notice-detail h3 { margin: 10px 0 0; color: #111; font-size: 27px; }
.notice-type,.read-state { display: inline-flex; padding: 6px 10px; border-radius: 14px; color: #087d9f; background: #dff7fd; font-size: 12px; font-weight: 800; }
.read-state { color: #555; background: #eef1f3; }
.mem-notice-detail dl { margin: 0; padding: 24px 0; display: grid; grid-template-columns: repeat(3,1fr); gap: 20px; border-bottom: 1px solid var(--border-color); }
.mem-notice-detail dl div { display: grid; gap: 7px; }
.mem-notice-detail dt { color: #666; font-size: 13px; font-weight: 700; }
.mem-notice-detail dd { margin: 0; font-weight: 700; }
.notice-message { min-height: 180px; padding: 28px 4px; }
.notice-message h4 { margin: 0 0 18px; color: #111; font-size: 18px; }
.notice-message p { margin: 0; color: #222; font-size: 17px; line-height: 1.8; white-space: pre-wrap; }
.detail-actions { display: flex; justify-content: flex-end; gap: 10px; }
.detail-actions button { min-width: 100px; height: 42px; }
.detail-actions .delete { border-color: #db4b4b; color: #db4b4b; background: #fff; }
.detail-actions .payment { border-color: #23a6d5; color: #fff; background: #23a6d5; }
.missing-notice { padding: 80px 20px; color: #555; text-align: center; }
@media (any-pointer: coarse) and (max-width: 820px), (any-pointer: coarse) and (max-height: 820px){.mem-notice-detail dl{grid-template-columns:1fr}.mem-notice-detail header{flex-direction:column}.detail-actions button{flex:1}}
</style>
