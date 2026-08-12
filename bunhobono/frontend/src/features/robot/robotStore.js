import { defineStore } from "pinia";
import { ref } from "vue";

import {
    completeRobotMaintenance,
    getRobotDetail,
    getRobotList,
    getRobotLogs
} from "./robotApi";

export const useRobotStore = defineStore("robot", () => {

    const list = ref([]);
    const detail = ref(null);
    const logs = ref([]);

    const loading = ref(false);
    const errorMessage = ref("");
    const logErrorMessage = ref("");

    // 전체 로봇 조회
    const loadList = async () => {
        loading.value = true;
        errorMessage.value = "";

        try {
            const response = await getRobotList();
            list.value = response.data ?? [];
        } catch (error) {
            console.error("로봇 목록 조회 실패", error);
            errorMessage.value =
                "로봇 목록을 불러오지 못했습니다.";
        } finally {
            loading.value = false;
        }
    };

    // 로봇 상세 조회
    const loadDetail = async (robotNo) => {
        errorMessage.value = "";

        try {
            const response =
                await getRobotDetail(robotNo);

            detail.value = response.data;
        } catch (error) {
            console.error("로봇 상세 조회 실패", error);
            detail.value = null;
            errorMessage.value =
                "로봇 정보를 불러오지 못했습니다.";
        }
    };

    // 선택한 로봇의 원시 상태 로그 조회
    const loadLogs = async (robotNo) => {
        logErrorMessage.value = "";

        try {
            const response =
                await getRobotLogs(robotNo);

            logs.value = response.data ?? [];
        } catch (error) {
            console.error("로봇 로그 조회 실패", error);
            logs.value = [];
            logErrorMessage.value =
                "원시 상태 로그를 불러오지 못했습니다.";
        }
    };

    // 로봇 상세와 원시 로그 동시 조회
    const loadDetailData = async (robotNo) => {
        loading.value = true;

        try {
            await Promise.all([
                loadDetail(robotNo),
                loadLogs(robotNo)
            ]);
        } finally {
            loading.value = false;
        }
    };

    // 로봇 점검 완료
    const completeMaintenance = async (robotNo) => {
        const response =
            await completeRobotMaintenance(robotNo);

        if (response.data !== 1) {
            return false;
        }

        await loadDetail(robotNo);
        return true;
    };

    // 상세 화면 데이터 초기화
    const clearDetail = () => {
        detail.value = null;
        logs.value = [];
        errorMessage.value = "";
        logErrorMessage.value = "";
    };

    return {
        list,
        detail,
        logs,

        loading,
        errorMessage,
        logErrorMessage,

        loadList,
        loadDetail,
        loadLogs,
        loadDetailData,
        completeMaintenance,
        clearDetail
    };
});