<template>
  <section class="purchase-result-page">
    <article>
      <h2>{{ processing ? "결제 승인 중" : success ? "충전 완료" : "결제 실패" }}</h2>
      <p>{{ message }}</p>
      <button v-if="!processing" type="button" @click="goVehicles">차량 목록으로 돌아가기</button>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { confirmMemPurchasePayment } from "./memPurchaseApi";

const route = useRoute();
const router = useRouter();
const processing = ref(true);
const success = ref(false);
const errorMessage = ref("");
const message = computed(() => processing.value
  ? "결제 정보를 확인하고 있습니다."
  : success.value ? "구매한 방문차량 이용 시간이 추가되었습니다." : errorMessage.value);

onMounted(async () => {
  if (route.name === "MemPurchaseFail") {
    errorMessage.value = String(route.query.message || "결제가 취소되었거나 실패했습니다.");
    processing.value = false;
    return;
  }
  const paymentKey = String(route.query.paymentKey || "");
  const orderId = String(route.query.orderId || "");
  const amount = Number(route.query.amount);
  if (!paymentKey || !orderId || !Number.isFinite(amount) || amount <= 0) {
    errorMessage.value = "결제 승인 정보를 확인할 수 없습니다.";
    processing.value = false;
    return;
  }
  try {
    await confirmMemPurchasePayment(paymentKey, orderId, amount);
    success.value = true;
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "결제를 승인하지 못했습니다.";
  } finally {
    processing.value = false;
  }
});

const goVehicles = () => router.replace({ path: "/resident/vehicles", query: { mode: "list" } });
</script>

<style scoped>
.purchase-result-page { min-height: 520px; display: grid; place-items: center; color: #111; }
.purchase-result-page article { width: min(520px, calc(100% - 32px)); padding: 54px 32px; border: 1px solid #d5dde4; border-radius: 14px; background: #fff; text-align: center; }
.purchase-result-page h2 { margin: 0 0 18px; }.purchase-result-page p { margin: 0 0 28px; }
.purchase-result-page button { min-height: 44px; padding: 10px 22px; border: 0; border-radius: 8px; color: #fff; background: #23a6d5; cursor: pointer; font-weight: 800; }
</style>
