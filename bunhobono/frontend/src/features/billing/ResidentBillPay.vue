<template>
  <section class="resident-bill-page">
    <header>
      <h2>방문차량 주차요금 결제</h2>
      <button type="button" @click="goNotifications">알림으로 돌아가기</button>
    </header>

    <p v-if="loading" class="page-state">고지서를 불러오는 중입니다.</p>
    <p v-else-if="errorMessage" class="page-state error">{{ errorMessage }}</p>

    <article v-else-if="bill" class="bill-card">
      <dl>
        <div><dt>고지서 번호</dt><dd>{{ bill.billNo }}</dd></div>
        <div><dt>차량번호</dt><dd>{{ bill.carNo }}</dd></div>
        <div><dt>과금시간</dt><dd>{{ bill.chargeMinutes }}분</dd></div>
        <div><dt>발생일시</dt><dd>{{ formatDateTime(bill.issuedAt) }}</dd></div>
        <div class="amount"><dt>결제금액</dt><dd>{{ formatAmount(bill.billAmount) }}</dd></div>
        <div><dt>결제상태</dt><dd>{{ bill.billStatus === "PAID" ? "결제완료" : "미결제" }}</dd></div>
      </dl>

      <button
        v-if="bill.billStatus !== 'PAID'"
        type="button"
        class="pay-button"
        :disabled="paymentLoading"
        @click="requestPayment"
      >
        {{ paymentLoading ? "결제창 여는 중" : "결제하기" }}
      </button>
      <p v-else class="paid-message">이미 결제가 완료된 고지서입니다.</p>
    </article>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ANONYMOUS, loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { getResidentBill } from "./billingApi";

const route = useRoute();
const router = useRouter();
const tossClientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
const bill = ref(null);
const loading = ref(true);
const paymentLoading = ref(false);
const errorMessage = ref("");

onMounted(async () => {
  try {
    const response = await getResidentBill(Number(route.params.billNo));
    bill.value = response.data;
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "고지서를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
});

const requestPayment = async () => {
  if (!tossClientKey || !bill.value?.paymentOrderId || Number(bill.value?.billAmount) <= 0) {
    errorMessage.value = "결제 정보를 확인할 수 없습니다.";
    return;
  }

  paymentLoading.value = true;
  try {
    const tossPayments = await loadTossPayments(tossClientKey);
    const payment = tossPayments.payment({ customerKey: ANONYMOUS });
    const billNo = bill.value.billNo;

    await payment.requestPayment({
      method: "CARD",
      amount: { currency: "KRW", value: Number(bill.value.billAmount) },
      orderId: bill.value.paymentOrderId,
      orderName: `${bill.value.carNo} 주차요금`,
      successUrl: `${window.location.origin}/resident/billing/${billNo}/success`,
      failUrl: `${window.location.origin}/resident/billing/${billNo}/fail`,
      card: { flowMode: "DEFAULT", useEscrow: false, useCardPoint: false, useAppCardOnly: false }
    });
  } catch (error) {
    errorMessage.value = error.message || "결제창을 실행하지 못했습니다.";
    paymentLoading.value = false;
  }
};

const goNotifications = () => router.push({ path: "/resident/vehicles", query: { mode: "notification" } });
const formatAmount = (value) => `${Number(value || 0).toLocaleString("ko-KR")}원`;
const formatDateTime = (value) => value ? new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "-";
</script>

<style scoped>
.resident-bill-page { width: min(900px, calc(100% - 48px)); margin: 0 auto; color: #111; }
.resident-bill-page header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.resident-bill-page header h2 { margin: 0; }
.resident-bill-page button { min-height: 42px; padding: 9px 18px; border: 1px solid #d5dde4; border-radius: 8px; background: #fff; cursor: pointer; font-weight: 700; }
.bill-card { padding: 32px; border: 1px solid #d5dde4; border-radius: 14px; background: #fff; }
.bill-card dl { margin: 0; display: grid; grid-template-columns: repeat(2, 1fr); gap: 0; border: 1px solid #dce3e8; }
.bill-card dl div { padding: 18px; display: grid; grid-template-columns: 120px 1fr; border-bottom: 1px solid #dce3e8; }
.bill-card dt { color: #666; font-weight: 700; }
.bill-card dd { margin: 0; font-weight: 800; }
.bill-card .amount dd { color: #23a6d5; font-size: 22px; }
.bill-card .pay-button { width: 100%; margin-top: 24px; border-color: #23a6d5; color: #fff; background: #23a6d5; }
.page-state { padding: 60px; text-align: center; }.page-state.error { color: #c33; }
.paid-message { margin: 24px 0 0; color: #087443; text-align: center; font-weight: 800; }
@media (any-pointer: coarse) and (max-width: 820px), (any-pointer: coarse) and (max-height: 820px){.resident-bill-page{width:calc(100% - 24px)}.bill-card dl{grid-template-columns:1fr}.resident-bill-page header{align-items:flex-start;flex-direction:column;gap:12px}}
</style>
