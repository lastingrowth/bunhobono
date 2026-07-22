import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { deleteCarLog, getCarLogs } from "./carlogApi";
import { toCarLogView } from "./carlogFormat";

export const useCarlogStore = defineStore("carlog", () => {

    const carLogs = ref([]);
    const feedbackMessage = ref("");
    const feedbackType = ref("success");
    let feedbackTimer;

    const showFeedback = (message, type = "success") => {
        feedbackMessage.value = message;
        feedbackType.value = type;
        window.clearTimeout(feedbackTimer);
        feedbackTimer = window.setTimeout(() => {
            feedbackMessage.value = "";
        }, 2500);
    };

    const search = ref({
        gateNo: null,
        parkingNo: null,
        parkingState: "",
        carKind: "",
        carNo: "",
        sort: "latest",
    });

    const totalCount = computed(() => carLogs.value.length);

    const parkingCount = computed(() =>
        carLogs.value.filter(log => log.parkingState === "PARKING").length
    );

    const outCount = computed(() =>
        carLogs.value.filter(log => log.parkingState === "OUT").length
    );

    const visitCount = computed(() =>
        carLogs.value.filter(log => log.carKind === "VISIT").length
    );

    // 목록 조회
    const loadCarLogs = async () => {
        try {
            const res = await getCarLogs(search.value);
            carLogs.value = res.data.map(toCarLogView);
        } catch (e) {
            console.error("차량 목록 조회 실패", e);
        }
    };

    // 검색 조건 초기화
    const resetSearch = async () => {
        search.value = {
            gateNo: null,
            parkingNo: null,
            parkingState: "",
            carKind: "",
            carNo: "",
            sort: "latest",
        };

        await loadCarLogs();
    };

    // 카로그 삭제
    const remove = async (carLogNo) => {
        try {
            const response = await deleteCarLog(carLogNo);

            if (response.data !== 1) {
                showFeedback("입출차 기록 삭제에 실패했습니다.", "error");
                return false;
            }

            carLogs.value = carLogs.value.filter((log) => {
                return Number(log.carLogNo ?? log.car_log_no) !== Number(carLogNo);
            });

            showFeedback("입출차 기록이 삭제되었습니다.");
            return true;
        } catch (error) {
            console.error("카로그 삭제 실패", error);
            showFeedback("입출차 기록 삭제에 실패했습니다.", "error");
            return false;
        }
    };

    return {
        carLogs,
        search,
        feedbackMessage,
        feedbackType,

        totalCount,
        parkingCount,
        outCount,
        visitCount,

        loadCarLogs,
        resetSearch,
        remove,
    };
});
