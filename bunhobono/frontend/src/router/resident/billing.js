import ResidentBillPay from "@/features/billing/ResidentBillPay.vue";
import ResidentBillPayResult from "@/features/billing/ResidentBillPayResult.vue";

export default [
  {
    path: "billing/:billNo",
    name: "ResidentBillPay",
    component: ResidentBillPay,
  },
  {
    path: "billing/:billNo/success",
    name: "ResidentBillPaySuccess",
    component: ResidentBillPayResult,
  },
  {
    path: "billing/:billNo/fail",
    name: "ResidentBillPayFail",
    component: ResidentBillPayResult,
  },
];
