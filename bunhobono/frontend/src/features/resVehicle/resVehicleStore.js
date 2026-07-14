import { defineStore } from "pinia";
import { computed, ref } from "vue";

import {
    getResVehicleMemberInfo,
    getResVehicleList,
    createResVehicle
} from "./resVehicleApi";

import { toVehicleView } from "../vehicle/vehicleFormat";

export const useResVehicleStore = defineStore("resVehicle", () => {

    const member = ref({});
    const vehicleList = ref([]);
    const vehicle = ref({});

    // 로그인한 입주민 정보
    const loadMyInfo = async () => {
        const res = await getResVehicleMemberInfo();
        member.value = res.data;
    };

    // 입주민 본인 차량 목록
    // 서버가 토큰 loginId 기준으로 본인 차량만 반환한다.
    const loadVehicleList = async () => {
        const res = await getResVehicleList();
        vehicleList.value = res.data.map(toVehicleView);
    };

    // 본인 normal 차량
    const normalVehicles = computed(() => {
        return vehicleList.value.filter((item) => {
            return item.vehicleType === "normal";
        });
    });

    // 본인이 신청한 visit 차량
    // 최근 차량이 위로 오도록 백에서 DESC 정렬되지만,
    // 혹시 몰라 프론트에서도 한 번 더 정렬한다.
    const visitVehicles = computed(() => {
        return vehicleList.value
            .filter((item) => {
                return item.vehicleType === "visit";
            })
            .sort((a, b) => {
                return Number(b.vehicleCarNo) - Number(a.vehicleCarNo);
            });
    });

    // 유효한 방문차량 존재 여부
    // WAITING 또는 APPROVED이고 아직 남은기간이 있는 차량이 있으면 신청 버튼을 막는 데 사용한다.
    // 최종 제한은 백에서도 한 번 더 한다.
    const hasActiveVisitVehicle = computed(() => {
        return visitVehicles.value.some((item) => {
            return item.vehicleStatus === "WAITING" || item.vehicleStatus === "APPROVED";
        });
    });

    // 입주민 방문차량 등록 신청
    const addVisitVehicle = async (data) => {
        await createResVehicle(data);
        await loadVehicleList();
    };

    return {
        member,
        vehicleList,
        vehicle,

        normalVehicles,
        visitVehicles,
        hasActiveVisitVehicle,

        loadMyInfo,
        loadVehicleList,
        addVisitVehicle
    };
});