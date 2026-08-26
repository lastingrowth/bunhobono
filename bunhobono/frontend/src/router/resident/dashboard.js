import { defineComponent, h, onBeforeUnmount, onMounted, ref } from "vue";
import ResidentDashboardView from "@/views/resident/ResidentDashboardView.vue";
import ResidentDashboardMobileView from "@/views/resident/mobile/ResidentDashboardMobileView.vue";
import ResidentExitRequestView from "@/views/resident/ResidentExitRequestView.vue";
import { isMobileDevice } from "@/shared/responsive/mobileDevice";

// 화면 너비에 따라 PC 또는 모바일 입주민 대시보드를 표시한다.
const ResidentDashboard = defineComponent({
    setup() {
        const isMobile = ref(false);

        const updateScreen = () => {
            isMobile.value = isMobileDevice();
        };

        onMounted(() => {
            updateScreen();
            window.addEventListener("orientationchange", updateScreen);
            window.addEventListener("resize", updateScreen);
        });

        onBeforeUnmount(() => {
            window.removeEventListener("orientationchange", updateScreen);
            window.removeEventListener("resize", updateScreen);
        });

        return () => h(
            isMobile.value ? ResidentDashboardMobileView : ResidentDashboardView
        );
    }
});

export default [
    {
        path : 'dashboard',
        name : 'ResidentDashboard',
        component : ResidentDashboard
    },
    {
        path: 'exit-request',
        name: 'ResidentExitRequest',
        component: ResidentExitRequestView
    }
]
