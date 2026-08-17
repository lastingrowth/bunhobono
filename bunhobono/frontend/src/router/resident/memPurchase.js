import MemPurchaseView from "@/features/memPurchase/MemPurchaseView.vue";
import MemPurchaseResult from "@/features/memPurchase/MemPurchaseResult.vue";

export default [
  { path: "visit-credit", name: "MemPurchase", component: MemPurchaseView },
  { path: "visit-credit/success", name: "MemPurchaseSuccess", component: MemPurchaseResult },
  { path: "visit-credit/fail", name: "MemPurchaseFail", component: MemPurchaseResult }
];
