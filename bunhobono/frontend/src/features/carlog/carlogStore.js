import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { deleteCarLog, getCarLogs } from "./carlogApi";
import { toCarLogView } from "./carlogFormat";

export const useCarlogStore = defineStore("carlog", () => {

    const carLogs = ref([]);

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
        const result = confirm("카로그를 삭제하시겠습니까?");

        if (!result) {
            return;
        }

        const response = await deleteCarLog(carLogNo);

        if (response.data === 1) {
            carLogs.value = carLogs.value.filter((log) => {
                return Number(log.carLogNo ?? log.car_log_no) !== Number(carLogNo);
            });

            alert("카로그 삭제 완료");
        } else {
            alert("카로그 삭제 실패");
        }
    };

    return {
        carLogs,
        search,

        totalCount,
        parkingCount,
        outCount,
        visitCount,

        loadCarLogs,
        resetSearch,
        remove,
    };
});