<template>
  <section class="purchase-page">
    <header class="purchase-header">
      <div>
        <h2>방문차량 등록 횟수 충전</h2>
        <p>필요한 추가 등록 횟수를 선택해 주세요.</p>
      </div>
      <button type="button" @click="goVehicles">차량 목록으로</button>
    </header>

    <div class="product-grid">
      <button
        v-for="product in products"
        :key="product.quantity"
        type="button"
        class="product-card"
        :class="{ selected: selectedQuantity === product.quantity }"
        @click="selectedQuantity = product.quantity"
      >
        <span v-if="product.badge" class="product-badge">{{ product.badge }}</span>
        <strong>{{ product.quantity }}회</strong>
        <b>{{ formatAmount(product.amount) }}</b>
        <small v-if="product.originalAmount">
          정상가 {{ formatAmount(product.originalAmount) }}
        </small>
        <small v-else>1회 기본 상품</small>
      </button>
    </div>

    <article class="purchase-summary">
      <div>
        <span>선택 상품</span>
        <strong>방문차량 추가 등록 {{ selectedProduct.quantity }}회</strong>
      </div>
      <div>
        <span>결제금액</span>
        <strong class="amount">{{ formatAmount(selectedProduct.amount) }}</strong>
      </div>
    </article>

    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <button
      type="button"
      class="payment-button"
      :disabled="paymentLoading"
      @click="requestPayment"
    >
      {{ paymentLoading ? "결제창 여는 중" : `${formatAmount(selectedProduct.amount)} 결제하기` }}
    </button>
  </section>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { ANONYMOUS, loadTossPayments } from "@tosspayments/tosspayments-sdk";
import { createMemPurchaseOrder } from "./memPurchaseApi";

const router = useRouter();
const tossClientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
const products = [
  { quantity: 1, amount: 5000, originalAmount: null, badge: "" },
  { quantity: 5, amount: 23000, originalAmount: 25000, badge: "5,000원 할인" },
  { quantity: 10, amount: 40000, originalAmount: 50000, badge: "7,000원 할인" }
];
const selectedQuantity = ref(5);
const paymentLoading = ref(false);
const errorMessage = ref("");
const selectedProduct = computed(() =>
  products.find((item) => item.quantity === selectedQuantity.value) || products[0]
);

const requestPayment = async () => {
  if (!tossClientKey) {
    errorMessage.value = "결제 환경설정을 확인할 수 없습니다.";
    return;
  }
  paymentLoading.value = true;
  errorMessage.value = "";
  try {
    const response = await createMemPurchaseOrder(selectedQuantity.value);
    const order = response.data;
    if (!order?.paymentOrderId || Number(order?.purchaseAmount) <= 0) {
      throw new Error("구매 주문정보를 확인할 수 없습니다.");
    }
    const tossPayments = await loadTossPayments(tossClientKey);
    const payment = tossPayments.payment({ customerKey: ANONYMOUS });
    await payment.requestPayment({
      method: "CARD",
      amount: { currency: "KRW", value: Number(order.purchaseAmount) },
      orderId: order.paymentOrderId,
      orderName: `방문차량 추가 등록 ${order.purchaseQuantity}회`,
      successUrl: `${window.location.origin}/resident/visit-credit/success`,
      failUrl: `${window.location.origin}/resident/visit-credit/fail`,
      card: {
        flowMode: "DEFAULT",
        useEscrow: false,
        useCardPoint: false,
        useAppCardOnly: false
      }
    });
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || "결제창을 실행하지 못했습니다.";
    paymentLoading.value = false;
  }
};

const goVehicles = () => router.push({ path: "/resident/vehicles", query: { mode: "list" } });
const formatAmount = (value) => `${Number(value || 0).toLocaleString("ko-KR")}원`;
</script>

<style scoped>
.purchase-page { width: min(960px, calc(100% - 48px)); margin: 0 auto; color: #111; }
.purchase-header { margin-bottom: 28px; display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.purchase-header h2 { margin: 0 0 8px; font-size: 32px; }.purchase-header p { margin: 0; color: #626b73; }
.purchase-header button { min-height: 44px; padding: 10px 18px; border: 1px solid #d5dde4; border-radius: 9px; background: #fff; cursor: pointer; font-weight: 800; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.product-card { position: relative; min-height: 190px; padding: 28px 18px 22px; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 10px; border: 2px solid #dce3e8; border-radius: 16px; color: #111; background: #fff; cursor: pointer; }
.product-card.selected { border-color: #23a6d5; background: #f0fbff; box-shadow: 0 8px 24px rgba(35,166,213,.15); }
.product-card > strong { font-size: 28px; }.product-card > b { color: #23a6d5; font-size: 24px; }.product-card > small { color: #707980; }
.product-badge { position: absolute; top: 12px; right: 12px; padding: 5px 9px; border-radius: 999px; color: #087443; background: #daf6e8; font-size: 12px; font-weight: 800; }
.purchase-summary { margin-top: 22px; padding: 22px 24px; display: grid; grid-template-columns: 1fr 1fr; gap: 20px; border: 1px solid #dce3e8; border-radius: 14px; background: #fff; }
.purchase-summary div { display: flex; justify-content: space-between; gap: 12px; }.purchase-summary span { color: #626b73; }.purchase-summary strong { font-weight: 900; }.purchase-summary .amount { color: #23a6d5; font-size: 20px; }
.payment-button { width: 100%; min-height: 56px; margin-top: 18px; border: 0; border-radius: 12px; color: #fff; background: #23a6d5; cursor: pointer; font-size: 18px; font-weight: 900; }
.payment-button:disabled { cursor: wait; opacity: .6; }.error-message { margin: 18px 0 0; color: #c33; text-align: center; font-weight: 700; }
@media (any-pointer: coarse) and (max-width: 820px), (any-pointer: coarse) and (max-height: 820px){.purchase-page{width:calc(100% - 24px)}.purchase-header{flex-direction:column}.product-grid{grid-template-columns:1fr}.product-card{min-height:150px}.purchase-summary{grid-template-columns:1fr}.purchase-summary div{flex-direction:column}}
</style>
