import AdminBillingDetailView from "@/features/billing/AdminBillingDetailView.vue";
import AdminBillingView from "@/features/billing/AdminBillingView.vue";

export default [
    {
        path: "billing",
        name: "AdminBillingList",
        component: AdminBillingView,
    },
    {
        path: "billing/:billNo/detail",
        name: "AdminBillingDetail",
        component: AdminBillingDetailView,
    },
];