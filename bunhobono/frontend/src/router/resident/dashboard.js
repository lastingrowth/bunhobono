import ResidentDashboardView from "@/views/resident/ResidentDashboardView.vue";
import ResidentExitRequestView from "@/views/resident/ResidentExitRequestView.vue";

export default [
    {
        path : 'dashboard',
        name : 'ResidentDashboard',
        component : ResidentDashboardView
    },
    {
        path: 'exit-request',
        name: 'ResidentExitRequest',
        component: ResidentExitRequestView
    }
]
