<template>
  <section class="resident-bill-page">
    <header>
      <h2>방문차량 주차요금 결제</h2>
      <button type="button" @click="goNotifications">알림으로 돌아가기</button>
    </header>

    <p v-if="loading" class="page-state">고지서를 불러오는 중입니다.</p>
    <p v-else-if="errorMessage" class="page-state error">{{ errorMessage }}</p>

    <article v-else-if="bill" class="bill-card">
      <dl class="bill-summary">
        <div class="bill-summary-item">
          <dt>차량번호</dt>
          <dd class="car-number">{{ bill.carNo }}</dd>
        </div>
        <div class="bill-summary-item">
          <dt>과금시간</dt>
          <dd>{{ formatChargeTime(bill.chargeMinutes) }}</dd>
        </div>
        <div class="bill-summary-item amount">
          <dt>결제금액</dt>
          <dd>{{ formatAmount(bill.billAmount) }}</dd>
          <span
            class="payment-status"
            :class="bill.billStatus === 'PAID' ? 'paid' : 'unpaid'"
          >
            {{ bill.billStatus === "PAID" ? "결제완료" : "미결제" }}
          </span>
        </div>
      </dl>

      <button
        v-if="bill.billStatus !== 'PAID'"
        type="button"
        class="pay-button"
        :disabled="paymentLoading || Number(bill.billAmount) <= 0"
        @click="requestPayment"
      >
        {{
          paymentLoading
            ? "결제창 여는 중"
            : Number(bill.billAmount) <= 0
              ? "0원 정산 대기"
              : "결제하기"
        }}
      </button>

      <p
        v-if="bill.billStatus === 'UNPAID' && Number(bill.billAmount) <= 0"
        class="free-billing-guide"
      >
        출차 전 키오스크에서 0원 정산을 완료해주세요.
      </p>

      <p
        v-if="bill.billStatus === 'PAID'"
        class="paid-message"
      >
        이미 결제가 완료된 고지서입니다.
      </p>
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
const formatChargeTime = (value) => {
  const totalMinutes = Math.max(0, Math.floor(Number(value) || 0));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  return `${String(hours).padStart(2, "0")}시간 ${String(minutes).padStart(2, "0")}분`;
};
</script>

<style scoped>
.resident-bill-page { width: min(900px, calc(100% - 48px)); margin: 0 auto; color: #111; }
.resident-bill-page header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.resident-bill-page header h2 { margin: 0; color: #172f42; font-size: 25px; }
.resident-bill-page button { min-height: 42px; padding: 9px 18px; border: 1px solid #d4e1e9; border-radius: 10px; color: #3c586b; background: #fff; cursor: pointer; font-weight: 700; transition: color .16s ease, border-color .16s ease, background-color .16s ease; }
.resident-bill-page header button:hover { border-color: #55acd3; color: #168ab8; background: #f3fbfe; }
.bill-card { padding: 30px; border: 1px solid #d7e4ec; border-radius: 18px; background: rgba(255, 255, 255, .97); box-shadow: 0 14px 34px rgba(37, 78, 104, .08); }
.bill-card .bill-summary { margin: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.bill-card .bill-summary-item { min-width: 0; min-height: 88px; padding: 18px 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; border: 1px solid #deeaf1; border-radius: 13px; background: #f8fbfd; text-align: center; }
.bill-card dt { color: #718695; font-size: 13px; font-weight: 700; }
.bill-card dd { margin: 0; color: #203a4d; font-size: 19px; font-weight: 800; }
.bill-card .car-number { font-size: 23px; letter-spacing: .025em; }
.bill-card .amount { min-height: 104px; grid-column: 1 / -1; border-color: #bfe5f3; background: linear-gradient(135deg, #f5fcff, #eaf8fd); }
.bill-card .amount dd { color: #149dcc; font-size: 28px; }
.bill-card .payment-status { padding: 7px 13px; border-radius: 999px; font-size: 14px; font-weight: 800; }
.bill-card .payment-status.paid { color: #087443; background: #e3f6ec; }
.bill-card .payment-status.unpaid { color: #b55315; background: #fff0dc; }
.bill-card .pay-button { width: 100%; min-height: 50px; margin-top: 20px; border-color: #20a4d2; color: #fff; background: linear-gradient(135deg, #2ab0db, #189acb); box-shadow: 0 9px 20px rgba(32, 164, 210, .2); }
.bill-card .pay-button:hover { border-color: #168fbd; color: #fff; background: linear-gradient(135deg, #20a4d2, #1188b7); }
.bill-card .pay-button:disabled { border-color: #cbd5dc; color: #71818c; background: #e8eef2; box-shadow: none; cursor: not-allowed; }
.free-billing-guide { margin: 14px 0 0; color: #647985; text-align: center; font-weight: 700; }
.page-state { padding: 60px; text-align: center; }.page-state.error { color: #c33; }
.paid-message { margin: 24px 0 0; color: #087443; text-align: center; font-weight: 800; }
@media (any-pointer: coarse) and (max-width: 820px), (any-pointer: coarse) and (max-height: 820px){.resident-bill-page{width:calc(100% - 24px)}.bill-card{padding:20px}.bill-card .bill-summary{grid-template-columns:1fr}.bill-card .amount{grid-column:auto}.resident-bill-page header{align-items:flex-start;flex-direction:column;gap:12px}}
</style>
