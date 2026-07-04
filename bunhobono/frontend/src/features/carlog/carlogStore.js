import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { getCarLogs } from "./carlogApi";

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
            carLogs.value = res.data;
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

    return {
        carLogs,
        search,

        totalCount,
        parkingCount,
        outCount,
        visitCount,

        loadCarLogs,
        resetSearch,
    };
});